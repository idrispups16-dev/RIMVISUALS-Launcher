# Effects visibility fix — 0.4.5-effects.1

The supplied PvP resource pack replaces the vanilla generic dust sprites with
256×256 images containing just one visible pixel. Previous effects created
particles successfully, but inherited those almost invisible sprites.

- Combat particles and trails now use `rimvisuals:spark`, a separate local particle
  type and sprite. Vanilla dust and resource-pack files are unchanged.
- The existing per-tick emission caps, opacity controls and access checks remain.
- Hit Color now converts strength to the inverse alpha used by `entity.fsh`:
  strength 1 means full tint, rather than no tint.
- Pearl Trails now respects its existing own-projectiles-only setting.
- Hit Marker explains its existing F1/HUD visibility requirement in its status.
  Sharpness Particles still requires a Sharpness-enchanted main-hand item.

Validation: the two new regression tests failed on the previous implementation;
the complete suite then passed (110 tests). An isolated Minecraft startup with
mixin audit confirmed the registered sprite and existing screens load.
This does not replace an in-world check on the user's servers and mod pack.

No public download endpoint is re-enabled by this change.
