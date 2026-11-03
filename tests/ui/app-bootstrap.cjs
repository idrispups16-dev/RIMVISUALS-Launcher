// Test-only Electron entry point. Production main has no test root override.
const {app}=require('electron'),fs=require('node:fs'),path=require('node:path'),os=require('node:os');
const root=process.env.RIM_UI_SMOKE_ROOT;
if(!root||!path.resolve(root).startsWith(path.resolve(os.tmpdir())+path.sep)||!path.basename(root).startsWith('rim-ui-smoke-'))throw Error('UI smoke must use its isolated temporary directory');
fs.mkdirSync(root,{recursive:true});app.setPath('appData',root);require('../../electron/main.cjs');
