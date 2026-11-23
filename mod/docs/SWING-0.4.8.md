# Swing attack fix — 0.4.8-swing.1

The reported setup uses Spin. Previously Spin rotated by 360 degrees times a
bell-shaped envelope, becoming identity at the exact middle of the hit before
rewinding. Spin now advances through a directional phase, so the mid-hit pose is
180 degrees instead of visually returning to rest.

Actual Minecraft doAttack swing invocations for entity hits and air swings start a
separate cosmetic clock. The first-person item and empty-arm render paths use it
even when their vanilla swing input is zero. Duration/speed settings are applied
once. Rapid repeats cannot continuously pin this visual clock to zero.

Block breaking retains its vanilla-driven visual phase; beginning to break blocks
clears the cosmetic attack override. World changes clear both hand clocks. Default,
disabled, account access and Reduced Motion behavior are preserved. No attack rate,
cooldown, reach, player swing ticks or network packets are changed by this clock.

Regression: Spin's mid-hit matrix test failed before the change and passes after.
Tests also cover zero-vanilla-phase attack transforms, repeated triggers and reset.
Full build and Minecraft startup/mixin audit pass. This is not a live combat visual
test with the user's complete running mod pack.
