/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import worker from "../src/worker.js";

import { readFileSync, readdirSync } from "node:fs";
const migrations = readdirSync(new URL("../migrations/",import.meta.url)).sort().map(name=>readFileSync(new URL(`../migrations/${name}`,import.meta.url),"utf8"));
class D1 {
  constructor(keepRecommendations=false) { this.db = new DatabaseSync(":memory:"); this.db.exec("PRAGMA foreign_keys=ON"); for(const sql of migrations)this.db.exec(sql); if(!keepRecommendations)this.db.exec("DELETE FROM public_servers"); }
  prepare(sql) { const statement=this.db.prepare(sql); let args=[]; return {
    bind(...values){args=values;return this}, first(){return statement.get(...args)??null},
    all(){return {results:statement.all(...args)}}, run(){return statement.run(...args)},
  }; }
}
const uuids={alice:"a".repeat(32),bob:"b".repeat(32)}, tokens={alice:"1".repeat(32),bob:"2".repeat(32)};
const call=async(db,user,path,method="GET",body,extraEnv={})=>{const headers={"content-type":"application/json"};if(user)headers.authorization=`Bearer ${tokens[user]}`;const response=await worker.fetch(new Request(`https://example.test${path}`,{method,headers,body:body===undefined?undefined:JSON.stringify(path.endsWith("/enroll")?{serverId:"a".repeat(40),...body}:body)}),{DB:db,SESSION_HTTP_FETCH:async()=>new Response(JSON.stringify({id:body?.uuid,name:body?.name})),...extraEnv});return {status:response.status,body:await response.json()}};
const enroll=(db,user)=>call(db,null,"/v2/enroll","POST",{uuid:uuids[user],name:user,token:tokens[user]});

test("both conversation views preserve sender and recipient for outgoing classification",async()=>{
  const db=new D1(); await enroll(db,"alice"); await enroll(db,"bob");
  await call(db,"alice","/v2/friends/request","POST",{name:"bob"});
  await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});
  await call(db,"alice","/v2/messages","POST",{to:"bob",body:"from alice"});
  await call(db,"bob","/v2/messages","POST",{to:"alice",body:"from bob"});
  for(const [viewer,peer] of [["alice","bob"],["bob","alice"]]) {
    const result=await call(db,viewer,`/v2/messages?with=${uuids[peer]}&after=0`);
    assert.equal(result.status,200);
    assert.deepEqual(result.body.messages.map(m=>[m.sender,m.recipient]),[[uuids.alice,uuids.bob],[uuids.bob,uuids.alice]]);
    assert.equal(result.body.messages.find(m=>m.body===`from ${viewer}`).recipient,uuids[peer]);
    assert.equal(result.body.messages.find(m=>m.body===`from ${peer}`).sender,uuids[peer]);
  }
});

