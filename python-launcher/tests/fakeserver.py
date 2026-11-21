"""Local fake of Mojang / Fabric / Modrinth / RIMVISUALS site / Microsoft endpoints for offline tests."""
import hashlib, json, threading, zipfile, io, urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

def sha1(b): return hashlib.sha1(b).hexdigest()

def make_mod_jar(version="9.9.9", mc="~1.21.11", mod_id="rimvisuals"):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": mod_id, "version": version,
                                                 "depends": {"minecraft": mc, "fabric-api": "*"}}))
    return buf.getvalue()

class Fake:
    def __init__(self):
        self.client = b"fake-client-jar"
        self.lib = b"fake-lib-jar"
        self.flib = b"fake-fabric-loader"
        self.assets = {f"sound{i}": f"asset-{i}".encode() for i in range(5)}
        self.fabric_api = b"fake-fabric-api"
        self.hits = []
        self.device_state = "pending"
        self.ms_entitled = True
        self.srv = ThreadingHTTPServer(("127.0.0.1", 0), self._handler())
        self.base = f"http://127.0.0.1:{self.srv.server_address[1]}"
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()

    def close(self): self.srv.shutdown(); self.srv.server_close()

    def routes(self):
        b = self.base
        idx = {"objects": {k: {"hash": sha1(v), "size": len(v)} for k, v in self.assets.items()}}
        idx_b = json.dumps(idx).encode()
        vanilla = {"id": "1.21.11", "mainClass": "net.minecraft.client.main.Main",
                   "downloads": {"client": {"url": b + "/client.jar", "sha1": sha1(self.client), "size": len(self.client)}},
                   "assetIndex": {"id": "99", "url": b + "/idx.json", "sha1": sha1(idx_b), "size": len(idx_b)},
                   "libraries": [{"name": "com.example:lib:1.0", "downloads": {"artifact": {"path": "com/example/lib/1.0/lib-1.0.jar",
                                  "url": b + "/lib.jar", "sha1": sha1(self.lib), "size": len(self.lib)}}},
                                 {"name": "org.lwjgl:lwjgl:3:natives-linux", "rules": [{"action": "allow", "os": {"name": "linux"}}],
                                  "downloads": {"artifact": {"path": "org/lwjgl/n-linux.jar", "url": b + "/lib.jar", "sha1": sha1(self.lib), "size": len(self.lib)}}}],
                   "arguments": {"jvm": ["-Djava.library.path=${natives_directory}", {"rules": [{"action": "allow", "os": {"name": "windows"}}], "value": "-XX:WinOnly"}, "-cp", "${classpath}"],
                                 "game": ["--username", "${auth_player_name}", "--version", "${version_name}", "--gameDir", "${game_directory}",
                                          "--assetsDir", "${assets_root}", "--accessToken", "${auth_access_token}", "--uuid", "${auth_uuid}",
                                          {"rules": [{"action": "allow", "features": {"is_demo_user": True}}], "value": "--demo"}]}}
        vb = json.dumps(vanilla).encode()
        fabric = {"id": "fabric-loader-0.19.3-1.21.11", "mainClass": "net.fabricmc.loader.impl.launch.knot.KnotClient",
                  "arguments": {"jvm": ["-DFabricMcEmu= net.minecraft.client.main.Main "], "game": []},
                  "libraries": [{"name": "net.fabricmc:fabric-loader:0.19.3", "url": b + "/maven/", "sha1": sha1(self.flib), "size": len(self.flib)}]}
        r = {
            "/manifest": json.dumps({"versions": [{"id": "1.21.11", "url": b + "/v.json", "sha1": sha1(vb)}]}).encode(),
            "/v.json": vb, "/idx.json": idx_b, "/client.jar": self.client, "/lib.jar": self.lib,
            "/maven/net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar": self.flib,
            "/v2/versions/loader/1.21.11/0.19.3/profile/json": json.dumps(fabric).encode(),
        }
        for k, v in self.assets.items():
            h = sha1(v); r[f"/res/{h[:2]}/{h}"] = v
        r["/fabric-api.jar"] = self.fabric_api
        return r

    def _handler(self):
        fake = self
        class H(BaseHTTPRequestHandler):
            def log_message(self, *a): pass
            def _send(self, body, code=200):
                self.send_response(code); self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)
            def do_GET(self):
                path = urllib.parse.urlparse(self.path).path
                fake.hits.append(path)
                if path == "/v2/project/fabric-api/version":
                    body = [{"version_number": "0.141.6+1.21.11", "files": [{"primary": True, "filename": "fabric-api-0.141.6+1.21.11.jar",
                             "url": fake.base + "/fabric-api.jar", "size": len(fake.fabric_api),
                             "hashes": {"sha512": hashlib.sha512(fake.fabric_api).hexdigest()}}]}]
                    return self._send(json.dumps(body).encode())
                if path == "/api/public-config":
                    return self._send(json.dumps({"authEnabled": True, "supabaseUrl": fake.base, "supabaseAnonKey": "anon-public"}).encode())
                r = fake.routes()
                if path in r:
                    return self._send(r[path])
                return self._send(b"nf", 404)
            def do_POST(self):
                path = urllib.parse.urlparse(self.path).path
                n = int(self.headers.get("Content-Length") or 0)
                raw = self.rfile.read(n)
                try: body = json.loads(raw) if raw and raw[:1] in b"{[" else raw.decode()
                except ValueError: body = {}
                fake.hits.append(path)
                J = lambda o: self._send(json.dumps(o).encode())
                if path.endswith("/rpc/mod_device_start"): return J({"status": "pending", "challenge_id": "c1", "code": "ABCD-1234"})
                if path.endswith("/rpc/mod_device_poll"): return J({"status": fake.device_state})
                if path.endswith("/rpc/mod_session_validate"): return J({"status": "active", "lease_seconds": 300} if body.get("p_token") else {"status": "invalid"})
                if path == "/ms/token": return J({"access_token": "ms-at", "refresh_token": "ms-rt-1"})
                if path == "/xbl": return J({"Token": "xbl-t", "DisplayClaims": {"xui": [{"uhs": "uhs1"}]}})
                if path == "/xsts": return J({"Token": "xsts-t"})
                if path == "/mc/login": return J({"access_token": "mc-at", "expires_in": 86400})
                return self._send(b"nf", 404)
            def do_GET_ent(self): pass
        return H

def patch_ms_get(fake):
    """entitlements/profile are GETs on the same server."""
    orig = fake.routes
    def routes():
        r = orig()
        r["/ent"] = json.dumps({"items": [{"name": "game_minecraft"}] if fake.ms_entitled else []}).encode()
        r["/prof"] = json.dumps({"id": "0123456789abcdef0123456789abcdef", "name": "TestPlayer"}).encode()
        return r
    fake.routes = routes
