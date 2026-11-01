# Current visuals test installer

Base: PR9 / integration/rimvisuals-complete at 41bd8679b648e0d607a42da5eba940cbca39c39e.
Launcher test version: 0.2.0-visuals.1. Mod sources and its version are unchanged.

This branch only refreshes installer packaging and its checks:
- Builds/tests the authoritative Fabric JAR from the checked-out commit and runs the existing Minecraft client/config startup smoke.
- Requires compiled classes for all twelve new Claude modules as well as the earlier modules.
- Windows embeds that exact tested artifact, not a separate rebuild or historical JAR. The bundled filename includes a short SHA256 suffix because Claude retained the mod version; byte content is unchanged. This avoids collision with an earlier managed JAR of the same version, allowing the existing installer to back it up safely. A focused upgrade fixture checks preservation of user mods/config/world data.
- Package evidence uses the tested JAR SHA256 instead of the obsolete hardcoded historical digest.
- Silent installation verifies the JAR extracted by the actual EXE equals the tested JAR, and checks the installed launcher window.
- Keeps current-build source/native inventory checks, Windows Defender and the required no-detection gate. Does not repeat historical installer investigation, npm signature audit or previous old-installer scans.

Artifacts include the installer, component manifest, tested JAR, test logs and current-build security evidence/checksums. Unsigned test installer: no assertion that Chrome/SmartScreen warnings have been cleared. New visual effects still require an actual Windows Minecraft world test; compilation/client startup are not that confirmation. New modules are disabled by default, as implemented by Claude.
