# RIMVISUALS (Fabric, Minecraft 1.21.4) - version 0.2.0 (source, NOT compiled/tested)

Cosmetic only. No gameplay advantages.

## What is implemented in the code
- ClickGUI (Right Shift, or the RIMVISUALS button on the title / pause screen): tabs, search, module cards,
  animated switches, sliders, mode buttons, RGB sliders + HEX field, profiles (save / load / delete).
- HUD editor (button "Edit HUD" in the menu): drag Info HUD and Watermark; positions are saved.
- Modules: Info HUD (FPS, ping, XYZ), Watermark, Pulse Glow, Particle Effects, Interface (themes, accent color,
  menu scale, HUD scale, animation speed, mobile mode).
- Themes: Violet, Blue, Midnight, Custom accent.
- Mobile layout: larger rows/buttons/sliders, touch drag-scroll. Mobile mode: Auto (heuristic) / On / Off.
- Settings: config/rimvisuals/config.properties; profiles: config/rimvisuals/profiles/*.properties

## Not verified (nothing was compiled or run)
Gradle build, loading in Minecraft 1.21.4, rendering/layout, touch input, Mojo Launcher, performance.
Expect to fix compile errors and visual details on the first build.

## Build
1. JDK 21. 2. Copy gradle wrapper from a template at https://fabricmc.net/develop (1.21.4).
3. Check versions in gradle.properties against the template.
4. ./gradlew build -> build/libs/rimvisuals-0.2.0.jar. Also install Fabric API in the mods folder.
Or push to GitHub: .github/workflows/build.yml builds the jar (Actions -> artifact rimvisuals-jar).