test("health endpoint identifies service",async()=>{const result=await call(new D1(),null,"/v1/health");assert.equal(result.status,200);assert.equal(result.body.service,"ReConfig")});
test("enrollment reclaims a username left by a stale uuid",async()=>{const db=new D1();await call(db,null,"/v2/enroll","POST",{uuid:"c".repeat(32),name:"duv14",token:"3".repeat(32)});const result=await call(db,null,"/v2/enroll","POST",{uuid:"d".repeat(32),name:"duv14",token:"4".repeat(32)});assert.equal(result.status,200);assert.equal(result.body.ok,true);assert.equal(db.db.prepare("SELECT uuid FROM users WHERE name=? COLLATE NOCASE").get("duv14").uuid,"d".repeat(32))});
test("friend request requires explicit acceptance",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");assert.equal((await call(db,"alice","/v2/friends/request","POST",{name:"bob"})).status,200);assert.equal((await call(db,"bob","/v2/friends")).body.friends[0].state,"incoming");assert.equal((await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice})).status,200);assert.equal((await call(db,"alice","/v2/friends")).body.friends[0].state,"friends")});
test("reciprocal requests become friends automatically",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});const result=await call(db,"bob","/v2/friends/request","POST",{name:"alice"});assert.equal(result.body.state,"friends");assert.equal((await call(db,"alice","/v2/friends")).body.friends[0].state,"friends");assert.equal((await call(db,"bob","/v2/friends")).body.friends[0].state,"friends")});
test("friend request creates a claimable contact for a Minecraft player without ReConfig",async()=>{const db=new D1();await enroll(db,"alice");const profileLookup=async name=>name.toLowerCase()==="charlie"?{id:"c".repeat(32),name:"Charlie"}:null;const request=await call(db,"alice","/v2/friends/request","POST",{name:"charlie"},{PROFILE_LOOKUP:profileLookup});assert.equal(request.status,200);assert.equal(request.body.state,"outgoing");assert.equal(request.body.name,"Charlie");assert.match(db.db.prepare("SELECT token_hash FROM users WHERE uuid=?").get("c".repeat(32)).token_hash,/^external:/);const claim=await call(db,null,"/v2/enroll","POST",{uuid:"c".repeat(32),name:"Charlie",token:"3".repeat(32)});assert.equal(claim.status,200);assert.doesNotMatch(db.db.prepare("SELECT token_hash FROM users WHERE uuid=?").get("c".repeat(32)).token_hash,/^external:/);assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:"c".repeat(32),name:"Charlie",token:"3".repeat(32)})).status,200)});
test("profile lookup falls back when Minecraft rejects Cloudflare with 403",async()=>{const db=new D1();await enroll(db,"alice");const urls=[];const profileFetch=async url=>{urls.push(url);if(url.includes("playerdb.co"))return new Response(JSON.stringify({success:true,data:{player:{id:"d".repeat(32),username:"Delta"}}}),{status:200});return new Response("forbidden",{status:403})};const request=await call(db,"alice","/v2/friends/request","POST",{name:"Delta"},{PROFILE_HTTP_FETCH:profileFetch});assert.equal(request.status,200);assert.equal(request.body.name,"Delta");assert.equal(request.body.uuid,"d".repeat(32));assert.ok(urls.some(url=>url.includes("playerdb.co")))});
test("friends can message across different servers",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});await call(db,"alice","/v2/presence","POST",{serverHash:"server-one"});await call(db,"bob","/v2/presence","POST",{serverHash:"server-two"});assert.equal((await call(db,"alice","/v2/messages","POST",{to:"bob",body:"cross-server hello"})).status,200);assert.equal((await call(db,"bob",`/v2/messages?with=${uuids.alice}&after=0`)).body.messages[0].body,"cross-server hello")});
test("custom emoji tokens survive messaging and replies",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});const emoji=await call(db,"alice","/v2/messages","POST",{to:"bob",body:"reconfig-emoji:rose"});assert.equal(emoji.status,200);const reply=await call(db,"bob","/v2/messages","POST",{to:"alice",body:"reconfig-emoji:pray",replyTo:emoji.body.id});assert.equal(reply.status,200);const messages=(await call(db,"alice",`/v2/messages?with=${uuids.bob}&after=0`)).body.messages;assert.equal(messages[0].body,"reconfig-emoji:rose");assert.equal(messages[1].body,"reconfig-emoji:pray");assert.equal(messages[1].reply_body,"reconfig-emoji:rose")});

test("friends can send messages while recipient is offline",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/messages","POST",{to:"bob",body:"offline hello"})).status,200);assert.equal((await call(db,"bob",`/v2/messages?with=${uuids.alice}&after=0`)).body.messages[0].body,"offline hello")});
test("server invitations are delivered to accepted friends",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"})).status,200);assert.equal((await call(db,"bob","/v2/events")).body.invitations[0].serverAddress,"play.example.net")});
test("server invitations remain pending until explicitly acknowledged",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"});const pending=(await call(db,"bob","/v2/events")).body.invitations;assert.equal(pending.length,1);assert.equal((await call(db,"bob","/v2/events")).body.invitations.length,1);await call(db,"bob","/v2/invitations/ack","POST",{ids:[pending[0].id]});assert.equal((await call(db,"bob","/v2/events")).body.invitations.length,0)});
test("server invitations have a server-enforced cooldown",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"})).status,200);const repeated=await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"});assert.equal(repeated.status,429);assert.ok(repeated.body.retryAfterSeconds>0)});
test("message replies are limited to the same conversation",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});const first=await call(db,"alice","/v2/messages","POST",{to:"bob",body:"original"});const reply=await call(db,"bob","/v2/messages","POST",{to:"alice",body:"reply",replyTo:first.body.id});assert.equal(reply.status,200);const messages=(await call(db,"alice",`/v2/messages?with=${uuids.bob}&after=0`)).body.messages;assert.equal(messages[1].reply_to,first.body.id);assert.equal(messages[1].reply_body,"original");assert.equal(messages[1].reply_sender,uuids.alice)});
test("friends can remove each other",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/friends/remove","POST",{uuid:uuids.bob})).status,200);assert.deepEqual((await call(db,"alice","/v2/friends")).body.friends,[]);assert.deepEqual((await call(db,"bob","/v2/friends")).body.friends,[])});
test("sender can cancel an outgoing friend request",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});assert.equal((await call(db,"alice","/v2/friends/cancel","POST",{uuid:uuids.bob})).status,200);assert.deepEqual((await call(db,"alice","/v2/friends")).body.friends,[]);assert.deepEqual((await call(db,"bob","/v2/friends")).body.friends,[])});
test("only message sender can delete a shared message",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});const sent=await call(db,"alice","/v2/messages","POST",{to:"bob",body:"delete me"});assert.equal((await call(db,"bob",`/v2/messages/${sent.body.id}`,"DELETE")).status,403);assert.equal((await call(db,"alice",`/v2/messages/${sent.body.id}`,"DELETE")).status,200);assert.deepEqual((await call(db,"bob",`/v2/messages?with=${uuids.alice}&after=0`)).body.messages,[])});

