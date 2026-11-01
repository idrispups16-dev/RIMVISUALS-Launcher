package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;

/** Coloured edge vignette that fades while your player's vanilla hurt timer runs. */
public final class DamageTint extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF1122));
    public final Setting.Num strength = add(new Setting.Num("Strength", .55, .1, 1, .05));
    public final Setting.Num edge = add(new Setting.Num("Edge size (%)", 18, 5, 40, 1));

    public DamageTint() {
        super("damage_tint", "Damage Tint", "Tints the screen edges when you take damage. 2D overlay drawn only while the hurt timer runs.", Category.VISUALS);
        enabled = false;
    }

    /** 0..1 intensity from the player's hurt timer. */
    public static float intensity(int hurtTime, int maxHurtTime) {
        if (maxHurtTime <= 0 || hurtTime <= 0) return 0f;
        return Math.min(1f, hurtTime / (float) maxHurtTime);
    }

    public void draw(DrawContext c, MinecraftClient mc, int w, int h) {
        if (mc.player == null) return;
        float k = intensity(mc.player.hurtTime, mc.player.maxHurtTime);
        if (k <= 0f) return;
        if (Modules.PERFORMANCE.reducedMotion.value) k = 1f;
        int band = Math.max(4, Math.round(Math.min(w, h) * (float) edge.value / 100f));
        int steps = Math.min(12, band);
        int per = Math.max(1, band / steps);
        for (int i = 0; i < steps; i++) {
            float a = (float) strength.value * k * (1f - i / (float) steps) * .6f;
            int col = Gfx.withAlpha(color.rgb, a);
            int o = i * per;
            c.fill(0, o, w, o + per, col);
            c.fill(0, h - o - per, w, h - o, col);
            c.fill(o, o + per, o + per, h - o - per, col);
            c.fill(w - o - per, o + per, w - o, h - o - per, col);
        }
    }
}
