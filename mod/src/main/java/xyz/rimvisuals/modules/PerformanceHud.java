package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.*;

/** Shares the existing monitor switch, so older configs retain their meaning. */
public final class PerformanceHud extends HudModule {
    public PerformanceHud() {
        super("performance_hud", "Performance Monitor", "CPU submission times, not GPU timings", 6, 220);
        toggleable = false;
    }
    @Override public boolean visible() { return Modules.PERFORMANCE.monitor.value; }
    @Override public int width(MinecraftClient mc) { return 230; }
    @Override public int height(MinecraftClient mc) { return 64; }
    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        Gfx.text(c, mc.textRenderer, "RIM CPU | Safe: " + Modules.PERFORMANCE.safe.value, x, y, accent());
        for (String line : PerformanceMonitor.top(5)) {
            y += 10;
            Gfx.text(c, mc.textRenderer, line, x, y, 0xffccccdd);
        }
    }
}
