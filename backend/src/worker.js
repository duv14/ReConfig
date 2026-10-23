/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
const json = (value, status=200) => new Response(JSON.stringify(value), {status, headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","access-control-allow-origin":"*",...(status===429?{"retry-after":String(value.retryAfterSeconds||60)}:{})}});
const cleanUuid = value => String(value || "").replaceAll("-", "").toLowerCase();
const cleanName = value => String(value || "").trim().slice(0, 16);
const tokenHash = async token => [...new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token)))].map(x=>x.toString(16).padStart(2,"0")).join("");

async function auth(request, env) {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return null;
  return env.DB.prepare("SELECT u.uuid,u.name FROM sessions s JOIN users u ON u.uuid=s.user_uuid WHERE s.token_hash=?").bind(await tokenHash(token)).first();
}

const pair = (a,b) => a < b ? [a,b] : [b,a];
const freshAfter = () => Date.now() - 150000;
const findUser = async (env,value) => {
  const raw=String(value||"").trim(),uuid=cleanUuid(raw);
  if(/^[a-f0-9]{32}$/.test(uuid))return env.DB.prepare("SELECT uuid,name FROM users WHERE uuid=?").bind(uuid).first();
  if(!/^[A-Za-z0-9_]{1,16}$/.test(raw))return null;
  return env.DB.prepare("SELECT uuid,name FROM users WHERE name=? COLLATE NOCASE").bind(raw).first();
};
const isFriend = async (env,a,b) => { const [low,high]=pair(a,b); return !!(await env.DB.prepare("SELECT 1 ok FROM friendships WHERE user_low=? AND user_high=?").bind(low,high).first()); };
const profileCaches = new WeakMap();
const lookupMinecraftProfileUncached = async (env,value) => {
  const name=String(value||"").trim(); if(!/^[A-Za-z0-9_]{1,16}$/.test(name))return null;
  if(typeof env.PROFILE_LOOKUP==="function")return env.PROFILE_LOOKUP(name);
  const httpFetch=typeof env.PROFILE_HTTP_FETCH==="function"?env.PROFILE_HTTP_FETCH:fetch;
  const encoded=encodeURIComponent(name),providers=[
    {url:`https://api.minecraftservices.com/minecraft/profile/lookup/name/${encoded}`,parse:data=>({id:data?.id,name:data?.name})},
    {url:`https://api.mojang.com/users/profiles/minecraft/${encoded}`,parse:data=>({id:data?.id,name:data?.name})},
    {url:`https://playerdb.co/api/player/minecraft/${encoded}`,parse:data=>({id:data?.data?.player?.id,name:data?.data?.player?.username})},
    {url:`https://api.ashcon.app/mojang/v2/user/${encoded}`,parse:data=>({id:data?.uuid,name:data?.username})},
  ];
  let unavailable=false;
  for(const provider of providers){
    try{
      const response=await httpFetch(provider.url,{headers:{accept:"application/json","user-agent":"ReConfig/1.0"},signal:AbortSignal.timeout(5000)});
      if(response.status===404||response.status===204)continue;
      if(!response.ok){unavailable=true;continue;}
      const profile=provider.parse(await response.json());
      if(/^[a-fA-F0-9-]{32,36}$/.test(String(profile.id||""))&&/^\w{1,16}$/.test(String(profile.name||"")))return profile;
    }catch{unavailable=true;}
  }
  if(unavailable)throw new Error("Minecraft profile lookup is temporarily unavailable");
  return null;
};
// Best-effort bounded isolate cache; existing contact records remain the durable cache.
const lookupMinecraftProfile = async (env,value) => {
  const key=String(value||"").trim().toLowerCase();
  if(!/^[a-z0-9_]{1,16}$/.test(key))return null;
  let cache=profileCaches.get(env.DB);if(!cache){cache=new Map();profileCaches.set(env.DB,cache);}
  const hit=cache.get(key);if(hit&&hit.expires>Date.now()){if(hit.error)throw new Error("Minecraft profile lookup is temporarily unavailable");return hit.profile;}
  if(cache.size>=256)cache.delete(cache.keys().next().value);
  try {const profile=await lookupMinecraftProfileUncached(env,value);cache.set(key,{profile,expires:Date.now()+(profile?600000:30000)});return profile;}
  catch(error){cache.set(key,{error:true,expires:Date.now()+30000});throw error;}
};
const readBudgets = new WeakMap();
function readLimited(env,uuid) {
  let budgets=readBudgets.get(env.DB);if(!budgets){budgets=new Map();readBudgets.set(env.DB,budgets);}
  const window=Math.floor(Date.now()/60000),old=budgets.get(uuid);
  if(!old||old.window!==window){if(budgets.size>=1024)budgets.delete(budgets.keys().next().value);budgets.set(uuid,{window,count:1});return false;}
  return ++old.count>120;
}
const findOrCreateMinecraftUser = async (env,value) => {
  const existing=await findUser(env,value); if(existing)return existing;
  const profile=await lookupMinecraftProfile(env,value),uuid=cleanUuid(profile?.id),name=cleanName(profile?.name);
  if(!/^[a-f0-9]{32}$/.test(uuid)||!/^\w{1,16}$/.test(name))return null;
  await env.DB.prepare("INSERT INTO users(uuid,name,token_hash,created_at) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name").bind(uuid,name,`external:${uuid}`,Date.now()).run();
  return {uuid,name};
};

