const {_electron}=require('playwright-core');const fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
(async()=>{
 const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-ui-smoke-'));let app;
 try{
  const base=path.join(root,'RIMVISUALS');await fs.mkdir(path.join(base,'saves'),{recursive:true});await fs.writeFile(path.join(base,'saves','existing-world'),'preserve');
  app=await _electron.launch({args:[path.resolve('tests/ui/app-bootstrap.cjs')],env:{...process.env,RIM_UI_SMOKE_ROOT:root},timeout:60000});
  app.process().stderr.on('data',data=>{const text=data.toString();if(/Error|ERR_FILE|crash/i.test(text))console.error(text);});const page=await app.firstWindow();const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.setViewportSize({width:1240,height:810});
  await page.getByRole('heading',{name:'Аккаунты',exact:true}).waitFor();await page.waitForFunction(async()=>!!(await window.rim.get()).minecraftPath);
  await page.getByRole('button',{name:'Добавить Microsoft-аккаунт',exact:true}).click();await page.waitForFunction(()=>document.querySelector('.toast')?.textContent.includes('Client ID'));
  await page.getByLabel('Имя локальной записи',{exact:true}).fill('UI Local');await page.getByRole('button',{name:'Добавить локальную запись',exact:true}).click();
  await page.waitForFunction(async()=>(await window.rim.clientStatus()).selectedId?.startsWith('local-'));
  assert(await page.getByRole('button',{name:'PLAY RIMVISUALS'}).isDisabled());await page.waitForFunction(()=>!document.querySelector('.toast')?.textContent.includes('Client ID'));
  await page.getByRole('button',{name:'Профили',exact:true}).click();await page.getByLabel('Новый профиль',{exact:true}).fill('UI isolated');await page.getByRole('button',{name:'Создать отдельный профиль',exact:true}).click();
  await page.waitForFunction(async()=>(await window.rim.get()).profile==='UI isolated');const first=await page.evaluate(()=>window.rim.clientStatus());assert(first.root.startsWith(path.join(base,'instances')+path.sep));
  await page.getByText('Импорт и экспорт настроек профиля',{exact:true}).click();await page.getByRole('button',{name:'Экспорт в поле',exact:true}).click();await page.waitForFunction(()=>document.querySelector('textarea')?.value.includes('UI isolated'));
  const exported=await page.getByRole('textbox',{name:'JSON профиля'}).inputValue();const profile=JSON.parse(exported);assert.equal(profile.name,'UI isolated');assert(!exported.includes('accountId')&&!exported.includes('microsoftClientId')&&!exported.includes('javaPath'));
  profile.name='UI imported';await page.getByRole('textbox',{name:'JSON профиля'}).fill(JSON.stringify(profile));await page.getByRole('button',{name:'Импорт как новый профиль',exact:true}).click();await page.waitForFunction(async()=>(await window.rim.get()).profile==='UI imported');
  const imported=await page.evaluate(()=>window.rim.clientStatus());assert.notEqual(imported.root,first.root);assert.equal(imported.selectedId,null);
  await fs.mkdir('test-output',{recursive:true});await page.screenshot({path:'test-output/accounts-profiles.png',fullPage:true});
  await page.setViewportSize({width:920,height:620});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false);
  await page.getByRole('button',{name:'Убрать профиль из списка',exact:true}).click();await page.waitForFunction(async()=>(await window.rim.get()).profile==='RIMVISUALS');await fs.access(imported.root);assert.equal(await fs.readFile(path.join(base,'saves','existing-world'),'utf8'),'preserve');
  assert.deepEqual(errors,[]);console.log('RIM_UI_OK: real Electron IPC, Client ID error, local account, PLAY gate, isolated profile, import/export, deletion preserving files, 1240/920 layouts');
 }finally{if(app)await app.close();await fs.rm(root,{recursive:true,force:true,maxRetries:5,retryDelay:200});}
})().catch(e=>{console.error(e);process.exitCode=1});
