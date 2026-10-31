# RIMVISUALS installer security investigation

## Scope and conclusion

Investigated PR7 commit `c48adbb83b175dc411c3db4543a88ee221607ae7` / version `0.2.0-versions.1`, compared with PR6 commit `aae262598e87771a093612aad8a715000ae4fbbc`, and hardened this separate branch as `0.2.0-security.1`. Main, source PR branches, website and users' .minecraft are not modified. No visual mod code changes.

**Root cause of Chrome's dangerous-download verdict is NOT established.** An unsigned file and low download reputation are possible contributing factors, not proof of a false positive. Chrome distinguishes dangerous downloads from merely uncommon downloads. A successful build, a matching hash, or one clean antivirus result cannot prove absence of malware or guarantee a future Chrome verdict. Do not disable protection or override the block on the basis of this report.

## Original artifact identity

- CI run: https://github.com/idrispups16-dev/RIMVISUALS-Launcher/actions/runs/41248974550
- Source commit: c48adbb83b175dc411c3db4543a88ee221607ae7
- Artifact ZIP SHA256: `d73b7d253b8710fff45856fc58aed3fc8b06f17f5b7c59cd2f2ff04a4f09c93b`
- EXE: `RIMVISUALS-Setup-0.2.0-versions.1.exe`, 87,830,720 bytes
- EXE SHA256: `94675527320adf2d8a83f34f55fe9e57f77be6d18a7b952d75ac26a3d3156b57`
- Downloaded bytes match the CI EXE digest. PE certificate/security directory is `(0,0)`: no embedded Authenticode signature. CI additionally records Windows Authenticode status and publisher subject. Product/copyright metadata is not a verified publisher identity.
- No old npm lockfile or SBOM exists. Exact historical build dependency resolutions cannot be reconstructed from package.json alone. Baseline audit JSON is a fresh resolution of the original ranges, NOT a claim about every original CI package. The new pipeline extracts the original NSIS archive without executing it and compares every main-process/preload source against its original commit (allowing only documented CRLF-to-LF normalization for the old Windows package; raw hashes and comparison mode are recorded), and records actual packaged runtime dependency versions. This still is not a bit-for-bit independent rebuild of NSIS/Chromium.

## Findings and repairs

| Area | Finding | Change / residual limitation |
|---|---|---|
| Dependencies | No lockfile; npm install resolves mutable version ranges and executes lifecycle scripts | Exact direct versions + committed SHA512 lock; npm ci --ignore-scripts, then explicit pinned Electron installer; SBOM and full audit artifacts |
| Runtime | Electron 33.4.11 is obsolete; old ZIP parser has high advisories | Electron 45.2.1, adm-zip 0.6.1, builder 26.18.0, Vite 8.3.5, Playwright 1.64.1; test all preserved functions |
| Audit | Fresh baseline resolution: 30 advisories (1 critical / 14 high / 15 moderate) | New resolution: no production advisories, no high/critical; 15 moderate build-tool advisories remain, disclosed in resolved-npm-audit.json. Do not blindly apply npm's incompatible downgrades or major overrides. Build patterns are repository-controlled, not arbitrary user glob input; this reduces exposure, not the existence of those advisories |
| IPC | No sender validation | Require exact application's top-level WebContents/frame/file URL for every IPC handler |
| Renderer | No CSP; external Google Font request | CSP rejects remote scripts, connections, frames, forms and objects; font request removed, local fallback; session denies network and permissions; sandbox/contextIsolation/nodeIntegration:false preserved. Inline styles retained for React animation, inline scripts forbidden |
| Browser | Browser opener accepts a URL argument internally | Allow only official Microsoft HTTPS authorization endpoint; no arbitrary shell/protocol opening |
| Files | Lexical containment alone follows existing links/junctions | Check existing managed ancestors for links before client/runtime/component/profile/mod operations. Same-user filesystem races cannot be completely prevented by these checks; custom Java and mods intentionally execute user-selected code |
| Downloads | Game/metadata responses unbounded | Stream limits: metadata 8 MiB, game files 512 MiB; HTTPS host allowlist, timeouts, digest checks retained. Java archive size/SHA256 and extraction limits already existed |
| Binary hardening | Default Electron fuses, possible unnecessary elevated install | Disable RunAsNode, NODE_OPTIONS and Node inspector; require app.asar and embedded ASAR integrity; per-user installer without elevation. This is hardening, not a Chrome workaround |
| Actions | Mutable action tags, unpinned npm resolution | Pin action commits; checkout credentials not persisted; read-only repository/actions token; no deployment/site writes |
| Tokens | Refresh tokens stored encrypted using Electron safeStorage on Windows; access tokens in memory; no passwords requested | Preserved with real Windows encrypted-storage smoke tests using synthetic credentials. PKCE S256, random state and loopback callback unchanged. No actual Microsoft account used in audit |

## Network/process/filesystem review

