# Rebuild branch status

Branch: rebuild/standalone-and-render-fixes. Main remains at the previous rollback point. This is a test build, not a completed 0.3.0 release.

## Changed after audit
- Swing transforms now run at first-person item submission and empty-arm rendering, rather than relying on applySwingOffset being called by a particular item branch. Vanilla transforms are retained; RIMVISUALS adds cosmetic transforms. Default preserves vanilla, Smooth is the initial custom style. Visual duration multiplier added. In-world visual confirmation is still required.
- Viewmodel runs at the same submission boundary. Main/off transforms plus sword/totem/crystal/food/other item-category XYZ, scale and rotation. Empty arms use hand settings. These hooks need user verification for maps, item-use actions and third-party renderer compatibility.
- Wider/responsive ClickGUI, wider sidebar, scissor clipping, full module tooltip, reset per module, panel dimensions/background/opacity, HSV + RGB + HEX, secondary gradient accent and gradient preview. Color edits automatically select Custom theme. Blur, font selection and the full theme/layout editor are not implemented.
- Hand Lighting is correctly labelled Item Lighting. No bloom/outline/blur shader switches are presented as implemented. Full Shader Hands remains absent.

## Standalone code
The test EXE uses only APPDATA/RIMVISUALS, opens no other launcher, downloads Minecraft 1.21.11, Fabric Loader 0.19.3, libraries/assets/natives, verifies published hashes and directly spawns Java 21 x64 after Microsoft/Xbox/Minecraft entitlement checks. Java 21 x64 is provisioned automatically into APPDATA/RIMVISUALS/runtime/java-21 (Eclipse Temurin JRE from the Adoptium API, SHA-256 verified, host allow-listed redirects) when no Java path is set; a user-chosen path still takes precedence. Unit tests use mocked network; the real download has not been run on Windows yet. Built-in mod and Fabric API are bundled by CI with a SHA-256 manifest. Only owned, unchanged previous component files are backed up and replaced. No .minecraft installation is modified or reused.

Microsoft Authorization Code + PKCE S256 code is present, refresh tokens are encrypted using Electron safeStorage and no password is collected. A RIMVISUALS public Microsoft Application Client ID is still missing. The real OAuth/Xbox/Minecraft chain, license check and authenticated Java launch cannot be marked verified without it. Microsoft may additionally require approval for Minecraft API access. Do not insert another launcher's Client ID.

Seven Node unit tests cover path containment, library OS/features, launch argument handling, network host restriction, missing account and missing Client ID. A Windows network installation smoke checks the actual game/library/assets installation; inspect Actions before relying on it. Gradle tests and Minecraft startup/mixin/ClickGUI smoke cover the mod. A dev-only matrix test exercises every custom swing style for both hands and Viewmodel transformations. This is not an in-world rendering acceptance test or an FPS benchmark.

## Still pending
See FEATURE_MATRIX.md for all requirements. Fade/Combat/Trails/World expansion, full Shader Hands, expanded HUD, cross-renderer compatibility, measured FPS benchmarks, full settings synchronization and profile import/export/backups are not complete. FT/HV require the user's exact function definitions.

## Required user tests once a build passes
1. JAR: replace only the old RIMVISUALS component in a test instance, Right Shift, enable Swing and select Slide or Spin. Hit with a sword and empty hand; report both separately. Test both hands with Viewmodel X=.5 and Scale=.5.
2. GUI: test at your normal GUI scale and at 1280x720; scroll all Viewmodel settings, edit HSV/HEX and reset the module. Report clipped/unclickable elements.
3. EXE: installation/opening can be tested without Client ID. Authenticated direct PLAY is blocked until the owner registers a public Microsoft app, enables device/public-client flow, and supplies its public Client ID. No client secret is needed. Test credentials must never be pasted into chat.
4. FPS: same world/scene/render distance/resolution, 60 seconds without RIMVISUALS, disabled modules, Safe Mode and Balanced. No improvement is claimed without those measurements.
