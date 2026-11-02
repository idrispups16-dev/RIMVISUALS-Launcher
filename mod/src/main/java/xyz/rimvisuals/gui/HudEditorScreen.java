package xyz.rimvisuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import xyz.rimvisuals.RimHud;
import xyz.rimvisuals.RimPlatform;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.HudModule;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Theme;

/** Drag HUD elements with the mouse or a finger. Positions are saved when you leave. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule grab, selected;
    private final Runnable afterClose;
    private double offX, offY;

    private int guideX = -1, guideY = -1;

    public HudEditorScreen(Screen parent) {
        this(parent, () -> {});
    }

    public HudEditorScreen(Screen parent, Runnable afterClose) {
        super(Text.literal("RIMVISUALS HUD"));
        this.parent = parent;
        this.afterClose = afterClose;
    }

    @Override protected void init() {
        String[] labels = {"Done", "Menu", "Grid", "Monitor", "Reset one", "Reset all",
                           "Left", "Center X", "Right", "Top", "Center Y", "Bottom"};
        int bw = Math.max(1, (width - 14) / 6);
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal(labels[i]), b -> action(action))
                .dimensions(4 + (i % 6) * (bw + 1), height - 48 + (i / 6) * 23, bw, 20).build());
        }
    }

    private void resetElement(HudModule m) {
        m.posX.set(m.defX); m.posY.set(m.defY); m.scale.set(1); m.elementOpacity.set(1);
        m.customAccent.value = false; m.accentColor.rgb = 0xA855F7;
    }

    private void action(int action) {
        if (action == 0) { close(); return; }
        if (action == 1) { MinecraftClient.getInstance().setScreen(new ClickGui()); return; }
        if (action == 2) { Modules.UI.gridSnap.set(Modules.UI.gridSnap.value > 0 ? 0 : 8); return; }
        if (action == 3) { Modules.PERFORMANCE.monitor.value = !Modules.PERFORMANCE.monitor.value; return; }
        if (action == 4) { if (selected != null) resetElement(selected); return; }
        if (action == 5) { for (HudModule m : Modules.HUD) resetElement(m); return; }
        if (selected == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        double hs = Modules.UI.hudScale.value;
        int vw = (int) Math.round(width / hs), vh = (int) Math.round(height / hs);
        if (action <= 8) selected.posX.set(xyz.rimvisuals.core.HudMath.align(vw, RimHud.sw(selected, mc), action - 6));
        else selected.posY.set(xyz.rimvisuals.core.HudMath.align(vh, RimHud.sh(selected, mc), action - 9));
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int acc = Theme.accent();
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        c.fill(0, 0, width, height, 0x99000000);
        int grid = (int) Modules.UI.gridSnap.value;
        if (grid > 0) {
            int gl = Gfx.withAlpha(0xFFFFFF, .06f);
            for (int gx = 0; gx * grid * hs < width; gx++) c.fill(Math.round(gx * grid * hs), 0, Math.round(gx * grid * hs) + 1, height, gl);
            for (int gy = 0; gy * grid * hs < height; gy++) c.fill(0, Math.round(gy * grid * hs), width, Math.round(gy * grid * hs) + 1, gl);
        }
        boolean any = false;
        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.scale(hs, hs);
        for (HudModule m : Modules.HUD) {
            if (!m.visible()) continue;
            any = true;
            int w = RimHud.sw(m, mc), h = RimHud.sh(m, mc), x = RimHud.clampX(m, vw, w), y = RimHud.clampY(m, vh, h);
            RimHud.drawScaled(c, mc, m, x, y);
            int col = m == selected ? acc : Gfx.alpha(acc, 0.7f);
            c.fill(x - 2, y - 2, x + w + 2, y - 1, col);
            c.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, col);
            c.fill(x - 2, y - 2, x - 1, y + h + 2, col);
            c.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, col);
            c.drawText(textRenderer, m.name, x, Math.max(0, y - 12), acc, false);
        }
        ms.popMatrix();

        String hint = "Drag | Wheel: scale | Shift+wheel: opacity | Right-click: reset";
        c.drawText(textRenderer, textRenderer.trimToWidth(hint, width - 12), 6, 6, Theme.text(), false);
        String state = "Grid: " + (grid > 0 ? grid : "OFF") + " | Selected: " + (selected == null ? "none" : selected.name);
        c.drawText(textRenderer, textRenderer.trimToWidth(state, width - 12), 6, 18, Theme.text(), false);
        super.render(c, mouseX, mouseY, delta);
        if (grab != null) {
            int gc = Gfx.withAlpha(0x38BDF8, .8f);
            if (guideX >= 0) c.fill(Math.round(guideX * hs), 0, Math.round(guideX * hs) + 1, height, gc);
            if (guideY >= 0) c.fill(0, Math.round(guideY * hs), width, Math.round(guideY * hs) + 1, gc);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx=click.x(), my=click.y(); int button=click.button();
        if (my >= height - 48) return super.mouseClicked(click, doubled);
        if (button == 1) {
            MinecraftClient mcr = MinecraftClient.getInstance();
            float hr = (float) Modules.UI.hudScale.value;
            int vwr = Math.round(width / hr), vhr = Math.round(height / hr);
            HudModule hit = hitTest(mcr, mx / hr, my / hr, vwr, vhr);
            if (hit != null) { selected = hit; resetElement(hit); return true; }
            return super.mouseClicked(click, doubled);
        }
        if (button != 0) return super.mouseClicked(click, doubled);
        MinecraftClient mc = MinecraftClient.getInstance();
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        double x = mx / hs, y = my / hs;
        for (int i = Modules.HUD.size() - 1; i >= 0; i--) {
            HudModule m = Modules.HUD.get(i);
            if (!m.visible()) continue;
            int w = RimHud.sw(m, mc), h = RimHud.sh(m, mc), ex = RimHud.clampX(m, vw, w), ey = RimHud.clampY(m, vh, h);
            if (x >= ex - 4 && x < ex + w + 4 && y >= ey - 4 && y < ey + h + 4) {
                grab = selected = m; offX = x - ex; offY = y - ey;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx=click.x(), my=click.y(); int button=click.button();
        if (grab == null) return super.mouseDragged(click, dx, dy);
        MinecraftClient mc = MinecraftClient.getInstance();
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        int w = RimHud.sw(grab, mc), h = RimHud.sh(grab, mc), grid = (int) Modules.UI.gridSnap.value;
        double rx = mx / hs - offX, ry = my / hs - offY;
        int[] xl = guideLines(mc, true, vw, vh), yl = guideLines(mc, false, vw, vh);
        double nx = xyz.rimvisuals.core.HudMath.guideSnap(rx, w, xl, 4), ny = xyz.rimvisuals.core.HudMath.guideSnap(ry, h, yl, 4);
        if (nx == rx) nx = xyz.rimvisuals.core.HudMath.snap(rx, grid);
        if (ny == ry) ny = xyz.rimvisuals.core.HudMath.snap(ry, grid);
        nx = Math.max(0, Math.min(vw - w, nx)); ny = Math.max(0, Math.min(vh - h, ny));
        grab.posX.set(nx); grab.posY.set(ny);
        guideX = xyz.rimvisuals.core.HudMath.matchedGuide(grab.posX.value, w, xl);
        guideY = xyz.rimvisuals.core.HudMath.matchedGuide(grab.posY.value, h, yl);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mx=click.x(), my=click.y(); int button=click.button();
        grab = null; guideX = guideY = -1;
        return super.mouseReleased(click);
    }

    /** Screen edges, screen centre and every other visible element's edges and centre. */
    private int[] guideLines(MinecraftClient mc, boolean horizontal, int vw, int vh) {
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
        int extent = horizontal ? vw : vh;
        out.add(0); out.add(extent / 2); out.add(extent);
        for (HudModule m : Modules.HUD) {
            if (!m.visible() || m == grab) continue;
            int w = RimHud.sw(m, mc), h = RimHud.sh(m, mc);
            int pos = horizontal ? RimHud.clampX(m, vw, w) : RimHud.clampY(m, vh, h), size = horizontal ? w : h;
            out.add(pos); out.add(pos + size); out.add(pos + size / 2);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private HudModule hitTest(MinecraftClient mc, double x, double y, int vw, int vh) {
        for (int i = Modules.HUD.size() - 1; i >= 0; i--) {
            HudModule m = Modules.HUD.get(i);
            if (!m.visible()) continue;
            int w = RimHud.sw(m, mc), h = RimHud.sh(m, mc), ex = RimHud.clampX(m, vw, w), ey = RimHud.clampY(m, vh, h);
            if (x >= ex - 4 && x < ex + w + 4 && y >= ey - 4 && y < ey + h + 4) return m;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float hs = (float) Modules.UI.hudScale.value;
        HudModule m = hitTest(mc, mouseX / hs, mouseY / hs, Math.round(width / hs), Math.round(height / hs));
        if (m == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        boolean shift = org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (shift) m.elementOpacity.set(m.elementOpacity.value + Math.signum(verticalAmount) * .05);
        else m.scale.set(m.scale.value + Math.signum(verticalAmount) * .05);
        selected = m;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        m.posX.set(RimHud.clampX(m, vw, RimHud.sw(m, mc)));
        m.posY.set(RimHud.clampY(m, vh, RimHud.sh(m, mc)));
        return true;
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); afterClose.run(); }
    @Override public void removed() { Modules.save(); }
}
