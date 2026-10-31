package xyz.rimvisuals.modules;

import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.RimPlatform;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

/** Soft pulsing glow along the screen edges. Layered fills, no shaders. */
public final class PulseGlow extends Module {
    public final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.3, 3.0, 0.1));
    public final Setting.Num intensity = add(new Setting.Num("Intensity", 0.18, 0.05, 0.4, 0.01));
    public final Setting.Mode edges = add(new Setting.Mode("Edges", 0, "All sides", "Top and bottom"));
    public final Setting.Bool useAccent = add(new Setting.Bool("Use theme accent", true));
    public final Setting.Color color = add(new Setting.Color("Color", 0x7C3AED));

    public PulseGlow() { super("pulse_glow", "Pulse Glow", "Soft glowing screen edges", Category.VISUALS); }

    public void render(DrawContext c, int w, int h) {
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        float p = 0.5f + 0.5f * (float) Math.sin(t * speed.value * Math.PI);
        float a = (float) intensity.value * (0.6f + 0.4f * p);
        int base = useAccent.value ? (Theme.accent() & 0xFFFFFF) : color.rgb;
        int layers = RimPlatform.mobile() ? 6 : 10;
        int th = Math.max(8, Math.min(w, h) / 7);
        int step = Math.max(1, th / layers);
        boolean sides = edges.index == 0;
        for (int i = 0; i < layers; i++) {
            float f = 1f - (float) i / layers;
            int al = Math.round(255 * a * f * f);
            if (al <= 0) continue;
            int col = (al << 24) | base;
            int o = i * step;
            c.fill(0, o, w, o + step, col);
            c.fill(0, h - o - step, w, h - o, col);
            if (sides) {
                c.fill(o, 0, o + step, h, col);
                c.fill(w - o - step, 0, w - o, h, col);
            }
        }
    }
}
