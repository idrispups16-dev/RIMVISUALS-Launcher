# RIMVISUALS 0.4.2 server helpers

Based on tested 0.4.1-pvp.1. Existing visual defaults and performance presets are unchanged.

## Public evidence (2026-11-02)

| Module | Confirmed from Pulse's public announcements | Assumptions / unknowns |
|---|---|---|
| HW Helper | Trap timers, drill area, compass/spawner/auction information advertised | Exact detection, durations, packets and settings unknown |
| FT Helper | Trap/platform cooldowns and drill area; later announcement mentions sound detection | Sound identifiers and duration rules unknown |
| FT Regions | Region dimensions and held-region-block preview advertised | Region data format, block mapping and protection rules unknown |
| HW Joiner | Quick join and event joining advertised | No public protocol; RIM implements only manual confirmed connection |
| FT Joiner | Seen in references | Exact behavior unconfirmed |
| Auc Helper | Resell/search and Knockback enchantment highlighting advertised | Price formats and menu identification unconfirmed; no automation implemented |

Sources: https://t.me/s/pulsevisuals?before=174 (public announcements, not source code);
https://t.me/holyworld (hub.holyworld.ru, hub.holyworld.me; 1.21.11 listed);
https://t.me/s/Funtime (play.funtime.su; channel lists through 1.21.8, so 1.21.11 connectivity requires a server test).

## Own implementation, not a Pulse clone

Utilities: HW Helper / FT Helper, disabled by default. Host matching uses exact domains or subdomains with a dot boundary. Additional exact hosts can be supplied (comma separated); aliases are trusted only when explicitly configured. This is host selection, not proof of server identity.

Client system chat, action bar and sidebar only. No player private-message collection, remote API, outgoing commands, purchases or automatic reconnect. Sources can be disabled independently. Sidebar is sampled twice a second, max 15 rows parsed, four shown. Action bar expires after five seconds. No parsing in render callbacks. HUD strings cached twice a second. Unchanged sidebar/action-bar text is not re-parsed, preventing countdown resets from repeated display. Incoming messages are limited to 64 per second. Helper tick CPU timings appear in the existing Performance Monitor when diagnostics are enabled.

Rules are case-sensitive literal prefixes, blank by default. No regular expressions or guessed formats. Event prefix extracts text. Timer prefix must be followed by seconds or mm:ss, e.g. `Timer: 1:30`; 0..86400 seconds accepted. Optional text after the number is the timer label; at most eight active labels are retained. Region prefix extracts the received region description on FT. It does NOT invent region bounds or claim a region overlay. Unknown/malformed messages are ignored. Actual received lines retain their source meaning; user must select prefixes matching trusted server messages.

HUD shows received sidebar/action bar, event, up to eight named received timers, FT region description and local low-health/armor warnings. Each server has its own draggable/scalable/opacity/color HUD in the existing editor. Notifications use the existing Notifications HUD (enable it to see popups). Server change, world exit or disabling helpers clears session state. Settings, aliases, prefixes and HUD layout use the existing CFG serialization and reset. Separate server CFG can be saved from each helper's settings; existing Profiles screen can bind the saved CFG to the current server address. Saves use unique names and never overwrite existing profiles.

Manual server connection opens a confirmation for each official address; no retry loop. Auction watch is an own read-only filter: configure the exact container title prefix and item-name substring; it counts matching visible container stacks (excluding player inventory) and optionally notifies when matches first appear. No prices or menu formats are assumed, no clicks or purchases. Full Auc Helper automation, automatic sound-based trap detection, drill/region world geometry and unknown server-specific cooldowns remain unimplemented until data formats are verified. Existing Cooldowns HUD continues showing normal client item cooldowns.

## Test on servers

1. Preserve/export your current CFG. Enable the matching helper in Utilities and its HUD in HUD; optionally enable Notifications. Connect manually. Wrong servers and singleplayer must not activate helpers.
2. Observe actual sidebar/action bar. Configure a literal event/timer/region prefix from a real received line. Without a matching rule there must be no invented event/countdown. Check seconds/mm:ss and malformed lines on a controlled test server using an explicitly configured alias.
3. Move/scale/color HUD, Shift-wheel opacity; save/reload CFG, restart client and verify values. Save separate server CFG and bind it in Profiles if desired.
4. Check low HP and equipped armor thresholds; warnings should rearm after healing/repair. Disconnect/switch servers and verify all event/region/timer data disappears.
5. Compare same scene/settings with helpers disabled/enabled. No FPS improvement claimed. Public server behavior and message formats are not verified by startup smoke tests. Smoke checks exercise synthetic received messages, source toggles, duplicate suppression, both host matches, wrong-server/disable cleanup and config reset/restore without making a connection.
