# RIMVISUALS rebuild test branch

See [rebuild status](docs/REBUILD_STATUS.md) and [complete feature audit](docs/FEATURE_MATRIX.md). This is not a finished client.

# RIMVISUALS Launcher 0.1.6 + Fabric mod 0.2.0

Target: Windows 11, Minecraft Java 1.21.11, Fabric Loader 0.19.3, Fabric API 0.141.6+1.21.11, Java 21.

The launcher opens the official Minecraft Launcher by executable or its discovered Store AUMID. It does not use minecraft://. Microsoft authentication and choosing/starting Fabric remain in the official launcher. Successful process creation/activation request does not prove the game started.

Settings and named launcher profiles are kept in Electron userData. Save also writes only `config/rimvisuals/launcher.json` in the selected Minecraft folder. The mod reads this on startup and every 40 client ticks when modified; switches control HUD, watermark, glow and particle overlay, plus accent and HUD scale. The in-game menu (Right Shift or RIMVISUALS button) retains its own detailed settings, HUD dragging and profiles. Existing mods, worlds and other configuration files are not removed.

## Install and test
1. Download the latest successful Windows workflow artifact, extract and install EXE.
2. Download the Fabric workflow artifact and extract rimvisuals-0.2.0.jar.
3. With Minecraft closed, place that JAR and Fabric API 0.141.6+1.21.11 in your chosen instance's mods folder. If an older RIMVISUALS JAR exists, move only that older JAR outside mods to avoid duplicate mod IDs. Keep all other mods and worlds.
4. Select that same instance directory in RIMVISUALS, save settings, open official launcher, sign in and manually choose Fabric 1.21.11 / Loader 0.19.3.
5. Test in a disposable world: HUD, watermark, glow, particles, accent and scale; then change settings from the launcher and verify in-game updates. Right Shift opens the editor. Test profile saving/loading and persistence after restart.

Build success is verified separately from Windows installation, Store activation and in-game rendering. The latter require the user's Windows machine and a licensed authenticated Minecraft session. No in-game test is claimed by CI.

## Build
Launcher: Node 22, npm install, node --test tests/launcher.cjs, npm run check, npm run dist:win (Windows).
Mod: Java 21, Gradle 9.2.1, gradle -p mod build. Loom 1.14.10, Yarn 1.21.11+build.6.

Rebuild CI also captures the Viewmodel editor for layout review.

The scaled clipping regression is covered by the Viewmodel screenshot review.
