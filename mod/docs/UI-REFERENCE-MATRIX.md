# RIMVISUALS 0.4 — reference comparison and acceptance

## Inputs and status rules

Reviewed all 16 available attached files, named Screenshot 2026-11-02 182012 through 182318.
182134 is a narrow strip without legible controls. The request says 17 screenshots; only 16 files
were available. User-listed Animations/Atmosphere/Crosshair/Item Physic supplement the visible lists.
No Pulse source, logo, textures, icons or assets are included. Similar feature names do not establish
what Pulse's implementation does. Every “hook exists” below still needs gameplay validation; this
is a source/behavior inventory, not a claim of in-world verification or feature parity.

| Reference module | RIMVISUALS status | Actual scope / unresolved behavior |
|---|---|---|
| Animations | Hook exists | Viewmodel transforms and nine visual Swing styles; no attack timing change |
| Atmosphere | Partial | Sky/Fog/Weather/Biome/Water/Lava colour controls; not every atmosphere feature |
| Crosshair | Missing | Custom shapes/attack-indicator behavior needs a defined specification |
| Target ESP | Partial | Target Highlight/Entity Outline use vanilla outline pass and performance gates; no parity claim |
| Kill Effect | Hook exists | Client-observed target death triggers bounded particles |
| Item Physic | Missing | Item physics vs purely cosmetic dropped-item transforms must be distinguished |
| No Fluid | Needs clarification | Meaning unknown; Water/Lava recolour is not “No Fluid” |
| Particles | Partial | Decorative/combat/explosion/trail effects; no claim to all reference particle modes |
| Render Tweaks | Partial | Hurt camera, totem/crystal scale/fade, lighting and world colour hooks; no omnibus toggle |
| See Invisible | Missing / clarification | Entity visibility policy and server rules must be specified |
| Self Nametag | Missing | Rendering/filtering specification needed |
| Tab Customizer | Missing | Tab-list formatting not implemented |
| Trails | Partial | Sword/pearl/projectile particle trails, bounded emission; no mesh/ribbon trails |
| World Customizer | Partial | Existing individual world modules, not one fictitious all-in-one switch |
| World Particles | Partial | Existing particle effects; ambient distribution/coverage not matched |
| Armor HUD | Hook exists | Equipment, durability bars/percent and layout |
| Cooldowns HUD | Hook exists | Own inventory cooldown groups, cached at 20 Hz |
| Effect Notify | Partial | Totem/low-health/low-armor notifications; not all potion events |
| Hit Counter | Missing | Attempted attacks and server-confirmed hits are not equivalent; choose definition |
| Hotkeys HUD | Missing | Keybind manager exists; always-visible active-bind overlay is separate |
| Inventory HUD | Hook exists | Own main inventory, icons/counts/bars, layout and opacity |
| Low HP | New hook | Dedicated own-health threshold HUD, plus existing notifications; gameplay check pending |
| Potions | Hook exists | Own potion/status-effect HUD |
| Saturation HUD | New hook | Own-player hunger/saturation, refreshed at 10 Hz; gameplay check pending |
| Target HUD | Hook exists | Crosshair/recent target, observed health/distance |
| TNT Timer | New hook | Up to eight loaded primed TNT in 48 blocks, fuse/distance HUD; gameplay check pending |
| Totem Bar | New hook | Own main inventory plus offhand count; no opponent inventory access |
| Watermark | Hook exists | RIMVISUALS branding and saved layout/accent |
| Armor Notifier | Hook exists | Existing threshold/hysteresis notifications |
| Auc Helper | Needs clarification | Server commands, auction protocol and intended actions unknown |
| Auto Command | Needs clarification | Trigger, exact command and confirmation policy unspecified |
| Auto Duel | Needs clarification | Server protocol and automation rules unspecified |
| Auto Eat | Missing | Not part of this visual/UI checkpoint |
| Auto Invest | Needs clarification | Meaning and server behavior unknown |
| Auto Leave | Missing | Disconnect policy not implemented |
| Auto Potion | Missing | Inventory/interaction automation not implemented |
| Auto Reconnect | Missing | Connection automation not implemented |
| Auto Respawn | Missing | No automatic respawn action |
| Chat Helper | Partial | Normal chat and Edit HUD entry; no message automation |
| Command Safe | Needs clarification | No assumed command filtering/rewriting |
| Cooldowns utility | Partial | Cooldowns HUD exists; separate utility behavior unknown |
| Elytra Swap | Missing | No inventory swapping implementation |
| Fake Player | Missing | No synthetic gameplay entity |
| Fast Exp | Needs clarification | Desired actions/server limits undefined |
| Fast Swap | Missing | No automated inventory swaps |
| FC Helper | Needs clarification | Exact server/functions required |
| Free Look | Missing | Camera control not implemented |
| FT Helper | Needs clarification | Exact commands, server, rules and behavior required; no empty category |
| FT Regions | Needs clarification | Region data source/permissions and visualization undefined |
| Healing Helper | Needs clarification | Alert vs item use/command automation must be specified |
| HW Helper | Needs clarification | Exact server/functions required; no empty category |
| HW Joiner | Needs clarification | Connection/command behavior unspecified |
| Item Highlighter | Missing | Dropped-item vs inventory highlight behavior unspecified |
| Item Pickup Logger | Missing | No pickup event log |
| Item Scroller | Missing | Inventory interaction changes not implemented |
| Item Swap | Missing | Item choices, trigger and server constraints need specification |
| Lock Slot | Missing | No slot-interaction interception |
| Mace Helper | Needs clarification | No assumed automated combat behavior |
| Optimization | Hook exists | PR12 budgets, Safe Mode, opt-in CPU diagnostics; no FPS gain claim |
| Predictions | Needs clarification | Predicted object/path and permitted data unspecified |
| PVP Safe | Needs clarification | Meaning and safety policy undefined |
| RW Helper / RW Joiner | Needs clarification | Exact server/functions required |
| Shift Tab | Needs clarification | Do not capture Left Shift by default; desired behavior unspecified |
| Shulker Preview | Missing | Container-data tooltip is separate work |
| Sound Controller | Missing | Category/filter/fade behavior must be specified |
| Sprint | Missing | No automatic sprint interaction changes |
| Streamer Mode | Missing | Fields to redact and screenshot behavior unspecified |
| Tape Mouse | Needs clarification | No assumed synthetic input behavior |
| Totem Tracker | Partial | Own totem events exist; no opponent inventory tracking |
| Zoom | New render hook | FOV multiplier and smooth transition; Toggle Zoom can be assigned to keyboard/mouse; gameplay check pending |
| Local CFG | Exists + updated | Full module settings, HUD, performance preset and RIM keybinds; searchable list |
| Keybind Manager | New real implementation | Registered Minecraft bindings; keyboard/mouse assignment, capture, defaults, unbind, conflict confirmation |
| Local friends | New real implementation | Search/add/delete Minecraft names, persisted on this device; no cloud relationship claim |
| Local markers | New real implementation | Save current position/world/dimension, search/delete and matching-world distance HUD |
| Quick/death/event marks | Missing | No automatic death or event capture; manual current-position marks only |
| Cloud CFG/friends/groups | Future API phase | No cloud switches, fabricated network data or simulated memberships |

