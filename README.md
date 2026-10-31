# RIMVISUALS Launcher 0.1.5 — Windows source

This is **source code**, not an already-built EXE. Minecraft Java 1.21.11 + Fabric is the target.

## What works in the source
- Electron desktop UI with Play / Visuals / HUD / Profiles / Settings.
- Local preferences saved in Electron userData (`settings.json`).
- Detects `%APPDATA%\.minecraft`, installed Fabric version folder names, and presence of a JAR whose name begins with `rimvisuals`.
- PLAY opens the **official Minecraft Launcher**, not Minecraft itself. It does not select the Fabric profile or bypass Microsoft authentication.
- Settings for visual effects are **not yet connected to a Fabric mod**. The existing Java source targets Minecraft 1.21.4 and needs porting to 1.21.11, compilation, and in-game testing.
- RAM/Java fields are stored only; they do not modify Minecraft launch parameters.

## Get a Windows installer without Node.js on your PC
1. Create a new GitHub repository dedicated to the launcher, or upload these files to an existing launcher repository (do not replace the website repository).
2. Upload **contents of `RIMVISUALS-Launcher/`** including the hidden `.github/workflows/windows-installer.yml` directory, preserving the folder structure.
3. Open Actions > RIMVISUALS Windows Installer > Run workflow.
4. Wait for a green check. Download `RIMVISUALS-Windows-Installer` under Artifacts and extract it.
5. Run `RIMVISUALS-Setup-0.1.5.exe`. This is unsigned and Windows SmartScreen may warn; only run software you trust.

**No successful GitHub Actions run or Windows EXE has been verified yet.**

## Developer build
Requires Node.js 22+ and npm: `npm install`, `npm run desktop`; on Windows `npm run dist:win`.

## Security
Do not upload Minecraft account/session files. The app does not read Microsoft tokens. Do not distribute a Windows installer until tested on Windows.
