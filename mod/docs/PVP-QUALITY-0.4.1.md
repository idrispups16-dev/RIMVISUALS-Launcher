# 0.4.1-pvp.1 — PvP quality checkpoint

Based on PR14 / 219f714. New branch: fix/rimvisuals-pvp-quality. This is a test build, not a
claim that reported PvP appearance/FPS problems have been reproduced or resolved on Windows.
Original PRs, main, launcher, site and user Minecraft folders are unchanged.

## Concrete findings and changes

- Safe Mode previously returned zero for every combat particle request. It now has an explicit
  Limited / Disabled setting. Limited permits at most six custom effect particles per tick;
  a manual global limit of zero still disables particles. Decorative overlays/glow stay suppressed.
- Hit/Sharpness could exhaust the cap before Critical ran. One attack now shares available
  capacity among its applicable effects. Damage/trails each receive a lower-priority sixth of the cap,
  leaving capacity for attack/explosion feedback and avoiding continuous trails starving damage bursts. With extremely small user caps, simultaneous
  effects can still be dropped; this is an intentional cap, not guaranteed emission per effect.
- Adaptive quality no longer rounds a positive allowed combat request to zero. Limits remain
  shared across all sources. Cards show LIMITED or PARTICLES OFF instead of generic enabled.
- Per-effect Opacity reaches the actual BillboardParticle alpha field. RGB/size still use vanilla
  DustParticleEffect; vanilla lighting, natural dust variation and shaders affect final appearance.
  Opacity changes apply to newly emitted particles, not every particle already alive.
- Criticals retain an immediate client estimate and add a deduplicated server animation fallback
  (animation 4) for the recent target. The packet does not identify the attacker: this is recent-
  target feedback, not authoritative attribution of a player's damage/critical hit.
- Damage Particles defaults to server hurt packets, which do not reveal damage amount. The old
  health-delta mode remains available; it requires the server to actually publish health changes.
- Explosion ring and sparks share an event's allowance so the ring does not consume it all.
- Diagnostic logs add cumulative attack/crit/hurt/explosion packet counts and requested/admitted/
  actually-created particle counts. These distinguish missing events from cap suppression or a
  particle factory returning null. Enable Diagnostic CPU timings; inspect RIM_CPU_DIAGNOSTICS.

## Fog

Custom Fog now offers a clear Visible range (start/end in blocks) plus Legacy percent. Old configs
with only fog_distance_(%) migrate to Legacy. The previous default 100% changed no distance;
far/horizon fog can be inconspicuous during close PvP. Visible range updates actual render-distance
fog uniforms; start/end are ordered. Only controls for the active mode are shown. Fluid fog is
opt-in so air settings do not unexpectedly replace water/lava visibility. Custom Fog takes priority
over Atmosphere. Colour uses the same vector passed into the fog uniform and returned as clear colour.

Iris/Sodium presence is detected, not certified. The UI explicitly warns that an active Iris shader
can replace fog. No shader pack is silently disabled or modified. Compare vanilla, Sodium only,
then Iris with shaders OFF/ON using the same scene. Compatibility is unconfirmed until those tests.

## Visual Hitbox

Depth-tested outlines around copies of existing entity bounds. Players/mobs switches, selected-
target-only, range, RGB, alpha and width in world units. Default selected-target-only; spatial
candidate refresh at 10 Hz; weak entity references. Caps: Safe 2, Balanced 4, Ultra 8. Twelve thin
prisms per entity share one custom submission, use cached topology/vanilla render layers, and own
no texture/framebuffer. No entity bounds, reach, positions, attacks or packets are changed. A bytecode
guard rejects entity mutation/network calls in this overlay. Shader/depth correctness still needs a world.

## PvP_Clean and motion

Profiles > PvP Clean is explicit opt-in and backs up settings before application. It uses Safe Limited
particles, smaller cyan/purple/gold bursts, short eased Hit Marker, restrained Swing and Viewmodel,
normal hand colour/lighting and target-only hitbox. Damage Tint and heavy hand glow are left off;
Tint has subtler values if enabled later. Crystal/totem settings are prepared conservatively, but
those modules remain opt-in. Existing Minimal/Crystal_PvP/Sword_PvP profiles remain available.

