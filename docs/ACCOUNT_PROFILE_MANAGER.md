# Accounts and game profiles on the PR #4 baseline

Branch: feature/accounts-and-profiles, based on 07bacbf75b83f2f2493430500613ccc3822eed9a. No changes to main, PR #4, the website or the Fabric mod implementation.

## Reference and independent implementation

Official references reviewed:
- https://llaun.ch/Ru
- https://docs.legacylauncher.ru/launcher/source
- https://docs.legacylauncher.ru/launcher/subfolders
- https://docs.legacylauncher.ru/launcher/settings
- https://github.com/LegacyLauncher/archive (LICENSE.txt; AccountManager, ProfileManager and JavaManager inspected as reference only).

The source archive is described as reference-only / all rights reserved. Its manager classes separate account providers, profile persistence and runtime management. RIMVISUALS uses independently written Node/Electron stores and UI, not Legacy code, assets, databases, Client IDs or authentication services. The direct source-download endpoint returned HTTP 403; the archive linked by the official documentation was available for inspection. No Legacy executables were run.

## Implemented behavior

- Multiple Microsoft account records, explicit selection/removal/logout; one encrypted Windows vault for refresh tokens and separate non-secret metadata. No token is returned over IPC or included in a profile export.
- Official Microsoft Authorization Code + PKCE remains the licensed game-launch provider. Login/refresh/entitlement/profile checks retain the existing chain. No borrowed application identity.
- Local records are independent labels for configuration and installation checks, not Minecraft identities or licensed sessions. They cannot launch the full game. No alternative skin/authentication service or auth-injection agent.
- Selected account belongs to the active game profile. Adding a Microsoft account selects it for that profile. Switching profiles selects its linked record; deleting an account clears its links.
- Existing encrypted account.enc migrates to accounts.enc only after the new vault is saved and verified; the old single-token file is then removed. Failed migration preserves the original and reports a generic message without decrypted data.
- Existing settings.json and profiles.json remain intact. Their presets migrate as profiles using the original RIMVISUALS directory, so existing worlds/configs are not moved. Newly created/imported profiles use APPDATA/RIMVISUALS/instances/<generated UUID>.
- Per-profile RAM (2–32 GB), Java path/native chooser or automatic Java, visual settings, rename/create/select/remove/reset and JSON import/export. Export excludes account links, Client ID and paths; import creates a new isolated profile with automatic Java. Removing a profile removes its record, retaining its worlds/files.
- Profile metadata writes are atomic with a previous-state .bak. The mod bridge stays schema 1; config.properties and mod profiles are not replaced.
- Minecraft/Fabric compatibility remains 1.21.11 / Loader 0.19.3 with the exact tested PR #4 visual JAR. Other game versions are rejected rather than shown as supported. Automatic Java 21 x64 is shared under the base runtime directory; libraries/assets are currently installed and validated per game directory.
- Install / verify is a real preflight: embedded JAR validation, Java installation/probe, Minecraft/Fabric/assets/natives installation in the selected directory. It performs no OAuth and spawns no Minecraft game process. PLAY separately requires a Microsoft provider and successful entitlement checks. Operations lock profile/account changes while sign-in, installation or a game is active.
- Existing direct Java launch, diagnostics and the single authoritative JAR-to-EXE pipeline are retained.

## Local testing without Azure

Supported: local account/profile persistence, imports/exports, preparation/download/integrity checks, synthetic OAuth lifecycle tests, native Windows encrypted-storage tests, and Fabric Loom development startup/GUI/configuration smoke in CI. Fabric documents runClient as a development/playtest task: https://wiki.fabricmc.net/documentation:fabric_loom . It is not a retail offline-login feature.

Minecraft offers an official time-limited trial: https://www.minecraft.net/en-us/free-trial . RIMVISUALS does not replace the official trial flow with an unverified full-game launch. Using a local name, dummy access token, another launcher's credentials or a skin-service account is not a license check.

An owner-registered/authorized Microsoft application and any necessary Minecraft API approval are still needed for RIMVISUALS Microsoft sign-in. Local checks deliberately do not remove this requirement.

## Verification boundaries

Node tests cover multi-account isolation/logout/restart, migration failure, local-provider launch denial, profile settings/isolation, import validation, config/world preservation and coordinator locking. Windows CI additionally tests native multi-account encryption and the real selected-profile preparation path using a local record without OAuth; PLAY must fail before spawn. The authoritative Fabric build/smoke and NSIS install checks remain.

These checks do not establish licensed login on the owner's PC, reaching a playable world, in-world correctness of visuals or FPS. The new branch/PR stays unmerged pending Windows testing.
