const fs = require('node:fs');
const path = require('node:path');
const {spawn, execFile} = require('node:child_process');
const {promisify} = require('node:util');
const exec = promisify(execFile);
async function discover(env = process.env) {
  const candidates = [
    path.join(env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env.ProgramFiles || 'C:\\Program Files', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env.LOCALAPPDATA || '', 'Programs', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env.LOCALAPPDATA || '', 'Minecraft Launcher', 'MinecraftLauncher.exe')
  ];
  const found = candidates.filter(p => fs.existsSync(p)).map(target => ({kind:'exe',target}));
  try {
    const ps = path.join(env.SystemRoot || 'C:\\Windows','System32','WindowsPowerShell','v1.0','powershell.exe');
    const script = "$ErrorActionPreference='Stop'; @(Get-StartApps | Where-Object { $_.Name -eq 'Minecraft Launcher' -and $_.AppID -match '^Microsoft\\.4297127D64EC6_.*!' } | Select-Object -ExpandProperty AppID) | ConvertTo-Json -Compress";
    const {stdout} = await exec(ps, ['-NoLogo','-NoProfile','-NonInteractive','-Command',script], {windowsHide:true,timeout:12000});
    const ids = JSON.parse(stdout.trim() || '[]');
    for(const id of (Array.isArray(ids) ? ids : [ids])) if(/^Microsoft\.4297127D64EC6_[\w]+![\w.]+$/.test(id)) found.push({kind:'store',target:id});
  } catch (error) { return {found,diagnostic:'Store discovery: '+error.message}; }
  return {found};
}
function start(exe,args=[]) {
  return new Promise((resolve,reject)=>{
    const child=spawn(exe,args,{detached:true,stdio:'ignore',windowsHide:false});
    child.once('error',reject);child.once('spawn',()=>{child.unref();resolve();});
  });
}
async function launch() {
  if(process.platform!=='win32') return {ok:false,error:'Запуск поддерживается на Windows.'};
  const {found,diagnostic}=await discover();const errors=[];
  for(const item of found) try {
    if(item.kind==='exe') await start(item.target);
    else await start(path.join(process.env.SystemRoot || 'C:\\Windows','explorer.exe'),['shell:AppsFolder\\'+item.target]);
    return {ok:true,detail:'Запрос открытия официального Minecraft Launcher отправлен ('+(item.kind==='store'?'Microsoft Store':'EXE')+'). Войди в Microsoft, выбери Fabric 1.21.11 и нажми «Играть» в нём. Игра ещё не запущена.'};
  } catch(e) {errors.push(e.message);}
  return {ok:false,error:'Не удалось открыть официальный Minecraft Launcher. Установи или восстанови его через Microsoft Store / minecraft.net, затем повтори. '+[...errors,diagnostic].filter(Boolean).join('; ')};
}
module.exports={discover,launch};
