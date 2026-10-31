import {app,safeStorage} from 'electron';
import {createRequire} from 'node:module';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
const require=createRequire(import.meta.url);
const {Auth}=require('../electron/auth.cjs');
app.disableHardwareAcceleration();
app.whenReady().then(async()=>{
 let root;
 try {
  if(process.platform!=='win32')throw Error('Windows-specific DPAPI test');
  if(!safeStorage.isEncryptionAvailable())throw Error('Windows encrypted storage unavailable');
  const sentinel='RIMVISUALS-NONSECRET-TEST-SENTINEL';
  const bytes=safeStorage.encryptString(sentinel);
  if(bytes.includes(Buffer.from(sentinel))||safeStorage.decryptString(bytes)!==sentinel)throw Error('Encrypted storage round-trip failed');
  root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-storage-smoke-'));
  const first=new Auth(root,safeStorage,()=>{});
  const data={clientId:'11111111-2222-4333-8444-555555555555',refresh:sentinel};
  await first.store(data);
  const stored=await fs.readFile(first.file);
  if(stored.includes(Buffer.from(sentinel)))throw Error('Account file contains plaintext');
  const second=new Auth(root,safeStorage,()=>{});
  if(safeStorage.decryptString(await fs.readFile(second.file))!==JSON.stringify(data))throw Error('Account persistence failed');
  await second.logout();
  try{await fs.access(second.file);throw Error('Logout left account file');}catch(e){if(e.code!=='ENOENT')throw e;}
  await first.store(data);
  const {Accounts}=require('../electron/accounts.cjs');
  const manager=new Accounts(root,safeStorage,()=>{});
  await manager.init();
  if((await manager.credentials()).refresh!==sentinel)throw Error('Legacy account migration failed');
  await manager.lock(()=>manager.remember({clientId:data.clientId,refresh:sentinel+'-A'},{id:'a'.repeat(32),name:'SyntheticA'}));
  await manager.lock(()=>manager.remember({clientId:data.clientId,refresh:sentinel+'-B'},{id:'b'.repeat(32),name:'SyntheticB'}));
  const encrypted=await fs.readFile(manager.vault);
  if(encrypted.includes(Buffer.from(sentinel)))throw Error('Multi-account vault contains plaintext');
  if((await fs.readFile(manager.file,'utf8')).includes(sentinel))throw Error('Account metadata leaked token');
  const restarted=new Accounts(root,safeStorage,()=>{});
  await restarted.select('a'.repeat(32));
  if((await restarted.credentials()).refresh!==sentinel+'-A')throw Error('First account lost after restart');
  await restarted.auth.logout();
  await restarted.select('b'.repeat(32));
  if((await restarted.credentials()).refresh!==sentinel+'-B')throw Error('Logout removed another account');
  console.log('RIM_ACCOUNTS_STORAGE_OK: two synthetic Microsoft accounts, native encrypted persistence, restart, selection and isolated logout verified');
  await fs.rm(root,{recursive:true,force:true});
  console.log('RIM_STORAGE_OK: Windows encrypted account persistence, fresh-instance decrypt and logout verified; synthetic credentials only');
  app.exit(0);
 }catch(error){if(root)await fs.rm(root,{recursive:true,force:true});console.error(error.message);app.exit(1);}
});
