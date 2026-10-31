# Register RIMVISUALS Microsoft sign-in

This desktop build uses Authorization Code + PKCE (S256) in the system browser. The previous Device Code Flow has been removed. There is no embedded password form, client secret, or borrowed launcher Client ID.

## One owner action: register the public desktop application

Open https://entra.microsoft.com/ > Entra ID > App registrations > New registration.

Use these values:

| Field | Value |
|---|---|
| Name | RIMVISUALS Launcher |
| Supported account types | Personal Microsoft accounts only (Xbox/Live/Hotmail), or any Entra tenant + personal Microsoft accounts |
| Platform | Mobile and desktop applications / public client-native |
| Redirect URI | http://localhost |
| Client secret | Do not create one |
| Implicit grant | Do not enable |

After registration, under Authentication > Add a platform, ensure **Mobile and desktop applications** includes `http://localhost` (not Web and not SPA). The launcher selects an available ephemeral localhost port; Microsoft ignores the port when matching a registered localhost redirect URI. It listens only on 127.0.0.1 and validates Host, path and a cryptographically random state. PKCE exchanges the authorization code using a fresh verifier.

Copy only the **Application (client) ID** from Overview into the launcher's Microsoft Application Client ID field and save. That ID is public configuration. Passwords, browser callback URLs, authorization codes, refresh tokens and client secrets must never be pasted into chat or GitHub.

Microsoft requires access to an Entra tenant and permission to register apps. If the portal does not let you register one, do not purchase a subscription just to work around the message without first checking the account/tenant requirement. This build cannot manufacture an application identity.

## Minecraft API access is a separate gate

A registered app is not a guarantee of access to Xbox/Minecraft services. If Microsoft sign-in succeeds but Minecraft returns HTTP 403 / Invalid app registration, preserve the non-secret error and consult Minecraft's application-registration guidance: https://help.minecraft.net/hc/en-us/articles/16254801392141 and https://aka.ms/AppRegInfo . Do not work around the restriction with another application's Client ID. No authenticated game launch is claimed until this chain succeeds with the owner's registered application and licensed account.

## What to test afterwards

Install RIMVISUALS, enter the public Client ID, save, and choose Sign in with Microsoft. Complete sign-in only on Microsoft's browser page. On return, the launcher must show the Minecraft profile name. PLAY then verifies entitlements and profile again, installs verified game files/components in APPDATA/RIMVISUALS and spawns Java 21 directly. Existing .minecraft files are not used or modified.

Java 21 x64 must currently be installed and selected or available in PATH; automatic Java runtime provisioning remains pending. The correct Java version and x64 architecture are checked before spawning Minecraft.

## Verified separately in CI

- Native Electron/Windows safeStorage encrypt/decrypt test with a fixed non-secret sentinel.
- PKCE RFC test vector, verifier/challenge pairing, exact redirect URI, one-time exchange, invalid/duplicate state rejection, cancellation, timeout, browser errors.
- Verified component installation, repeatability, backup on owned-component update, preservation of unrelated mods and modified destinations.
- Real Minecraft/Fabric download and hash validation in an isolated temporary directory; argument resolution and bundled JAR installation.
- Final unpacked EXE resources contain the real RIMVISUALS and Fabric API JARs and match their manifest hashes.

These automated checks do not prove real Microsoft consent, Minecraft API approval or a licensed in-world game launch.

Official references:
- https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app
- https://learn.microsoft.com/en-us/entra/identity-platform/reply-url
- https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow
