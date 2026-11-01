package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

import java.util.ArrayList;
import java.util.List;

/** FPS / ping / coordinates panel. Shows only values the game already exposes. */
public final class InfoHud extends HudModule {
    public final Setting.Bool fps = add(new Setting.Bool("Show FPS", true));
    public final Setting.Bool ping = add(new Setting.Bool("Show ping", true));
    public final Setting.Bool xyz = add(new Setting.Bool("Show XYZ", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", 0.65, 0.2, 1.0, 0.05));
    private final List<String> lines = new ArrayList<>();

    public InfoHud() { super("info_hud", "Info HUD", "FPS, ping and coordinates", 6, 6); }

    private long nextRefresh;
    private void build(MinecraftClient mc) {
        long now=System.nanoTime();if(now<nextRefresh)return;nextRefresh=now+100_000_000;
        lines.clear();
        if (fps.value) lines.add("FPS: " + mc.getCurrentFps());
        if (ping.value) {
            String p = "-";
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) p = e.getLatency() + "ms";
            }
            lines.add("Ping: " + p);
        }
        if (xyz.value) {
            lines.add(mc.player == null ? "XYZ: 0 0 0"
                    : "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
        }
    }

    @Override public int width(MinecraftClient mc) {
        build(mc);
        int w = mc.textRenderer.getWidth("RIMVISUALS");
        for (String s : lines) w = Math.max(w, mc.textRenderer.getWidth(s));
        return w + 14;
    }

    @Override public int height(MinecraftClient mc) {
        build(mc);
        return (lines.size() + 1) * 10 + 8;
    }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        build(mc);
        int w = width(mc), h = height(mc);
        int acc = accent();
        Gfx.round(c, x, y, w, h, 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + h - 3, acc);
        c.drawTextWithShadow(mc.textRenderer, "RIMVISUALS", x + 8, y + 4, acc);
        int ty = y + 14;
        for (String s : lines) {
            c.drawTextWithShadow(mc.textRenderer, s, x + 8, ty, Theme.text());
            ty += 10;
        }
    }
}
