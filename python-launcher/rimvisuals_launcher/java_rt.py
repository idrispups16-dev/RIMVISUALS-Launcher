"""Find or install Java 21 (x64) without touching the system Java."""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

from .core import (TARGET_JAVA, CancelToken, Paths, download, http_json, log, safe_join)

CREATE_NO_WINDOW = 0x08000000 if sys.platform == "win32" else 0
ADOPTIUM_ASSETS = ("https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre"
                   "&os=windows&vendor=eclipse")


@dataclass
class JavaInfo:
    path: Path
    major: int
    arch: str
    vendor: str
    raw: str

    @property
    def ok(self) -> bool:
        return self.major == TARGET_JAVA and self.arch in ("x64", "amd64", "x86_64", "aarch64")


_VER = re.compile(r'version "(\d+)(?:\.(\d+))?[^"]*"')


def parse_version_output(text: str) -> tuple[int, str, str]:
    """Return (major, arch, vendor) from `java -XshowSettings:properties -version` output."""
    m = _VER.search(text)
    major = int(m.group(1)) if m else 0
    if major == 1 and m and m.group(2):
        major = int(m.group(2))              # "1.8.0" style
    arch = ""
    am = re.search(r"os\.arch\s*=\s*(\S+)", text)
    if am:
        arch = am.group(1)
    elif "64-Bit" in text:
        arch = "x64"
    vm = re.search(r"java\.vendor\s*=\s*(.+)", text)
    return major, arch, (vm.group(1).strip() if vm else "")


def probe(java: Path, timeout: float = 15) -> JavaInfo | None:
    try:
        r = subprocess.run([str(java), "-XshowSettings:properties", "-version"], capture_output=True, text=True,
                           timeout=timeout, creationflags=CREATE_NO_WINDOW)
    except (OSError, subprocess.SubprocessError):
        return None
    out = (r.stderr or "") + (r.stdout or "")
    major, arch, vendor = parse_version_output(out)
    if not major:
        return None
    return JavaInfo(Path(java), major, arch, vendor, out[:400])


def _exe_name() -> str:
    return "java.exe" if sys.platform == "win32" else "java"


def candidates(paths: Paths, custom: str = "") -> list[Path]:
    out: list[Path] = []
    if custom:
        p = Path(custom)
        out.append(p / "bin" / _exe_name() if p.is_dir() else p)
    rt = paths.runtime
    if rt.exists():
        out += sorted(rt.glob(f"*/bin/{_exe_name()}")) + sorted(rt.glob(f"*/*/bin/{_exe_name()}"))
    if os.environ.get("JAVA_HOME"):
        out.append(Path(os.environ["JAVA_HOME"]) / "bin" / _exe_name())
    w = shutil.which("java")
    if w:
        out.append(Path(w))
    if sys.platform == "win32":
        for base in (os.environ.get("ProgramFiles", r"C:\Program Files"), os.environ.get("ProgramFiles(x86)", "")):
            for vendor in ("Eclipse Adoptium", "Java", "Microsoft", "Zulu", "BellSoft", "Amazon Corretto"):
                d = Path(base) / vendor if base else None
                if d and d.exists():
                    out += sorted(d.glob(f"*/bin/{_exe_name()}"))
    else:
        out += [Path("/usr/lib/jvm") / d / "bin" / "java" for d in (os.listdir("/usr/lib/jvm") if os.path.isdir("/usr/lib/jvm") else [])]
    seen, uniq = set(), []
    for p in out:
        k = str(p).lower()
        if k not in seen and p.exists():
            seen.add(k)
            uniq.append(p)
    return uniq


def find_java(paths: Paths, custom: str = "") -> JavaInfo | None:
    """First usable Java 21 (a user-selected path wins, and is reported even when wrong)."""
    first_bad: JavaInfo | None = None
    for c in candidates(paths, custom):
        info = probe(c)
        if info is None:
            continue
        if info.ok:
            return info
        first_bad = first_bad or info
    return first_bad if custom else None


def install_temurin(paths: Paths, cancel: CancelToken | None = None, progress=None) -> JavaInfo:
    """Download Eclipse Temurin 21 JRE (Windows x64) into RIMVISUALS/runtime with SHA-256 verification."""
    if sys.platform != "win32":
        raise RuntimeError("Автоустановка Java доступна только на Windows. Установите Java 21 вручную.")
    data = http_json(ADOPTIUM_ASSETS)
    pkg = None
    for item in data if isinstance(data, list) else []:
        p = (item.get("binary") or {}).get("package") or {}
        if str(p.get("name", "")).endswith(".zip") and p.get("checksum") and str(p.get("link", "")).startswith("https://"):
            pkg = p
            break
    if not pkg:
        raise RuntimeError("Adoptium не вернул подходящий пакет Java 21")
    paths.runtime.mkdir(parents=True, exist_ok=True)
    zpath = paths.cache / pkg["name"]
    download(pkg["link"], zpath, sha256=pkg["checksum"], size=pkg.get("size"), cancel=cancel, progress=progress)
    dest = paths.runtime / pkg["name"].removesuffix(".zip")
    with zipfile.ZipFile(zpath) as z:
        for i in z.infolist():
            out = safe_join(dest, *i.filename.split("/"))
            if i.is_dir():
                out.mkdir(parents=True, exist_ok=True)
            else:
                out.parent.mkdir(parents=True, exist_ok=True)
                with z.open(i) as src, open(out, "wb") as dst:
                    shutil.copyfileobj(src, dst)
    info = find_java(paths)
    if not info or not info.ok:
        raise RuntimeError("Java установлена, но проверка версии не прошла")
    log.info("Java %s installed at %s", info.major, info.path)
    return info
