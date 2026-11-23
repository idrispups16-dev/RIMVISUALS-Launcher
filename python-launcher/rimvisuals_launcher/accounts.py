"""Accounts.

* RIMVISUALS site account: same device-code flow the Fabric mod uses (Supabase RPC behind rimvisuals.vercel.app).
  Only the public anon key from /api/public-config is used; no service-role key exists in this app.
* Microsoft / Minecraft: Authorization Code + PKCE in the system browser with a localhost redirect, using the
  owner's *own* Azure public Client ID (settings). No borrowed Client IDs, no client secret.
* Tokens are stored encrypted with Windows DPAPI (current user). Elsewhere they are kept only in memory.
"""
from __future__ import annotations

import base64
import hashlib
import html
import http.server
import json
import os
import secrets
import socket
import sys
import threading
import time
import urllib.parse
import webbrowser
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

from .core import SITE, HttpError, Paths, atomic_write, http_json, log, read_json

SUPABASE_HOST = "https://pixqnuuvpxxxwaxjntpl.supabase.co"


# ------------------------------------------------------------------ secret storage
class SecretStore:
    """Small encrypted key/value store. Windows: DPAPI bound to the user. Other OS: memory only."""

    def __init__(self, paths: Paths):
        self.file = paths.secrets_file
        self._mem: dict[str, str] = {}
        self._load()

    @property
    def persistent(self) -> bool:
        return sys.platform == "win32"

    def _load(self) -> None:
        if not self.persistent or not self.file.exists():
            return
        try:
            self._mem = json.loads(_dpapi(self.file.read_bytes(), decrypt=True).decode("utf-8"))
        except Exception:
            log.warning("secret store unreadable; starting empty")
            self._mem = {}

    def _flush(self) -> None:
        if self.persistent:
            atomic_write(self.file, _dpapi(json.dumps(self._mem).encode("utf-8"), decrypt=False))

    def get(self, key: str) -> str | None:
        return self._mem.get(key)

    def set(self, key: str, value: str) -> None:
        self._mem[key] = value
        self._flush()

    def delete(self, key: str) -> None:
        self._mem.pop(key, None)
        self._flush()


def _dpapi(data: bytes, decrypt: bool) -> bytes:
    import ctypes
    from ctypes import wintypes

    class BLOB(ctypes.Structure):
        _fields_ = [("cbData", wintypes.DWORD), ("pbData", ctypes.POINTER(ctypes.c_char))]

    buf = ctypes.create_string_buffer(data, len(data))
    inp = BLOB(len(data), ctypes.cast(buf, ctypes.POINTER(ctypes.c_char)))
    out = BLOB()
    crypt32, kernel32 = ctypes.windll.crypt32, ctypes.windll.kernel32
    fn = crypt32.CryptUnprotectData if decrypt else crypt32.CryptProtectData
    ok = (fn(ctypes.byref(inp), None, None, None, None, 0, ctypes.byref(out)) if decrypt
          else fn(ctypes.byref(inp), "RIMVISUALS", None, None, None, 0, ctypes.byref(out)))
    if not ok:
        raise OSError("DPAPI call failed")
    try:
        return ctypes.string_at(out.pbData, out.cbData)
    finally:
        kernel32.LocalFree(out.pbData)


