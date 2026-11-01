package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.ColorMath;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;

/** Tints and fades the first-person ARM model (bare hand and the arm holding an item). Items themselves are not tinted. */
public final class HandColor extends Module {
    public final Setting.Mode mode = add(new Setting.Mode("Mode", 0, "Solid", "Gradient", "Rainbow"));
    public final Setting.Color color = add(new Setting.Color("Color", 0xA855F7));
    public final Setting.Color color2 = add(new Setting.Color("Second color", 0x3B82F6));
    public final Setting.Num speed = add(new Setting.Num("Animation speed", .25, .05, 2, .05));
    public final Setting.Bool main = add(new Setting.Bool("Main hand", true));
    public final Setting.Bool off = add(new Setting.Bool("Off hand", true));
    public final Setting.Num mainOpacity = add(new Setting.Num("Main hand opacity", 1, .1, 1, .05));
    public final Setting.Num offOpacity = add(new Setting.Num("Off hand opacity", 1, .1, 1, .05));

    public HandColor() {
        super("hand_color", "Hand Color", "Tints the first-person arm: solid, gradient or rainbow, with per-hand opacity. Multiplies the skin colour; item models are not changed.", Category.SHADER_HANDS);
        enabled = false;
    }

    /** ARGB tint for the given hand, or -1 (no change) when the hand is not affected. */
    public int tint(boolean mainHand, long nanos) {
        if (!enabled || !(mainHand ? main.value : off.value)) return -1;
        double phase = Modules.PERFORMANCE.reducedMotion.value ? 0 : nanos / 1e9 * speed.value;
        int rgb = switch (mode.index) {
            case 1 -> ColorMath.gradient(color.rgb, color2.rgb, phase);
            case 2 -> ColorMath.rainbow(phase);
            default -> color.rgb;
        };
        return ColorMath.argb(rgb, (float) (mainHand ? mainOpacity.value : offOpacity.value));
    }
}
