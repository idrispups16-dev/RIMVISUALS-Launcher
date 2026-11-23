"""Get the RIMVISUALS mod JAR from the project's official release when none is bundled (running from source / IDLE).

The release must contain ``rimvisuals-mod.json`` ({"file": "...jar", "sha256": "...", "version": "..."}) next to the JAR.
The JAR is only accepted if its SHA-256 equals the manifest value and it really is the ``rimvisuals`` Fabric mod.
TLS is always verified; only the project's own release URL is used (no mirrors).
"""
from __future__ import annotations

import json
import re

from .core import CancelToken, HttpError, Paths, diagnose_network_error, download, http_request, log
from .minecraft import Endpoints
from .modmgr import ModJar, read_jar_meta

NAME_RE = re.compile(r"^rimvisuals-[A-Za-z0-9._+-]+\.jar$")
HASH_RE = re.compile(r"^[0-9a-fA-F]{64}$")


class JarUnavailable(RuntimeError):
    """Human-readable reason why the JAR could not be obtained."""


def fetch_release_jar(paths: Paths, ep: Endpoints, cancel: CancelToken | None = None, progress=None) -> ModJar:
    base = ep.release.rstrip("/")
    try:
        mf = json.loads(http_request(base + "/rimvisuals-mod.json", timeout=20, max_bytes=1 << 16))
    except HttpError as e:
        cached = _cached(paths)
        if cached:
            log.warning("release manifest unreachable, using verified cached JAR %s", cached.path.name)
            return cached
        raise JarUnavailable(f"Официальный релиз RIMVISUALS недоступен ({base}): {diagnose_network_error(e)}. "
                             "Если релиз ещё не опубликован, выберите JAR вручную или используйте установщик со встроенным JAR.") from None
    except ValueError:
        raise JarUnavailable("Манифест релиза повреждён (rimvisuals-mod.json)") from None
    name, want = str(mf.get("file", "")), str(mf.get("sha256", ""))
    if not NAME_RE.match(name) or not HASH_RE.match(want):
        raise JarUnavailable("Манифест релиза некорректен: имя файла или SHA-256")
    dest = paths.cache / "jar" / name
    try:
        download(base + "/" + name, dest, sha256=want, cancel=cancel, progress=progress)
    except HttpError as e:
        dest.unlink(missing_ok=True)
        raise JarUnavailable(f"Не удалось скачать {name}: {diagnose_network_error(e)}") from None
    info = read_jar_meta(dest)
    if info.mod_id != "rimvisuals" or not info.targets_current:
        dest.unlink(missing_ok=True)
        raise JarUnavailable(f"{name}: это не RIMVISUALS для Minecraft 1.21.11")
    log.info("Release JAR %s v%s verified (%s)", name, info.version, info.sha256[:12])
    return info


def _cached(paths: Paths) -> ModJar | None:
    """A JAR downloaded earlier (its SHA-256 was checked then); lets the install resume without the network."""
    d = paths.cache / "jar"
    for j in sorted(d.glob("rimvisuals-*.jar"), reverse=True) if d.is_dir() else []:
        try:
            info = read_jar_meta(j)
        except ValueError:
            continue
        if info.mod_id == "rimvisuals" and info.targets_current:
            return info
    return None
