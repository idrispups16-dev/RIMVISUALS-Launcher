# RIMVISUALS 0.4.2-helpers.2

This update adds gameplay tracking to the helpers. It does not change existing visuals, main HUD, performance presets, launcher or site. The sole ClickGUI change is the helper-specific button opening the new settings screen.

## Sources and limits

- HolyWorld public endpoints: https://api.holyworld.me/v1/events and /v1/servers. Public API announcement: https://t.me/s/StarikZako?before=5152. HWEvents project description: https://modrinth.com/mod/hwevents (MIT; source URL absent in Modrinth metadata). Its published JAR wire-model fields were inspected solely to identify protocol fields; no implementation/assets were copied or bundled.
- Known event wire shape: server -> array of instanceId/id/metadata.displayName/metadata.rare. These fields identify listed active events; they do NOT provide a confirmed start time, end time or coordinates. RIM displays missing values as unknown. Disappearance from the listing is NOT presented as a confirmed completed event.
- The /servers JSON was accessible through web retrieval. Development HTTP requests to /events returned 403 / error 1010. No protection bypass, borrowed token, unofficial proxy or fabricated fallback is used. CI records an additional public availability probe in hw-api-probe.json. A working adapter is not proof of a successful live event feed.
- FunTime /event delay is confirmed on the official forum: https://forum.funtime.su/thread417655 . https://funtime.gg/ru/iventy/ describes /events and /event delay but this guide's official affiliation has not been independently established. No public authenticated or undocumented FunTime API is used. Manual refresh prepares /event delay in chat; the player must press Enter. The /events button similarly prepares the command, without automatically sending it.
- Current exact FunTime response/menu formats and server item cooldown constants remain unverified. The parser handles bounded semantic event lines and configured literal rules; unknown data is ignored. It does not assume 15/30-minute schedules or trap durations.

## Gameplay

Utilities > HW Helper / FT Helper > Open HW/FT Settings. Six tabs: Events, Cooldowns, Notifications, Regions, Keybinds, HUD Settings. HUD category continues to contain appearance/layout controls only. Events and cooldown tracking are enabled inside an enabled helper; helpers remain off by default and activate only on matching hosts.

Events: HolyWorld async API polling (20 seconds minimum, timeout, fixed HTTPS host, no redirects/auth, 256 KiB body cap, one outstanding request); optional server filter such as LITE_ANARCHY_1 and comma-separated event interests. FT receives events from system messages/action bar/sidebar and an opened matching /events container. Event menu title substring is configurable; item names and lore are read from visible slots only. Names, server/anarchy, explicit status, explicitly supplied XYZ and reported durations are shown. Unknown time/coordinates stay unknown. API entries become stale after 60 seconds without an update and disappear from the local list after five minutes; this is not an event-end prediction.

Cooldowns: Trapka / Plast item-name matching configurable per helper. Server CooldownUpdate packet is matched to the cooldown group of the named held/inventory item, including a recent use-attempt group when a consumed item disappears. Explicit server-message countdown prefixes are also supported (prefix + seconds or mm:ss). Progress uses nominal client seconds; it never changes the server's cooldown or attack logic.

Fallback is OFF by default and duration defaults to 0. Enable Estimated use-attempt timers and specify your measured duration only if needed. A real mouse use attempt is observed through Fabric callbacks; this does not prove the server accepted the use. Such rows always carry ~ and manual/use-attempt source, including READY notification. Server-confirmed packet/message data replaces the estimate. No invented server cooldown constants. A server denial can invalidate a manual estimate; disable/reset/reconnect rather than treating it as authoritative.

Dedicated editable Events, Cooldowns, Regions and Helper Notifications HUDs exist for both servers. Cooldowns show seconds, progress and READY; colors are per helper. Helper notices have a dedicated HUD and optional sound, so disabling the existing general Notifications HUD does not suppress them. All new HUDs use the unchanged editor and CFG machinery. Regions shows received region description only, not guessed boundaries.

## Binds

Existing Keybind Manager includes Toggle HW Helper, Toggle FT Helper, Toggle Events HUD, Toggle Cooldowns HUD, Toggle Regions HUD, Show/Hide Helper Notifications, Open HW/FT Settings, Manual Refresh Events, Toggle Trapka/Plast Timer Visibility. All are unbound by default. Contextual toggles affect the current matching helper, or both sets of preferences outside a server. The standard existing keyboard/mouse CFG serialization is used. Left Shift, chat and existing GUI binds are unchanged.

## Performance and lifecycle

No HTTP, parsing or inventory scanning in rendering. Cached tracker HUD updates at most four times/second, sidebar remains twice/second. Limits: 64 local events, 256 API candidates, eight cooldown groups, 54 visible event-menu slots, 32 message dedup entries, existing 64 incoming message/sec budget. Helper CPU timings appear in Performance Monitor. Disconnect/host switch/disable clears events, cooldowns, notices and pending-use groups, cancels current API future and rejects stale callbacks with a generation token. No FPS improvement or regression-free guarantee without a Windows comparison.

## Windows/server checks

1. Export existing CFG, install this JAR instead of the old RIM JAR. Enable matching helper in Utilities, open its settings. Enable dedicated HUDs and position them in the existing editor.
2. HolyWorld: enter a Lite Anarchy; observe API status. If HTTP 403/unavailable appears, report it. No events must be fabricated. If API OK, compare listed names/server IDs with real public events; unknown time/XYZ is expected when omitted by API.
3. FunTime: Manual Refresh prepares /event delay; press Enter once. Compare the actual server reply with Events HUD. Open /events yourself; if the menu title differs, set Events menu title contains. Report actual message/menu text if no entry appears. No automatic command loop.
4. Use a real trap/plast. A server cooldown packet should produce a non-~ timer if the custom item name matches. Otherwise configure the exact item-name substring or server CD prefix. Only enable ~ estimates with your measured duration and compare actual availability; READY on an estimate is not server confirmation.
5. Bind the helper actions (including a mouse button), save/export CFG, restart Minecraft and verify bindings/settings/HUD. Disconnect and switch to another server: no retained events/timers/sounds. Disable notifications and verify both sound and helper notice stop.
6. Compare helper OFF/ON in the same scene, view distance and visual preset; record average FPS, frame-time spikes and helper CPU timings. Public HolyWorld/FunTime gameplay and Windows restart/FPS are user tests, not compilation claims.
