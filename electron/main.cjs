const {app,BrowserWindow,ipcMain,shell,safeStorage,dialog}=require('electron');
const fs=require('node:fs'),path=require('node:path');
if(!app.requestSingleInstanceLock()){app.quit();}else{
app.on('second-instance',()=>{const w=BrowserWindow.getAllWindows()[0];if(w){if(w.isMinimized())w.restore();w.focus();}});
const base=path.join(app.getPath('appData'),'RIMVISUALS');
app.setPath('userData',path.join(base,'launcher'));
const workspaces=new (require('./workspaces.cjs').Workspaces)(base);
const accounts=new (require('./accounts.cjs').Accounts)(base,safeStorage,url=>shell.openExternal(url),undefined,id=>workspaces.bindAccount(id));
const auth=accounts.auth;
let coordinator;
const get=()=>workspaces.get();
const save=v=>{coordinator.guard();return workspaces.save(v);};
const resources=()=>app.isPackaged?path.join(process.resourcesPath,'bundled'):path.join(__dirname,'../bundled');
function detect(){const p=get().minecraftPath;let versions=[];try{versions=fs.readdirSync(path.join(p,'versions'),{withFileTypes:true}).filter(x=>x.isDirectory()).map(x=>x.name)}catch{}let modFiles=[];try{modFiles=fs.readdirSync(path.join(p,'mods')).filter(x=>/^rimvisuals.*\.jar$/i.test(x))}catch{}const fabricVersions=versions.filter(id=>{try{const j=JSON.parse(fs.readFileSync(path.join(p,'versions',id,id+'.json'),'utf8'));return (j.inheritsFrom==='1.21.11'||j.id==='1.21.11')&&j.libraries?.some(l=>/^net.fabricmc:fabric-loader:/.test(l.name))}catch{return false}});let verified=false;try{const managed=JSON.parse(fs.readFileSync(path.join(p,'launcher','managed-mods.json'),'utf8'));verified=managed.some(e=>/^rimvisuals-[A-Za-z0-9.+_-]+\.jar$/.test(e.name)&&require('crypto').createHash('sha256').update(fs.readFileSync(path.join(p,'mods',e.name))).digest('hex')===e.sha256)}catch{};return {exists:fs.existsSync(path.join(p,'versions')),path:p,fabricVersions,rimMod:modFiles.length>0,rimModVerified:verified,rimModFiles:modFiles}}

function win(){const w=new BrowserWindow({width:1240,height:810,minWidth:920,minHeight:620,backgroundColor:'#090915',autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.cjs'),contextIsolation:true,nodeIntegration:false,sandbox:true}});w.webContents.setWindowOpenHandler(()=>({action:'deny'}));w.webContents.on('will-navigate',e=>e.preventDefault());w.loadFile(path.join(__dirname,'../dist/index.html'));}
app.whenReady().then(async()=>{
 app.setAppUserModelId('xyz.rimvisuals.launcher');coordinator=new (require('./coordinator.cjs').Coordinator)(base,workspaces,accounts,resources());
 let migrationError='';try{await accounts.init();const current=workspaces.current();if(!current.accountId){const a=await accounts.selected();if(a)workspaces.bindAccount(a.id);}else await accounts.select(current.accountId);}catch(e){migrationError=e.message;}
 const state=async()=>({...await accounts.list().catch(e=>({selectedId:null,accounts:[],accountError:e.message})),profiles:workspaces.list(),selectedProfileId:workspaces.state.selectedId});
 const select=async id=>{coordinator.guard();workspaces.select(id);await accounts.select(workspaces.current().accountId);return get();};
 ipcMain.handle('get',get);ipcMain.handle('save',(_,v)=>save(v));ipcMain.handle('detect',detect);
 ipcMain.handle('profiles',()=>workspaces.list().map(p=>p.name));
 ipcMain.handle('loadProfile',(_,name)=>{const p=workspaces.list().find(p=>p.name===name);if(!p)throw Error('Профиль не найден');return select(p.id);});
 ipcMain.handle('workspace',state);
 ipcMain.handle('selectProfile',(_,id)=>select(id));
 ipcMain.handle('createProfile',async(_,name)=>{coordinator.guard();workspaces.create(name);await accounts.select(workspaces.current().accountId);return get();});
 ipcMain.handle('removeProfile',async(_,id)=>{coordinator.guard();workspaces.remove(id);await accounts.select(workspaces.current().accountId);return get();});
 ipcMain.handle('exportProfile',()=>workspaces.export());
 ipcMain.handle('importProfile',async(_,text)=>{coordinator.guard();workspaces.import(text);await accounts.select(null);return get();});
 ipcMain.handle('resetProfile',()=>{coordinator.guard();return workspaces.reset();});
 ipcMain.handle('addLocalAccount',async(_,label)=>{coordinator.guard();return accounts.local(label);});
 ipcMain.handle('selectAccount',async(_,id)=>{coordinator.guard();await accounts.select(id);workspaces.bindAccount(id);});
 ipcMain.handle('removeAccount',async(_,id)=>{coordinator.guard();await accounts.remove(id);workspaces.clearAccount(id);});
 ipcMain.handle('signIn',()=>{coordinator.guard();return auth.start(get().microsoftClientId);});
 ipcMain.handle('signOut',()=>{if(coordinator.busy)throw Error('Дождитесь завершения установки или игры');return auth.logout();});
 ipcMain.handle('chooseJava',async()=>{coordinator.guard();const result=await dialog.showOpenDialog({title:'Выберите Java 21 x64 (java.exe или javaw.exe)',properties:['openFile'],filters:[{name:'Java executable',extensions:['exe']}]});if(result.canceled)return null;const selected=result.filePaths[0];if(!/^javaw?\.exe$/i.test(path.basename(selected)))throw Error('Выберите java.exe или javaw.exe');return selected;});
 ipcMain.handle('choose',()=>{throw Error('Рабочие папки профилей управляются RIMVISUALS');});
 ipcMain.handle('diagnostics',()=>require('./diagnostics.cjs').diagnostics(workspaces.gameRoot(),resources(),get(),base));
 ipcMain.handle('clientStatus',async()=>({phase:coordinator.phase,busy:coordinator.busy,auth:{...auth.publicState,busy:auth.busy},root:workspaces.gameRoot(),migrationError,...await state()}));
 ipcMain.handle('prepare',()=>coordinator.prepare());ipcMain.handle('launch',()=>coordinator.launch());win();
});
app.on('window-all-closed',()=>app.quit());

}
