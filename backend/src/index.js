import {connect} from 'cloudflare:sockets';
import worker from './worker.js';
import {createSessionFetch} from './session-transport.js';
const sessionFetch=createSessionFetch(connect);
export default {fetch(request,env,ctx){return worker.fetch(request,{...env,SESSION_HTTP_FETCH:sessionFetch},ctx);}};
