package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.ScaleMath;
import xyz.rimvisuals.core.Setting;

/** Size of the totem-pop item that floats in front of the camera. Duration and gameplay are unchanged. */
public final class TotemScale extends Module {
    public final Setting.Num scale = add(new Setting.Num("Totem scale", 1, .3, 2, .05));
    public final Setting.Bool shrinkOut = add(new Setting.Bool("Shrink at end", false));
    public final Setting.Num shrinkStart = add(new Setting.Num("Shrink starts at", .6, .2, .95, .05));

    public TotemScale() {
        super("totem_scale", "Totem Scale", "Changes the size of the totem pop animation and can shrink it away at the end. Visual only.", Category.VISUALS);
        enabled = false;
    }

    /** Multiplier applied on top of the vanilla 0.8 scale; {@code progress} is 0..1 through the 40-tick animation. */
    public float factor(float progress) {
        if (!isActive()) return 1f;
        float shrink = shrinkOut.value && !xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value
                ? ScaleMath.shrinkOut(progress, (float) shrinkStart.value, 0f) : 1f;
        return (float) scale.value * shrink;
    }
}
