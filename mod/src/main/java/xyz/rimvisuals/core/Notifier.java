package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Short on-screen messages for events detected on the client. Drawn by the Notifications HUD element. */
public final class Notifier {
    public record Note(String text, int rgb, long untilNanos, long fromNanos) {}

    private static final List<Note> NOTES = new ArrayList<>();
    private static final boolean[] ARMOR_WARNED = new boolean[4];
    private static boolean lowHealthWarned;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};

    private Notifier() {}

    public static List<Note> active() {
        long now = System.nanoTime();
        NOTES.removeIf(n -> n.untilNanos < now);
        return NOTES;
    }

    public static void clearPrefix(String prefix) { NOTES.removeIf(n -> n.text.startsWith(prefix)); }

    public static void push(String text, int rgb) {
        var h = Modules.NOTIFICATIONS;
        long now = System.nanoTime();
        NOTES.add(new Note(text, rgb, now + (long) (h.seconds.value * 1e9), now));
        while (NOTES.size() > 6) NOTES.remove(0);
    }

    /** True when remaining durability fraction is at or below the threshold (and the item can be damaged). */
    public static boolean lowDurability(int damage, int max, double thresholdPercent) {
        return max > 0 && (max - damage) * 100.0 <= thresholdPercent * max;
    }

    /** Hysteresis: re-arm once the value is 5 points above the threshold again (repaired or swapped). */
    public static boolean rearm(int damage, int max, double thresholdPercent) {
        return max <= 0 || (max - damage) * 100.0 > (thresholdPercent + 5) * max;
    }

    public static void tick(MinecraftClient mc) {
        var cfg = Modules.NOTIFICATIONS;
        if (!cfg.isActive() || mc.player == null) return;
        if (cfg.lowHealth.value) {
            float hp = mc.player.getHealth();
            if (hp > 0 && hp <= cfg.healthThreshold.value && !lowHealthWarned) { lowHealthWarned = true; push("Low health: " + Math.round(hp / 2f * 10) / 10f + " hearts", 0xFF5555); }
            else if (hp > cfg.healthThreshold.value + 2) lowHealthWarned = false;
        }
        if (cfg.armor.value) {
            for (int i = 0; i < 4; i++) {
                ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
                if (s.isEmpty() || !s.isDamageable()) { ARMOR_WARNED[i] = false; continue; }
                if (!ARMOR_WARNED[i] && lowDurability(s.getDamage(), s.getMaxDamage(), cfg.armorThreshold.value)) { ARMOR_WARNED[i] = true; push(NAMES[i] + " durability low", 0xFFC93C); }
                else if (ARMOR_WARNED[i] && rearm(s.getDamage(), s.getMaxDamage(), cfg.armorThreshold.value)) ARMOR_WARNED[i] = false;
            }
        }
    }

    public static void totemUsed() {
        var cfg = Modules.NOTIFICATIONS;
        if (cfg.isActive() && cfg.totem.value) push("Totem of Undying used", 0xFFD34D);
    }
}
