const {test}=require('node:test');const assert=require('node:assert/strict');const path=require('node:path');const {inside,allowed,args,download,launch}=require('../electron/client-core.cjs');
test('installer rejects path traversal',()=>{assert.throws(()=>inside(path.resolve('root'),'../.minecraft/options.txt'));assert.equal(inside(path.resolve('root'),'assets/a'),path.resolve('root/assets/a'))});
test('Windows library rules exclude OSX and Linux',()=>{assert.equal(allowed([{action:'allow',os:{name:'windows'}}]),true);assert.equal(allowed([{action:'allow',os:{name:'osx'}}]),false);assert.equal(allowed([{action:'allow'},{action:'disallow',os:{name:'windows'}}]),false)});
test('unsupported feature arguments are excluded',()=>{assert.equal(allowed([{action:'allow',features:{is_demo_user:true}}]),false);assert.equal(allowed([{action:'allow',features:{is_demo_user:false}}]),true)});
test('argument substitution preserves spaces as one argument',()=>{assert.deepEqual(args(['-cp','${classpath}'],{classpath:'C:/a b/a.jar'}),['-cp','C:/a b/a.jar']);assert.throws(()=>args(['${unknown}'],{}))});
test('downloads reject untrusted hosts before network access',async()=>{await assert.rejects(download('https://example.com/file','x','a'.repeat(40)),/Untrusted/)});
test('direct launch cannot bypass sign-in',async()=>{await assert.rejects(launch('x',{},null),/Microsoft/)});
test('unregistered client ID fails before network access',async()=>{const {Auth}=require('../electron/auth.cjs');const a=new Auth('x',{});await assert.rejects(a.start(''),/Client ID/)});
