"""Shared basics: paths, atomic JSON, hashing, logging, HTTP helpers.

Standard library only, so the launcher runs from IDLE on a clean Python 3.14
and is easy to freeze into an EXE.
"""
from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import shutil
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
from collections import deque
from pathlib import Path
from typing import Any, Callable, Iterable

APP_NAME = "RIMVISUALS"
APP_VERSION = "1.0.0-py.1"
TARGET_MC = "1.21.11"
TARGET_FABRIC = "0.19.3"
TARGET_FABRIC_API = "0.141.6+1.21.11"
TARGET_JAVA = 21
SITE = "https://rimvisuals.vercel.app"
USER_AGENT = f"RIMVISUALS-Launcher/{APP_VERSION} (+{SITE})"


# --------------------------------------------------------------------------- paths
def home_dir() -> Path:
    """RIMVISUALS' own folder. The normal .minecraft is never written to."""
    override = os.environ.get("RIMVISUALS_HOME")
    if override:
        return Path(override)
    if sys.platform == "win32":
        base = os.environ.get("APPDATA") or str(Path.home() / "AppData" / "Roaming")
        return Path(base) / APP_NAME
    return Path(os.environ.get("XDG_DATA_HOME", str(Path.home() / ".local" / "share"))) / APP_NAME.lower()


class Paths:
    def __init__(self, root: Path | None = None):
        self.root = Path(root) if root else home_dir()

    @property
    def minecraft(self) -> Path:        # shared game files (libraries, assets, versions)
        return self.root / "minecraft"

    @property
    def libraries(self) -> Path:
        return self.minecraft / "libraries"

    @property
    def assets(self) -> Path:
        return self.minecraft / "assets"

    @property
    def versions(self) -> Path:
        return self.minecraft / "versions"

    @property
    def runtime(self) -> Path:
        return self.root / "runtime"

    @property
    def cache(self) -> Path:
        return self.root / "cache"

    @property
    def logs(self) -> Path:
        return self.root / "logs"

    @property
    def profiles_dir(self) -> Path:
        return self.root / "profiles"

    @property
    def backups(self) -> Path:
        return self.root / "backups"

    @property
    def settings_file(self) -> Path:
        return self.root / "settings.json"

    @property
    def profiles_file(self) -> Path:
        return self.root / "profiles.json"

    @property
    def secrets_file(self) -> Path:
        return self.root / "secrets.bin"

    def ensure(self) -> None:
        for p in (self.root, self.libraries, self.assets, self.versions, self.runtime, self.cache, self.logs,
                  self.profiles_dir, self.backups):
            p.mkdir(parents=True, exist_ok=True)


