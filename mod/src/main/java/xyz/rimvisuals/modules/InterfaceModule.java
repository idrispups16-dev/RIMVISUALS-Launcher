package xyz.rimvisuals.modules;

import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

public final class InterfaceModule extends Module {
    public final Setting.Mode theme = add(new Setting.Mode("Theme", 0, "Violet", "Blue", "Midnight", "Custom"));
    public final Setting.Color custom = add(new Setting.Color("Custom accent", 0xA855F7));
    public final Setting.Num guiScale = add(new Setting.Num("Menu scale", 1.0, 0.6, 1.8, 0.05));
    public final Setting.Num hudScale = add(new Setting.Num("HUD scale", 1.0, 0.6, 2.0, 0.05));
    public final Setting.Num animationSpeed = add(new Setting.Num("Animation speed", 1.0, 0.5, 2.0, 0.1));
    public final Setting.Mode mobile = add(new Setting.Mode("Mobile mode", 0, "Auto", "On", "Off"));

    public InterfaceModule() {
        super("interface", "Interface", "Theme, colors, scale and touch mode", Category.INTERFACE);
        toggleable = false;
    }

    public float animSpeed() { return (float) animationSpeed.value; }
}
