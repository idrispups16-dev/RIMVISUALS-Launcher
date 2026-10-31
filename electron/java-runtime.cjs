'use strict';
// Automatic Java 21 x64 provisioning for the standalone RIMVISUALS launcher.
// Source: Eclipse Temurin JRE metadata from the Adoptium API. The archive is only
// accepted after its SHA-256 (from the same API) matches. Everything is installed
// inside <root>/runtime and never touches an existing Java or .minecraft.
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const exec = promisify(execFile);

const API_URL = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse';
const API_HOSTS = new Set(['api.adoptium.net']);
const DOWNLOAD_HOSTS = new Set(['github.com', 'objects.githubusercontent.com', 'release-assets.githubusercontent.com']);
const MAX_ARCHIVE_BYTES = 400 * 1024 * 1024;
const MAX_HOPS = 5;

function inside(root, relative) {
  if(typeof relative!=='string'||path.isAbsolute(relative)||path.win32.isAbsolute(relative)||relative.split(/[\\/]/).includes('..'))throw Error('Unsafe runtime path');
  const base = path.resolve(root);
  const p = path.resolve(base, relative);
  if (p !== base && !p.startsWith(base + path.sep)) throw Error('Unsafe runtime path');
  return require('./owned-path.cjs').ownedPath(root,p);
}

function isJava21x64(output) {
  return /(?:version|openjdk)\s+"?21[.\s"]/.test(output) && /sun\.arch\.data\.model\s*=\s*64/.test(output);
}

function checkUrl(url, hosts) {
  const u = new URL(url);
  if (u.protocol !== 'https:' || !hosts.has(u.hostname)) throw Error('Untrusted Java download host');
  return u;
}

// Picks one acceptable Windows x64 JRE .zip from an Adoptium "latest assets" response.
function pickRelease(list) {
  if (!Array.isArray(list)) throw Error('Unexpected Java release metadata');
  for (const item of list) {
    const b = item && item.binary;
    const pkg = b && b.package;
    if (!pkg || !b) continue;
    if (b.os !== 'windows' || b.architecture !== 'x64' || b.image_type !== 'jre') continue;
    if (!item.version || item.version.major !== 21) continue;
    if (typeof pkg.name !== 'string' || !/\.zip$/i.test(pkg.name) || /[\\/]/.test(pkg.name)) continue;
    if (typeof pkg.checksum !== 'string' || !/^[a-f0-9]{64}$/i.test(pkg.checksum)) continue;
    if (!Number.isInteger(pkg.size) || pkg.size <= 0 || pkg.size > MAX_ARCHIVE_BYTES) continue;
    try { checkUrl(pkg.link, DOWNLOAD_HOSTS); } catch { continue; }
    return { name: pkg.name, link: pkg.link, sha256: pkg.checksum.toLowerCase(), size: pkg.size, release: item.release_name || '' };
  }
  throw Error('No suitable Java 21 x64 runtime was published');
}

// Follows redirects by hand so every hop is checked against the host allow-list.
async function fetchChecked(url, hosts, fetchImpl, timeout) {
  let current = url;
  for (let hop = 0; hop <= MAX_HOPS; hop++) {
    checkUrl(current, hosts);
    const r = await fetchImpl(current, { redirect: 'manual', signal: AbortSignal.timeout(timeout) });
    if (r.status >= 300 && r.status < 400) {
      const location = r.headers.get('location');
      if (!location) throw Error('Redirect without location');
      current = new URL(location, current).href;
      continue;
    }
    if (!r.ok) throw Error('Java download HTTP ' + r.status);
    return r;
  }
  throw Error('Too many redirects while downloading Java');
}

