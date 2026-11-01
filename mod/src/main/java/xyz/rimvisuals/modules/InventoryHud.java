package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

/** Your main inventory (27 slots) as a compact grid. Only reads your own inventory. */
public final class InventoryHud extends HudModule {
    public final Setting.Num rows = add(new Setting.Num("Rows", 3, 1, 3, 1));
    public final Setting.Bool counts = add(new Setting.Bool("Show stack counts", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));

    public InventoryHud() { super("inventory_hud", "Inventory HUD", "Main inventory grid; hotbar is not duplicated", 300, 100); }

    @Override public int width(MinecraftClient mc) { return 9 * 18 + 8; }
    @Override public int height(MinecraftClient mc) { return (int) rows.value * 18 + 8; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        Gfx.round(c, x, y, width(mc), height(mc), 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + height(mc) - 3, accent());
        int r = (int) rows.value;
        for (int row = 0; row < r; row++) for (int col = 0; col < 9; col++) {
            ItemStack st = mc.player.getInventory().getStack(9 + row * 9 + col);
            if (st.isEmpty()) continue;
            int ix = x + 4 + col * 18 + 1, iy = y + 4 + row * 18 + 1;
            c.drawItem(st, ix, iy);
            if (counts.value) c.drawStackOverlay(mc.textRenderer, st, ix, iy);
        }
    }
}
