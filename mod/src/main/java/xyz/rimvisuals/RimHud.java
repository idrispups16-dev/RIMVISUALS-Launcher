package xyz.rimvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.PerformanceMonitor;
import xyz.rimvisuals.gui.HudEditorScreen;

public final class RimHud {
    private RimHud() {}

    /** Element size in HUD units after the per-element scale. */
    public static int sw(HudModule m, MinecraftClient mc) { return xyz.rimvisuals.core.HudMath.scaled(m.width(mc), fittedScale(m, mc)); }
    public static int sh(HudModule m, MinecraftClient mc) { return xyz.rimvisuals.core.HudMath.scaled(m.height(mc), fittedScale(m, mc)); }

    public static double fittedScale(HudModule m, MinecraftClient mc) {
        double hs = Modules.UI.hudScale.value;
        return Math.min(m.scale.value, Math.min(
            mc.getWindow().getScaledWidth() / hs / Math.max(1, m.width(mc)),
            mc.getWindow().getScaledHeight() / hs / Math.max(1, m.height(mc))));
    }

    public static void drawScaled(DrawContext c, MinecraftClient mc, HudModule m, int x, int y) {
        float s = (float) fittedScale(m, mc);
        xyz.rimvisuals.core.Gfx.hudAlpha = (float) m.elementOpacity.value;
        try {
            if (s == 1f) { m.draw(c, mc, x, y); return; }
            var ms = c.getMatrices();
            ms.pushMatrix();
            ms.translate(x, y);
            ms.scale(s, s);
            try { m.draw(c, mc, 0, 0); } finally { ms.popMatrix(); }
        } finally { xyz.rimvisuals.core.Gfx.hudAlpha = 1f; }
    }

    public static int clampX(HudModule m, int vw, int w) { return Math.max(0, Math.min((int) m.posX.value, Math.max(0, vw - w))); }
    public static int clampY(HudModule m, int vh, int h) { return Math.max(0, Math.min((int) m.posY.value, Math.max(0, vh - h))); }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        if(!xyz.rimvisuals.auth.AccessGate.allowed())return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if(Modules.PERFORMANCE.diagnostics.value)xyz.rimvisuals.core.FrameTimes.record(System.nanoTime());
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long total=PerformanceMonitor.begin();
        if (Modules.PULSE.isActive() && !Modules.PERFORMANCE.safe.value) {long t=PerformanceMonitor.begin();Modules.PULSE.render(ctx,w,h);PerformanceMonitor.end("Pulse Glow",t);}
        if (Modules.PARTICLES.isActive() && Modules.PARTICLES.overlay.value && !Modules.PERFORMANCE.safe.value) {long t=PerformanceMonitor.begin();Modules.PARTICLES.drawOverlay(ctx,w,h);PerformanceMonitor.end("Particles",t);}
        if (Modules.DAMAGE_TINT.isActive()) {long t=PerformanceMonitor.begin();Modules.DAMAGE_TINT.draw(ctx,mc,w,h);PerformanceMonitor.end("Damage Tint",t);}
        if (Modules.GOLDEN_APPLE_GLOW.isActive()) {long t=PerformanceMonitor.begin();Modules.GOLDEN_APPLE_GLOW.draw(ctx,mc,w,h);PerformanceMonitor.end("Golden Apple Glow",t);}
        if (Modules.HIT_MARKER.isActive() && mc.currentScreen == null) {long t=PerformanceMonitor.begin();Modules.HIT_MARKER.draw(ctx,w,h);PerformanceMonitor.end("Hit Marker",t);}
        if (mc.currentScreen instanceof HudEditorScreen) {PerformanceMonitor.end("HUD overlays",total);return;}
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(w / hs), vh = Math.round(h / hs);
        var ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.scale(hs, hs);
        for (HudModule m : Modules.HUD) {
            if (!m.visible()) continue;
            long moduleStart=PerformanceMonitor.begin();
            int mw = sw(m, mc), mh = sh(m, mc);
            drawScaled(ctx, mc, m, clampX(m, vw, mw), clampY(m, vh, mh));
            PerformanceMonitor.end(m.name,moduleStart);
        }
        ms.popMatrix();
        PerformanceMonitor.end("HUD total",total);
    }
}
