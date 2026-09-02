/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
const json = (value, status=200) => new Response(JSON.stringify(value), {status, headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","access-control-allow-origin":"*"}});
const cleanUuid = value => String(value || "").replaceAll("-", "").toLowerCase();
const cleanName = value => String(value || "").trim().slice(0, 16);
const tokenHash = async token => [...new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token)))].map(x=>x.toString(16).padStart(2,"0")).join("");

async function auth(request, env) {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return null;
  return env.DB.prepare("SELECT u.uuid,u.name FROM sessions s JOIN users u ON u.uuid=s.user_uuid WHERE s.token_hash=?").bind(await tokenHash(token)).first();
}

const pair = (a,b) => a < b ? [a,b] : [b,a];
const freshAfter = () => Date.now() - 15000;
const findUser = (env,value) => env.DB.prepare("SELECT uuid,name FROM users WHERE uuid=? OR name=? COLLATE NOCASE").bind(cleanUuid(value),cleanName(value)).first();
const isFriend = async (env,a,b) => { const [low,high]=pair(a,b); return !!(await env.DB.prepare("SELECT 1 ok FROM friendships WHERE user_low=? AND user_high=?").bind(low,high).first()); };
const lookupMinecraftProfile = async (env,value) => {
  const name=cleanName(value); if(!/^[A-Za-z0-9_]{1,16}$/.test(name))return null;
  if(typeof env.PROFILE_LOOKUP==="function")return env.PROFILE_LOOKUP(name);
  const httpFetch=typeof env.PROFILE_HTTP_FETCH==="function"?env.PROFILE_HTTP_FETCH:fetch;
  const encoded=encodeURIComponent(name),providers=[
    {url:`https://api.minecraftservices.com/minecraft/profile/lookup/name/${encoded}`,parse:data=>({id:data?.id,name:data?.name})},
    {url:`https://api.mojang.com/users/profiles/minecraft/${encoded}`,parse:data=>({id:data?.id,name:data?.name})},
    {url:`https://playerdb.co/api/player/minecraft/${encoded}`,parse:data=>({id:data?.data?.player?.id,name:data?.data?.player?.username})},
  ];
  let unavailable=false;
  for(const provider of providers){
    try{
      const response=await httpFetch(provider.url,{headers:{accept:"application/json","user-agent":"ReConfig/1.0"}});
      if(response.status===404||response.status===204)continue;
      if(!response.ok){unavailable=true;continue;}
      const profile=provider.parse(await response.json());
      if(/^[a-fA-F0-9-]{32,36}$/.test(String(profile.id||""))&&/^\w{1,16}$/.test(String(profile.name||"")))return profile;
    }catch{unavailable=true;}
  }
  if(unavailable)throw new Error("Minecraft profile lookup is temporarily unavailable");
  return null;
};
const findOrCreateMinecraftUser = async (env,value) => {
  const existing=await findUser(env,value); if(existing)return existing;
  const profile=await lookupMinecraftProfile(env,value),uuid=cleanUuid(profile?.id),name=cleanName(profile?.name);
  if(!/^[a-f0-9]{32}$/.test(uuid)||!/^\w{1,16}$/.test(name))return null;
  await env.DB.prepare("INSERT INTO users(uuid,name,token_hash,created_at) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name").bind(uuid,name,`external:${uuid}`,Date.now()).run();
  return {uuid,name};
};

