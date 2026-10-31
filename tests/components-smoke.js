import {createRequire} from 'node:module';import fs from 'node:fs/promises';import path from 'node:path';import os from 'node:os';import assert from 'node:assert/strict';
const require=createRequire(import.meta.url);const {installBundled}=require('../electron/components.cjs');
const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-bundle-smoke-'));
try{
 const resources=path.resolve('bundled');const manifest=JSON.parse(await fs.readFile(path.join(resources,'manifest.json')));
 const files=await installBundled(root,resources);const engine=files.find(x=>x.id==='rimvisuals');assert.equal(engine.version,'0.3.0-integration.2');
 const Zip=require('adm-zip');const jar=new Zip(await fs.readFile(path.join(root,'mods',engine.name)));
 for(const cls of ['CrystalFade','HitParticles','PearlTrails','ProjectileTrails','SwordTrails','ArmorHud','TargetHud'])assert(jar.getEntry('xyz/rimvisuals/modules/'+cls+'.class'));
 const config=path.join(root,'config','rimvisuals','config.properties');await fs.mkdir(path.dirname(config),{recursive:true});await fs.writeFile(config,'crystal_fade.enabled=true\npearl_trails.enabled=true\n');
 const before=(await fs.stat(path.join(root,'mods',engine.name))).mtimeMs;await installBundled(root,resources);assert.equal((await fs.stat(path.join(root,'mods',engine.name))).mtimeMs,before);assert.match(await fs.readFile(config,'utf8'),/crystal_fade.enabled=true/);
 assert.equal(manifest.launcherConfigSchema,1);console.log('RIM_COMPONENTS_OK: current JAR metadata/classes, repeat installation and existing config preservation verified');
}finally{await fs.rm(root,{recursive:true,force:true});}
