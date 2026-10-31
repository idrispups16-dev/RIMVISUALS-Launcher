const {installBundled}=require('./components.cjs'),core=require('./client-core.cjs'),{ensureJava}=require('./java-runtime.cjs');
class Coordinator {
 constructor(root,workspaces,accounts,resources,deps={}){this.root=root;this.workspaces=workspaces;this.accounts=accounts;this.resources=resources;this.deps={installBundled,core,ensureJava,...deps};this.busy=false;this.phase='Готов к настройке';}
 guard(){if(this.busy||this.accounts.auth.busy)throw Error('Дождитесь завершения входа, установки или игры');}
 report(text){this.phase=text;if(text.startsWith('Minecraft завершён'))this.busy=false;}
 async run(play){this.guard();this.busy=true;const settings=this.workspaces.get(),gameRoot=this.workspaces.gameRoot();try{
  // Only the licensed Microsoft provider may return a session for a game launch.
  const session=play?await this.accounts.session(settings.microsoftClientId):null;
  this.phase='Проверка встроенных визуалов';await this.deps.installBundled(gameRoot,this.resources);this.workspaces.bridge();
  const java=settings.javaPath||await this.deps.ensureJava(this.root,t=>this.report(t));
  if(play){const result=await this.deps.core.launch(gameRoot,{...settings,javaPath:java},session,t=>this.report(t));return {ok:true,detail:'Java-процесс запущен (PID '+result.pid+'). Лог: '+result.log};}
  const {execFile}=require('node:child_process');const {promisify}=require('node:util');const path=require('node:path');let probe=java;if(/javaw\.exe$/i.test(java))probe=path.join(path.dirname(java),'java.exe');const out=await (this.deps.probe||promisify(execFile))(probe,['-XshowSettings:properties','-version'],{windowsHide:true,timeout:20000});if(!this.deps.core.validJavaOutput(out.stderr+' '+out.stdout))throw Error('Требуется Java 21 x64');
  const state=await this.deps.core.install(gameRoot,t=>this.report(t));this.phase='Файлы Minecraft, Fabric, Java и визуалы проверены';this.busy=false;return {ok:true,detail:this.phase+'. Библиотек: '+state.classpath.length+'. Игра не запускалась; Microsoft-вход для этой проверки не нужен.'};
 }catch(e){this.busy=false;this.phase='Ошибка: '+e.message;return {ok:false,error:e.message};}}
 prepare(){return this.run(false);}launch(){return this.run(true);}
}
module.exports={Coordinator};
