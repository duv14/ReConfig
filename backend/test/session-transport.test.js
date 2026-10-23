import test from 'node:test';
import assert from 'node:assert/strict';
import {createSessionFetch} from '../src/session-transport.js';
function connection(raw, chunks=1){return {readable:new ReadableStream({start(c){const data=new TextEncoder().encode(raw);for(let i=0;i<data.length;i+=chunks)c.enqueue(data.slice(i,i+chunks));c.close();}}),writable:new WritableStream({write(){}}),closed:Promise.resolve(),close:async()=>{}};}
const url='https://sessionserver.mojang.com/session/minecraft/hasJoined?username=Alice&serverId='+'a'.repeat(40);
test('blocked fetch falls back to TLS against the same official host',async()=>{
 let dials=0;
 const fetch=createSessionFetch((address,options)=>{dials++;assert.deepEqual(address,{hostname:'sessionserver.mojang.com',port:443});assert.equal(options.secureTransport,'on');return connection('HTTP/1.0 200 OK\r\nContent-Length: 13\r\n\r\n{"id":"real"}',3);},async()=>new Response(null,{status:403}));
 assert.deepEqual(await(await fetch(url)).json(),{id:'real'});assert.equal(dials,1);
});
test('successful normal fetch needs no socket and failed ownership is not bypassed',async()=>{
 const noSocket=()=>{throw new Error('unexpected fallback');};
 assert.equal((await createSessionFetch(noSocket,async()=>new Response(null,{status:204}))(url)).status,204);
 const blocked=createSessionFetch(()=>connection('HTTP/1.0 403 Forbidden\r\nContent-Length: 0\r\n\r\n'),async()=>new Response(null,{status:403}));
 assert.equal((await blocked(url)).status,403);
});
test('socket transport cannot be used as an arbitrary proxy',async()=>{
 const fetch=createSessionFetch(()=>{throw new Error('should not dial');},async()=>{throw new Error('should not fetch');});
 for(const invalid of ['https://evil.test/','http://sessionserver.mojang.com/session/minecraft/hasJoined','https://sessionserver.mojang.com/other'])await assert.rejects(fetch(invalid),/Invalid/);
});
test('chunked TLS response is decoded and truncated responses fail closed',async()=>{
 const fetch=createSessionFetch(()=>connection('HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\nd\r\n{"id":"real"}\r\n0\r\n\r\n',2),async()=>new Response(null,{status:403}));
 assert.deepEqual(await(await fetch(url)).json(),{id:'real'});
 const broken=createSessionFetch(()=>connection('HTTP/1.0 200 OK\r\nContent-Length: 100\r\n\r\n{}'),async()=>new Response(null,{status:403}));
 await assert.rejects(broken(url),/Truncated/);
});
