# Independent Windows launcher branch

Development branch: `feature/launcher-gpt`. No further writes to the Claude branch or main.
This branch inherits the existing standalone client and reviewed Claude Java provisioning patch. Changes in this continuation are restricted to the launcher, launcher tests, CI and documentation; no visual modules are changed.

## Changes

- Managed Java 21 x64 download from Eclipse Adoptium with SHA-256 validation, HTTPS redirect allow-list, streamed download size bound, ZIP path/symlink/expanded-size checks and rollback of the previous managed runtime.
- Full Microsoft → Xbox → XSTS → Minecraft entitlement → Java profile chain now has injectable transport for lifecycle tests. Production endpoints and PKCE are unchanged; no third-party Client ID or client secret.
- Tests cover interactive PKCE completion, refresh rotation, missing entitlement, invalid Java profile, changed Client ID, unavailable encrypted storage and cancellation/logout.
- Windows CI downloads and actually executes the managed Java runtime, checks offline reuse, and checks real Electron safeStorage persistence and removal using synthetic data.
- PLAY is disabled while the client is busy. Microsoft sign-in state is exposed without credentials. Removed obsolete device-code UI and external-launcher fallback wording. Settings reject malformed Client ID and non-string Java path.

## Verification boundaries

Node tests and production UI build run locally. Windows CI additionally builds the bundled Fabric JAR, installs actual Minecraft/Fabric files in a temporary directory, verifies embedded JAR hashes and metadata, and builds the NSIS installer.
A successful CI run does **not** prove licensed Microsoft sign-in, installation of the EXE on an end-user PC, or Minecraft reaching a playable world. These remain Windows user checks. No FPS measurements or visual correctness claims are made.

## One Windows check after the app registration is available

Install the artifact from this branch, enter your own public Application Client ID, click **Войти через Microsoft**, complete sign-in only in the system browser, then click **PLAY RIMVISUALS**. Report whether a Minecraft 1.21.11 Fabric window opens and reaches a single-player world; if it fails, provide the visible error and the launch log with any account identifiers removed. Never send passwords, authorization codes or tokens.

All game files and managed Java belong under `%APPDATA%\RIMVISUALS`; existing `.minecraft` and the website are outside scope. Keep the PR unmerged until the real Windows launch is confirmed.