# ------------------------------------------------------------------ site account (device code)
def _sha256_hex(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8")).hexdigest()


class SiteAccount:
    """Mirror of xyz.rimvisuals.auth.DeviceAuth (same RPCs), so one session can serve launcher and mod."""

    def __init__(self, secrets_store: SecretStore, site: str = SITE, supabase_host: str = SUPABASE_HOST):
        self.secrets, self.site, self.host = secrets_store, site.rstrip("/"), supabase_host
        self._api = self._key = None
        self.code = ""
        self._challenge = self._verifier = self._candidate = None
        self.expires = 0.0

    def _configure(self) -> None:
        if self._api:
            return
        cfg = http_json(self.site + "/api/public-config")
        if not cfg.get("authEnabled"):
            raise HttpError("Вход на сайте сейчас отключён")
        url = str(cfg.get("supabaseUrl", ""))
        if url != self.host:
            raise HttpError("Неожиданный сервер авторизации — вход отменён")
        self._key, self._api = cfg["supabaseAnonKey"], url + "/rest/v1/rpc/"

    def _rpc(self, name: str, args: dict[str, Any]) -> dict[str, Any]:
        self._configure()
        return http_json(self._api + name, payload=args, headers={"apikey": self._key})

    @property
    def token(self) -> str | None:
        t = self.secrets.get("site_token")
        return t if t and len(t) == 64 and all(c in "0123456789abcdef" for c in t) else None

    def start(self) -> str:
        self._verifier, self._candidate = secrets.token_hex(32), secrets.token_hex(32)
        r = self._rpc("mod_device_start", {"p_verifier_hash": _sha256_hex(self._verifier),
                                            "p_session_hash": _sha256_hex(self._candidate),
                                            "p_device_name": "RIMVISUALS Launcher / " + sys.platform[:12]})
        if r.get("status") != "pending":
            raise HttpError("Сервер отклонил запрос кода")
        self._challenge, self.code = r["challenge_id"], r["code"]
        self.expires = time.monotonic() + 300
        return self.code

    def browser_url(self) -> str:
        return f"{self.site}/account.html" + (f"?device={self.code}#device" if self.code else "#device")

    def poll(self) -> str:
        """pending | approved | expired | idle"""
        if not self._challenge:
            return "idle"
        if time.monotonic() >= self.expires:
            self._challenge, self.code = None, ""
            return "expired"
        r = self._rpc("mod_device_poll", {"p_challenge_id": self._challenge, "p_verifier": self._verifier})
        st = r.get("status", "invalid")
        if st == "approved":
            self.secrets.set("site_token", self._candidate)
            self._challenge, self.code = None, ""
            return "approved"
        if st in ("expired", "invalid"):
            self._challenge, self.code = None, ""
            return "expired"
        return "pending"

    def validate(self) -> dict[str, Any]:
        t = self.token
        if not t:
            return {"status": "signed_out"}
        try:
            r = self._rpc("mod_session_validate", {"p_token": t})
        except HttpError as e:
            return {"status": "offline", "detail": str(e)}
        if r.get("status") == "active":
            return {"status": "active", "lease_seconds": int(r.get("lease_seconds", 0))}
        self.secrets.delete("site_token")
        return {"status": "no_access"}

    def sign_out(self) -> None:
        self.secrets.delete("site_token")

    def share_with_mod(self, game_dir: Path) -> bool:
        """Write the same session file the mod reads, so the mod needs no second login."""
        t = self.token
        if not t:
            return False
        f = game_dir / "config" / "rimvisuals" / "mod-session.json"
        atomic_write(f, json.dumps({"token": t}))
        try:
            os.chmod(f, 0o600)
        except OSError:
            pass
        return True


# ------------------------------------------------------------------ Microsoft (PKCE)
@dataclass
class McSession:
    name: str
    uuid: str
    access_token: str
    expires_at: float


@dataclass
class MsEndpoints:
    authorize: str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
    token: str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    xbl: str = "https://user.auth.xboxlive.com/user/authenticate"
    xsts: str = "https://xsts.auth.xboxlive.com/xsts/authorize"
    mc_login: str = "https://api.minecraftservices.com/authentication/login_with_xbox"
    entitlements: str = "https://api.minecraftservices.com/entitlements/mcstore"
    profile: str = "https://api.minecraftservices.com/minecraft/profile"


def pkce_pair() -> tuple[str, str]:
    verifier = base64.urlsafe_b64encode(secrets.token_bytes(48)).rstrip(b"=").decode()
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    return verifier, challenge


class _Callback(http.server.BaseHTTPRequestHandler):
    result: dict[str, str] = {}

    def do_GET(self) -> None:  # noqa: N802
        q = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        type(self).result = {k: v[0] for k, v in q.items()}
        body = ("<html><body style='background:#0b0713;color:#fff;font-family:Segoe UI,sans-serif;text-align:center;padding-top:20vh'>"
                "<h2>RIMVISUALS</h2><p>Вход завершён. Можно закрыть вкладку и вернуться в лаунчер.</p></body></html>")
        data = body.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *a: Any) -> None:
        pass


