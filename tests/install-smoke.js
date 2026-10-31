import {createRequire} from 'node:module';const require=createRequire(import.meta.url);
const fs=require('node:fs/promises'),os=require('node:os'),path=require('node:path'),assert=require('node:assert/strict');
const {Workspaces}=require('../electron/workspaces.cjs'),{Accounts}=require('../electron/accounts.cjs'),{Coordinator}=require('../electron/coordinator.cjs');
const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-install-'));let demoPid;
try{
 const w=new Workspaces(root);w.create('CI isolated profile');const gameRoot=w.gameRoot();
 const a=new Accounts(root,{isEncryptionAvailable:()=>false},()=>{throw Error('Browser must not open during local preflight')},()=>{throw Error('OAuth must not run during local preflight')});await a.init();const local=await a.local('CI local check');w.bindAccount(local.id);
 const c=new Coordinator(root,w,a,path.resolve('bundled'));const result=await c.prepare();assert.equal(result.ok,true,result.error);console.log(result.detail);
 const state=JSON.parse(await fs.readFile(path.join(gameRoot,'installation.json'),'utf8'));assert.equal(state.fabric.mainClass,'net.fabricmc.loader.impl.launch.knot.KnotClient');assert(state.classpath.length>10);assert(state.classpath.every(p=>p.startsWith(gameRoot+path.sep)));
 const bundled=JSON.parse(await fs.readFile(path.join(gameRoot,'launcher','managed-mods.json')));assert.equal(bundled.length,2);await fs.access(path.join(root,'runtime','java-21','bin','java.exe'));
 const core=require('../electron/client-core.cjs');
 const plan=core.buildDemoPlan(gameRoot,state,w.get());assert(plan.game.includes('--demo'));assert(plan.game.includes('--disableMultiplayer'));assert.equal(plan.mainClass,'net.fabricmc.loader.impl.launch.knot.KnotClient');
 const originalFetch=global.fetch;global.fetch=()=>{throw Error('Offline cache unexpectedly requested network')};try{const cached=await core.installation(gameRoot);assert.equal(cached.classpath.length,state.classpath.length);}finally{global.fetch=originalFetch;}
 const java=path.join(root,'runtime','java-21','bin','java.exe');const {promisify}=require('node:util'),{execFile}=require('node:child_process');
 await promisify(execFile)(java,['--dry-run',...plan.jvm,plan.mainClass,...plan.game],{cwd:gameRoot,windowsHide:true,timeout:30000});
 await fs.mkdir('test-output',{recursive:true});
 const resultDemo=await c.launch();assert.equal(resultDemo.ok,true,resultDemo.error);demoPid=Number(resultDemo.detail.match(/PID (\d+)/)[1]);
 // Observe the real native client process; do not infer a loaded world from a PID.
 const logs=(await fs.readdir(path.join(gameRoot,'logs'))).filter(n=>/^launch-.*\.log$/.test(n)).sort();const file=path.join(gameRoot,'logs',logs.at(-1));
 let text='';for(let i=0;i<90;i++){await new Promise(r=>setTimeout(r,500));text=await fs.readFile(file,'utf8');if(/Exit [0-9-]+/.test(text))break;if(/OpenAL initialized|Created:.*atlas|Starting integrated minecraft server/i.test(text))break;}
 await fs.writeFile('test-output/demo-launch.log',text);
 const graphicsBlocked=/GLFW error 6554[23]|does not.*support OpenGL|driver does not appear to support OpenGL|Failed to create window/i.test(text);
 const fabricObserved=/Loading Minecraft 1\.21\.11 with Fabric Loader 0\.19\.3/.test(text),modObserved=/rimvisuals 0\.3\.0-integration\.2/.test(text);
 assert(fabricObserved,'Fabric Loader did not start; inspect demo-launch.log');assert(modObserved,'Bundled RIMVISUALS did not load');
 if((/Exit (?:-[0-9]+|[1-9][0-9]*)/.test(text)||/Unreported exception|Exception in thread/.test(text))&&!graphicsBlocked)throw Error('Demo client failed; inspect demo-launch.log');
 const ids=[...resultDemo.detail.matchAll(/PID (\d+)/g)];assert(ids.length===1);try{process.kill(Number(ids[0][1]));}catch{}
 await fs.writeFile('test-output/demo-process.json',JSON.stringify({dryRun:true,fabricObserved,modObserved,graphicsBlocked,worldConfirmed:false},null,2));
 console.log('RIM_DEMO_PROCESS_OK: Java dry-run and actual Fabric/RIMVISUALS process observed; graphicsBlocked='+graphicsBlocked+'; worldConfirmed=false');
 const restarted=new Workspaces(root);assert.equal(restarted.gameRoot(),gameRoot);assert.equal(restarted.current().accountId,local.id);
 for(let i=0;i<40&&c.busy;i++)await new Promise(r=>setTimeout(r,250));assert.equal(c.busy,false,'Demo process must close before changing runtime');
 // Verify actual official metadata, downloads, classpath and Java entrypoints for each added runtime.
 for(const spec of [{minecraftVersion:'1.21.4',loader:'vanilla',rimVisuals:false},{minecraftVersion:'1.21.10',loader:'fabric',rimVisuals:false}]){
  w.create('Matrix '+spec.minecraftVersion,spec);const result=await c.prepare();assert.equal(result.ok,true,result.error);const dir=w.gameRoot(),installed=await core.installation(dir,()=>{},w.get());assert.equal(installed.base.id,spec.minecraftVersion);assert.equal(installed.fabric.mainClass,spec.loader==='vanilla'?installed.base.mainClass:'net.fabricmc.loader.impl.launch.knot.KnotClient');const plan=core.buildDemoPlan(dir,installed,w.get());await promisify(execFile)(java,['--dry-run',...plan.jvm,plan.mainClass,...plan.game],{cwd:dir,windowsHide:true,timeout:30000});assert.equal(await fs.stat(path.join(dir,'launcher','managed-mods.json')).then(()=>true,()=>false),false);console.log('RIM_VERSION_MATRIX_OK',spec.minecraftVersion,spec.loader,'real files and Java dry-run; world NOT tested');
 }
 console.log('RIM_INSTALL_OK',state.classpath.length,'classpath entries; 2 verified bundled JARs; isolated profile; shared Java; offline cache; native Demo launch. No authenticated game launch claimed.');
}finally{if(demoPid){try{process.kill(demoPid);}catch{}await new Promise(r=>setTimeout(r,1000));}await fs.rm(root,{recursive:true,force:true,maxRetries:5,retryDelay:200});}
