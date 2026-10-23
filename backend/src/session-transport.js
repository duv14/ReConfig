/* ReConfig by duv14. Only Mojang's official TLS endpoint is permitted here. */
const HOST = 'sessionserver.mojang.com';
const MAX_BYTES = 131072;

function parseResponse(bytes) {
  const wire = new TextDecoder().decode(bytes), split = wire.indexOf('\r\n\r\n');
  if(split<0)throw new Error('Truncated Minecraft verification response');
  const headers=wire.slice(0,split).split('\r\n'),status=/^HTTP\/1\.[01] (\d{3})\b/.exec(headers.shift());
  if(!status)throw new Error('Invalid Minecraft verification response');
  const fields=new Headers();
  for(const line of headers){const colon=line.indexOf(':');if(colon<1)throw new Error('Invalid Minecraft verification headers');fields.append(line.slice(0,colon),line.slice(colon+1).trim());}
  let body=wire.slice(split+4);
  if(fields.get('transfer-encoding')?.toLowerCase()==='chunked'){
    let decoded='';
    while(true){
      const end=body.indexOf('\r\n');if(end<0)throw new Error('Truncated Minecraft verification chunk');
      const sizeText=body.slice(0,end).split(';')[0];if(!/^[0-9a-f]+$/i.test(sizeText))throw new Error('Invalid Minecraft verification chunk');
      const size=parseInt(sizeText,16);body=body.slice(end+2);
      if(size===0)break;
      if(body.length<size+2||body.slice(size,size+2)!=='\r\n')throw new Error('Truncated Minecraft verification chunk');
      decoded+=body.slice(0,size);body=body.slice(size+2);
    }
    body=decoded;
  }else if(fields.has('content-length')){
    const size=Number(fields.get('content-length'));
    const actual=new TextEncoder().encode(body).length;
    if(!Number.isSafeInteger(size)||size<0||actual!==size)throw new Error('Truncated Minecraft verification body');
  }
  const code=Number(status[1]);
  return new Response([204,205,304].includes(code)?null:body,{status:code,headers:{'content-type':'application/json'}});
}

async function socketFetch(connect,url) {
  const socket=connect({hostname:HOST,port:443},{secureTransport:'on'});
  socket.closed?.catch(()=>{});
  let timer;
  try {
    const exchange=(async()=>{
      const writer=socket.writable.getWriter();
      try {await writer.write(new TextEncoder().encode(`GET ${url.pathname}${url.search} HTTP/1.0\r\nHost: ${HOST}\r\nAccept: application/json\r\nAccept-Encoding: identity\r\nUser-Agent: ReConfig/3.3.0\r\nConnection: close\r\n\r\n`));}finally{writer.releaseLock();}
      const reader=socket.readable.getReader(),chunks=[];let total=0;
      try {while(true){const {value,done}=await reader.read();if(done)break;total+=value.byteLength;if(total>MAX_BYTES)throw new Error('Minecraft verification response too large');chunks.push(value);}}finally{reader.releaseLock();}
      const bytes=new Uint8Array(total);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.byteLength;}
      return parseResponse(bytes);
    })();
    return await Promise.race([exchange,new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error('Minecraft verification connection timed out')),6000);})]);
  } finally {clearTimeout(timer);await socket.close().catch(()=>{});}
}

/** TLS validates Mojang itself; no client-supplied assertion or third-party proxy is trusted. */
export function createSessionFetch(connect,httpFetch=fetch) {
  return async (input,options={})=>{
    const url=new URL(input);
    if(url.protocol!=='https:'||url.hostname!==HOST||url.port||url.username||url.password||url.pathname!=='/session/minecraft/hasJoined')throw new Error('Invalid Minecraft verification endpoint');
    const result=await httpFetch(url.href,options);
    if(![403,429,503].includes(result.status))return result;
    await result.body?.cancel();
    return socketFetch(connect,url);
  };
}
