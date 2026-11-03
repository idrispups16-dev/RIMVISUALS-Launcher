package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Gold edge glow while you have absorption hearts (golden apple / enchanted golden apple). */
public final class GoldenAppleGlow extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFFC93C));
    public final Setting.Num strength = add(new Setting.Num("Strength", .5, .1, 1, .05));
    public final Setting.Num edge = add(new Setting.Num("Edge size (%)", 14, 5, 40, 1));
    public final Setting.Bool pulse = add(new Setting.Bool("Pulse", true));

    public GoldenAppleGlow() {
        super("golden_apple_glow", "Golden Apple Glow", "Gold screen-edge glow while you have absorption. 2D overlay, drawn only while active.", Category.VISUALS);
        enabled = false;
    }

    /** 0..1: scales with absorption (full at 8 hearts' worth, 16 points), 0 when none. */
    public static float intensity(float absorption) { return absorption <= 0f ? 0f : Math.min(1f, .45f + absorption / 16f * .55f); }

    public void draw(DrawContext c, MinecraftClient mc, int w, int h) {
        if (mc.player == null) return;
        float k = intensity(mc.player.getAbsorptionAmount());
        if (k <= 0f) return;
        if (pulse.value && !xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value)
            k *= .8f + .2f * (float) Math.sin(System.nanoTime() / 4.0e8);
        int band = Math.max(4, Math.round(Math.min(w, h) * (float) edge.value / 100f));
        int steps = Math.min(12, band), per = Math.max(1, band / steps);
        for (int i = 0; i < steps; i++) {
            int col = Gfx.withAlpha(color.rgb, (float) strength.value * k * (1f - i / (float) steps) * .6f);
            int o = i * per;
            c.fill(0, o, w, o + per, col);
            c.fill(0, h - o - per, w, h - o, col);
            c.fill(o, o + per, o + per, h - o - per, col);
            c.fill(w - o - per, o + per, w - o, h - o - per, col);
        }
    }
}
