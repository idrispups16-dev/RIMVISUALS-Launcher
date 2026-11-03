# 0.4.0 HUD, CPU diagnostics and CFG checkpoint

Historical PR12 baseline. The newer 0.4.0-ui.1 input/UI behavior is documented in
UI-REFERENCE-MATRIX.md; Right Shift now opens ClickGUI, and HUD Editor is a separate entry.

Branch: `fix/rimvisuals-hud-fps-gpt`, based on Claude's `decc476bbc78c0cd69108b44050153876636a8d1`.
No launcher, website, main or user Minecraft directory changes.

## Regression and HUD

The failing decc476 build compiled successfully, then failed an arbitrary `HUD.size() >= 14` assertion.
The required catalogue has 13 elements. The registry now derives every HudModule from ALL,
including the previously omitted Cooldowns/Notifications and a movable Performance Monitor.
The monitor retains the existing `performance.performance_monitor` switch for old configs.

In the PR12 baseline Right Shift opened HUD Editor; 0.4.0-ui.1 corrects this to ClickGUI.
The current editor has Color for selected-element settings and returns to its parent; title/pause RIMVISUALS entry points remain available. Chat's HUD Editor button
returns to the same ChatScreen and restores draft text and field focus on Done/Escape.
Drag selects an element; selection survives mouse release. Wheel scales; Shift+wheel changes
its opacity. Right click resets layout, scale, opacity and custom accent for that element.
Toolbar: Grid toggle, Monitor toggle, Reset one/all, Left/Center X/Right/Top/Center Y/Bottom.
The grid toggle retains the chosen spacing for the editor session. Magnetic guides remain active
independently of the grid. Elements fit to the viewport without overwriting the saved scale;
positions are clamped on drawing and dragging. Toolbar wraps on narrow screens.

All 13 HUDs participate in snapshot/CFG/JSON/backup, including custom accent, scale and opacity.
Opacity now accompanies deferred vanilla item-atlas submissions for Armor/Inventory/Cooldowns,
without creating extra framebuffers or rerendering the item model. Inventory counts and durability
bars use the same alpha-aware helpers. Custom oversized/special item renderers and third-party GUI
renderer replacements are not certified by startup/mixin tests. Interactive dragging, item opacity,
overlap and resizing still need the Windows world checks below.

## Work removed or bounded

- CPU timing collection is opt-in (Monitor or Diagnostic CPU timings); disabled begin/end calls
  skip clock reads and maps. Toggle resets samples/cached labels. Up to 64 sample categories.
- Diagnostic logging every 200 client ticks includes calls, cumulative CPU milliseconds,
  mean call time and peak. Existing Hand Glow/Color, Viewmodel, Swing, Fog, Crystal, HUD/Trails
  timers remain; outline decisions and terrain reload are also measured. These are inclusive
  CPU submission times, not GPU execution times or a frame-time benchmark.
- Safe Mode suppresses Hand Glow geometry shells, custom entity/target outlines, decorative
  overlays and optional combat/trail/explosion particles. Module switches/settings remain saved.
- Presets: Performance (Safe), Balanced (one glow shell, 32-block outline range, 24-particle cap),
  Ultra (up to four shells, 64-block outline range, 64-particle cap). Custom preserves manual values.
  Old numeric Low-End preset index 0 remains compatible, now labelled Performance.
- All Fx bursts/rings and trails share one per-tick emission cap. Unused trail reservations return
  to the budget. Bursts/rings beyond 48 blocks do not spawn; vanilla particles own their cleanup/culling.
  This caps emission, not the total population of vanilla particles already alive.
- World colour signatures use primitive values, ignore disabled module colour edits, and defer
  chunk rebuild until ClickGUI closes and changes settle. A 100-tick cooldown bounds reload frequency.
- Notifications no longer copy the note list during each size/draw query. Armor reuses stack arrays
  and lazy editor previews. Cooldowns scans the inventory at most 20 times/second; Target labels
  refresh at most 10 times/second while health-bar animation remains frame-driven.

No controlled FPS measurements were possible here. No FPS gain is claimed. This is not a complete
GPU profiler, allocation profiler, Iris/Sodium compatibility certification or leak soak test.

## CFG

Create never overwrites an existing file. Save of an existing name requires confirmation in GUI;
Rename/Delete/Restore also require confirmation. Load/Restore first back up current settings;
failed persistence restores the in-memory settings. Failed writes do not update autosave's saved
snapshot. Auto Save CFG is configurable under Interface; normal exit/editor close still saves.
Atomic replace is used where the filesystem supports it, with a replacement fallback otherwise.
Rename migrates server bindings; Delete backs up the profile and removes stale bindings.
Built-in PvP presets no longer silently overwrite a same-named user profile.
JSON accepts historical keys containing parentheses and percent signs; unsupported schemas are
rejected, legacy no-schema exports remain readable, imports choose a fresh profile name.
Unknown module keys remain inert; missing keys preserve current settings for legacy partial configs.
NaN/infinite numeric input is ignored. Backups retain ten snapshots.

## Automated evidence

GitHub Actions runs Gradle build/JUnit, a bounded xvfb client startup, mixin audit and ClickGUI
rendering. HUD tests serialize every registered HUD and a full module snapshot through real disk
CFG, JSON and backup. They compare restored values, including Viewmodel/Swing/preset values.
Smoke checks exercise viewmodel/swing matrices and HUD toolbar initialization/close. Vanilla
ChatScreen requires an in-world player; actual chat draft/focus return remains a Windows world check. These checks do not
load a gameplay world or measure FPS. Local Gradle was attempted but the environment denied a
network socket during Loom initialization; authoritative build results come from GitHub runners.

## Windows acceptance procedure

Use a disposable RIMVISUALS game directory and back up its config; leave `.minecraft` alone.
Use the same world, camera, resolution, render distance, Java/RAM, VSync/FPS limit and shader/mod
list in every run. Wait for chunks to settle. Record 60 seconds each: no RIMVISUALS; RIMVISUALS
with optional visuals off; Performance; Balanced; Ultra. Repeat each condition three times.
Record median FPS and frame-time spikes (an external frame-time recorder is preferable).

Then isolate Hand Glow, outlines/target highlighting and explosions/trails one at a time in the
same scene. Enable CPU diagnostics for a separate diagnostic pass; share RIM_CPU_DIAGNOSTICS
lines from latest.log with the preset, enabled modules and GPU/CPU details. Compare with diagnostics
off because profiling itself has a cost. No network credentials or tokens are needed.

HUD: type a draft in chat, click HUD Editor, drag every enabled element, wheel/Shift-wheel,
right-click one element, toggle grid and use every alignment button. Test 1920x1080 and a narrow
window, different Minecraft GUI scales, then restart and compare layout/colours/opacity. Confirm
normal chat typing after return. Check item-icon opacity separately, including enchanted items and third-party resource packs.

CFG: Create, Save/replace Cancel and Confirm, Copy, Rename Cancel and Confirm, Export/Import,
Delete Cancel and Confirm, Backup/Restore and restart. Bind a PvP profile to a test server,
rename it and reconnect; verify the binding still loads. Delete/recreate the same name and
confirm the old server binding is gone. Keep server authentication unchanged.
