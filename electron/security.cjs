const {pathToFileURL}=require('node:url');
function trustedSender(event,contents,index){const frame=event?.senderFrame;return !!frame&&event.sender===contents&&frame===contents.mainFrame&&frame.url===pathToFileURL(index).href;}
function registerIpc(ipc,contents,index){return (channel,handler)=>ipc.handle(channel,(event,...args)=>{if(!trustedSender(event,contents(),index))throw Error('Недоверенный отправитель IPC');return handler(event,...args);});}
function secureSession(session){session.setPermissionRequestHandler((_wc,_permission,callback)=>callback(false));session.setPermissionCheckHandler(()=>false);session.webRequest.onBeforeRequest((details,callback)=>{let allowed=false;try{const u=new URL(details.url);allowed=u.protocol==='file:'||u.protocol==='devtools:';}catch{}callback({cancel:!allowed});});}
function microsoftBrowserUrl(value){const u=new URL(value);if(u.protocol!=='https:'||u.hostname!=='login.microsoftonline.com'||u.port||u.username||u.password||u.pathname!=='/consumers/oauth2/v2.0/authorize')throw Error('Недоверенный адрес входа Microsoft');return u.href;}
module.exports={trustedSender,registerIpc,secureSession,microsoftBrowserUrl};
