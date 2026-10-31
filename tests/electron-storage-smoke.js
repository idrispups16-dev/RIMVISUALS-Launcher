import {app,safeStorage} from 'electron';
app.disableHardwareAcceleration();
app.whenReady().then(()=>{
 try {
  if(process.platform!=='win32')throw Error('Windows-specific DPAPI test');
  if(!safeStorage.isEncryptionAvailable())throw Error('Windows encrypted storage unavailable');
  const sentinel='RIMVISUALS-NONSECRET-TEST-SENTINEL';
  const bytes=safeStorage.encryptString(sentinel);
  if(bytes.includes(Buffer.from(sentinel))||safeStorage.decryptString(bytes)!==sentinel)throw Error('Encrypted storage round-trip failed');
  console.log('RIM_STORAGE_OK: native Windows safeStorage encrypt/decrypt verified; no account credentials used');
  app.exit(0);
 }catch(error){console.error(error.message);app.exit(1);}
});
