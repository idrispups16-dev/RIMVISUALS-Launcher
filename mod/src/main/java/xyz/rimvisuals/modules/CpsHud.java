package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

/** Left/right clicks per second, measured from the attack/use key state seen each rendered frame. */
public final class CpsHud extends HudModule {
    public final Setting.Bool both = add(new Setting.Bool("Show right click", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private final HudMath.RateCounter left = new HudMath.RateCounter(1), right = new HudMath.RateCounter(1);
    private boolean lastL, lastR;

    public CpsHud() { super("cps_hud", "CPS", "Clicks per second (attack and use key presses seen per rendered frame; very fast clicking above your FPS is under-counted).", 6, 100); }

    private void sample(MinecraftClient mc) {
        long now = System.nanoTime();
        boolean l = mc.currentScreen == null && mc.options.attackKey.isPressed(), r = mc.currentScreen == null && mc.options.useKey.isPressed();
        if (l && !lastL) left.hit(now);
        if (r && !lastR) right.hit(now);
        lastL = l; lastR = r;
    }

    private String text(MinecraftClient mc) {
        long now = System.nanoTime();
        return both.value ? left.count(now) + " | " + right.count(now) + " CPS" : left.count(now) + " CPS";
    }

    @Override public int width(MinecraftClient mc) { return Math.max(46, mc.textRenderer.getWidth(both.value ? "00 | 00 CPS" : "00 CPS") + 14); }
    @Override public int height(MinecraftClient mc) { return 18; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        sample(mc);
        Gfx.round(c, x, y, width(mc), height(mc), 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + height(mc) - 3, Theme.accent());
        c.drawTextWithShadow(mc.textRenderer, text(mc), x + 8, y + 5, Theme.text());
    }
}
