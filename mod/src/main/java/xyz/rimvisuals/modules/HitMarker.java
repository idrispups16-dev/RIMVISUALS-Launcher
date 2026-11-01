package xyz.rimvisuals.modules;

import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.CombatEvents;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;

/** Four diagonal ticks around the crosshair that fade out after you hit a living entity. */
public final class HitMarker extends Module {
    public final Setting.Color color = add(new Setting.Color("Color", 0xFFFFFF));
    public final Setting.Num size = add(new Setting.Num("Length", 5, 2, 12, 1));
    public final Setting.Num gap = add(new Setting.Num("Gap", 3, 0, 10, 1));
    public final Setting.Num thickness = add(new Setting.Num("Thickness", 1, 1, 3, 1));
    public final Setting.Num duration = add(new Setting.Num("Duration (ms)", 350, 100, 1000, 25));

    public HitMarker() {
        super("hit_marker", "Hit Marker", "Shows a marker at the crosshair when you attack a living entity. Pure 2D overlay, a handful of rectangles while visible.", Category.VISUALS);
        enabled = false;
    }

    public void draw(DrawContext c, int w, int h) {
        double ageMs = CombatEvents.msSinceLastHit();
        if (ageMs < 0 || ageMs > duration.value) return;
        float f = Modules.PERFORMANCE.reducedMotion.value ? 1f : HudMath.fadeOut(ageMs, duration.value);
        int col = Gfx.withAlpha(color.rgb, f);
        int cx = w / 2, cy = h / 2, t = (int) thickness.value, g = (int) gap.value, len = (int) size.value;
        for (int i = g; i < g + len; i++) {
            c.fill(cx + i, cy - i - t + 1, cx + i + t, cy - i + 1, col);
            c.fill(cx - i - t + 1, cy - i - t + 1, cx - i + 1, cy - i + 1, col);
            c.fill(cx + i, cy + i, cx + i + t, cy + i + t, col);
            c.fill(cx - i - t + 1, cy + i, cx - i + 1, cy + i + t, col);
        }
    }
}
