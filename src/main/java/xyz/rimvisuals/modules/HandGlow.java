package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.ColorMath;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;

/**
 * Glow halo around the first-person arm, made of 1-4 enlarged, fully-lit, translucent copies of the arm model drawn behind it
 * in the same render pass. This is a geometry halo, not a post-processing bloom: it has no blur kernel.
 */
public final class HandGlow extends Module {
    public static final int MAX_LAYERS = 4;
    public final Setting.Color color = add(new Setting.Color("Color", 0xA855F7));
    public final Setting.Bool rainbow = add(new Setting.Bool("Rainbow", false));
    public final Setting.Num speed = add(new Setting.Num("Rainbow speed", .25, .05, 2, .05));
    public final Setting.Num layers = add(new Setting.Num("Layers", 3, 1, MAX_LAYERS, 1));
    public final Setting.Num size = add(new Setting.Num("Size", .08, .02, .2, .01));
    public final Setting.Num strength = add(new Setting.Num("Strength", .35, .05, 1, .05));
    public final Setting.Bool main = add(new Setting.Bool("Main hand", true));
    public final Setting.Bool off = add(new Setting.Bool("Off hand", true));

    public HandGlow() {
        super("hand_glow", "Hand Glow", "Translucent glowing halo around your first-person arm (stacked enlarged copies of the arm, fully lit). Geometry halo, not a bloom filter. Safe Mode suppresses the halo; Balanced caps it at one layer.", Category.SHADER_HANDS);
        enabled = false;
    }

    public int layerCount() { return Modules.PERFORMANCE.glowLayers((int) Math.max(1, Math.min(MAX_LAYERS, Math.round(layers.value)))); }

    /** Uniform scale of shell {@code i} (0 = closest to the arm). */
    public static float shellScale(int i, int count, double size) { return 1f + (float) size * (i + 1) / count; }

    /** Alpha of shell {@code i}: strongest near the arm, fading outward, summing to roughly {@code strength} overall. */
    public static float shellAlpha(int i, int count, double strength) { return (float) (strength * (count - i) / (count * (count + 1) / 2.0)); }

    public int rgb(long nanos) {
        return rainbow.value ? ColorMath.rainbow(Modules.PERFORMANCE.reducedMotion.value ? 0 : nanos / 1e9 * speed.value) : color.rgb;
    }
}
