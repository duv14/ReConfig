/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import worker from "../src/worker.js";

const schema = `
CREATE TABLE users(uuid TEXT PRIMARY KEY,name TEXT NOT NULL COLLATE NOCASE UNIQUE,token_hash TEXT NOT NULL,created_at INTEGER NOT NULL);
CREATE TABLE requests(sender TEXT NOT NULL,recipient TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(sender,recipient));
CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,recipient TEXT NOT NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL,delivered INTEGER NOT NULL DEFAULT 0);
CREATE TABLE sessions(token_hash TEXT PRIMARY KEY,user_uuid TEXT NOT NULL,updated_at INTEGER NOT NULL);
CREATE TABLE friendships(user_low TEXT NOT NULL,user_high TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(user_low,user_high));
CREATE TABLE presence(user_uuid TEXT PRIMARY KEY,server_hash TEXT,last_seen INTEGER NOT NULL);
CREATE TABLE messages_v2(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,recipient TEXT NOT NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL,acknowledged INTEGER NOT NULL DEFAULT 0);
CREATE TABLE invitations(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,recipient TEXT NOT NULL,server_address TEXT NOT NULL,created_at INTEGER NOT NULL,acknowledged INTEGER NOT NULL DEFAULT 0);
`;
class D1 {
  constructor() { this.db = new DatabaseSync(":memory:"); this.db.exec(schema); }
  prepare(sql) { const statement=this.db.prepare(sql); let args=[]; return {
    bind(...values){args=values;return this}, first(){return statement.get(...args)??null},
    all(){return {results:statement.all(...args)}}, run(){return statement.run(...args)},
  }; }
}
const uuids={alice:"a".repeat(32),bob:"b".repeat(32)}, tokens={alice:"1".repeat(32),bob:"2".repeat(32)};
const call=async(db,user,path,method="GET",body,extraEnv={})=>{const headers={"content-type":"application/json"};if(user)headers.authorization=`Bearer ${tokens[user]}`;const response=await worker.fetch(new Request(`https://example.test${path}`,{method,headers,body:body===undefined?undefined:JSON.stringify(body)}),{DB:db,...extraEnv});return {status:response.status,body:await response.json()}};
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

test("friends can send messages while recipient is offline",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/messages","POST",{to:"bob",body:"offline hello"})).status,200);assert.equal((await call(db,"bob",`/v2/messages?with=${uuids.alice}&after=0`)).body.messages[0].body,"offline hello")});
test("server invitations are delivered to accepted friends",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});assert.equal((await call(db,"alice","/v2/invitations","POST",{to:"bob",serverAddress:"play.example.net"})).status,200);assert.equal((await call(db,"bob","/v2/events")).body.invitations[0].serverAddress,"play.example.net")});
test("sender can cancel an outgoing friend request",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});assert.equal((await call(db,"alice","/v2/friends/cancel","POST",{uuid:uuids.bob})).status,200);assert.deepEqual((await call(db,"alice","/v2/friends")).body.friends,[]);assert.deepEqual((await call(db,"bob","/v2/friends")).body.friends,[])});
test("only message sender can delete a shared message",async()=>{const db=new D1();await enroll(db,"alice");await enroll(db,"bob");await call(db,"alice","/v2/friends/request","POST",{name:"bob"});await call(db,"bob","/v2/friends/accept","POST",{uuid:uuids.alice});const sent=await call(db,"alice","/v2/messages","POST",{to:"bob",body:"delete me"});assert.equal((await call(db,"bob",`/v2/messages/${sent.body.id}`,"DELETE")).status,403);assert.equal((await call(db,"alice",`/v2/messages/${sent.body.id}`,"DELETE")).status,200);assert.deepEqual((await call(db,"bob",`/v2/messages?with=${uuids.alice}&after=0`)).body.messages,[])});