Viewmodel reuses per-hand target arrays instead of allocating one each submission. Damage Tint's
fade is eased; Hit Marker uses eased opacity. Crystal/Totem curve helpers were retained. Startup
checks validate actual 1.21.11 totem-quad submission routing plus fade endpoints; this is not visual
proof of a totem pop in a world. Reduced Motion is respected and Swing suppression is labelled.

## Reproducible particle scenarios (test one module first)

Use a disposable local world / permitted private PvP server, particles at a normal setting, no
shader pack initially. Enable Diagnostic CPU timings. Start with global cap 24, Safe OFF. Then
repeat with Safe ON + Limited and finally Disabled to verify the policy. Wait for old particles
to expire after changing colour/size/opacity. Confirm module settings survive save/restart.

| Effect | Reproduction | Expected / diagnostic distinction |
|---|---|---|
| Hit | Enable only Hit Particles; strike a nearby living mob/player, not a block | Immediate client attack-attempt burst; attacks counter rises; not proof of damage acceptance |
| Critical | Jump, stop sprinting, attack while falling with a charged attack; compare standing attack | Critical estimate or recent-target server animation burst; crit-packets can confirm an animation, without attacker attribution |
| Sharpness | Compare an ordinary sword with a Sharpness-enchanted sword | Enchanted held item produces the extra chosen-colour burst; level is read from held-item enchantments |
| Damage | Enable only Damage Particles, Trigger=Server hurt packet; damage a nearby target, then Targets=Self and take damage | Hurt packets trigger a fixed count; Health loss mode is separately tested where synced health is available |
| Explosion | Enable Explosion Effects; ignite TNT / explode crystal/anchor on a permitted disposable world | Ring and sparks both receive bounded capacity; explosion packet counter rises; no block/server changes |
| Combined | Enable Hit + Critical + Sharpness, use an enchanted sword for a falling charged hit | Applicable effects share allowance; compare cap 24, Safe cap 6, and cap 0; do not expect unlimited bursts |
| Opacity/size | At close range compare alpha 1 vs .2, size .5 vs 1.5, cyan vs red | New particles visibly differ; inspect created count if nothing appears; light/shaders still affect final pixels |
| Trails | Throw a pearl/projectile and swing a sword, alone then alongside damage effects | Nearby bounded dust samples; lower priority than interactive bursts; limited modes can reduce density |

## Fog / hitbox / other visual acceptance

1. Overworld daytime, open terrain: enable Custom Fog, Visible range start 4/end 20, Override color ON,
   strength 1, an obvious colour. Compare toggle OFF/ON, then start 12/end 48 and weaker blend. Check
   Legacy 50/100/150%, fluid opt-in, Atmosphere priority, Sodium, Iris shader OFF/ON separately.
2. Enable Visual Hitbox; aim at a player then mob. Test target-only vs nearby, player/mob switches,
   range 8 vs 32, width .005 vs .04 and opacity .2 vs 1. Walk behind a wall to check depth behavior.
   Compare real F3+B boxes where allowed; confirm attacks/reach and collision behave identically.
3. Apply PvP Clean after saving a named CFG. Check both hands with sword/totem/crystal/food, attack
   styles and return to defaults. Test crystal spawn scale/fade and a real totem pop separately.
   Check Hit Color, Hit Marker, Damage Tint, crosshair cooldown bar and all HUD layouts after restart.
4. Fixed camera/scene/resolution/render distance/shader/mod list: warm chunks and repeat three
   60-second samples without mod, optional effects off, PvP Clean, then individual effects. Record
   frame-time spikes and FPS. Diagnostics itself adds cost; compare it OFF/ON. No FPS gain is claimed.

Return: build version, enabled preset/modules, GPU/CPU, shader/mod list, short video and diagnostic
lines for a failed scenario. No account credentials, passwords or tokens are needed.
