# RIMVISUALS 0.3.0-alpha.2

This is a staged alpha, not the complete requested visual client.

## Implemented source behavior
- Safe Mode defaults on and suppresses RIMVISUALS screen glow and particle overlay. It never changes vanilla particle settings or world lighting.
- Low-End / Balanced / Ultra presets, user particle ceiling and FPS-dependent particle budget. Reduced Motion freezes decorative particles/glow and disables interpolated GUI/viewmodel motion and custom swing overrides.
- CPU submission average/peak timings for HUD total, HUD modules, glow, particles, viewmodel and swing; inactive measurements are marked. These are not GPU times, frame times, or measured FPS improvements.
- HUD data refresh is bounded to 10 Hz rather than rebuilding strings repeatedly every frame. Particle positions use fixed-size arrays, particle counts are bounded and offscreen 2D dots are skipped. No custom GPU resources, textures, or fonts are allocated.
- Opt-in Viewmodel: separate main/off-hand XYZ translation, uniform scale, XYZ rotation; sword/totem/crystal/food depth offsets; Default/Compact/Wide/reset presets; exponential transition smoothing.
- Opt-in visual Swing: Default, Smooth, Classic, Slide, Spin, Swipe, Push, Reverse, Minimal, Custom. Separate main/off visual speed, common angle/offset/intensity/easing. Only client matrix transforms change; attack logic and cooldown are untouched.
- Opt-in Hand Lighting: separate main/off switches and light level for first-person items. This is packed render light, not a bloom, outline, transparency or framebuffer shader. No Iris/Sodium integration has been tested.
- Existing Right Shift binding is rebindable through Minecraft Controls. Existing ClickGUI has search, numeric sliders, colors, HUD dragging and persisted module profiles. It now exposes Optimization/Viewmodel/Swing/Shader Hands categories; it is not the complete redesigned ClickGUI.

## Not implemented
Anchor/Crystal/Totem Fade; custom explosion/combat effects; trails/tracers; world/sky tweaks; shader hands glow/bloom/outline/gradient/blur/transparency/custom textures; separate item rotation or item FOV; the full expanded HUD; favourites; complete bidirectional launcher synchronization for new modules, profile import/export/backups.

## Verification
GitHub Actions builds the remapped Fabric JAR on Java 21. JUnit covers finite/bounded swing envelopes, rest at attack boundaries and frame-independent smoothing. An opt-in virtual-display dev client test audits mixins and renders ClickGUI before quitting; it does not enter a world or validate visual appearance, FPS, Iris or Sodium. Check the workflow result and test logs for each revision.

## Windows/game acceptance test required
Use Fabric 0.19.3 / Minecraft 1.21.11 and Fabric API 0.141.6+1.21.11. Replace only the previous RIMVISUALS JAR (move it outside mods); preserve worlds and other mods. Test a disposable world.
1. Compare the same scene/resolution/render distance with RIMVISUALS off, Safe Mode on, then Balanced. Record FPS over 60 seconds per run plus CPU monitor values. Repeat without shaders, then with your usual Sodium/Iris setup. No FPS improvement is claimed before this measurement.
2. Right Shift > Viewmodel: enable it; move each hand separately, change scale and rotations, switch sword/totem/crystal/food and reset. Test left-handed player settings and a map.
3. Swing: test every style with sword and empty hand, each main-hand selection. Confirm visual changes do not alter attack cooldown/damage. Reduced Motion should restore vanilla swings.
4. Hand Lighting: test each hand independently in a dark area, then disable. Test Iris/Sodium separately; disable this module if a rendering conflict occurs.
5. Change settings, restart, reload module profiles and verify persistence. New module settings are currently configured in-game; the 0.1.6 launcher only syncs its existing visual fields.
6. Report screenshots/video and latest.log or crash report for failures. A successful build or title-screen smoke test is not an in-world confirmation.