test("background sync returns new incoming messages and invitations in one request",async()=>{
  const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
  await call(db,"alice","/v2/friends/request","POST",{name:"bob"});
  await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});
  await call(db,"alice","/v2/messages","POST",{to:"bob",body:"background hello"});
  await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"});
  const sync=await call(db,"bob","/v2/sync?after=0");
  assert.equal(sync.status,200);
  assert.equal(sync.body.messages[0].body,"background hello");
  assert.equal(sync.body.messages[0].sender_name,"alice");
  assert.equal(sync.body.invitations[0].serverAddress,"play.example.net");
  assert.equal((await call(db,"bob",`/v2/sync?after=${sync.body.cursor}`)).body.messages.length,0);
});

test("publication requires separate consent, validates fields, and hides private profiles",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 assert.equal((await call(db,"alice","/v2/profile/me")).body.profile.public,false);
 const profile={bio:"Hello",tag:"AB123",emoji:"rose",public:true};
 assert.equal((await call(db,"alice","/v2/profile/me","POST",profile)).status,400);
 for(const invalid of [{tag:"ABCDEF"},{tag:"A<>"},{emoji:"unknown"},{bio:"x".repeat(281)},{bio:"bad\u202e"}])assert.equal((await call(db,"alice","/v2/profile/me","POST",{...profile,publicationConsent:true,...invalid})).status,400);
 assert.equal((await call(db,"alice","/v2/profile/me","POST",{...profile,publicationConsent:true})).status,200);
 const result=await call(db,"bob","/v2/profiles?q=ali");assert.equal(result.body.profiles.length,1);assert.equal(result.body.profiles[0].tag,"AB123");assert.deepEqual(Object.keys(result.body.profiles[0]).sort(),["bio","emoji","name","tag","uuid"]);
 assert.equal((await call(db,"alice","/v2/profile/me","POST",{...profile,public:false})).status,200);
 assert.equal((await call(db,"bob",`/v2/profiles/${uuids.alice}`)).status,404);
 assert.equal((await call(db,"bob","/v2/profiles")).body.profiles.length,0);
});

test("directory contains only explicitly listed curated records, never presence or invites",async()=>{
 const db=new D1();await enroll(db,"alice");await call(db,"alice","/v2/presence","POST",{serverHash:"private-server"});
 assert.deepEqual((await call(db,"alice","/v2/servers")).body.servers,[]);
 db.db.exec("INSERT INTO public_servers(id,name,address,listed) VALUES('visible','Example','play.example.org',1),('hidden','Hidden','private.example.org',0)");
 const result=await call(db,"alice","/v2/servers");assert.equal(result.body.servers.length,1);assert.equal(result.body.servers[0].address,"play.example.org");
 assert.deepEqual((await call(db,"alice","/v2/servers?q=absent")).body.servers,[]);
});

test("request events and deletions sync incrementally only to participants",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 await call(db,"alice","/v2/friends/request","POST",{name:"bob"});
 const requested=await call(db,"bob","/v2/sync");assert.equal(requested.body.requests[0].name,"alice");
 assert.deepEqual((await call(db,"bob",`/v2/sync?afterRequest=${requested.body.requestCursor}`)).body.requests,[]);
 await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});
 const sent=await call(db,"alice","/v2/messages","POST",{to:"bob",body:"delete me"});
 await call(db,"alice",`/v2/messages/${sent.body.id}`,"DELETE");
 for(const user of ["alice","bob"]){const sync=await call(db,user,"/v2/sync");assert.equal(sync.body.deleted[0].message_id,sent.body.id);assert.deepEqual((await call(db,user,`/v2/sync?afterDeletion=${sync.body.deletionCursor}`)).body.deleted,[]);}
});

