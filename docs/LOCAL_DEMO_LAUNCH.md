# Local / Demo direct launch on PR #5

Branch: feature/local-demo-launch; baseline PR #5 a7856af2db1ac28a26a46db47fe10ac560ef09b6. No new Fabric mod implementation, no third-party launcher, no Microsoft identity borrowed from another app.

## Scope and conditions

The local launch path uses Minecraft's native `--demo` flag. It is not a full/unlimited offline retail mode. The official Minecraft trial page describes approximately 100 minutes, a fixed Player name and offline play, with online multiplayer disabled: https://www.minecraft.net/en-us/free-trial . Minecraft's EULA and usage rules still apply: https://www.minecraft.net/en-us/eula . Yarn documents the client demo flag separately from offlineDeveloperMode: https://maven.fabricmc.net/docs/yarn-1.21.11+build.3/net/minecraft/client/RunArgs.Game.html . These references establish the native trial behavior; they are not Microsoft approval of RIMVISUALS.

The launcher always adds --demo, --disableMultiplayer and --disableChat for local records. It preserves Minecraft's own trial timer and world rules; it does not reset worlds/timers or change demo checks. All local labels use the trial name Player and a stable local UUID, not a Microsoft identity. The required CLI accessToken field contains a clearly non-credential Demo marker, never a borrowed/persisted account token. No OAuth request or Client ID is used. Server/quick-play/developer-offline arguments are rejected in this path. In particular, --offlineDeveloperMode is not used.

Microsoft records retain the separate official PKCE → Xbox → XSTS → entitlement → Minecraft profile path. Full game launch still needs that validation and a registered/authorized application; local records do not feed its session provider. The account/profile manager from PR #5 remains.

## Existing installation and offline cache

Minecraft 1.21.11, Fabric 0.19.3, Java 21 x64 and the exact embedded RIMVISUALS JAR are reused. Install / verify seals installation.json with a cache version and integrity records for metadata, libraries, assets, logging config and extracted native DLLs. A valid cache is loaded and checked entirely from local files, including the chosen classpath. Missing/changed files or launch metadata cause a normal verified repair through the existing official download pipeline.

PR #5 installations do not yet have the new integrity ledger. Their first launch needs a one-time metadata/checksum verification online to upgrade the cache; already valid game files are reused rather than downloaded again. Afterward Demo can start offline if Java, components and all checked files remain present. Nothing is imported from or written into the user's existing .minecraft. Demo world data stays in the selected RIMVISUALS profile; existing worlds are not deleted or reset.

## Test boundaries

Unit tests cover native Demo arguments, identity separation, server/developer-flag rejection, Microsoft session requirements, cache integrity/containment, local-vs-Microsoft coordinator dispatch and profile persistence. Windows CI prepares a real isolated profile, verifies a subsequent cache load with fetch disabled, executes Java --dry-run with the real classpath and KnotClient entry point, then actually attempts the native Demo process and captures its log.

The native process probe records whether Fabric and the embedded mod are observed. If hosted Windows graphics lacks OpenGL support, it records graphicsBlocked rather than claiming a game/world test. Non-graphics client failures fail the probe. The process is stopped after observation; no world is automatically created, modified or reset. Evidence is in demo-launch.log and demo-process.json. Electron UI tests check that Local selects PLAY DEMO rather than a blocked Microsoft button. Existing JUnit/Fabric smoke, packaged component checks and silent NSIS installation remain.

Even a successful Java PID / Fabric startup does not confirm a playable world. On the owner's Windows PC, select a local record, click PLAY DEMO, then Minecraft's Play Demo World button, and report whether the world loads. Microsoft sign-in, in-world visual appearance and FPS are not claimed from these checks.
