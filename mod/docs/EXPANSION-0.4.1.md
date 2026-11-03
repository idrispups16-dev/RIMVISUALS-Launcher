# RIMVISUALS 0.4.1-expansion.1 — scope and Windows acceptance

Historical PR14 baseline. The newer PvP quality branch changes Safe Mode combat particles
to explicit Limited/Disabled and adds Visual Hitbox; see PVP-QUALITY-0.4.1.md.

Based on PR13 / 8aa4a0e. This branch retains ClickGUI, HUD Editor, local CFG, keybinds and PR12
budgets. It does not change the launcher/site/main/source PRs. No Pulse code or assets are used.
New optional modules are off by default; Item Swap is an explicit unbound action with settings.

## FPS work, not a measured FPS fix

Damage/trail processing uses spatial entity queries instead of a full loaded-world enumeration.
Safe Mode also skips damage-health scanning and anchor model creation/submission. Anchor ghosts
are limited to eight and 48 blocks. Combat target references are cleared on world change/leave;
Session HUD no longer holds a strong world reference. Geometry uses existing vanilla render
layers and owns no new framebuffer/textures. Hat circle samples are cached and geometry is capped.

Diagnostic CPU timings additionally log HUD callback intervals: mean, p95 upper bound (1 ms bins),
and worst interval. Long pauses >=1 second are excluded and explicitly labelled. This is a bounded
histogram, not a GPU timer or controlled benchmark. Diagnostic mode is opt-in and has a cost.
No increase in FPS, resolution of 30 FPS drops, GPU compatibility or leak soak-test result is claimed.

## New behavior and limits

| Module | Real implementation | Windows world scenario |
|---|---|---|
| China Hat | Conical depth-tested geometry, third-person self; optional players within 32 blocks, 4/8 player caps and 16/32 segments | Disable Safe Mode, use third person, crouch/walk, compare other-player toggle; verify shader/pose behavior |
| Block Overlay | Translucent bounding box of the selected block's outline shape | Aim at full/slab/stair blocks, change colour/alpha; confirm interaction shape is unchanged |
| Crosshair | Replaces first-person crosshair with colour/gap/length/thickness/dot; optional cooldown bar | Compare idle/attacking, hidden HUD, first/third person and spectator |
| Aspect Ratio | Changes camera projection matrix horizontal scale only | Compare matching ratio, 4:3 and wide ratios; GUI/mouse/targeting remain vanilla |
| Atmosphere | Shared sky/fog palette and blend | Test Dusk/Night/Day/Custom; existing enabled Custom Sky/Fog have priority; no clock/weather/light simulation changes |
| Item Physics (visual) | Ground tilt, extra airborne rotation, scale of dropped-item render | Drop flat/block items, wait for landing, change scale/tilt; vanilla bob remains; pickup/physics unchanged |
| Shulker Preview | Cached 27-slot container-data tooltip, no server inventory requests | Hover a shulker with empty interior slots and known counts, modify contents and hover again; test resource packs |
| Sound Controller | Overall/music/ambient/weather/hostile multipliers in SoundSystem; original volume options untouched | Play each category, change multiplier, disable module and verify original loudness; headless CI cannot hear audio |
| Item Swap | Explicit key uses vanilla SWAP to offhand; default requires crouch, only player inventory handler, 10-tick rate limit | Put a totem/shield/pearl/crystal in inventory, bind action, crouch+press; check server acknowledgement and both stacks. No repeated automatic combat action |
| Assigned Keys HUD | Up to eight configured RIM bindings, cached at 10 Hz | Assign/unbind keyboard/mouse actions and verify HUD after refresh/restart |
| Compass HUD | Local heading/yaw, cached at 10 Hz | Turn through cardinal directions, move/resize/recolour HUD and restart |

Safe Mode intentionally suppresses costly world geometry/effects. Their cards show SAFE MODE
instead of silently presenting an ordinary enabled state. Other particle budgets still apply.
Block Overlay uses a shape bounding box, not a detailed wireframe of every voxel face. China Hat
is a cosmetic cone, not a skinned head model for every swimming/sleeping pose. Aspect Ratio does
not alter server hitboxes. Item Physics is not a replacement physics engine. Atmosphere does not
replace dimension sky geometry. These limits must not be described as Pulse feature parity.

## Automated validation

Gradle/JUnit and bounded xvfb Minecraft startup run after each block. Startup audits mixins and
preserves the existing config/GUI/keyboard/mouse tests. A real shulker container component with
an empty interior slot verifies tooltip-data injection and Fabric component renderer registration. A startup fixture also
renders this actual tooltip to a screenshot without a gameplay world.
Unit tests cover projection scaling, geometry limits, sound multiplication and frame histogram.
All new module settings participate in the existing full snapshot/CFG/JSON/backup tests; new HUDs
participate in the all-HUD position/scale/opacity/accent test. These do not prove world appearance,
actual inventory transaction acknowledgement or audible sound correctness.

## Required performance comparison

Use the same disposable world, camera/resolution, render distance, Java/RAM, shader/mod list and
FPS cap. Warm up chunks, then record three 60-second runs each: no RIMVISUALS, effects disabled,
Performance/Safe, Balanced, Ultra. Record frame times as well as FPS. Isolate Hand Glow, outlines,
particles/explosions and each new world effect separately. Compare diagnostics OFF and ON.
Report GPU/CPU, enabled modules/preset, scene and RIM_CPU_DIAGNOSTICS lines. Startup UI screenshots
and compilation cannot establish that an intermittent 30 FPS drop is fixed.

## Still missing / requires specification

The full Pulse catalogue is not complete. Shaders/blur beyond existing hand effects, detailed
item-physics simulation, remaining unspecified render tweaks and server utilities remain separate
work. HW Helper and FT Helper require exact server names and a list of desired commands, alerts
or inventory actions before implementation. No empty HW/FT categories or cloud services are added.