## UI and input contract

Right Shift opens ClickGUI, not HUD Editor. Left Shift and T are never globally intercepted.
Old user-assigned binds are preserved: the manager shows their current assignment and conflicts.
Minecraft Controls remains usable. RIM actions (ClickGUI, HUD, monitor, module switches) are
registered with vanilla KeyBinding, so keyboard/mouse dispatch and game-screen suppression are
shared with Minecraft rather than implemented by a global raw-input hook.

The menu has Visuals/HUD/Utilities, two-column cards, cached search, animated switches,
independently scrollable selected settings, CFG and local management entry points. A narrow
layout displays settings as a full detail pane with Back; a wide layout keeps cards alongside it.
Existing module settings, HSV/RGB/HEX editors, presets and profile actions are retained.
Escape closes the current menu/subscreen; chat's Edit HUD returns to its parent and restores text.
No screen, texture or font resources are created every frame while the menu is closed.

## Required Windows acceptance

1. In a RIMVISUALS test instance, verify current Minecraft Controls bindings. Right Shift opens
   ClickGUI; Left Shift still crouches; T still types in chat. If another mod shares Right Shift,
   change the assignment in Keybinds and verify both actions. Rebind a module to keyboard and to
   mouse, confirm/cancel a detected conflict, unbind/reset, restart and reload a CFG.
2. Type a nonempty chat draft; click Edit HUD; drag/scale/Shift-wheel/reset several elements;
   change accent in HUD settings. Close to chat and continue typing; restart and compare layout.
3. Test two columns, selecting settings, independent scrolling, search and Back/Escape at GUI
   scales 2/3/4 and a small window. Test long labels through hover tooltips.
4. Create/replace/cancel/copy/rename/delete/import/export CFG; search its name; verify that
   viewmodel/swing/colours/HUD/performance preset and RIM keybinds survive disk round trip.
5. Add/remove a local friend and a current-position marker, restart, switch dimension/server;
   marker distance HUD must only show matching-world marks. Malformed local data must remain
   preserved with an error, not silently overwritten.
6. Compare a fixed scene and identical settings with UI closed/open, Safe Mode, Balanced and
   Ultra. Warm up chunks; repeat 60-second samples. CPU timings are not GPU measurements.
   No controlled FPS improvement or in-world visual correctness is claimed by build success.
