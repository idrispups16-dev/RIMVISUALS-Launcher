package xyz.rimvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.gui.HudEditorScreen;

public final class RimHud {
    private RimHud() {}

    public static int clampX(HudModule m, int vw, int w) { return Math.max(0, Math.min((int) m.posX.value, Math.max(0, vw - w))); }
    public static int clampY(HudModule m, int vh, int h) { return Math.max(0, Math.min((int) m.posY.value, Math.max(0, vh - h))); }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        if (Modules.PULSE.enabled) Modules.PULSE.render(ctx, w, h);
        if (Modules.PARTICLES.enabled && Modules.PARTICLES.overlay.value) Modules.PARTICLES.drawOverlay(ctx, w, h);
        if (mc.currentScreen instanceof HudEditorScreen) return;
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(w / hs), vh = Math.round(h / hs);
        var ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.scale(hs, hs);
        for (HudModule m : Modules.HUD) {
            if (!m.enabled) continue;
            int mw = m.width(mc), mh = m.height(mc);
            m.draw(ctx, mc, clampX(m, vw, mw), clampY(m, vh, mh));
        }
        ms.popMatrix();
    }
}
