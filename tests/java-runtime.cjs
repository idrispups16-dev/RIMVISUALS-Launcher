const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');
const { ensureJava, pickRelease, fetchChecked, isJava21x64, inside, API_URL } = require('../electron/java-runtime.cjs');

const GOOD_OUT = { stderr: 'openjdk version "21.0.5" 2024-10-15\n', stdout: 'sun.arch.data.model = 64\n' };
const archive = Buffer.from('fake-archive-bytes');
const sha = crypto.createHash('sha256').update(archive).digest('hex');
const item = (over = {}, pkgOver = {}) => ({
  release_name: 'jdk-21.0.5+11', version: { major: 21 },
  binary: { os: 'windows', architecture: 'x64', image_type: 'jre', package: { name: 'OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip', link: 'https://github.com/adoptium/temurin21-binaries/releases/download/x/jre.zip', checksum: sha, size: archive.length, ...pkgOver } },
  ...over,
});
const res = (status, body, headers = {}) => ({ status, ok: status >= 200 && status < 300, headers: { get: k => headers[k.toLowerCase()] ?? null }, json: async () => body, arrayBuffer: async () => body });

async function tmp() { return fs.mkdtemp(path.join(os.tmpdir(), 'rim-java-')); }

test('Java 21 x64 output is recognised and Java 17 or 32-bit rejected', () => {
  assert.equal(isJava21x64(GOOD_OUT.stderr + GOOD_OUT.stdout), true);
  assert.equal(isJava21x64('openjdk version "17.0.1"\nsun.arch.data.model = 64'), false);
  assert.equal(isJava21x64('openjdk version "21.0.5"\nsun.arch.data.model = 32'), false);
});

test('release picker accepts only a verified windows x64 jre zip', () => {
  assert.equal(pickRelease([item()]).sha256, sha);
  assert.throws(() => pickRelease([item({}, { checksum: 'zz' })]), /No suitable/);
  assert.throws(() => pickRelease([item({}, { link: 'https://evil.example/jre.zip' })]), /No suitable/);
  assert.throws(() => pickRelease([item({}, { link: 'http://github.com/jre.zip' })]), /No suitable/);
  assert.throws(() => pickRelease([item({}, { name: '../jre.zip' })]), /No suitable/);
  assert.throws(() => pickRelease([item({ version: { major: 17 } })]), /No suitable/);
  assert.throws(() => pickRelease([item({}, { size: 10 * 1024 * 1024 * 1024 })]), /No suitable/);
  assert.throws(() => pickRelease({}), /Unexpected/);
});

test('every redirect hop is host checked', async () => {
  const hosts = new Set(['github.com', 'objects.githubusercontent.com']);
  const ok = async url => url.startsWith('https://github.com/') ? res(302, null, { location: 'https://objects.githubusercontent.com/f' }) : res(200, 'done');
  assert.equal((await fetchChecked('https://github.com/a', hosts, ok, 1000)).status, 200);
  const bad = async () => res(302, null, { location: 'https://evil.example/f' });
  await assert.rejects(fetchChecked('https://github.com/a', hosts, bad, 1000), /Untrusted/);
  const loop = async () => res(302, null, { location: 'https://github.com/a' });
  await assert.rejects(fetchChecked('https://github.com/a', hosts, loop, 1000), /Too many/);
});

test('path containment protects the runtime directory', () => {
  assert.throws(() => inside('/root/r', '../x'));
  assert.throws(() => inside('/root/r', 'a/../../x'));
});

function makeDeps(over = {}) {
  const calls = { download: 0 };
  const fetchImpl = async url => {
    if (url === API_URL) return res(200, [item()]);
    calls.download++;
    return res(200, archive.buffer.slice(archive.byteOffset, archive.byteOffset + archive.length));
  };
  const extract = async (zip, dest) => { await fs.mkdir(path.join(dest, 'jdk-21.0.5+11-jre', 'bin'), { recursive: true }); await fs.writeFile(path.join(dest, 'jdk-21.0.5+11-jre', 'bin', 'java.exe'), 'x'); };
  const exec = async () => GOOD_OUT;
  return { calls, deps: { fetch: fetchImpl, extract, exec, ...over } };
}

test('ensureJava downloads, verifies, installs and then reuses the runtime', async () => {
  const root = await tmp();
  const { calls, deps } = makeDeps();
  const messages = [];
  const exe = await ensureJava(root, m => messages.push(m), deps);
  assert.equal(exe, path.join(root, 'runtime', 'java-21', 'bin', 'java.exe'));
  assert.equal(calls.download, 1);
  const again = await ensureJava(root, () => {}, deps);
  assert.equal(again, exe);
  assert.equal(calls.download, 1, 'second call must not download again');
  assert.ok(messages.length >= 2);
  assert.deepEqual((await fs.readdir(path.join(root, 'runtime'))).sort(), ['java-21'], 'temporary install folder removed');
});

test('ensureJava rejects a corrupted archive before extracting', async () => {
  const root = await tmp();
  let extracted = false;
  const { deps } = makeDeps({ extract: async () => { extracted = true; } });
  const base = deps.fetch;
  deps.fetch = async url => url === API_URL ? base(url) : res(200, Buffer.from('tampered-archive').buffer);
  await assert.rejects(ensureJava(root, () => {}, deps), /size mismatch|integrity/);
  assert.equal(extracted, false);
});

test('ensureJava rejects a runtime that is not Java 21 x64 and installs nothing', async () => {
  const root = await tmp();
  const { deps } = makeDeps({ exec: async () => ({ stderr: 'openjdk version "17.0.1"', stdout: 'sun.arch.data.model = 64' }) });
  await assert.rejects(ensureJava(root, () => {}, deps), /failed verification/);
  await assert.rejects(fs.access(path.join(root, 'runtime', 'java-21')));
});

test('a broken existing runtime is replaced', async () => {
  const root = await tmp();
  await fs.mkdir(path.join(root, 'runtime', 'java-21', 'bin'), { recursive: true });
  await fs.writeFile(path.join(root, 'runtime', 'java-21', 'bin', 'java.exe'), 'corrupt');
  let healthy = false;
  const { calls, deps } = makeDeps({ exec: async exe => { if (!healthy && (await fs.readFile(exe, 'utf8')) === 'corrupt') throw Error('bad'); healthy = true; return GOOD_OUT; } });
  const exe = await ensureJava(root, () => {}, deps);
  assert.equal(calls.download, 1);
  assert.equal(await fs.readFile(exe, 'utf8'), 'x');
});

test('cross-platform extraction paths reject absolute and Windows traversal paths',()=>{for(const name of ['C:\\outside\\java.exe','../escape','a\\..\\escape','/outside/java.exe'])assert.throws(()=>inside(os.tmpdir(),name));});
test('archive stream is stopped at the expected size',async()=>{const {readBounded}=require('../electron/java-runtime.cjs');const response={headers:{get:()=>null},body:(async function*(){yield Buffer.alloc(10);yield Buffer.alloc(10)})()};await assert.rejects(readBounded(response,15),/limit/);});
test('oversized declared download is rejected before reading body',async()=>{const {readBounded}=require('../electron/java-runtime.cjs');await assert.rejects(readBounded({headers:{get:()=> '1000'},arrayBuffer:()=>assert.fail('must not buffer')},10),/size mismatch/);});
