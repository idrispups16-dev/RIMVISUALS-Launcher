# Browser-confirmed accounts (0.4.0-alpha.1)

Requires Minecraft 1.21.11, Java 21 and the existing Fabric dependencies. This is RIMVISUALS account authorization, separate from the licensed Microsoft/Minecraft login.

1. Start Minecraft with the new JAR. RIMVISUALS shows its account screen once on the title screen.
2. Select **Получить новый код**, then **Войти через сайт**. The browser opens `https://rimvisuals.vercel.app/account.html?device=CODE#device`.
3. Sign in with Google/email, check that the displayed code matches the mod, select **Проверить код**, then **Подтвердить мой вход**.
4. The account must have an active manual grant (issued through the existing admin page) or active subscription. Registration alone grants no mod access.
5. Return to Minecraft. Confirmation is polled every 5 seconds; **Продолжить** returns to Minecraft. Right Shift opens the existing menu when authorized.

An **Account** button on title/pause screens reopens account controls. Vanilla Minecraft remains available when unauthorized; RIMVISUALS HUD, held-item effects and menu are gated. This is not Microsoft authentication or a game launcher replacement.

One server session per account. Confirming a new device replaces the old session. The client validates every 30 seconds and fails closed after its maximum 75-second lease on network failure, revocation or replacement. Access expiry caps the lease; sessions expire after 7 days. No hardware fingerprint is collected. The website can revoke the device session.

The local `config/rimvisuals/mod-session.json` contains a bearer credential, not a password. Do not share it or include it in support archives. Deleting it logs out locally; use the website to revoke it remotely. File permissions are restricted on POSIX systems; Windows uses the existing user directory permissions. A copied token can be used until revoked: client-side code is not unbreakable DRM and cannot prevent a determined patcher or account owner from sharing credentials.

The GitHub workflow builds and runs an account-screen startup smoke check without a world or live account. Manual acceptance: real Google/email sign-in, grant access, approve matching code, test HUD/item effects, restart, replace session from a second instance, revoke on website, disconnect network for 75 seconds, check vanilla game still works. Existing visual profiles must persist.
