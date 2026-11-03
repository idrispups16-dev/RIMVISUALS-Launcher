# Version manager 0.2.0-versions.1

Built incrementally on PR #6 (aae262598e87771a093612aad8a715000ae4fbbc). No visual mod source changes.

## Implemented
- Bounded release catalog: Minecraft 1.21.11, 1.21.10, 1.21.4; Vanilla or Fabric 0.19.3, Java 21 x64.
- Existing default profile retains its original game folder and native Demo behavior.
- Each newly selected runtime is a separate profile under RIMVISUALS/instances; changing an existing profile's runtime is rejected to avoid world conversion.
- Built-in Visuals only for Fabric 1.21.11. Other profiles deliberately contain no bundled visual JAR/API.
- Java installation remains shared; Minecraft assets/libraries are verified inside each isolated instance. Cache must match the selected runtime.
- Window width, height and fullscreen affect actual Java arguments. RAM and Java selection remain per-profile.
- Account switching and native Demo vs licensed Microsoft authentication remain separate. No borrowed Client ID or alternate authentication provider.
- Local Fabric JAR import/update, enable/disable, displayed Minecraft requirements, backup before replacement, bundled component protection. No deletion of worlds or user mods. Only import trusted code. Fabric resolves dependency compatibility at startup; import alone is not a compatibility guarantee. Disabled mod updates are enabled after import.
- Profile export schema 2 includes runtime and window settings; schema 1 imports remain supported. Credentials and Java paths remain excluded.

## Checks and boundaries
Node unit tests, TypeScript/Vite, Windows Electron UI, real official-file install + Java dry-run for Vanilla 1.21.4/Fabric 1.21.10, original Fabric 1.21.11 Demo process smoke, Fabric build/startup checks and NSIS installer checks run in CI. A dry-run or process startup does not prove a playable world. User previously confirmed Demo World on PR6; new configurations still need an actual Windows world test. Microsoft login still needs an authorized application Client ID. Forge/NeoForge, arbitrary versions, dependency auto-resolution, remote mod catalogs and automatic application self-update are not implemented.

Legacy Launcher documentation was consulted as product reference, not copied: https://docs.legacylauncher.ru/launcher/settings , https://docs.legacylauncher.ru/launcher/subfolders , https://docs.legacylauncher.ru/launcher/source . Published source is for reference; no Legacy code, assets, authentication IDs or resources are included.