class MicrosoftAccount:
    def __init__(self, secrets_store: SecretStore, client_id: str, ep: MsEndpoints | None = None):
        self.secrets, self.client_id, self.ep = secrets_store, client_id.strip(), ep or MsEndpoints()
        self.session: McSession | None = None

    @property
    def configured(self) -> bool:
        return bool(self.client_id)

    def _form(self, url: str, fields: dict[str, str]) -> dict[str, Any]:
        from .core import http_request
        body = http_request(url, data=urllib.parse.urlencode(fields).encode(), method="POST",
                            headers={"Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json"})
        return json.loads(body.decode("utf-8"))

    # ---- full chain given a Microsoft access token
    def _chain(self, ms_access: str) -> McSession:
        xbl = http_json(self.ep.xbl, payload={"Properties": {"AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com",
                                                             "RpsTicket": "d=" + ms_access},
                                              "RelyingParty": "http://auth.xboxlive.com", "TokenType": "JWT"})
        uhs = xbl["DisplayClaims"]["xui"][0]["uhs"]
        xsts = http_json(self.ep.xsts, payload={"Properties": {"SandboxId": "RETAIL", "UserTokens": [xbl["Token"]]},
                                                "RelyingParty": "rp://api.minecraftservices.com/", "TokenType": "JWT"})
        mc = http_json(self.ep.mc_login, payload={"identityToken": f"XBL3.0 x={uhs};{xsts['Token']}"})
        auth = {"Authorization": "Bearer " + mc["access_token"]}
        ent = http_json(self.ep.entitlements, headers=auth)
        owns = any(i.get("name") in ("game_minecraft", "product_minecraft") for i in ent.get("items", []))
        if not owns:
            raise HttpError("На этом аккаунте не найдено лицензии Minecraft Java Edition")
        prof = http_json(self.ep.profile, headers=auth)
        u = prof["id"]
        return McSession(prof["name"], f"{u[:8]}-{u[8:12]}-{u[12:16]}-{u[16:20]}-{u[20:]}", mc["access_token"],
                         time.time() + int(mc.get("expires_in", 86400)) - 60)

    def sign_in(self, open_browser: Callable[[str], Any] = webbrowser.open, timeout: float = 180.0) -> McSession:
        if not self.configured:
            raise HttpError("Не указан Microsoft Application (client) ID в настройках")
        verifier, challenge = pkce_pair()
        state = secrets.token_urlsafe(16)
        _Callback.result = {}
        srv = http.server.HTTPServer(("127.0.0.1", 0), _Callback)
        srv.timeout = 1.0
        port = srv.server_address[1]
        redirect = f"http://localhost:{port}"
        q = urllib.parse.urlencode({"client_id": self.client_id, "response_type": "code", "redirect_uri": redirect,
                                    "scope": "XboxLive.signin offline_access", "state": state,
                                    "code_challenge": challenge, "code_challenge_method": "S256", "prompt": "select_account"})
        open_browser(f"{self.ep.authorize}?{q}")
        deadline = time.monotonic() + timeout
        try:
            while time.monotonic() < deadline and not _Callback.result:
                srv.handle_request()
        finally:
            srv.server_close()
        res = _Callback.result
        if not res:
            raise HttpError("Время ожидания входа истекло")
        if res.get("state") != state:
            raise HttpError("Ответ входа не прошёл проверку state")
        if "error" in res:
            raise HttpError("Microsoft вернул ошибку: " + html.escape(res.get("error", "?")))
        tok = self._form(self.ep.token, {"client_id": self.client_id, "grant_type": "authorization_code",
                                         "code": res["code"], "redirect_uri": redirect, "code_verifier": verifier,
                                         "scope": "XboxLive.signin offline_access"})
        if tok.get("refresh_token"):
            self.secrets.set("ms_refresh", tok["refresh_token"])
        self.session = self._chain(tok["access_token"])
        return self.session

    def refresh(self) -> McSession | None:
        if self.session and self.session.expires_at > time.time():
            return self.session
        rt = self.secrets.get("ms_refresh")
        if not rt or not self.configured:
            return None
        try:
            tok = self._form(self.ep.token, {"client_id": self.client_id, "grant_type": "refresh_token",
                                             "refresh_token": rt, "scope": "XboxLive.signin offline_access"})
            if tok.get("refresh_token"):
                self.secrets.set("ms_refresh", tok["refresh_token"])
            self.session = self._chain(tok["access_token"])
            return self.session
        except (HttpError, KeyError, ValueError) as e:
            log.warning("Microsoft refresh failed: %s", e)
            return None

    def sign_out(self) -> None:
        self.session = None
        self.secrets.delete("ms_refresh")


DEMO_UUID = "00000000-0000-0000-0000-00000000de40"


def demo_account():
    from .minecraft import Account
    return Account("Player", DEMO_UUID, "demo-no-credential", "legacy", True)
