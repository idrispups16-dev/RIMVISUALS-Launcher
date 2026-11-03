package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;

/** Replaces vanilla's flashing Night Vision warning (last 10 s) with a smooth linear fade. */
public final class NightVisionFade extends Module {
    public NightVisionFade() {
        super("night_vision_fade", "Night Vision Fade", "Night Vision fades out smoothly over its last 10 seconds instead of flickering.", Category.VISUALS);
        enabled = false;
    }

    /** Vanilla: 1 while more than 200 ticks remain, otherwise 0.7 + 0.3*sin flicker. This: 1 -> 0 linearly. */
    public static float strength(int durationTicks) {
        if (durationTicks >= 200) return 1f;
        return Math.max(0f, durationTicks / 200f);
    }
}
