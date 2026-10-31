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

    public final Setting.Num panelWidth=add(new Setting.Num("Panel width",600,480,900,10));
    public final Setting.Num panelHeight=add(new Setting.Num("Panel height",360,280,600,10));
    public final Setting.Num opacity=add(new Setting.Num("Panel opacity",.94,.2,1,.01));
    public final Setting.Num backdrop=add(new Setting.Num("Backdrop opacity",.7,0,1,.01));
    public final Setting.Color background=add(new Setting.Color("Panel background",0x0e0b18));
    public final Setting.Color secondary=add(new Setting.Color("Gradient accent",0x3b82f6));
    public float animSpeed() { return (float) animationSpeed.value; }
}
