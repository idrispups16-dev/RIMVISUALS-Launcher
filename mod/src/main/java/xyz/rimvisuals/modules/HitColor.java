package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Recolours the red hurt flash on damaged entities by rewriting the vanilla overlay texture. */
public final class HitColor extends Module {
    public static final int VANILLA_ARGB = 0xB2FF0000;
    public final Setting.Color color = add(new Setting.Color("Color", 0xFF0000));
    public final Setting.Num alpha = add(new Setting.Num("Strength", .7, .1, 1, .05));

    public HitColor() {
        super("hit_color", "Hit Color", "Changes the colour of the damage flash on every entity (including you). One small texture upload when the setting changes; no per-frame cost.", Category.VISUALS);
        enabled = false;
    }

    /** ARGB the overlay texture should currently use. */
    public int argb() {
        if (!isActive()) return VANILLA_ARGB;
        return (Math.round((float) alpha.value * 255f) << 24) | (color.rgb & 0xFFFFFF);
    }
}
