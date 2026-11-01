package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import xyz.rimvisuals.core.CombatEvents;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;
import xyz.rimvisuals.gui.HudEditorScreen;

/** Name, health and distance of the entity under your crosshair or the one you hit last. */
public final class TargetHud extends HudModule {
    public final Setting.Bool crosshair = add(new Setting.Bool("Use crosshair target", true));
    public final Setting.Num hold = add(new Setting.Num("Keep last hit (s)", 5, 1, 15, 1));
    public final Setting.Bool distance = add(new Setting.Bool("Show distance", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private float shown = 1f;
    private int lastId = -1;

    public TargetHud() { super("target_hud", "Target HUD", "Name, health bar and distance of your current target. Uses health the game already syncs to you.", 200, 6); }

    private LivingEntity target(MinecraftClient mc) {
        if (crosshair.value && mc.targetedEntity instanceof LivingEntity le && le.isAlive()) return le;
        return CombatEvents.recentTarget(hold.value);
    }

    @Override public int width(MinecraftClient mc) { return 130; }
    @Override public int height(MinecraftClient mc) { return distance.value ? 38 : 28; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        LivingEntity t = mc.player == null ? null : target(mc);
        boolean preview = t == null && mc.currentScreen instanceof HudEditorScreen;
        if (t == null && !preview) { lastId = -1; return; }
        float hp = preview ? 14f : t.getHealth(), max = preview ? 20f : Math.max(1f, t.getMaxHealth());
        float frac = Math.max(0f, Math.min(1f, hp / max));
        if (preview || t.getId() != lastId || Modules.PERFORMANCE.reducedMotion.value) shown = frac; else shown += (frac - shown) * .2f;
        lastId = preview ? -1 : t.getId();
        int w = width(mc), h = height(mc);
        Gfx.round(c, x, y, w, h, 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        Gfx.fill(c, x, y + 3, x + 2, y + h - 3, accent());
        String name = preview ? "Target" : t.getDisplayName().getString();
        while (name.length() > 1 && mc.textRenderer.getWidth(name) > w - 14) name = name.substring(0, name.length() - 1);
        Gfx.text(c, mc.textRenderer, name, x + 8, y + 4, Theme.text());
        int bx = x + 8, by = y + 15, bw = w - 16;
        Gfx.fill(c, bx, by, bx + bw, by + 6, 0xFF000000);
        Gfx.fill(c, bx, by, bx + Math.round(bw * shown), by + 6, Gfx.mix(0xFFFF4444, 0xFF55FF55, frac));
        String num = String.format(java.util.Locale.ROOT, "%.1f / %.0f", hp, max);
        if (!distance.value) { Gfx.text(c, mc.textRenderer, num, bx, by + 8, Theme.muted()); return; }
        Gfx.text(c, mc.textRenderer, num, bx, by + 8, Theme.muted());
        String d = preview ? "4.2 m" : String.format(java.util.Locale.ROOT, "%.1f m", mc.player.distanceTo(t));
        Gfx.text(c, mc.textRenderer, d, x + w - 8 - mc.textRenderer.getWidth(d), by + 8, Theme.muted());
    }
}