test("presence stays online between 60 second heartbeats and eventually expires",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});
 db.db.prepare("INSERT INTO presence(user_uuid,last_seen) VALUES(?,?)").run(uuids.bob,Date.now()-65000);
 assert.equal((await call(db,"alice","/v2/friends")).body.friends[0].online,1);
 db.db.prepare("UPDATE presence SET last_seen=?").run(Date.now()-160000);
 assert.equal((await call(db,"alice","/v2/friends")).body.friends[0].online,0);
});

test("new enrollment and token rotation require verified Minecraft ownership",async()=>{
 const db=new D1(),denied={SESSION_HTTP_FETCH:async()=>new Response(null,{status:204})};
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:tokens.alice},denied)).status,403);
 await enroll(db,"alice");
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:tokens.alice},denied)).status,200);
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"bob",token:tokens.alice},denied)).status,403);
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:"x".repeat(32)},denied)).status,403);
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:"x".repeat(32)})).status,200);
 assert.equal((await call(db,"alice","/v2/friends")).status,200);
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:tokens.alice},denied)).status,200);
});

test("a bearer token cannot be reassigned to a different identity",async()=>{
 const db=new D1();await enroll(db,"alice");
 assert.equal((await call(db,null,"/v2/enroll","POST",{uuid:uuids.bob,name:"bob",token:tokens.alice})).status,409);
 assert.equal((await call(db,"alice","/v2/profile/me")).body.profile.uuid,uuids.alice);
});

test("write rate limits survive requests and return friendly retry time",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 for(let i=0;i<10;i++)assert.equal((await call(db,"alice","/v2/friends/request","POST",{name:"bob"})).status,200);
 const blocked=await call(db,"alice","/v2/friends/request","POST",{name:"bob"});assert.equal(blocked.status,429);assert.equal(blocked.body.retryAfterSeconds,60);
});

test("new migration is repeatable and preserves existing accounts and messages",()=>{
 const db=new DatabaseSync(":memory:");for(const sql of migrations.slice(0,-1))db.exec(sql);
 db.exec("INSERT INTO users VALUES('existing','Existing','hash',1);INSERT INTO messages_v2(sender,recipient,body,created_at) VALUES('existing','peer','keep me',1)");
 db.exec(migrations.at(-1));db.exec(migrations.at(-1));
 assert.equal(db.prepare("SELECT body FROM messages_v2").get().body,"keep me");assert.equal(db.prepare("SELECT name FROM users").get().name,"Existing");
 const plan=db.prepare("EXPLAIN QUERY PLAN SELECT id FROM messages_v2 WHERE recipient=? AND id>? ORDER BY id LIMIT 100").all("peer",0);assert.ok(plan.some(row=>row.detail.includes("messages_v2_recipient_id_idx")));
 db.close();
});

test("bootstrap suppresses historic notification pages and returns isolated highwater cursors",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 db.db.exec(`INSERT INTO messages_v2(sender,recipient,body,created_at) VALUES('${uuids.alice}','${uuids.bob}','historic',1);INSERT INTO requests VALUES('${uuids.alice}','${uuids.bob}',1);INSERT INTO invitations(sender,recipient,server_address,created_at) VALUES('${uuids.alice}','${uuids.bob}','old.example.org',1)`);
 const boot=(await call(db,"bob","/v2/sync?bootstrap=1")).body;
 assert.equal(boot.cursor,1);assert.equal(boot.requestCursor,1);assert.equal(boot.invitationCursor,1);assert.deepEqual(boot.messages,[]);assert.deepEqual(boot.requests,[]);assert.deepEqual(boot.invitations,[]);
 assert.equal((await call(db,"alice","/v2/sync?bootstrap=1")).body.cursor,0);
 db.db.exec(`INSERT INTO messages_v2(sender,recipient,body,created_at) VALUES('${uuids.alice}','${uuids.bob}','new',2);INSERT INTO invitations(sender,recipient,server_address,created_at) VALUES('${uuids.alice}','${uuids.bob}','new.example.org',2)`);
 const sync=(await call(db,"bob",`/v2/sync?after=${boot.cursor}&afterRequest=${boot.requestCursor}&afterInvitation=${boot.invitationCursor}`)).body;
 assert.deepEqual(sync.messages.map(m=>m.body),["new"]);assert.deepEqual(sync.requests,[]);assert.deepEqual(sync.invitations.map(i=>i.serverAddress),["new.example.org"]);
 assert.equal(db.db.prepare("SELECT count(*) n FROM invitations WHERE acknowledged=0").get().n,2);
 await call(db,"alice","/v2/invitations/ack","POST",{ids:[2]});assert.equal(db.db.prepare("SELECT acknowledged FROM invitations WHERE id=2").get().acknowledged,0);
 await call(db,"bob","/v2/invitations/ack","POST",{ids:[2]});assert.equal(db.db.prepare("SELECT acknowledged FROM invitations WHERE id=2").get().acknowledged,1);
 assert.equal((await call(db,"bob","/v2/invitations/ack","POST",{ids:[-1]})).status,400);
});

