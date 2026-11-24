"""Network self-check for the hosts the launcher needs: DNS, TCP 443, verified TLS (reports the issuer on failure), clock, proxy."""
from __future__ import annotations

import socket
import ssl
import time
import urllib.error
import urllib.request

from .core import ssl_context

HOSTS = ["github.com", "cdn.modrinth.com", "api.modrinth.com", "piston-meta.mojang.com", "libraries.minecraft.net", "meta.fabricmc.net", "maven.fabricmc.net"]


def probe(host: str, timeout: float = 8.0) -> dict:
    r = {"host": host, "dns": None, "tcp": False, "tls": False, "issuer": "", "error": ""}
    try:
        r["dns"] = socket.getaddrinfo(host, 443, proto=socket.IPPROTO_TCP)[0][4][0]
    except OSError as e:
        if urllib.request.getproxies().get("https"):          # behind a proxy the PC need not resolve the host itself
            try:
                with urllib.request.urlopen(urllib.request.Request(f"https://{host}/", method="HEAD"), timeout=timeout, context=ssl_context()):
                    pass
                r.update(dns="через прокси", tcp=True, tls=True, issuer="(проверено через системный прокси)")
                return r
            except urllib.error.HTTPError:
                r.update(dns="через прокси", tcp=True, tls=True, issuer="(проверено через системный прокси)")
                return r
            except Exception as e2:
                r["error"] = f"через прокси: {e2}"
                return r
        r["error"] = f"DNS: {e}"
        return r
    try:
        with socket.create_connection((host, 443), timeout=timeout) as s:
            r["tcp"] = True
            with ssl_context().wrap_socket(s, server_hostname=host) as t:
                r["tls"] = True
                cert = t.getpeercert() or {}
                r["issuer"] = ", ".join(v for tup in cert.get("issuer", ()) for k, v in tup if k in ("organizationName", "commonName"))
    except ssl.SSLCertVerificationError as e:
        r["error"] = f"TLS certificate: {e.verify_message or e}"
    except (OSError, ssl.SSLError) as e:
        r["error"] = f"{'TLS' if r['tcp'] else 'TCP'}: {e}"
    return r


def run(hosts: list[str] | None = None) -> tuple[list[dict], str]:
    res = [probe(h) for h in (hosts or HOSTS)]
    proxies = urllib.request.getproxies()
    lines = [f"Системное время: {time.strftime('%Y-%m-%d %H:%M:%S')} (если дата неверна — TLS работать не будет)",
             f"Прокси: {proxies or 'нет'}"]
    for r in res:
        ok = r["tls"]
        lines.append(f"[{'OK' if ok else '!!'}] {r['host']}: " + (f"{r['dns']}, TLS ok, издатель: {r['issuer']}" if ok else r["error"] or "ошибка"))
    bad = [r for r in res if not r["tls"]]
    if bad:
        if any("certificate" in r["error"].lower() for r in bad):
            lines.append("→ Сертификат не прошёл проверку. Причины: неверная дата, антивирус/прокси подменяет HTTPS (добавьте его корневой сертификат в Windows "
                         "или задайте SSL_CERT_FILE), либо устаревший набор корневых сертификатов Windows (Центр обновления). Проверка НЕ отключается.")
        if any(r["error"].startswith("DNS") for r in bad):
            lines.append("→ DNS не разрешает имя (Errno 11001/11002): проверьте интернет, смените DNS на 1.1.1.1 / 8.8.8.8, отключите VPN/фильтры.")
    return res, "\n".join(lines)
