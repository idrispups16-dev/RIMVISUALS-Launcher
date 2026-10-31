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
  await fs.rm(root,{recursive:true,force:true});
  console.log('RIM_STORAGE_OK: Windows encrypted account persistence, fresh-instance decrypt and logout verified; synthetic credentials only');
  app.exit(0);
 }catch(error){if(root)await fs.rm(root,{recursive:true,force:true});console.error(error.message);app.exit(1);}
});
