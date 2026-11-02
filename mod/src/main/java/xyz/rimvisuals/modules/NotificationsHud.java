package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Notifier;
import xyz.rimvisuals.core.Setting;

import java.util.List;

/** Short messages for events your client can see: totem used, low health, low armor durability. */
public final class NotificationsHud extends HudModule {
    public final Setting.Bool totem = add(new Setting.Bool("Totem used", true));
    public final Setting.Bool lowHealth = add(new Setting.Bool("Low health", true));
    public final Setting.Num healthThreshold = add(new Setting.Num("Health threshold (HP)", 6, 2, 12, 1));
    public final Setting.Bool armor = add(new Setting.Bool("Low armor durability", true));
    public final Setting.Num armorThreshold = add(new Setting.Num("Durability threshold (%)", 10, 3, 40, 1));
    public final Setting.Num seconds = add(new Setting.Num("Display time (s)", 4, 1, 10, .5));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .7, .2, 1, .05));
    private static final List<Notifier.Note> PREVIEW = List.of(new Notifier.Note("Example notification",0xFFC93C,Long.MAX_VALUE,0));
    private static final int ROW = 16, W = 150;

    public NotificationsHud() { super("notifications", "Notifications", "Short alerts: totem used, low health, low armor durability", 6, 150); }

    private List<Notifier.Note> notes(MinecraftClient mc) {
        List<Notifier.Note> n = Notifier.active();
        if (n.isEmpty() && mc.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen) return PREVIEW;
        return n;
    }

    @Override public int width(MinecraftClient mc) { return W; }
    @Override public int height(MinecraftClient mc) { return Math.max(1, notes(mc).size()) * ROW; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        long now = System.nanoTime();
        int ry = y;
        for (Notifier.Note n : notes(mc)) {
            float fadeIn = Math.min(1f, (now - n.fromNanos()) / 150e6f), fadeOut = Math.min(1f, (n.untilNanos() - now) / 300e6f);
            float k = Math.max(0f, Math.min(fadeIn, fadeOut));
            Gfx.round(c, x, ry, W, ROW - 2, 4, Gfx.withAlpha(0x08080F, (float) opacity.value * k));
            Gfx.fill(c, x, ry + 2, x + 2, ry + ROW - 4, Gfx.withAlpha(n.rgb(), k));
            Gfx.text(c, mc.textRenderer, mc.textRenderer.trimToWidth(n.text(), W - 14), x + 8, ry + 3, Gfx.withAlpha(0xFFFFFF, k));
            ry += ROW;
        }
    }
}
