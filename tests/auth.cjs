const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs/promises'),os=require('node:os'),path=require('node:path');
const {Auth}=require('../electron/auth.cjs');
const id='11111111-2222-4333-8444-555555555555';
// Deliberately fake storage: this suite tests account lifecycle, not DPAPI.
const storage={isEncryptionAvailable:()=>true,encryptString:s=>Buffer.from(s),decryptString:b=>b.toString()};
async function fixture(t,override={}){
 const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-auth-'));t.after(()=>fs.rm(root,{recursive:true,force:true}));const calls=[];
 const request=async(url,body,bearer)=>{calls.push({url,body,bearer});if(url.endsWith('/token'))return {access_token:'FAKE_MS',refresh_token:'FAKE_ROTATED'};
 if(url.includes('user.auth.xboxlive'))return {Token:'FAKE_XBOX'};
 if(url.includes('xsts.auth'))return {Token:'FAKE_XSTS',DisplayClaims:{xui:[{uhs:'TEST_UHS'}]}};
 if(url.endsWith('/login_with_xbox'))return {access_token:'FAKE_MC'};
 if(url.endsWith('/mcstore'))return override.ent||{items:[{name:'game_minecraft'}]};
 if(url.endsWith('/minecraft/profile'))return override.profile||{id:'a'.repeat(32),name:'TestPlayer'};
 throw Error('Unexpected authentication endpoint');};
 const auth=new Auth(root,storage,async()=>{},request);return {auth,calls,root};
}
test('refresh validates Xbox, entitlement and profile and rotates stored refresh token',async t=>{
 const {auth,calls}=await fixture(t);await auth.store({clientId:id,refresh:'FAKE_OLD'});
 const session=await auth.restore(id);assert.equal(session.name,'TestPlayer');assert.equal(calls.length,6);
 assert.equal(calls[0].body.get('refresh_token'),'FAKE_OLD');assert.equal(calls[1].body.Properties.RpsTicket,'d=FAKE_MS');
 assert.equal(calls[3].body.identityToken,'XBL3.0 x=TEST_UHS;FAKE_XSTS');assert.equal(calls[4].bearer,'FAKE_MC');
 assert.equal(JSON.parse(storage.decryptString(await fs.readFile(auth.file))).refresh,'FAKE_ROTATED');
 assert.deepEqual(auth.publicState,{signedIn:true,name:'TestPlayer'});assert(!JSON.stringify(auth.publicState).includes('FAKE_'));
 await auth.logout();assert.equal(auth.session,null);await assert.rejects(fs.access(auth.file));
});
test('missing entitlement denies session and never requests profile',async t=>{
 const {auth,calls}=await fixture(t,{ent:{items:[]}});await auth.store({clientId:id,refresh:'FAKE_OLD'});
 await assert.rejects(auth.restore(id),/лицензию/);assert.equal(auth.session,null);assert.equal(auth.publicState.signedIn,false);
 assert(!calls.some(c=>c.url.endsWith('/minecraft/profile')));
 assert.equal(JSON.parse((await fs.readFile(auth.file)).toString()).refresh,'FAKE_OLD');
});
test('invalid Java profile cannot produce authenticated session',async t=>{
 const {auth}=await fixture(t,{profile:{id:'invalid',name:'TestPlayer'}});await auth.store({clientId:id,refresh:'FAKE_OLD'});
 await assert.rejects(auth.restore(id),/profile missing/);assert.equal(auth.session,null);
});
test('changing Client ID requires new sign-in without refreshing old credentials',async t=>{
 const {auth,calls}=await fixture(t);await auth.store({clientId:id,refresh:'FAKE_OLD'});
 await assert.rejects(auth.restore('different'),/Client ID/);assert.equal(calls.length,0);
});
test('unavailable protected storage refuses sign-in before opening browser',async t=>{
 const {root}=await fixture(t);const auth=new Auth(root,{isEncryptionAvailable:()=>false},()=>assert.fail('must not open browser'));
 await assert.rejects(auth.start(id),/storage unavailable/);await assert.rejects(auth.store({refresh:'FAKE'}),/хранилище/);
 await assert.rejects(fs.access(auth.file));
});
test('interactive PKCE callback completes full account chain and stores only refresh token',async t=>{
 const {auth}=await fixture(t);auth.openExternal=async raw=>{const u=new URL(raw);const callback=new URL(u.searchParams.get('redirect_uri'));callback.search=new URLSearchParams({state:u.searchParams.get('state'),code:'FAKE_CODE'});assert.equal((await fetch(callback)).status,200);};
 await auth.start(id);await auth.pending;
 assert.equal(auth.publicState.signedIn,true);assert.equal(auth.busy,false);
 const saved=JSON.parse((await fs.readFile(auth.file)).toString());assert.deepEqual(saved,{clientId:id,refresh:'FAKE_ROTATED'});
});
test('logout cancels pending browser sign-in without leaving stored credentials',async t=>{
 const {auth}=await fixture(t);await auth.start(id);assert.equal(auth.busy,true);await auth.logout();
 assert.deepEqual(auth.publicState,{signedIn:false});assert.equal(auth.session,null);assert.equal(auth.busy,false);await assert.rejects(fs.access(auth.file));
});
