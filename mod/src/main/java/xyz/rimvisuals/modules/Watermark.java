package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

public final class Watermark extends HudModule {
    public final Setting.Bool logo = add(new Setting.Bool("Show RV logo", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", 0.65, 0.2, 1.0, 0.05));

    public Watermark() { super("watermark", "Watermark", "RIMVISUALS tag", 6, 62); }

    @Override public int width(MinecraftClient mc) { return (logo.value ? 14 : 0) + mc.textRenderer.getWidth("RIMVISUALS") + 12; }
    @Override public int height(MinecraftClient mc) { return 16; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        int w = width(mc), h = height(mc), acc = accent();
        Gfx.round(c, x, y, w, h, 8, Gfx.withAlpha(0x08080F, (float) opacity.value));
        int tx = x + 6;
        if (logo.value) { Gfx.rv(c, x + 11, y + h / 2, 4, acc); tx += 14; }
        Gfx.text(c, mc.textRenderer, "RIMVISUALS", tx, y + 4, Theme.text());
    }
}
