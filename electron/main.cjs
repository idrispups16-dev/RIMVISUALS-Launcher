const {app,BrowserWindow,ipcMain,shell,safeStorage,dialog,session}=require('electron');
const fs=require('node:fs'),path=require('node:path');
if(!app.requestSingleInstanceLock()){app.quit();}else{
app.on('second-instance',()=>{const w=BrowserWindow.getAllWindows()[0];if(w){if(w.isMinimized())w.restore();w.focus();}});
const base=path.join(app.getPath('appData'),'RIMVISUALS');
app.setPath('userData',path.join(base,'launcher'));
const workspaces=new (require('./workspaces.cjs').Workspaces)(base);
const accounts=new (require('./accounts.cjs').Accounts)(base,safeStorage,url=>shell.openExternal(require('./security.cjs').microsoftBrowserUrl(url)),undefined,id=>workspaces.bindAccount(id));
const auth=accounts.auth;
let coordinator,mainWindow;
const index=path.join(__dirname,'../dist/index.html');
const handle=require('./security.cjs').registerIpc(ipcMain,()=>mainWindow?.webContents,index);
const get=()=>workspaces.get();
const save=v=>{coordinator.guard();return workspaces.save(v);};
const resources=()=>app.isPackaged?path.join(process.resourcesPath,'bundled'):path.join(__dirname,'../bundled');
function detect(){const p=get().minecraftPath;let versions=[];try{versions=fs.readdirSync(path.join(p,'versions'),{withFileTypes:true}).filter(x=>x.isDirectory()).map(x=>x.name)}catch{}let modFiles=[];try{modFiles=fs.readdirSync(path.join(p,'mods')).filter(x=>/^rimvisuals.*\.jar$/i.test(x))}catch{}const fabricVersions=versions.filter(id=>{try{const j=JSON.parse(fs.readFileSync(path.join(p,'versions',id,id+'.json'),'utf8'));return (j.inheritsFrom===get().minecraftVersion||j.id===get().minecraftVersion)&&j.libraries?.some(l=>/^net.fabricmc:fabric-loader:/.test(l.name))}catch{return false}});let verified=false;try{const managed=JSON.parse(fs.readFileSync(path.join(p,'launcher','managed-mods.json'),'utf8'));verified=managed.some(e=>/^rimvisuals-[A-Za-z0-9.+_-]+\.jar$/.test(e.name)&&require('crypto').createHash('sha256').update(fs.readFileSync(path.join(p,'mods',e.name))).digest('hex')===e.sha256)}catch{};return {exists:fs.existsSync(path.join(p,'versions')),path:p,fabricVersions,rimMod:modFiles.length>0,rimModVerified:verified,rimModFiles:modFiles}}

function win(){const w=mainWindow=new BrowserWindow({width:1240,height:810,minWidth:920,minHeight:620,backgroundColor:'#090915',autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.cjs'),contextIsolation:true,nodeIntegration:false,sandbox:true}});w.webContents.setWindowOpenHandler(()=>({action:'deny'}));w.webContents.on('will-navigate',e=>e.preventDefault());w.webContents.on('will-attach-webview',e=>e.preventDefault());w.loadFile(index);}
app.whenReady().then(async()=>{
 require('./security.cjs').secureSession(session.defaultSession);app.setAppUserModelId('xyz.rimvisuals.launcher');coordinator=new (require('./coordinator.cjs').Coordinator)(base,workspaces,accounts,resources());
 let migrationError='';try{await accounts.init();const current=workspaces.current();if(!current.accountId){const a=await accounts.selected();if(a)workspaces.bindAccount(a.id);}else await accounts.select(current.accountId);}catch(e){migrationError=e.message;}
 const state=async()=>({...await accounts.list().catch(e=>({selectedId:null,accounts:[],accountError:e.message})),profiles:workspaces.list(),selectedProfileId:workspaces.state.selectedId});
 const select=async id=>{coordinator.guard();workspaces.select(id);await accounts.select(workspaces.current().accountId);return get();};
 handle('get',get);handle('save',(_,v)=>save(v));handle('detect',detect);
 handle('profiles',()=>workspaces.list().map(p=>p.name));
 handle('loadProfile',(_,name)=>{const p=workspaces.list().find(p=>p.name===name);if(!p)throw Error('Профиль не найден');return select(p.id);});
 handle('workspace',state);
 handle('mods',()=>require('./mod-manager.cjs').list(workspaces.gameRoot()));
 handle('toggleMod',(_,name)=>{coordinator.guard();return require('./mod-manager.cjs').toggle(workspaces.gameRoot(),name);});
 handle('importMod',async()=>{coordinator.guard();if(get().loader==='vanilla')throw Error('Моды требуют Fabric-профиль');const root=workspaces.gameRoot();const r=await dialog.showOpenDialog({properties:['openFile'],filters:[{name:'Fabric Mod',extensions:['jar']}]});coordinator.guard();if(workspaces.gameRoot()!==root)throw Error('Профиль изменился; выберите JAR повторно');if(!r.canceled)return require('./mod-manager.cjs').install(root,r.filePaths[0],{allowFabricApi:get().rimVisuals===false});});
 handle('versionCatalog',()=>require('./catalog.cjs').catalog());
 handle('selectProfile',(_,id)=>select(id));
 handle('createProfile',async(_,name,spec)=>{coordinator.guard();workspaces.create(name,spec);await accounts.select(workspaces.current().accountId);return get();});
 handle('removeProfile',async(_,id)=>{coordinator.guard();workspaces.remove(id);await accounts.select(workspaces.current().accountId);return get();});
 handle('exportProfile',()=>workspaces.export());
 handle('importProfile',async(_,text)=>{coordinator.guard();workspaces.import(text);await accounts.select(null);return get();});
 handle('resetProfile',()=>{coordinator.guard();return workspaces.reset();});
 handle('addLocalAccount',async(_,label)=>{coordinator.guard();return accounts.local(label);});
 handle('selectAccount',async(_,id)=>{coordinator.guard();await accounts.select(id);workspaces.bindAccount(id);});
 handle('removeAccount',async(_,id)=>{coordinator.guard();await accounts.remove(id);workspaces.clearAccount(id);});
 handle('signIn',()=>{coordinator.guard();return auth.start(get().microsoftClientId);});
 handle('signOut',()=>{if(coordinator.busy)throw Error('Дождитесь завершения установки или игры');return auth.logout();});
 handle('chooseJava',async()=>{coordinator.guard();const result=await dialog.showOpenDialog({title:'Выберите Java 21 x64 (java.exe или javaw.exe)',properties:['openFile'],filters:[{name:'Java executable',extensions:['exe']}]});if(result.canceled)return null;const selected=result.filePaths[0];if(!/^javaw?\.exe$/i.test(path.basename(selected)))throw Error('Выберите java.exe или javaw.exe');return selected;});
 handle('choose',()=>{throw Error('Рабочие папки профилей управляются RIMVISUALS');});
 handle('diagnostics',()=>require('./diagnostics.cjs').diagnostics(workspaces.gameRoot(),resources(),get(),base));
 handle('clientStatus',async()=>({phase:coordinator.phase,busy:coordinator.busy,auth:{...auth.publicState,busy:auth.busy},root:workspaces.gameRoot(),migrationError,...await state()}));
 handle('prepare',()=>coordinator.prepare());handle('launch',()=>coordinator.launch());win();
});
app.on('window-all-closed',()=>app.quit());

}
