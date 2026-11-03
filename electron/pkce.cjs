const http=require('node:http');
const crypto=require('node:crypto');
const BASE='https://login.microsoftonline.com/consumers/oauth2/v2.0/';
const validClientId=id=>/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id||'');
function challenge(verifier){return crypto.createHash('sha256').update(verifier).digest('base64url');}
function equal(a,b){const x=Buffer.from(a||''),y=Buffer.from(b||'');return x.length===y.length&&crypto.timingSafeEqual(x,y);}
async function beginPkce({clientId,openExternal,request,timeoutMs=300000}){
 if(!validClientId(clientId))throw Error('Нужен собственный Microsoft Application (client) ID');
 const verifier=crypto.randomBytes(32).toString('base64url'),state=crypto.randomBytes(32).toString('base64url');
 let resolve,reject,timer,done=false,exchanging=false;
 const completion=new Promise((yes,no)=>{resolve=yes;reject=no});completion.catch(()=>{});
 const server=http.createServer();
 const finish=(error,result)=>{if(done)return;done=true;clearTimeout(timer);server.close();server.closeAllConnections();error?reject(error):resolve(result);};
 await new Promise((yes,no)=>{server.once('error',no);server.listen(0,'127.0.0.1',yes)});
 const redirectUri='http://localhost:'+server.address().port+'/';
 server.on('error',()=>finish(Error('Не удалось принять ответ Microsoft')));
 server.on('request',async(req,res)=>{
  res.setHeader('Cache-Control','no-store');res.setHeader('Content-Security-Policy',"default-src 'none'; frame-ancestors 'none'");res.setHeader('Referrer-Policy','no-referrer');res.setHeader('Content-Type','text/plain; charset=utf-8');
  if(req.method!=='GET'||req.headers.host!=='localhost:'+server.address()?.port){res.writeHead(400);res.end('Invalid request');return;}
  const u=new URL(req.url,redirectUri);
  if(u.pathname!=='/'){res.writeHead(404);res.end();return;}
  if(u.searchParams.getAll('state').length!==1||!equal(u.searchParams.get('state'),state)){res.writeHead(400);res.end('Invalid OAuth state');return;}
  if(done||exchanging){res.writeHead(409);res.end('Sign-in already handled');return;}
  if(u.searchParams.has('error')){res.end('Microsoft sign-in cancelled. Return to RIMVISUALS.');finish(Error('Вход Microsoft отменён или отклонён'));return;}
  const code=u.searchParams.get('code');if(!code||u.searchParams.getAll('code').length!==1){res.writeHead(400);res.end('Missing authorization code');return;}
  exchanging=true;
  // Do not echo the code, state, verifier or token in browser content or logs.
  res.end('Microsoft response received. You can return to RIMVISUALS.');
  try{const token=await request(BASE+'token',new URLSearchParams({client_id:clientId,grant_type:'authorization_code',code,redirect_uri:redirectUri,code_verifier:verifier,scope:'XboxLive.signin offline_access'}));finish(null,token);}catch{finish(Error('Не удалось обменять код Microsoft. Проверь регистрацию приложения и повтори вход'));}
 });
 timer=setTimeout(()=>finish(Error('Время входа Microsoft истекло')),timeoutMs);
 const url=new URL(BASE+'authorize');url.search=new URLSearchParams({client_id:clientId,response_type:'code',response_mode:'query',redirect_uri:redirectUri,scope:'XboxLive.signin offline_access',state,code_challenge:challenge(verifier),code_challenge_method:'S256',prompt:'select_account'}).toString();
 try{await openExternal(url.href);}catch{finish(Error('Не удалось открыть системный браузер'));}
 return {completion,cancel:()=>finish(Error('Вход отменён')),redirectUri};
}
module.exports={beginPkce,challenge,validClientId};
