import {createRequire} from 'node:module';import fs from 'node:fs/promises';import path from 'node:path';import os from 'node:os';import assert from 'node:assert/strict';
const require=createRequire(import.meta.url);const {installBundled}=require('../electron/components.cjs');
const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-bundle-smoke-'));
try{
 const resources=path.resolve('bundled');const manifest=JSON.parse(await fs.readFile(path.join(resources,'manifest.json')));
 const files=await installBundled(root,resources);const engine=files.find(x=>x.id==='rimvisuals');assert.equal(engine.version,'0.3.0-integration.2');
 const Zip=require('adm-zip');const jar=new Zip(await fs.readFile(path.join(root,'mods',engine.name)));
 for(const cls of ['CrystalFade','HitParticles','PearlTrails','ProjectileTrails','SwordTrails','ArmorHud','TargetHud','HitMarker','KillEffects','DamageTint','HurtCamera','HandColor','CustomFog','CustomSky','CpsHud','KeystrokesHud','PotionHud','SpeedHud','SessionHud'])assert(jar.getEntry('xyz/rimvisuals/modules/'+cls+'.class'));
 const config=path.join(root,'config','rimvisuals','config.properties');await fs.mkdir(path.dirname(config),{recursive:true});await fs.writeFile(config,'crystal_fade.enabled=true\npearl_trails.enabled=true\n');
 const before=(await fs.stat(path.join(root,'mods',engine.name))).mtimeMs;await installBundled(root,resources);assert.equal((await fs.stat(path.join(root,'mods',engine.name))).mtimeMs,before);assert.match(await fs.readFile(config,'utf8'),/crystal_fade.enabled=true/);
 // Exercise a real component install over a synthetic, hash-owned previous same-version filename.
 const upgrade=path.join(root,'upgrade'),oldName='rimvisuals-0.3.0-integration.2.jar';assert.notEqual(engine.name,oldName,'Updated bytes must not collide with the previous bundled filename');
 const previousZip=new Zip();previousZip.addFile('fabric.mod.json',Buffer.from(JSON.stringify({id:'rimvisuals',version:engine.version,depends:{minecraft:'1.21.11'}})));previousZip.addFile('previous-build-fixture',Buffer.from('previous'));const oldBytes=previousZip.toBuffer();
 await fs.mkdir(path.join(upgrade,'mods'),{recursive:true});await fs.mkdir(path.join(upgrade,'launcher'),{recursive:true});await fs.mkdir(path.join(upgrade,'saves','existing'),{recursive:true});await fs.mkdir(path.join(upgrade,'config'),{recursive:true});
 await fs.writeFile(path.join(upgrade,'mods',oldName),oldBytes);await fs.writeFile(path.join(upgrade,'mods','user-mod.jar'),'preserve-user-mod');await fs.writeFile(path.join(upgrade,'saves','existing','level.dat'),'preserve-world');await fs.writeFile(path.join(upgrade,'config','user.properties'),'preserve-config');
 await fs.writeFile(path.join(upgrade,'launcher','managed-mods.json'),JSON.stringify([{name:oldName,sha256:require('node:crypto').createHash('sha256').update(oldBytes).digest('hex'),id:'rimvisuals',version:engine.version}]));
 await installBundled(upgrade,resources);assert.equal(await fs.stat(path.join(upgrade,'mods',oldName)).then(()=>true,()=>false),false);assert.deepEqual(await fs.readFile(path.join(upgrade,'mods',engine.name)),await fs.readFile(path.join(resources,engine.name)));
 const backups=await fs.readdir(path.join(upgrade,'launcher','component-backups'));assert.equal(backups.length,1);assert.deepEqual(await fs.readFile(path.join(upgrade,'launcher','component-backups',backups[0])),oldBytes);
 assert.equal(await fs.readFile(path.join(upgrade,'mods','user-mod.jar'),'utf8'),'preserve-user-mod');assert.equal(await fs.readFile(path.join(upgrade,'saves','existing','level.dat'),'utf8'),'preserve-world');assert.equal(await fs.readFile(path.join(upgrade,'config','user.properties'),'utf8'),'preserve-config');
 console.log('RIM_BUNDLE_UPGRADE_OK: same-version owned fixture backed up; new tested bytes installed; unrelated mod/config/world preserved');
 assert.equal(manifest.launcherConfigSchema,1);console.log('RIM_COMPONENTS_OK: current JAR metadata/classes, repeat installation and existing config preservation verified');
}finally{await fs.rm(root,{recursive:true,force:true});}