Runtime requests are to official Microsoft/Xbox/Minecraft authorization endpoints, Mojang manifests/assets/libraries, Fabric metadata/Maven, Adoptium metadata and GitHub Temurin release assets. Minecraft/Fabric files use upstream digests; Java uses SHA256 from Adoptium. Digests from the same upstream HTTPS trust domain detect corruption but do not independently authenticate a compromised upstream. No telemetry or credential exfiltration endpoint was found in reviewed first-party sources. This is a source-review observation, not a malware clearance of all transitive/native binaries.

Direct Java spawn/execFile receives arrays without a command shell. Custom Java selection intentionally allows executing a user-chosen executable; imported mods execute arbitrary Java code inside Minecraft, so use trusted mods only. The dormant old launcher.cjs helper contains fixed PowerShell Store discovery, but is not imported by main/coordinator and cannot be invoked via IPC/PLAY. No minecraft:// use is introduced. No self-update agent, downloaded PowerShell, scheduled task, browser manipulation or antivirus exclusion is installed. NSIS creates the requested app/shortcuts/uninstaller. Only owned RIMVISUALS directories are used; profile deletion preserves worlds; JAR updates back up previous user files.

PR7 additions over PR6 are catalog/version-aware installation/cache, resolution arguments, mod manager, profile schema2 export and CI coverage. The auth/PKCE/vault implementation and Fabric engine were not changed by PR7. The absent lockfile means no reliable historical dependency-diff claim is possible. New package-evidence artifacts provide executable-to-commit/source/dependency/JAR traceability going forward.

## CI evidence and release gates

The first audit correctly refused a scan in an excluded runner directory and blocked publication. Windows records Get-AuthenticodeSignature for EXE/DLL files, Defender engine/signature versions and dates, exact scan targets, result codes and new threat detections. Scans run on isolated copies in a directory selected outside configured runner path exclusions; the actual directory/exclusions are recorded. The official runner image preconfigures whole-drive C:/D: exclusions and disables archive/script/realtime protection (see actions/runner-images Configure-WindowsDefender.ps1). This pipeline explicitly ENABLES archive/script/realtime protection and REMOVES those CI exclusions, recording before/after settings. The enabling script refuses to run unless GITHUB_ACTIONS=true and RUNNER_ENVIRONMENT=github-hosted. No exclusions are added and no antivirus is disabled; no user computer settings are touched. Missing/stale/disabled scanner, exclusions, failed scan or detection fails the gate; no installer is uploaded as checked. Original NSIS is never executed. After source comparison, its unpacked application and expanded ASAR sources are scanned separately; the new scan also includes expanded ASAR sources, not only the outer installer. New installer is installed/tested only after its scan. Afterward the actual installed application's window is checked with fuses enabled; that does not claim a Minecraft world test.

`RIMVISUALS-Security-Evidence` contains full npm audit/SBOM/lock, registry-signature verification result (may be incomplete/fail; do not call it successful unless examined), scan logs, Authenticode status, original/new packaged source comparisons, SHA256 checksums and source-commit/run metadata. A clean Defender scan is one engine's result, not Google's Safe Browsing verdict. Third-party scanners/VirusTotal API and Google's internal verdict are not available in this environment. No public file submission is performed automatically.

## Proper distribution and false-positive handling

1. Obtain a code-signing certificate from a trusted CA or an eligible managed signing service for the actual legal publisher. Configure signing in repository/CI secrets; never commit/export the private key or use a self-signed certificate as public trust. Microsoft login Client ID is unrelated to Windows code signing.
2. For public release, require `forceCodeSigning:true`, Authenticode Status=Valid, expected subject and trusted timestamp on installer and application. No real signing identity/certificate has been provided, so this branch must remain an explicitly unsigned test build, not a signed production release. Do not invent a publisher name. Signing alone does not guarantee instant Chrome/SmartScreen reputation or absence of malware.
3. Submit the exact flagged SHA256/file through the official Microsoft Security Intelligence software-developer submission flow (select Defender AV vs SmartScreen appropriately). For Chrome, follow Google Safe Browsing's developer malware/unwanted-software review channel; Search Console ownership only applies to a site you control, not github.com. Include source/run/artifact hashes, scan evidence and precise download URL. Wait for the vendor's response before declaring a false positive or claiming the block is resolved.
4. Publish unchanged, signed, scanned bytes with checksums and provenance from a stable official release location once approved. Merely renaming, re-zipping, hiding the EXE, using a password archive or changing browsers is not a fix.

Official references:
- https://www.electronjs.org/docs/latest/tutorial/security
- https://www.electronjs.org/blog/electron-45-0
- https://www.electronjs.org/docs/latest/tutorial/asar-integrity
- https://www.electron.build/v26/docs/features/code-signing/
- https://support.google.com/chrome/answer/6261569
- https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation
- https://www.microsoft.com/en-us/wdsi/filesubmission