export default { async fetch(request, env) {
  if (request.method === "OPTIONS") return new Response(null,{headers:{"access-control-allow-origin":"*","access-control-allow-headers":"authorization,content-type","access-control-allow-methods":"GET,POST,OPTIONS"}});
  const url = new URL(request.url), path = url.pathname;
  try {
    if (path === "/v1/health") return json({ok:true,service:"ReConfig"});
    if ((path === "/v1/enroll" || path === "/v2/enroll") && request.method === "POST") {
      const b = await request.json(), uuid = cleanUuid(b.uuid), name = cleanName(b.name), token = String(b.token || "");
      if (!/^[a-f0-9]{32}$/.test(uuid) || !/^[A-Za-z0-9_]{1,16}$/.test(name) || token.length < 32) return json({error:"invalid enrollment"},400);
      const existing = await env.DB.prepare("SELECT token_hash FROM users WHERE uuid=?").bind(uuid).first();
      const hash = await tokenHash(token);
      if (existing && !existing.token_hash.startsWith("external:") && existing.token_hash !== hash) return json({error:"identity already enrolled"},409);
      const nameOwner = await env.DB.prepare("SELECT uuid FROM users WHERE name=? COLLATE NOCASE").bind(name).first();
      if (nameOwner && nameOwner.uuid !== uuid) {
        await env.DB.prepare("UPDATE users SET name=? WHERE uuid=?").bind(`stale_${nameOwner.uuid.slice(0,10)}`,nameOwner.uuid).run();
      }
      await env.DB.prepare("INSERT INTO users(uuid,name,token_hash,created_at) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,token_hash=excluded.token_hash").bind(uuid,name,hash,Date.now()).run();
      await env.DB.prepare("INSERT INTO sessions(token_hash,user_uuid,updated_at) VALUES(?,?,?) ON CONFLICT(token_hash) DO UPDATE SET user_uuid=excluded.user_uuid,updated_at=excluded.updated_at").bind(hash,uuid,Date.now()).run();
      return json({ok:true});
    }
    const me = await auth(request, env); if (!me) return json({error:"unauthorized"},401);
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
    if(path==="/v2/messages"&&request.method==="POST"){const b=await request.json(),other=await findUser(env,b.to),body=String(b.body||"").trim().slice(0,500),replyTo=Number(b.replyTo||0);if(!other||!body)return json({error:"invalid message"},400);if(!(await isFriend(env,me.uuid,other.uuid)))return json({error:"friendship required"},403);let reply=null;if(replyTo){reply=await env.DB.prepare("SELECT id FROM messages_v2 WHERE id=? AND ((sender=? AND recipient=?) OR (sender=? AND recipient=?))").bind(replyTo,me.uuid,other.uuid,other.uuid,me.uuid).first();if(!reply)return json({error:"reply target is not in this conversation"},400);}const result=await env.DB.prepare("INSERT INTO messages_v2(sender,recipient,body,created_at,reply_to) VALUES(?,?,?,?,?)").bind(me.uuid,other.uuid,body,Date.now(),reply?.id||null).run();return json({ok:true,id:Number(result.meta?.last_row_id||result.lastInsertRowid||0)});}
    if(path.startsWith("/v2/messages/")&&request.method==="DELETE"){const id=Number(path.slice("/v2/messages/".length));if(!Number.isSafeInteger(id)||id<=0)return json({error:"invalid message"},400);const message=await env.DB.prepare("SELECT sender FROM messages_v2 WHERE id=?").bind(id).first();if(!message)return json({error:"message not found"},404);if(message.sender!==me.uuid)return json({error:"only the sender can delete this message"},403);await env.DB.prepare("DELETE FROM messages_v2 WHERE id=? AND sender=?").bind(id,me.uuid).run();return json({ok:true});}
    if(path==="/v2/messages"&&request.method==="GET"){const other=await findUser(env,url.searchParams.get("with")),after=Math.max(0,Number(url.searchParams.get("after")||0));if(!other)return json({error:"player not found"},404);const rows=await env.DB.prepare("SELECT m.id,m.sender,m.recipient,m.body,m.created_at,m.acknowledged,m.reply_to,r.body reply_body,r.sender reply_sender FROM messages_v2 m LEFT JOIN messages_v2 r ON r.id=m.reply_to WHERE m.id>? AND ((m.sender=? AND m.recipient=?) OR (m.sender=? AND m.recipient=?)) ORDER BY m.id LIMIT 200").bind(after,me.uuid,other.uuid,other.uuid,me.uuid).all();return json({messages:rows.results});}
    if(path==="/v2/sync"&&request.method==="GET"){
      const after=Math.max(0,Number(url.searchParams.get("after")||0));
      const messages=await env.DB.prepare("SELECT m.id,m.sender,m.recipient,m.body,m.created_at,m.reply_to,u.name sender_name,r.body reply_body,r.sender reply_sender FROM messages_v2 m JOIN users u ON u.uuid=m.sender LEFT JOIN messages_v2 r ON r.id=m.reply_to WHERE m.recipient=? AND m.id>? ORDER BY m.id LIMIT 100").bind(me.uuid,after).all();
      const invitations=await env.DB.prepare("SELECT i.id,u.name sender,i.server_address serverAddress,i.created_at FROM invitations i JOIN users u ON u.uuid=i.sender WHERE i.recipient=? AND i.acknowledged=0 ORDER BY i.id LIMIT 50").bind(me.uuid).all();
      if(invitations.results.length){const ids=invitations.results.map(row=>row.id);await env.DB.prepare(`UPDATE invitations SET acknowledged=1 WHERE recipient=? AND id IN (${ids.map(()=>"?").join(",")})`).bind(me.uuid,...ids).run();}
      const cursor=messages.results.reduce((value,row)=>Math.max(value,Number(row.id)||0),after);
      return json({cursor,messages:messages.results,invitations:invitations.results});
    }
    if(path==="/v2/invitations"&&request.method==="POST"){const b=await request.json(),other=await findUser(env,b.to),address=String(b.serverAddress||"").trim().slice(0,255);if(!other||!address)return json({error:"invalid invitation"},400);if(!(await isFriend(env,me.uuid,other.uuid)))return json({error:"friendship required"},403);const now=Date.now(),last=await env.DB.prepare("SELECT created_at FROM invitations WHERE sender=? AND recipient=? ORDER BY id DESC LIMIT 1").bind(me.uuid,other.uuid).first(),cooldown=30000;if(last&&now-last.created_at<cooldown){const retry=Math.ceil((cooldown-(now-last.created_at))/1000);return json({error:`invite cooldown: retry in ${retry}s`,retryAfterSeconds:retry},429);}await env.DB.prepare("INSERT INTO invitations(sender,recipient,server_address,created_at) VALUES(?,?,?,?)").bind(me.uuid,other.uuid,address,now).run();return json({ok:true,cooldownSeconds:30});}
    if(path==="/v2/events"&&request.method==="GET"){const invitations=await env.DB.prepare("SELECT i.id,u.name sender,i.server_address serverAddress,i.created_at FROM invitations i JOIN users u ON u.uuid=i.sender WHERE i.recipient=? AND i.acknowledged=0 ORDER BY i.id LIMIT 50").bind(me.uuid).all();if(invitations.results.length){const ids=invitations.results.map(row=>row.id);await env.DB.prepare(`UPDATE invitations SET acknowledged=1 WHERE recipient=? AND id IN (${ids.map(()=>"?").join(",")})`).bind(me.uuid,...ids).run();}return json({invitations:invitations.results});}
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
  } catch (error) { const detail=String(error?.message||error).slice(0,300);return json({error:`server error: ${detail}`,detail},500); }
}};
