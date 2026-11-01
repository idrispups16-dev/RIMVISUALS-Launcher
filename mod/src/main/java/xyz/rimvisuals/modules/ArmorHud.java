package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;
import xyz.rimvisuals.gui.HudEditorScreen;

/** Your worn armour with durability. Reads only your own equipment. */
public final class ArmorHud extends HudModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public final Setting.Bool vertical = add(new Setting.Bool("Vertical layout", true));
    public final Setting.Bool percent = add(new Setting.Bool("Durability percent", true));
    public final Setting.Bool bars = add(new Setting.Bool("Durability bar", true));
    public final Setting.Bool hideEmpty = add(new Setting.Bool("Hide empty slots", false));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));

    public ArmorHud() { super("armor_hud", "Armor HUD", "Worn armour pieces with durability percent and bar", 6, 60); }

    private ItemStack[] stacks(MinecraftClient mc) {
        ItemStack[] out = new ItemStack[4];
        boolean any = false;
        for (int i = 0; i < 4; i++) { out[i] = mc.player == null ? ItemStack.EMPTY : mc.player.getEquippedStack(SLOTS[i]); any |= !out[i].isEmpty(); }
        if (!any && mc.currentScreen instanceof HudEditorScreen) {
            out[0] = new ItemStack(Items.NETHERITE_HELMET); out[1] = new ItemStack(Items.NETHERITE_CHESTPLATE);
            out[2] = new ItemStack(Items.NETHERITE_LEGGINGS); out[3] = new ItemStack(Items.NETHERITE_BOOTS);
            out[1].setDamage(out[1].getMaxDamage() / 3); out[3].setDamage(out[3].getMaxDamage() / 2);
        }
        return out;
    }

    private int cell(MinecraftClient mc) { return percent.value ? 20 + mc.textRenderer.getWidth("100%") + 4 : 20; }

    private int shown(ItemStack[] s) { int n = 0; for (ItemStack i : s) if (!hideEmpty.value || !i.isEmpty()) n++; return Math.max(n, 1); }

    @Override public int width(MinecraftClient mc) { int n = shown(stacks(mc)); return (vertical.value ? cell(mc) : n * cell(mc)) + 8; }
    @Override public int height(MinecraftClient mc) { int n = shown(stacks(mc)); return (vertical.value ? n * 20 : 20) + 8; }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        ItemStack[] s = stacks(mc);
        boolean any = false;
        for (ItemStack i : s) any |= !i.isEmpty();
        if (!any && !(mc.currentScreen instanceof HudEditorScreen)) return;
        Gfx.round(c, x, y, width(mc), height(mc), 4, Gfx.withAlpha(0x08080F, (float) opacity.value));
        c.fill(x, y + 3, x + 2, y + height(mc) - 3, accent());
        int cx = x + 6, cy = y + 4, step = vertical.value ? 20 : cell(mc);
        for (ItemStack st : s) {
            if (hideEmpty.value && st.isEmpty()) continue;
            if (!st.isEmpty()) {
                c.drawItem(st, cx, cy);
                if (st.isDamageable()) {
                    float f = 1f - st.getDamage() / (float) st.getMaxDamage();
                    int col = Gfx.mix(0xFFFF4444, 0xFF55FF55, f);
                    if (bars.value) { c.fill(cx, cy + 15, cx + 16, cy + 16, 0xFF000000); c.fill(cx, cy + 15, cx + Math.round(16 * f), cy + 16, col); }
                    if (percent.value) c.drawTextWithShadow(mc.textRenderer, Math.round(f * 100) + "%", cx + 19, cy + 4, col);
                }
            }
            if (vertical.value) cy += step; else cx += step;
        }
    }
}
