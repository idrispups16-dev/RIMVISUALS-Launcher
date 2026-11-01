package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

import java.util.ArrayList;
import java.util.List;

/** Time in the current world and the server address you are connected to. */
public final class SessionHud extends HudModule {
    public final Setting.Bool time = add(new Setting.Bool("Show play time", true));
    public final Setting.Bool server = add(new Setting.Bool("Show server", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private final List<String> lines = new ArrayList<>();
    private long worldStart, next;
    private Object lastWorld;

    public SessionHud() { super("session_hud", "Session Info", "Play time in this world and the current server address", 6, 208); }

    private void build(MinecraftClient mc) {
        long now = System.nanoTime();
        if (mc.world != lastWorld) { lastWorld = mc.world; worldStart = now; }
        if (now < next) return;
        next = now + 250_000_000L;
        lines.clear();
        if (time.value) lines.add("Time: " + HudMath.clock((now - worldStart) / 1_000_000_000L));
        if (server.value) {
            ServerInfo info = mc.getCurrentServerEntry();
            lines.add(mc.isInSingleplayer() ? "Singleplayer" : info != null ? info.address : "Offline");
        }
    }

    @Override public int width(MinecraftClient mc) { build(mc); int w = 40; for (String s : lines) w = Math.max(w, mc.textRenderer.getWidth(s)); return w + 14; }
    @Override public int height(MinecraftClient mc) { build(mc); return lines.isEmpty() ? 0 : lines.size() * 10 + 8; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        build(mc);
        if (lines.isEmpty()) return;
        Gfx.round(c, x, y, width(mc), height(mc), 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + height(mc) - 3, Theme.accent());
        int ty = y + 4;
        for (String s : lines) { c.drawTextWithShadow(mc.textRenderer, s, x + 8, ty, Theme.text()); ty += 10; }
    }
}
