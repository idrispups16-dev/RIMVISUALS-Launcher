import { createRequire } from 'node:module';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';
const require=createRequire(import.meta.url);
const {ensureJava}=require('../electron/java-runtime.cjs');
const root=await fs.mkdtemp(path.join(os.tmpdir(),'rim-java-smoke-'));
try {
  const exe=await ensureJava(root,console.log);
  assert(exe.startsWith(root+path.sep));
  assert.equal(await ensureJava(root,console.log,{fetch:()=>{throw Error('Reuse unexpectedly downloaded');}}),exe);
  console.log('RIM_JAVA_OK: downloaded, SHA-256 verified, extracted, executed Java 21 x64 and reused');
} finally {await fs.rm(root,{recursive:true,force:true});}