test("conversation history pages backwards without losing or duplicating messages",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 const insert=db.db.prepare("INSERT INTO messages_v2(sender,recipient,body,created_at) VALUES(?,?,?,?)");
 for(let i=1;i<=451;i++)insert.run(uuids.alice,uuids.bob,`message ${i}`,i);
 const first=(await call(db,"bob","/v2/messages?with=alice&after=0")).body;
 assert.equal(first.messages.length,200);assert.equal(first.messages[0].id,252);assert.equal(first.hasMore,true);
 const middle=(await call(db,"bob",`/v2/messages?with=alice&before=${first.beforeCursor}`)).body;
 assert.equal(middle.messages[0].id,52);assert.equal(middle.hasMore,true);
 const last=(await call(db,"bob",`/v2/messages?with=alice&before=${middle.beforeCursor}`)).body;
 assert.equal(last.messages.length,51);assert.equal(last.hasMore,false);
 assert.deepEqual([...last.messages,...middle.messages,...first.messages].map(m=>m.id),Array.from({length:451},(_,i)=>i+1));
 const forward=(await call(db,"bob","/v2/messages?with=alice&after=200")).body;assert.equal(forward.cursor,400);assert.equal(forward.hasMore,true);
 assert.equal((await call(db,"bob","/v2/messages?with=alice&after=2&before=3")).status,400);
 assert.equal((await call(db,"bob","/v2/messages?with=alice&before=garbage")).status,400);
});

test("negative Minecraft lookups are cached instead of repeatedly contacting providers",async()=>{
 const db=new D1();await enroll(db,"alice");let attempts=0;
 const env={PROFILE_LOOKUP:async()=>{attempts++;return null;}};
 for(let i=0;i<3;i++)assert.equal((await call(db,"alice","/v2/friends/request","POST",{name:"missing"},env)).status,404);
 assert.equal(attempts,1);
});

test("read burst budget returns a friendly retry response",async()=>{
 const db=new D1();await enroll(db,"alice");
 for(let i=0;i<120;i++)assert.equal((await call(db,"alice","/v2/profile/me")).status,200);
 const response=await call(db,"alice","/v2/profile/me");assert.equal(response.status,429);assert.equal(response.body.retryAfterSeconds,60);
});

test("UUID lookups cannot resolve another account with a matching truncated name",async()=>{
 const db=new D1();await enroll(db,"alice");await enroll(db,"bob");
 db.db.prepare("INSERT INTO users(uuid,name,token_hash,created_at) VALUES(?,?,?,?)").run("c".repeat(32),"d".repeat(16),"external:contact",1);
 const result=await call(db,"alice",`/v2/messages?with=${"d".repeat(32)}`);assert.equal(result.status,404);
 assert.equal((await call(db,"alice","/v2/friends/request","POST",{name:"d".repeat(17)})).status,404);
});
test("verification upstream errors are availability failures, not rejected ownership",async()=>{
 const db=new D1();
 for(const status of [403,429,503]){
  const result=await call(db,null,"/v2/enroll","POST",{uuid:uuids.alice,name:"alice",token:tokens.alice},{SESSION_HTTP_FETCH:async()=>new Response(null,{status})});
  assert.equal(result.status,503);
  assert.equal(db.db.prepare("SELECT count(*) n FROM sessions").get().n,0);
 }
});
test('recommended public servers are populated and searchable by name, address and mode',async()=>{
 const db=new D1(true);await enroll(db,'alice');
 const recommended=await call(db,'alice','/v2/servers');
 assert.ok(recommended.body.servers.length>=5);
 assert.equal(recommended.body.servers[0].id,'donutsmp');
 for(const q of [' donut smp ','donutsmp.net','bed wars','RPG','play.cubecraft.net']){
  const result=await call(db,'alice','/v2/servers?q='+encodeURIComponent(q));
  assert.equal(result.status,200);assert.ok(result.body.servers.length>0,q);
 }
 const absent=await call(db,'alice','/v2/servers?q=definitely-no-such-server');assert.deepEqual(absent.body.servers,[]);
 db.db.prepare('UPDATE public_servers SET listed=0 WHERE id=?').run('donutsmp');
 assert.deepEqual((await call(db,'alice','/v2/servers?q=donutsmp.net')).body.servers,[]);
});
