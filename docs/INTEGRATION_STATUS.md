# Launcher / visuals integration

Branch: `integration/launcher-visuals`; source snapshots:
- PR #2: `b5cf39c86c35dcd766b44d4bbe48d23ae6e8401c` (`feature/launcher-gpt`).
- PR #3: `327d604fb2e81c7b426ba3e1705faa3073eb5e9f` (`feature/visuals-claude`).

Three merge conflicts: Java runtime, Java tests, launcher UI. Kept PR #2's bounded downloads/extraction, rollback, PKCE UI and tests; retained PR #3's visual implementation. No changes to Claude's module or render-hook implementation. A pre-existing Viewmodel config collision discovered by the smoke test is fixed: legacy sword_z/totem_z/crystal_z/food_z remain depth-offset keys; separate category Z settings now use *_category_z. Other category transform keys are unchanged. The old colliding file could store only one value for each pair; the unavailable second value cannot be recovered automatically. Source branches and main are not updated.

## Version contract

| Component | Integration value |
|---|---|
| Launcher | 0.2.0-integration.2 |
| RIMVISUALS JAR | 0.3.0-integration.2 |
| Minecraft / Java | 1.21.11 / 21 x64 |
| Fabric Loader / API | 0.19.3 / 0.141.6+1.21.11 |
| Yarn / Gradle / Loom | 1.21.11+build.6 / 9.2.1 / 1.14.10 |
| Launcher settings bridge | schemaVersion 1, unchanged |
| Bundled manifest | schemaVersion 2, explicit component identities/versions and compatibility |

The distinct JAR version avoids replacing alpha.3 under the same filename. Hash-matching older managed JARs are backed up before removal; unrelated mods/configs remain untouched. A modified old managed JAR blocks the update before new files are installed, avoiding duplicate engines. The runtime reads fabric.mod.json and checks identity/version/game compatibility, in addition to SHA-256. There is no remote executable or mod update channel: updates are supplied by a new launcher installer.

Existing launcher fields still map only Pulse Glow, decorative Particles, Watermark, Info HUD, accent and HUD scale. Claude's additional settings remain in config.properties/profiles and ClickGUI; no claim that all new settings can be edited from the launcher. Smoke assertions check that applying old bridge properties preserves the new visual settings and that module config round-trips without key collisions.

## Validation

- 48 Node tests and production TypeScript/Vite build locally.
- Windows CI builds and embeds the JAR from this exact commit, checks expected new module classes and metadata, verifies actual installation/reinstallation and config preservation, downloads and runs Java 21, exercises native encrypted storage, installs Minecraft/Fabric into temporary storage, packages EXE and performs an isolated silent NSIS installation with installed-file hash checks.
- Fabric CI runs Gradle/JUnit and client startup/GUI/mixin audit plus the configuration assertions.

Compilation, title/GUI smoke and silent NSIS installation do not prove Microsoft sign-in, visual appearance in a world, performance or successful licensed game launch on the owner's Windows PC. Keep the integration PR in draft. No changes to the production website or existing .minecraft.

After the first successful integration CI, launcher diagnostics were added: the home-screen button writes logs/diagnostics.json with component versions and missing/verified/modified status. It does not read account.enc or export Client ID, tokens, profile names or custom Java paths. It does not claim game launch success or FPS measurements.
