const fs=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto');
const {validClientId}=require('./pkce.cjs');
async function diagnostics(root,resources,settings={},runtimeRoot=root){
 const report={schemaVersion:1,createdAt:new Date().toISOString(),launcherVersion:require('../package.json').version,minecraftVersion:settings.minecraftVersion||'1.21.11',loader:settings.loader||'fabric',fabricLoaderVersion:settings.loader==='vanilla'?null:'0.19.3',rimVisuals:settings.rimVisuals!==false,platform:process.platform,architecture:process.arch,ramGB:Number.isFinite(settings.ram)?settings.ram:null,javaMode:settings.javaPath?'custom':'managed',clientIdConfigured:validClientId(settings.microsoftClientId),components:[]};
 // Intentionally never read account.enc, profiles, launch logs or arbitrary user paths.
 try{await fs.access(path.join(runtimeRoot,'runtime','java-21','bin','java.exe'));report.managedJavaPresent=true;}catch{report.managedJavaPresent=false;}
 try{
  const m=JSON.parse(await fs.readFile(path.join(resources,'manifest.json'),'utf8'));
  if(m.schemaVersion!==2||!Array.isArray(m.files)||m.files.length!==2)throw Error('manifest');
  for(const e of m.files){if(!/^[A-Za-z0-9.+_-]+\.jar$/.test(e.name)||!['rimvisuals','fabric-api'].includes(e.id)||!/[0-9]/.test(e.version)||!/^[A-Za-z0-9.+_-]{1,80}$/.test(e.version)||!/^[a-f0-9]{64}$/.test(e.sha256))throw Error('manifest');}
  for(const e of m.files){let state='missing';try{const bytes=await fs.readFile(path.join(root,'mods',e.name));state=crypto.createHash('sha256').update(bytes).digest('hex')===e.sha256?'verified':'modified';}catch(error){if(error.code!=='ENOENT')state='unreadable';}report.components.push({id:e.id,version:e.version,state});}
 }catch{report.components=[];report.bundleError='invalid-or-missing-manifest';}
 const logDir=path.join(root,'logs');await fs.mkdir(logDir,{recursive:true});const file=path.join(logDir,'diagnostics.json');const temp=file+'.'+crypto.randomUUID()+'.tmp';await fs.writeFile(temp,JSON.stringify(report,null,2),{mode:0o600});await fs.rename(temp,file);
 return {path:file,detail:'Minecraft '+report.minecraftVersion+' / Fabric '+report.fabricLoaderVersion+'; '+(report.bundleError?'манифест компонентов недоступен':report.components.map(e=>e.id+' '+e.version+': '+({verified:'проверен',missing:'не установлен',modified:'изменён',unreadable:'ошибка чтения'}[e.state])).join('; ')),report};
}
module.exports={diagnostics};
