import {createRequire} from 'node:module';const require=createRequire(import.meta.url);
const fs=require('node:fs/promises'),os=require('node:os'),path=require('node:path'),assert=require('node:assert/strict');
const {Workspaces}=require('../electron/workspaces.cjs'),{Accounts}=require('../electron/accounts.cjs'),{Coordinator}=require('../electron/coordinator.cjs');
const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-install-'));
try{
 const w=new Workspaces(root);w.create('CI isolated profile');const gameRoot=w.gameRoot();
 const a=new Accounts(root,{isEncryptionAvailable:()=>false},()=>{throw Error('Browser must not open during local preflight')},()=>{throw Error('OAuth must not run during local preflight')});await a.init();const local=await a.local('CI local check');w.bindAccount(local.id);
 const c=new Coordinator(root,w,a,path.resolve('bundled'));const result=await c.prepare();assert.equal(result.ok,true,result.error);console.log(result.detail);
 const state=JSON.parse(await fs.readFile(path.join(gameRoot,'installation.json'),'utf8'));assert.equal(state.fabric.mainClass,'net.fabricmc.loader.impl.launch.knot.KnotClient');assert(state.classpath.length>10);assert(state.classpath.every(p=>p.startsWith(gameRoot+path.sep)));
 const bundled=JSON.parse(await fs.readFile(path.join(gameRoot,'launcher','managed-mods.json')));assert.equal(bundled.length,2);await fs.access(path.join(root,'runtime','java-21','bin','java.exe'));
 const denied=await c.launch();assert.equal(denied.ok,false);assert.match(denied.error,/PLAY требует Microsoft/);assert.equal(c.busy,false);
 const restarted=new Workspaces(root);assert.equal(restarted.gameRoot(),gameRoot);assert.equal(restarted.current().accountId,local.id);
 console.log('RIM_INSTALL_OK',state.classpath.length,'classpath entries; 2 verified bundled JARs; isolated profile; shared Java; local preflight; unlicensed PLAY denied before spawn. No authenticated game launch claimed.');
}finally{await fs.rm(root,{recursive:true,force:true,maxRetries:5,retryDelay:200});}