const emojis = new Set(["", "sob", "sob-blue", "rose", "pray", "kiss", "kiss-closed", "cute"]);
const safeText = value => typeof value === "string" && !/[\u0000-\u001f\u007f\u202a-\u202e\u2066-\u2069]/u.test(value);
const cursorValue = value => { const n=Number(value||0); return Number.isSafeInteger(n)&&n>=0?n:0; };
async function limited(env, uuid, action, maximum) {
  const window=Math.floor(Date.now()/60000);
  const row=await env.DB.prepare("INSERT INTO rate_limits(user_uuid,action,window,count) VALUES(?,?,?,1) ON CONFLICT(user_uuid,action) DO UPDATE SET window=excluded.window,count=CASE WHEN rate_limits.window=excluded.window THEN rate_limits.count+1 ELSE 1 END RETURNING count").bind(uuid,action,window).first();
  return row.count>maximum;
}
async function verifyOwnership(env,name,uuid,serverId) {
  if(!/^[a-f0-9]{40}$/.test(String(serverId||"")))return false;
  const httpFetch=env.SESSION_HTTP_FETCH||fetch;
  const result=await httpFetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${serverId}`,{signal:AbortSignal.timeout(8000)});
  if(result.status===204)return false;
  if(!result.ok)throw new Error(`Minecraft verification upstream HTTP ${result.status}`);
  const profile=await result.json();
  return cleanUuid(profile.id)===uuid && String(profile.name).toLowerCase()===name.toLowerCase();
}

export default { async fetch(request, env) {
  if (request.method === "OPTIONS") return new Response(null,{headers:{"access-control-allow-origin":"*","access-control-allow-headers":"authorization,content-type","access-control-allow-methods":"GET,POST,DELETE,OPTIONS"}});
  const url = new URL(request.url), path = url.pathname;
  try {
    if (path === "/v1/health") return json({ok:true,service:"ReConfig"});
    if ((path === "/v1/enroll" || path === "/v2/enroll") && request.method === "POST") {
      const b = await request.json(), uuid = cleanUuid(b.uuid), name = cleanName(b.name), token = String(b.token || "");
      if (!/^[a-f0-9]{32}$/.test(uuid) || !/^[A-Za-z0-9_]{1,16}$/.test(name) || token.length < 32 || token.length > 256) return json({error:"invalid enrollment"},400);
      const existing = await env.DB.prepare("SELECT token_hash,name FROM users WHERE uuid=?").bind(uuid).first();
      const hash = await tokenHash(token);
      const tokenOwner=await env.DB.prepare("SELECT user_uuid FROM sessions WHERE token_hash=?").bind(hash).first();
      const knownToken=tokenOwner?.user_uuid===uuid || existing?.token_hash===hash;
      if ((!existing || !knownToken || existing.name.toLowerCase() !== name.toLowerCase()) && !(await verifyOwnership(env,name,uuid,b.serverId))) return json({error:"Minecraft session ownership verification required"},403);
      if(tokenOwner && tokenOwner.user_uuid!==uuid)return json({error:"token already registered"},409);
      const nameOwner = await env.DB.prepare("SELECT uuid FROM users WHERE name=? COLLATE NOCASE").bind(name).first();
      if (nameOwner && nameOwner.uuid !== uuid) {
        await env.DB.prepare("UPDATE users SET name=? WHERE uuid=?").bind(`stale_${nameOwner.uuid.slice(0,10)}`,nameOwner.uuid).run();
      }
      await env.DB.prepare("INSERT INTO users(uuid,name,token_hash,created_at) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,token_hash=excluded.token_hash").bind(uuid,name,hash,Date.now()).run();
      await env.DB.prepare("INSERT INTO sessions(token_hash,user_uuid,updated_at) VALUES(?,?,?) ON CONFLICT(token_hash) DO UPDATE SET updated_at=excluded.updated_at WHERE sessions.user_uuid=excluded.user_uuid").bind(hash,uuid,Date.now()).run();
      return json({ok:true});
    }
    const me = await auth(request, env); if (!me) return json({error:"unauthorized"},401);
    if(request.method==="GET"&&readLimited(env,me.uuid))return json({error:"Please wait before refreshing",retryAfterSeconds:60},429);
    const action=path==="/v2/profile/me"&&request.method==="POST"?"profile":path.endsWith("/friends/request")?"friend-request":path==="/v2/messages"&&request.method==="POST"?"message":path==="/v2/invitations"?"invitation":null;
    if(action && await limited(env,me.uuid,action,action==="message"?60:10))return json({error:"Please wait before trying again",retryAfterSeconds:60},429);
    if(path==="/v2/profile/me" && request.method==="GET") {
      const profile=await env.DB.prepare("SELECT bio,tag,emoji,public FROM profiles WHERE user_uuid=?").bind(me.uuid).first();
      return json({profile:{uuid:me.uuid,name:me.name,bio:"",tag:"",emoji:"",...profile,public:!!profile?.public}});
    }
    if(path==="/v2/profile/me" && request.method==="POST") {
      const b=await request.json();
      if(!safeText(b.bio)||b.bio.length>280||typeof b.tag!=="string"||! /^[A-Za-z0-9]{0,5}$/.test(b.tag)||!emojis.has(b.emoji)||typeof b.public!=="boolean")return json({error:"Bio must be at most 280 characters; tag must be at most 5 letters/numbers; select a bundled emoji"},400);
      const old=await env.DB.prepare("SELECT public FROM profiles WHERE user_uuid=?").bind(me.uuid).first();
      if(b.public&&!old?.public&&b.publicationConsent!==true)return json({error:"Explicit public-profile consent is required"},400);
      await env.DB.prepare("INSERT INTO profiles(user_uuid,bio,tag,emoji,public,consented_at,updated_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT(user_uuid) DO UPDATE SET bio=excluded.bio,tag=excluded.tag,emoji=excluded.emoji,public=excluded.public,consented_at=CASE WHEN excluded.public=1 THEN COALESCE(profiles.consented_at,excluded.consented_at) ELSE NULL END,updated_at=excluded.updated_at").bind(me.uuid,b.bio.trim(),b.tag,b.emoji,b.public?1:0,b.public?Date.now():null,Date.now()).run();
      return json({ok:true});
    }
    if(path==="/v2/profiles"&&request.method==="GET") {
      const q=String(url.searchParams.get("q")||"").slice(0,32);
      const rows=await env.DB.prepare("SELECT u.uuid,u.name,p.bio,p.tag,p.emoji FROM profiles p JOIN users u ON u.uuid=p.user_uuid WHERE p.public=1 AND (instr(lower(u.name),lower(?))>0 OR instr(lower(p.tag),lower(?))>0) ORDER BY u.name LIMIT 50").bind(q,q).all();
      return json({profiles:rows.results});
    }
    if(path.startsWith("/v2/profiles/")&&request.method==="GET") {
      const profile=await env.DB.prepare("SELECT u.uuid,u.name,p.bio,p.tag,p.emoji FROM profiles p JOIN users u ON u.uuid=p.user_uuid WHERE p.public=1 AND p.user_uuid=?").bind(cleanUuid(path.split("/").pop())).first();
      return profile?json({profile}):json({error:"Public profile not found"},404);
    }
    if(path==="/v2/servers"&&request.method==="GET") {
      const q=String(url.searchParams.get("q")||"").trim().slice(0,64);
      const mode=url.searchParams.get("sort"),sort=mode==="players"?"players DESC,recommendation_rank,name":mode==="name"?"name":"recommendation_rank,name";
      const rows=await env.DB.prepare(`SELECT id,name,address,description,icon,players,COALESCE(r.rank,1000) recommendation_rank FROM public_servers LEFT JOIN public_server_recommendations r ON r.server_id=public_servers.id WHERE listed=1 AND instr(replace(lower(name||' '||address||' '||description),' ',''),replace(lower(?),' ',''))>0 ORDER BY ${sort} LIMIT 100`).bind(q).all();
      return json({servers:rows.results});
    }
    if (path === "/v2/friends/request" && request.method === "POST") {
      const other=await findOrCreateMinecraftUser(env,(await request.json()).name); if(!other||other.uuid===me.uuid)return json({error:"player not found"},404);
      if(await isFriend(env,me.uuid,other.uuid))return json({ok:true,state:"friends",uuid:other.uuid,name:other.name});
      const reciprocal=await env.DB.prepare("SELECT 1 ok FROM requests WHERE sender=? AND recipient=?").bind(other.uuid,me.uuid).first();
      if(reciprocal){const [low,high]=pair(me.uuid,other.uuid);await env.DB.prepare("INSERT OR IGNORE INTO friendships(user_low,user_high,created_at) VALUES(?,?,?)").bind(low,high,Date.now()).run();await env.DB.prepare("DELETE FROM requests WHERE (sender=? AND recipient=?) OR (sender=? AND recipient=?)").bind(me.uuid,other.uuid,other.uuid,me.uuid).run();return json({ok:true,state:"friends",uuid:other.uuid,name:other.name});}
      await env.DB.prepare("INSERT OR IGNORE INTO requests(sender,recipient,created_at) VALUES(?,?,?)").bind(me.uuid,other.uuid,Date.now()).run();
      return json({ok:true,state:"outgoing",uuid:other.uuid,name:other.name});
    }
    if (path === "/v2/friends/accept" && request.method === "POST") {
      const other=await findUser(env,(await request.json()).uuid); if(!other)return json({error:"player not found"},404);
      const pending=await env.DB.prepare("SELECT 1 ok FROM requests WHERE sender=? AND recipient=?").bind(other.uuid,me.uuid).first(); if(!pending)return json({error:"request not found"},404);
      const [low,high]=pair(me.uuid,other.uuid); await env.DB.prepare("INSERT OR IGNORE INTO friendships(user_low,user_high,created_at) VALUES(?,?,?)").bind(low,high,Date.now()).run();
      await env.DB.prepare("DELETE FROM requests WHERE (sender=? AND recipient=?) OR (sender=? AND recipient=?)").bind(me.uuid,other.uuid,other.uuid,me.uuid).run(); return json({ok:true});
    }
    if (path === "/v2/friends/decline" && request.method === "POST") { const other=await findUser(env,(await request.json()).uuid);if(other)await env.DB.prepare("DELETE FROM requests WHERE sender=? AND recipient=?").bind(other.uuid,me.uuid).run();return json({ok:true}); }
    if (path === "/v2/friends/cancel" && request.method === "POST") { const b=await request.json(),other=await findUser(env,b.uuid||b.name);if(!other)return json({error:"player not found"},404);await env.DB.prepare("DELETE FROM requests WHERE sender=? AND recipient=?").bind(me.uuid,other.uuid).run();return json({ok:true}); }
    if (path === "/v2/friends/remove" && request.method === "POST") { const other=await findUser(env,(await request.json()).uuid);if(other){const [low,high]=pair(me.uuid,other.uuid);await env.DB.prepare("DELETE FROM friendships WHERE user_low=? AND user_high=?").bind(low,high).run()}return json({ok:true}); }
    if (path === "/v2/friends" && request.method === "GET") {
      const rows=await env.DB.prepare(`SELECT u.uuid,u.name,'friends' state,CASE WHEN p.last_seen>=? THEN 1 ELSE 0 END online FROM friendships f JOIN users u ON u.uuid=CASE WHEN f.user_low=? THEN f.user_high ELSE f.user_low END LEFT JOIN presence p ON p.user_uuid=u.uuid WHERE f.user_low=? OR f.user_high=? UNION ALL SELECT u.uuid,u.name,'incoming' state,CASE WHEN p.last_seen>=? THEN 1 ELSE 0 END online FROM requests r JOIN users u ON u.uuid=r.sender LEFT JOIN presence p ON p.user_uuid=u.uuid WHERE r.recipient=? UNION ALL SELECT u.uuid,u.name,'outgoing' state,CASE WHEN p.last_seen>=? THEN 1 ELSE 0 END online FROM requests r JOIN users u ON u.uuid=r.recipient LEFT JOIN presence p ON p.user_uuid=u.uuid WHERE r.sender=?`).bind(freshAfter(),me.uuid,me.uuid,me.uuid,freshAfter(),me.uuid,freshAfter(),me.uuid).all();
      return json({friends:rows.results});
    }
    if(path==="/v2/presence"&&request.method==="POST"){const hash=String((await request.json()).serverHash||"").slice(0,128);await env.DB.prepare("INSERT INTO presence(user_uuid,server_hash,last_seen) VALUES(?,?,?) ON CONFLICT(user_uuid) DO UPDATE SET server_hash=excluded.server_hash,last_seen=excluded.last_seen").bind(me.uuid,hash,Date.now()).run();return json({ok:true});}
    if(path==="/v2/messages"&&request.method==="POST"){const b=await request.json(),other=await findUser(env,b.to),body=String(b.body||"").trim().slice(0,500),replyTo=Number(b.replyTo||0);if(!other||!body||!safeText(body)||(body.startsWith("reconfig-emoji:")&&!emojis.has(body.slice(15))))return json({error:"invalid message"},400);if(!(await isFriend(env,me.uuid,other.uuid)))return json({error:"friendship required"},403);let reply=null;if(replyTo){reply=await env.DB.prepare("SELECT id FROM messages_v2 WHERE id=? AND ((sender=? AND recipient=?) OR (sender=? AND recipient=?))").bind(replyTo,me.uuid,other.uuid,other.uuid,me.uuid).first();if(!reply)return json({error:"reply target is not in this conversation"},400);}const result=await env.DB.prepare("INSERT INTO messages_v2(sender,recipient,body,created_at,reply_to) VALUES(?,?,?,?,?)").bind(me.uuid,other.uuid,body,Date.now(),reply?.id||null).run();return json({ok:true,id:Number(result.meta?.last_row_id||result.lastInsertRowid||0)});}
    if(path.startsWith("/v2/messages/")&&request.method==="DELETE"){const id=Number(path.slice("/v2/messages/".length));if(!Number.isSafeInteger(id)||id<=0)return json({error:"invalid message"},400);const message=await env.DB.prepare("SELECT sender FROM messages_v2 WHERE id=?").bind(id).first();if(!message)return json({error:"message not found"},404);if(message.sender!==me.uuid)return json({error:"only the sender can delete this message"},403);await env.DB.prepare("DELETE FROM messages_v2 WHERE id=? AND sender=?").bind(id,me.uuid).run();return json({ok:true});}
    if(path==="/v2/messages"&&request.method==="GET") {
      const other=await findUser(env,url.searchParams.get("with")),after=cursorValue(url.searchParams.get("after")),before=cursorValue(url.searchParams.get("before"));
      if(!other)return json({error:"player not found"},404);
      if(url.searchParams.has("before")&&!before)return json({error:"invalid history cursor"},400);
      if(before&&after)return json({error:"choose either before or after"},400);
      const backwards=before>0||after===0,comparison=before?"<":">",boundary=before||after;
      const rows=await env.DB.prepare(`SELECT m.id,m.sender,m.recipient,m.body,m.created_at,m.acknowledged,m.reply_to,r.body reply_body,r.sender reply_sender FROM messages_v2 m LEFT JOIN messages_v2 r ON r.id=m.reply_to WHERE m.id${comparison}? AND ((m.sender=? AND m.recipient=?) OR (m.sender=? AND m.recipient=?)) ORDER BY m.id ${backwards?"DESC":"ASC"} LIMIT 201`).bind(boundary,me.uuid,other.uuid,other.uuid,me.uuid).all();
      const hasMore=rows.results.length>200,messages=rows.results.slice(0,200);if(backwards)messages.reverse();
      return json({messages,hasMore,beforeCursor:messages[0]?.id||0,cursor:messages.at(-1)?.id||after});
    }
    if(path==="/v2/sync"&&request.method==="GET"){
      if(url.searchParams.get("bootstrap")==="1") {
        const marks=await env.DB.prepare("SELECT COALESCE((SELECT MAX(id) FROM messages_v2 WHERE recipient=?),0) cursor,COALESCE((SELECT MAX(id) FROM request_events WHERE recipient=?),0) requestCursor,COALESCE((SELECT MAX(id) FROM message_deletions WHERE sender=? OR recipient=?),0) deletionCursor,COALESCE((SELECT MAX(id) FROM invitations WHERE recipient=?),0) invitationCursor").bind(me.uuid,me.uuid,me.uuid,me.uuid,me.uuid).first();
        return json({...marks,messages:[],requests:[],deleted:[],invitations:[]});
      }
      const after=cursorValue(url.searchParams.get("after"));
      const messages=await env.DB.prepare("SELECT m.id,m.sender,m.recipient,m.body,m.created_at,m.reply_to,u.name sender_name,r.body reply_body,r.sender reply_sender FROM messages_v2 m JOIN users u ON u.uuid=m.sender LEFT JOIN messages_v2 r ON r.id=m.reply_to WHERE m.recipient=? AND m.id>? ORDER BY m.id LIMIT 100").bind(me.uuid,after).all();
      const afterInvitation=cursorValue(url.searchParams.get("afterInvitation"));
      const invitations=await env.DB.prepare("SELECT i.id,u.name sender,i.server_address serverAddress,i.created_at FROM invitations i JOIN users u ON u.uuid=i.sender WHERE i.recipient=? AND i.acknowledged=0 AND i.id>? ORDER BY i.id LIMIT 50").bind(me.uuid,afterInvitation).all();
      
      const cursor=messages.results.reduce((value,row)=>Math.max(value,Number(row.id)||0),after);
      const afterRequest=cursorValue(url.searchParams.get("afterRequest")),afterDeletion=cursorValue(url.searchParams.get("afterDeletion"));
      const requests=await env.DB.prepare("SELECT e.id,e.sender,u.name,e.created_at FROM request_events e JOIN users u ON u.uuid=e.sender JOIN requests r ON r.sender=e.sender AND r.recipient=e.recipient AND r.created_at=e.created_at WHERE e.recipient=? AND e.id>? ORDER BY e.id LIMIT 100").bind(me.uuid,afterRequest).all();
      const deleted=await env.DB.prepare("SELECT id,message_id,sender,recipient,deleted_at FROM message_deletions WHERE (sender=? OR recipient=?) AND id>? ORDER BY id LIMIT 200").bind(me.uuid,me.uuid,afterDeletion).all();
      return json({invitationCursor:invitations.results.at(-1)?.id||afterInvitation,cursor,messages:messages.results,invitations:invitations.results,requests:requests.results,requestCursor:requests.results.at(-1)?.id||afterRequest,deleted:deleted.results,deletionCursor:deleted.results.at(-1)?.id||afterDeletion});
    }
    if(path==="/v2/invitations/ack"&&request.method==="POST") {
      const {ids}=await request.json();
      if(!Array.isArray(ids)||ids.length>50||ids.some(id=>!Number.isSafeInteger(id)||id<=0))return json({error:"invalid invitation IDs"},400);
      if(ids.length)await env.DB.prepare(`UPDATE invitations SET acknowledged=1 WHERE recipient=? AND id IN (${ids.map(()=>"?").join(",")})`).bind(me.uuid,...ids).run();
      return json({ok:true});
    }
    if(path==="/v2/invitations"&&request.method==="POST"){const b=await request.json(),other=await findUser(env,b.to),address=String(b.serverAddress||"").trim().slice(0,255);if(!other||!address)return json({error:"invalid invitation"},400);if(!(await isFriend(env,me.uuid,other.uuid)))return json({error:"friendship required"},403);const now=Date.now(),last=await env.DB.prepare("SELECT created_at FROM invitations WHERE sender=? AND recipient=? ORDER BY id DESC LIMIT 1").bind(me.uuid,other.uuid).first(),cooldown=30000;if(last&&now-last.created_at<cooldown){const retry=Math.ceil((cooldown-(now-last.created_at))/1000);return json({error:`invite cooldown: retry in ${retry}s`,retryAfterSeconds:retry},429);}await env.DB.prepare("INSERT INTO invitations(sender,recipient,server_address,created_at) VALUES(?,?,?,?)").bind(me.uuid,other.uuid,address,now).run();return json({ok:true,cooldownSeconds:30});}
    if(path==="/v2/events"&&request.method==="GET"){const invitations=await env.DB.prepare("SELECT i.id,u.name sender,i.server_address serverAddress,i.created_at FROM invitations i JOIN users u ON u.uuid=i.sender WHERE i.recipient=? AND i.acknowledged=0 ORDER BY i.id LIMIT 50").bind(me.uuid).all();return json({invitations:invitations.results});}
    if (path === "/v1/friends/request" && request.method === "POST") {
      const name = cleanName((await request.json()).name), other = await env.DB.prepare("SELECT uuid,name FROM users WHERE name=? COLLATE NOCASE").bind(name).first();
      if (!other || other.uuid === me.uuid) return json({error:"player not found"},404);
      await env.DB.prepare("INSERT OR IGNORE INTO requests(sender,recipient,created_at) VALUES(?,?,?)").bind(me.uuid,other.uuid,Date.now()).run();
      const mutual = await env.DB.prepare("SELECT 1 ok FROM requests WHERE sender=? AND recipient=?").bind(other.uuid,me.uuid).first();
      return json({name:other.name,uuid:other.uuid,status:mutual?"friends":"pending"});
    }
    if (path === "/v1/friends" && request.method === "GET") {
      const rows = await env.DB.prepare("SELECT u.uuid,u.name,CASE WHEN r2.sender IS NULL THEN 'incoming' ELSE 'friends' END status FROM requests r JOIN users u ON u.uuid=r.sender LEFT JOIN requests r2 ON r2.sender=r.recipient AND r2.recipient=r.sender WHERE r.recipient=? UNION SELECT u.uuid,u.name,'pending' status FROM requests r JOIN users u ON u.uuid=r.recipient LEFT JOIN requests r2 ON r2.sender=r.recipient AND r2.recipient=r.sender WHERE r.sender=? AND r2.sender IS NULL").bind(me.uuid,me.uuid).all();
      return json({friends:rows.results});
    }
    if (path === "/v1/messages" && request.method === "POST") {
      const b=await request.json(), other=await env.DB.prepare("SELECT uuid,name FROM users WHERE name=? COLLATE NOCASE").bind(cleanName(b.to)).first(), body=String(b.body||"").trim().slice(0,500);
      if (!other || !body) return json({error:"invalid message"},400);
      const pair=await env.DB.prepare("SELECT (SELECT count(*) FROM requests WHERE sender=? AND recipient=?) + (SELECT count(*) FROM requests WHERE sender=? AND recipient=?) n").bind(me.uuid,other.uuid,other.uuid,me.uuid).first();
      if (pair.n !== 2) return json({error:"mutual friendship required"},403);
      await env.DB.prepare("INSERT INTO messages(sender,recipient,body,created_at) VALUES(?,?,?,?)").bind(me.uuid,other.uuid,body,Date.now()).run();
      return json({ok:true});
    }
    if (path === "/v1/events" && request.method === "GET") {
      const rows=await env.DB.prepare("SELECT m.id,u.name sender,m.body,m.created_at FROM messages m JOIN users u ON u.uuid=m.sender WHERE m.recipient=? AND m.delivered=0 ORDER BY m.id LIMIT 50").bind(me.uuid).all();
      if(rows.results.length) await env.DB.prepare(`UPDATE messages SET delivered=1 WHERE id IN (${rows.results.map(()=>"?").join(",")})`).bind(...rows.results.map(x=>x.id)).run();
      return json({messages:rows.results});
    }
    return json({error:"not found"},404);
  } catch (error) {
    if(error instanceof SyntaxError)return json({error:"Invalid JSON request"},400);
    const detail=String(error?.message||error);
    if(/daily row read limit|D1_ERROR|code:\s*7500/i.test(detail))return json({error:"service capacity reached",retryAfterSeconds:900},429);
    console.error("ReConfig request failed",detail);
    return json({error:"temporary service error"},503);
  }
}};
