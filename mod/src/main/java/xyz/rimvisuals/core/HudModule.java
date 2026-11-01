package xyz.rimvisuals.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A HUD element that can be dragged in the HUD editor. Position is stored in HUD units. */
public abstract class HudModule extends Module {
    public final Setting.Num posX, posY;
    public final Setting.Num scale;
    public final Setting.Bool customAccent;
    public final Setting.Color accentColor;
    public final int defX, defY;

    protected HudModule(String id, String name, String description, int defX, int defY) {
        super(id, name, description, Category.HUD);
        this.defX = defX; this.defY = defY;
        posX = add(new Setting.Num("Pos X", defX, 0, 4000, 1)); posX.hidden = true;
        posY = add(new Setting.Num("Pos Y", defY, 0, 4000, 1)); posY.hidden = true;
        scale = add(new Setting.Num("Element scale", 1, .5, 2, .05));
        customAccent = add(new Setting.Bool("Custom accent", false));
        accentColor = add(new Setting.Color("Element accent", 0xA855F7));
    }

    /** Accent used for this element: its own colour when enabled, otherwise the theme accent. */
    public int accent() { return customAccent.value ? 0xFF000000 | accentColor.rgb : Theme.accent(); }

    public abstract int width(MinecraftClient mc);
    public abstract int height(MinecraftClient mc);
    public abstract void draw(DrawContext c, MinecraftClient mc, int x, int y);
}