# --------------------------------------------------------------------------- files
def atomic_write(path: Path, data: bytes | str) -> None:
    """Write via temp file + rename; keep the previous state as <name>.bak."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = data.encode("utf-8") if isinstance(data, str) else data
    fd, tmp = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(raw)
            f.flush()
            os.fsync(f.fileno())
        if path.exists():
            try:
                shutil.copy2(path, path.with_name(path.name + ".bak"))
            except OSError:
                pass
        os.replace(tmp, path)
    except BaseException:
        try:
            os.unlink(tmp)
        except OSError:
            pass
        raise


def read_json(path: Path, default: Any) -> Any:
    """Read JSON; fall back to <name>.bak, then to the default (never raises)."""
    for candidate in (Path(path), Path(path).with_name(Path(path).name + ".bak")):
        try:
            return json.loads(candidate.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
    return default


def write_json(path: Path, value: Any) -> None:
    atomic_write(path, json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True))


def sha256_file(path: Path, chunk: int = 1 << 20) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while block := f.read(chunk):
            h.update(block)
    return h.hexdigest()


def file_hash(path: Path, algo: str) -> str:
    h = hashlib.new(algo)
    with open(path, "rb") as f:
        while block := f.read(1 << 20):
            h.update(block)
    return h.hexdigest()


def safe_join(base: Path, *parts: str) -> Path:
    """Join and refuse anything that escapes `base` (zip-slip / '..')."""
    base_r = Path(base).resolve()
    out = base_r.joinpath(*parts).resolve()
    if out != base_r and base_r not in out.parents:
        raise ValueError(f"path escapes {base}: {'/'.join(parts)}")
    return out


_NAME_OK = re.compile(r"[^0-9A-Za-zА-Яа-яЁё _.\-]")


def clean_name(name: str, fallback: str = "profile") -> str:
    n = _NAME_OK.sub("", name or "").strip(" .")
    return n[:40] or fallback


# --------------------------------------------------------------------------- logging
class RingHandler(logging.Handler):
    """Keeps the last lines in memory so the GUI log page can show them."""

    def __init__(self, size: int = 2000):
        super().__init__()
        self.lines: deque[tuple[float, str, str]] = deque(maxlen=size)
        self.listeners: list[Callable[[float, str, str], None]] = []

    def emit(self, record: logging.LogRecord) -> None:
        try:
            msg = self.format(record)
            entry = (record.created, record.levelname, msg)
            self.lines.append(entry)
            for fn in list(self.listeners):
                fn(*entry)
        except Exception:  # logging must never break the app
            pass


RING = RingHandler()
log = logging.getLogger("rimvisuals")
_log_ready = False


def setup_logging(paths: Paths | None = None) -> logging.Logger:
    global _log_ready
    if _log_ready:
        return log
    _log_ready = True
    log.setLevel(logging.DEBUG)
    RING.setFormatter(logging.Formatter("%(message)s"))
    log.addHandler(RING)
    if paths is not None:
        try:
            paths.logs.mkdir(parents=True, exist_ok=True)
            from logging.handlers import RotatingFileHandler
            fh = RotatingFileHandler(paths.logs / "launcher.log", maxBytes=1_000_000, backupCount=3, encoding="utf-8")
            fh.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(message)s"))
            log.addHandler(fh)
        except OSError:
            pass
    return log


_SECRET_RE = re.compile(r"(accessToken|access_token|refresh_token|token|code|Authorization|apikey)([\"'=: ]+)([A-Za-z0-9._\-~+/=]{8,})", re.I)


def redact(text: str) -> str:
    """Strip anything token-like before it reaches a log line or a report."""
    return _SECRET_RE.sub(lambda m: f"{m.group(1)}{m.group(2)}<redacted>", text)


# --------------------------------------------------------------------------- HTTP
class HttpError(RuntimeError):
    pass


class Cancelled(RuntimeError):
    pass


class CancelToken:
    def __init__(self) -> None:
        self._ev = threading.Event()

    def cancel(self) -> None:
        self._ev.set()

    @property
    def cancelled(self) -> bool:
        return self._ev.is_set()

    def check(self) -> None:
        if self._ev.is_set():
            raise Cancelled("cancelled")


def is_safe_url(url: str) -> bool:
    return str(url).lower().startswith(("https://", "http://127.0.0.1", "http://localhost"))


def http_request(url: str, *, data: bytes | None = None, headers: dict[str, str] | None = None,
                 method: str | None = None, timeout: float = 20.0, max_bytes: int = 64 << 20) -> bytes:
    if not is_safe_url(url):
        raise HttpError(f"refusing non-HTTPS url: {url.split('?')[0]}")
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("User-Agent", USER_AGENT)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read(max_bytes + 1)
    except urllib.error.HTTPError as e:
        raise HttpError(f"HTTP {e.code} for {url.split('?')[0]}") from None
    except (urllib.error.URLError, TimeoutError, OSError) as e:
        raise HttpError(f"network error for {url.split('?')[0]}: {e}") from None
    if len(body) > max_bytes:
        raise HttpError("response too large")
    return body


def http_json(url: str, *, payload: Any = None, headers: dict[str, str] | None = None, timeout: float = 20.0,
              method: str | None = None) -> Any:
    h = {"Accept": "application/json"}
    data = None
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        h["Content-Type"] = "application/json"
    h.update(headers or {})
    body = http_request(url, data=data, headers=h, method=method, timeout=timeout, max_bytes=16 << 20)
    try:
        return json.loads(body.decode("utf-8"))
    except ValueError:
        raise HttpError("invalid JSON response") from None


def download(url: str, dest: Path, *, sha1: str | None = None, sha256: str | None = None, sha512: str | None = None,
             size: int | None = None, cancel: CancelToken | None = None,
             progress: Callable[[int, int], None] | None = None, retries: int = 3, timeout: float = 30.0) -> Path:
    """Download to dest atomically and verify the hash. Existing valid files are reused."""
    dest = Path(dest)
    algo, want = (("sha256", sha256) if sha256 else ("sha512", sha512) if sha512 else ("sha1", sha1) if sha1 else (None, None))
    if dest.exists() and (algo is None or file_hash(dest, algo) == want.lower()) and (size is None or dest.stat().st_size == size):
        if algo or size is not None:
            return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    last: Exception | None = None
    for attempt in range(retries):
        if cancel:
            cancel.check()
        tmp = dest.with_name(dest.name + f".part{os.getpid()}")
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", USER_AGENT)
            if not is_safe_url(url):
                raise HttpError("refusing non-HTTPS url")
            h = hashlib.new(algo) if algo else None
            got = 0
            with urllib.request.urlopen(req, timeout=timeout) as resp, open(tmp, "wb") as f:
                total = int(resp.headers.get("Content-Length") or size or 0)
                while True:
                    if cancel:
                        cancel.check()
                    block = resp.read(1 << 16)
                    if not block:
                        break
                    f.write(block)
                    got += len(block)
                    if h:
                        h.update(block)
                    if progress:
                        progress(got, total)
            if size is not None and got != size:
                raise HttpError(f"size mismatch for {dest.name}: {got} != {size}")
            if h and h.hexdigest() != want.lower():
                raise HttpError(f"{algo} mismatch for {dest.name}")
            os.replace(tmp, dest)
            return dest
        except Cancelled:
            raise
        except (HttpError, urllib.error.URLError, OSError, TimeoutError) as e:
            last = e
            log.debug("download retry %s: %s", dest.name, e)
            time.sleep(min(2 * (attempt + 1), 5) if attempt < retries - 1 else 0)
        finally:
            try:
                tmp.unlink()
            except OSError:
                pass
    raise HttpError(f"download failed for {dest.name}: {last}")


def parallel_map(fn: Callable[[Any], Any], items: Iterable[Any], workers: int = 8,
                 cancel: CancelToken | None = None) -> list[Any]:
    from concurrent.futures import ThreadPoolExecutor
    items = list(items)
    results: list[Any] = [None] * len(items)
    if not items:
        return results
    with ThreadPoolExecutor(max_workers=max(1, workers)) as ex:
        futs = {ex.submit(fn, it): i for i, it in enumerate(items)}
        from concurrent.futures import as_completed
        try:
            for fut in as_completed(futs):
                results[futs[fut]] = fut.result()
                if cancel:
                    cancel.check()
        except BaseException:
            for f in futs:
                f.cancel()
            raise
    return results
