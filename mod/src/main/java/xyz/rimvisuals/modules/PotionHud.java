package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudMath;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;
import xyz.rimvisuals.gui.HudEditorScreen;

import java.util.ArrayList;
import java.util.List;

/** Active status effects with level and remaining time. Reads only your own player. */
public final class PotionHud extends HudModule {
    public final Setting.Bool level = add(new Setting.Bool("Show level", true));
    public final Setting.Bool warn = add(new Setting.Bool("Highlight last 10 s", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private final List<String> names = new ArrayList<>(), times = new ArrayList<>();
    private final List<Boolean> low = new ArrayList<>();
    private long next;

    public PotionHud() { super("potion_hud", "Potion HUD", "Active effects with level and time left", 6, 160); }

    private void build(MinecraftClient mc) {
        long now = System.nanoTime();
        if (now < next) return;
        next = now + 100_000_000L;
        names.clear(); times.clear(); low.clear();
        if (mc.player != null) {
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String n = e.getEffectType().value().getName().getString();
                if (level.value && e.getAmplifier() > 0) n += " " + (e.getAmplifier() + 1);
                names.add(n);
                times.add(HudMath.effectTime(e.getDuration(), e.isInfinite()));
                low.add(!e.isInfinite() && e.getDuration() <= 200);
            }
        }
        if (names.isEmpty() && mc.currentScreen instanceof HudEditorScreen) { names.add("Speed 2"); times.add("1:30"); low.add(false); names.add("Strength"); times.add("0:08"); low.add(true); }
    }

    @Override public int width(MinecraftClient mc) {
        build(mc);
        int w = mc.textRenderer.getWidth("Effects");
        for (int i = 0; i < names.size(); i++) w = Math.max(w, mc.textRenderer.getWidth(names.get(i)) + 8 + mc.textRenderer.getWidth(times.get(i)));
        return w + 14;
    }
    @Override public int height(MinecraftClient mc) { build(mc); return names.isEmpty() ? 0 : (names.size() + 1) * 10 + 8; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        build(mc);
        if (names.isEmpty()) return;
        int w = width(mc), h = height(mc);
        Gfx.round(c, x, y, w, h, 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + h - 3, Theme.accent());
        c.drawTextWithShadow(mc.textRenderer, "Effects", x + 8, y + 4, Theme.accent());
        int ty = y + 14;
        for (int i = 0; i < names.size(); i++) {
            c.drawTextWithShadow(mc.textRenderer, names.get(i), x + 8, ty, Theme.text());
            String t = times.get(i);
            c.drawTextWithShadow(mc.textRenderer, t, x + w - 6 - mc.textRenderer.getWidth(t), ty, warn.value && low.get(i) ? 0xFFFF5555 : Theme.muted());
            ty += 10;
        }
    }
}