async function readBounded(response, expected) {
  const declared=response.headers.get('content-length');
  if(declared && Number(declared)>expected)throw Error('Java archive size mismatch');
  if(!response.body){const bytes=Buffer.from(await response.arrayBuffer());if(bytes.length!==expected)throw Error('Java archive size mismatch');return bytes;}
  const chunks=[];let count=0;
  for await(const chunk of response.body){count+=chunk.length;if(count>expected||count>MAX_ARCHIVE_BYTES)throw Error('Java archive exceeds download limit');chunks.push(Buffer.from(chunk));}
  if(count!==expected)throw Error('Java archive size mismatch');return Buffer.concat(chunks,count);
}
async function extractZip(zipPath, dest) {
  const Zip = require('adm-zip');const entries=new Zip(zipPath).getEntries();
  if(entries.length>30000)throw Error('Too many Java archive entries');let total=0;
  for(const entry of entries){inside(dest,entry.entryName);if(((entry.attr>>>16)&0o170000)===0o120000)throw Error('Java archive symlink rejected');total+=entry.header.size;if(entry.header.size>256*1024*1024||total>1024*1024*1024)throw Error('Java extraction size limit');}
  for (const entry of entries) {
    const target = inside(dest, entry.entryName);
    if (entry.isDirectory) { await fs.mkdir(target, { recursive: true }); continue; }
    await fs.mkdir(path.dirname(target), { recursive: true });
    await fs.writeFile(target, entry.getData());
  }
}

async function findJavaExe(dir) {
  const direct = path.join(dir, 'bin', 'java.exe');
  try { await fs.access(direct); return direct; } catch { /* look one level down */ }
  let names;
  try { names = await fs.readdir(dir, { withFileTypes: true }); } catch { return null; }
  for (const n of names) {
    if (!n.isDirectory()) continue;
    const candidate = path.join(dir, n.name, 'bin', 'java.exe');
    try { await fs.access(candidate); return candidate; } catch { /* next */ }
  }
  return null;
}

async function probe(javaExe, run = exec) {
  try {
    const out = await run(javaExe, ['-XshowSettings:properties', '-version'], { windowsHide: true, timeout: 20000 });
    return isJava21x64(out.stderr + ' ' + out.stdout);
  } catch { return false; }
}

// Returns the path to a verified Java 21 x64 java.exe inside <root>/runtime/java-21,
// installing it first when missing or broken. Dependencies are injectable for tests.
async function ensureJava(root, report = () => {}, deps = {}) {
  const fetchImpl = deps.fetch || fetch;
  const extract = deps.extract || extractZip;
  const run = deps.exec || exec;
  const finalDir = inside(root, 'runtime/java-21');

  const existing = await findJavaExe(finalDir);
  if (existing && await probe(existing, run)) return existing;

  report('Java 21 не найдена — загружаю проверенную среду выполнения');
  const metadata=await fetchChecked(API_URL, API_HOSTS, fetchImpl, 45000);
  const meta=metadata.body?JSON.parse((await require('./bounded-response.cjs').readLimited(metadata,8*1024*1024)).toString('utf8')):await metadata.json();
  const release = pickRelease(meta);

  const response = await fetchChecked(release.link, DOWNLOAD_HOSTS, fetchImpl, 180000);
  const data = await readBounded(response,release.size);
  if (data.length !== release.size) throw Error('Java archive size mismatch');
  if (crypto.createHash('sha256').update(data).digest('hex') !== release.sha256) throw Error('Java integrity check failed');

  const work = inside(root, 'runtime/.install-' + crypto.randomUUID());
  try {
    await fs.mkdir(work, { recursive: true });
    const archive = path.join(work, release.name);
    await fs.writeFile(archive, data);
    report('Распаковка Java 21');
    const unpacked = path.join(work, 'jre');
    await fs.mkdir(unpacked, { recursive: true });
    await extract(archive, unpacked);
    const exe = await findJavaExe(unpacked);
    if (!exe || !(await probe(exe, run))) throw Error('Downloaded Java failed verification');
    const jreRoot = path.dirname(path.dirname(exe));
    await fs.writeFile(path.join(jreRoot, '.rimvisuals-runtime.json'), JSON.stringify({ release: release.release, sha256: release.sha256 }));
    const backup=inside(root,'runtime/.previous-'+crypto.randomUUID());let moved=false;
    await fs.mkdir(path.dirname(finalDir), { recursive: true });
    try{await fs.rename(finalDir,backup);moved=true;}catch(e){if(e.code!=='ENOENT')throw e;}
    try{await fs.rename(jreRoot,finalDir);}catch(e){if(moved)await fs.rename(backup,finalDir);throw e;}
    if(moved)await fs.rm(backup,{recursive:true,force:true});
  } finally {
    await fs.rm(work, { recursive: true, force: true });
  }
  const installed = await findJavaExe(finalDir);
  if (!installed) throw Error('Java installation incomplete');
  report('Java 21 установлена и проверена');
  return installed;
}

module.exports = { ensureJava, pickRelease, fetchChecked, isJava21x64, inside, readBounded, API_URL };
