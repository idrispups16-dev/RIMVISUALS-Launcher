package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;
import xyz.rimvisuals.core.Modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Item cooldowns (ender pearl, chorus fruit, shield, golden apple, ...) for items in your hotbar, off-hand and inventory. */
public final class CooldownsHud extends HudModule {
    public final Setting.Bool bars = add(new Setting.Bool("Progress bar", true));
    public final Setting.Bool percent = add(new Setting.Bool("Show percent", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private static final int ROW = 20, W = 92;

    private record Row(ItemStack stack, float progress) {}

    public CooldownsHud() { super("cooldowns_hud", "Cooldowns", "Active item cooldowns with progress bars", 6, 120); }

    private List<Row> rows(MinecraftClient mc) {
        List<Row> out = new ArrayList<>();
        if (mc.player == null) return out;
        var cd = mc.player.getItemCooldownManager();
        Set<Object> seen = new HashSet<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            float p = cd.getCooldownProgress(s, 0f);
            if (p > 0f && seen.add(cd.getGroup(s))) out.add(new Row(s, p));
            if (out.size() >= 6) break;
        }
        return out;
    }

    private List<Row> cached=List.of();
    private long nextRefresh;
    private Object lastPlayer;
    private List<Row> shown(MinecraftClient mc) {
        long now=System.nanoTime();
        if(now<nextRefresh&&lastPlayer==mc.player) return cached;
        lastPlayer=mc.player;nextRefresh=now+50_000_000L;
        List<Row> r = rows(mc);
        if (r.isEmpty() && mc.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen) r.add(new Row(new ItemStack(Items.ENDER_PEARL), .6f)); // editor preview only
        cached=r;return cached;
    }

    @Override public int width(MinecraftClient mc) { return W; }
    @Override public int height(MinecraftClient mc) { return Math.max(1, shown(mc).size()) * ROW + 4; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        List<Row> r = shown(mc);
        if (r.isEmpty()) return;
        Gfx.round(c, x, y, W, r.size() * ROW + 4, 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        Gfx.fill(c, x, y + 3, x + 2, y + r.size() * ROW + 1, accent());
        int ry = y + 3;
        for (Row row : r) {
            c.drawItem(row.stack, x + 6, ry);
            if (bars.value) {
                Gfx.fill(c, x + 26, ry + 12, x + W - 6, ry + 15, 0xFF000000);
                Gfx.fill(c, x + 26, ry + 12, x + 26 + Math.round((W - 32) * row.progress), ry + 15, accent());
            }
            if (percent.value) Gfx.text(c, mc.textRenderer, Math.round(row.progress * 100) + "%", x + 26, ry + 2, Theme.text());
            ry += ROW;
        }
    }
}
