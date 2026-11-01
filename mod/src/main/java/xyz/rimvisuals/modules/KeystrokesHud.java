package xyz.rimvisuals.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

/** WASD, jump and mouse buttons lit while the game's own key bindings are held. */
public final class KeystrokesHud extends HudModule {
    public final Setting.Bool mouse = add(new Setting.Bool("Show mouse buttons", true));
    public final Setting.Bool space = add(new Setting.Bool("Show jump bar", true));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", .65, .2, 1, .05));
    private static final int K = 20, G = 2;

    public KeystrokesHud() { super("keystrokes_hud", "Keystrokes", "Movement keys and mouse buttons using your configured key bindings", 6, 124); }

    @Override public int width(MinecraftClient mc) { return K * 3 + G * 2; }
    @Override public int height(MinecraftClient mc) { return K * 2 + G + (mouse.value ? K + G : 0) + (space.value ? 12 + G : 0); }

    private void key(DrawContext c, MinecraftClient mc, int x, int y, int w, int h, KeyBinding k, String label) {
        boolean down = mc.currentScreen == null && k.isPressed();
        int bg = down ? Gfx.withAlpha(accent() & 0xFFFFFF, .85f) : Gfx.withAlpha(0x08080F, (float) opacity.value);
        Gfx.round(c, x, y, w, h, 3, bg);
        int tw = mc.textRenderer.getWidth(label);
        c.drawTextWithShadow(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, down ? 0xFFFFFFFF : Theme.text());
    }

    @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        var o = mc.options;
        key(c, mc, x + K + G, y, K, K, o.forwardKey, "W");
        key(c, mc, x, y + K + G, K, K, o.leftKey, "A");
        key(c, mc, x + K + G, y + K + G, K, K, o.backKey, "S");
        key(c, mc, x + (K + G) * 2, y + K + G, K, K, o.rightKey, "D");
        int cy = y + (K + G) * 2;
        if (mouse.value) {
            int w = (K * 3 + G * 2 - G) / 2;
            key(c, mc, x, cy, w, K, o.attackKey, "LMB");
            key(c, mc, x + w + G, cy, w, K, o.useKey, "RMB");
            cy += K + G;
        }
        if (space.value) key(c, mc, x, cy, K * 3 + G * 2, 12, o.jumpKey, "___");
    }
}
