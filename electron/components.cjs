const fs=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto');
const {inside}=require('./client-core.cjs');
const digest=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
async function exists(p){try{await fs.access(p);return true}catch{return false}}
async function installBundled(root,resources){
 const manifest=JSON.parse(await fs.readFile(path.join(resources,'manifest.json'),'utf8'));
 if(!Array.isArray(manifest.files)||manifest.files.length!==2||new Set(manifest.files.map(x=>x.name)).size!==2)throw Error('Invalid bundled component list');
 const inputs=[];let engine=false,api=false;
 // Validate every bundled file and existing destination before making any changes.
 for(const entry of manifest.files){if(!/^[A-Za-z0-9.+_-]+\.jar$/.test(entry.name)||!(/^[a-f0-9]{64}$/).test(entry.sha256))throw Error('Invalid bundled manifest');const bytes=await fs.readFile(path.join(resources,entry.name));if(digest(bytes)!==entry.sha256)throw Error('Bundled JAR integrity failed');if(/^rimvisuals-/.test(entry.name))engine=true;else if(/^fabric-api-/.test(entry.name))api=true;else throw Error('Unexpected bundled component');const target=inside(root,'mods/'+entry.name);if(await exists(target)&&digest(await fs.readFile(target))!==entry.sha256)throw Error('Файл компонента изменён; сохранён без перезаписи: '+entry.name);inputs.push({entry,bytes,target,alreadyInstalled:await exists(target)});}
 if(!engine||!api)throw Error('Missing RIMVISUALS or Fabric API component');
 const managed=inside(root,'launcher/managed-mods.json');let previous=[];try{previous=JSON.parse(await fs.readFile(managed,'utf8'))}catch{}
 await fs.mkdir(inside(root,'mods'),{recursive:true});await fs.mkdir(path.dirname(managed),{recursive:true});
 for(const x of inputs){if(x.alreadyInstalled)continue;const temp=x.target+'.'+crypto.randomUUID()+'.tmp';await fs.writeFile(temp,x.bytes);await fs.rename(temp,x.target);}
 for(const old of previous){if(!/^[A-Za-z0-9.+_-]+\.jar$/.test(old.name)||manifest.files.some(x=>x.name===old.name))continue;const target=inside(root,'mods/'+old.name);if(await exists(target)&&digest(await fs.readFile(target))===old.sha256){const backup=inside(root,'launcher/component-backups/'+Date.now()+'-'+old.name);await fs.mkdir(path.dirname(backup),{recursive:true});await fs.copyFile(target,backup);await fs.unlink(target);}}
 const temp=managed+'.tmp';await fs.writeFile(temp,JSON.stringify(manifest.files));await fs.rename(temp,managed);return manifest.files;
}
module.exports={installBundled};
