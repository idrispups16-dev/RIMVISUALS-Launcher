package xyz.rimvisuals.modules;

import net.minecraft.client.gui.DrawContext;
import xyz.rimvisuals.RimPlatform;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.ParticleField;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;
import xyz.rimvisuals.core.Modules;
import net.minecraft.client.MinecraftClient;

public final class ParticleOverlay extends Module {
    public final Setting.Num count = add(new Setting.Num("Count", 30, 5, 80, 1));
    public final Setting.Num size = add(new Setting.Num("Size", 2, 1, 4, 1));
    public final Setting.Bool overlay = add(new Setting.Bool("In-game overlay", false));
    public final Setting.Bool menu = add(new Setting.Bool("Menu particles", true));
    private final ParticleField hudField = new ParticleField(80);
    private final ParticleField menuField = new ParticleField(80);

    public ParticleOverlay() { super("particles", "Particle Effects", "Decorative floating particles", Category.VISUALS); }

    private int n() { int v = (int) count.value; return Modules.PERFORMANCE.particles(RimPlatform.mobile()?Math.min(v,24):v,MinecraftClient.getInstance().getCurrentFps()); }

    public void drawOverlay(DrawContext c, int w, int h) {
        hudField.render(c, w, h, n(), (int) size.value, Theme.accent() & 0xFFFFFF, 0.8f);
    }

    public void drawMenu(DrawContext c, int w, int h, float fade) {
        menuField.render(c, w, h, n(), (int) size.value, Theme.accent() & 0xFFFFFF, 0.9f * fade);
    }
}
