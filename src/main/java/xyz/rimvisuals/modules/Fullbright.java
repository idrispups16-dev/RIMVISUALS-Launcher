package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

/** Raises the gamma value passed to the lightmap so dark areas are lit. Does not touch your Brightness option. */
public final class Fullbright extends Module {
    public final Setting.Num gamma = add(new Setting.Num("Brightness", 12, 2, 16, 1));

    public Fullbright() {
        super("fullbright", "Fullbright", "Lights dark areas by raising the lightmap gamma. Your Brightness option is left unchanged.", Category.VISUALS);
        enabled = false;
    }

    /** Gamma sent to the lightmap: never lower than the game's own value. */
    public static float effective(float vanilla, double wanted) { return Math.max(vanilla, (float) wanted); }
}
