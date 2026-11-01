package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

/** Horizontal speed in blocks per second from your own position changes. */
public final class SpeedHud extends HudModule {
    public final Setting.Bool unit = add(new Setting.Bool("Show unit", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private double lx, lz, shown;
    private long lastNanos;

    public SpeedHud() { super("speed_hud", "Speed (BPS)", "Your horizontal speed in blocks per second", 6, 184); }

    private void sample(MinecraftClient mc) {
        if (mc.player == null) return;
        long now = System.nanoTime();
        if (lastNanos == 0) { lx = mc.player.getX(); lz = mc.player.getZ(); lastNanos = now; return; }
        double dt = (now - lastNanos) / 1e9;
        if (dt < .1) return;
        double x = mc.player.getX(), z = mc.player.getZ();
        double v = dt > 1 ? 0 : HudMath.bps(x - lx, z - lz, dt); // ignore teleports after long gaps
        shown += (v - shown) * .5;
        lx = x; lz = z; lastNanos = now;
    }

    @Override public int width(MinecraftClient mc) { return mc.textRenderer.getWidth("00.00 b/s") + 14; }
    @Override public int height(MinecraftClient mc) { return 18; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        sample(mc);
        Gfx.round(c, x, y, width(mc), height(mc), 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        Gfx.fill(c, x, y + 3, x + 2, y + height(mc) - 3, accent());
        Gfx.text(c, mc.textRenderer, String.format(java.util.Locale.ROOT, "%.2f", shown) + (unit.value ? " b/s" : ""), x + 8, y + 5, Theme.text());
    }
}
