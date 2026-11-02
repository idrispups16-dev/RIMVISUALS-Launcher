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
    private HudModule grab;
    private double offX, offY;
    private int bh, doneX, resetX, monX, btnY, btnW;
    private int guideX = -1, guideY = -1;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("RIMVISUALS HUD"));
        this.parent = parent;
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
            int col = m == grab ? acc : Gfx.alpha(acc, 0.7f);
            c.fill(x - 2, y - 2, x + w + 2, y - 1, col);
            c.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, col);
            c.fill(x - 2, y - 2, x - 1, y + h + 2, col);
            c.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, col);
            c.drawText(textRenderer, m.name, x, Math.max(0, y - 12), acc, false);
        }
        ms.popMatrix();

        boolean mob = RimPlatform.mobile();
        String hint = any ? "Drag to move (snaps to guides and grid)  |  wheel: size  |  Shift+wheel: opacity  |  right-click: reset element  |  accent colour: menu > HUD" : "No HUD element is enabled. Turn one on in the menu.";
        c.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 10, Theme.text(), false);
        bh = mob ? 32 : 22; btnW = mob ? 110 : 90; btnY = height - bh - 12;
        int total = btnW * 3 + 24, start = (width - total) / 2;
        doneX = start; resetX = start + btnW + 12; monX = start + (btnW + 12) * 2;
        button(c, doneX, "Done", acc);
        button(c, resetX, "Reset", Theme.card());
        button(c, monX, "Monitor: " + (Modules.PERFORMANCE.monitor.value ? "On" : "Off"), Theme.card());
        if (grab != null) {
            int gc = Gfx.withAlpha(0x38BDF8, .8f);
            if (guideX >= 0) c.fill(Math.round(guideX * hs), 0, Math.round(guideX * hs) + 1, height, gc);
            if (guideY >= 0) c.fill(0, Math.round(guideY * hs), width, Math.round(guideY * hs) + 1, gc);
        }
    }

    private void button(DrawContext c, int x, String label, int bg) {
        Gfx.roundBorder(c, x, btnY, btnW, bh, 5, Gfx.mix(Theme.card(), bg, 0.45f), Gfx.alpha(Theme.accent(), 0.8f));
        c.drawText(textRenderer, label, x + (btnW - textRenderer.getWidth(label)) / 2, btnY + (bh - 8) / 2, Theme.text(), false);
    }

    private boolean inBtn(double mx, double my, int x) { return mx >= x && mx < x + btnW && my >= btnY && my < btnY + bh; }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx=click.x(), my=click.y(); int button=click.button();
        if (button == 1) {
            MinecraftClient mcr = MinecraftClient.getInstance();
            float hr = (float) Modules.UI.hudScale.value;
            int vwr = Math.round(width / hr), vhr = Math.round(height / hr);
            HudModule hit = hitTest(mcr, mx / hr, my / hr, vwr, vhr);
            if (hit != null) { hit.posX.set(hit.defX); hit.posY.set(hit.defY); hit.scale.set(1); hit.elementOpacity.set(1); return true; }
            return super.mouseClicked(click, doubled);
        }
        if (button != 0) return super.mouseClicked(click, doubled);
        if (inBtn(mx, my, doneX)) { close(); return true; }
        if (inBtn(mx, my, monX)) { Modules.PERFORMANCE.monitor.value = !Modules.PERFORMANCE.monitor.value; return true; }
        if (inBtn(mx, my, resetX)) {
            for (HudModule m : Modules.HUD) { m.posX.set(m.defX); m.posY.set(m.defY); m.scale.set(1); m.elementOpacity.set(1); }
            return true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        double x = mx / hs, y = my / hs;
        for (int i = Modules.HUD.size() - 1; i >= 0; i--) {
            HudModule m = Modules.HUD.get(i);
            if (!m.visible()) continue;
            int w = RimHud.sw(m, mc), h = RimHud.sh(m, mc), ex = RimHud.clampX(m, vw, w), ey = RimHud.clampY(m, vh, h);
            if (x >= ex - 4 && x < ex + w + 4 && y >= ey - 4 && y < ey + h + 4) {
                grab = m; offX = x - ex; offY = y - ey;
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
        return true;
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
    @Override public void removed() { Modules.save(); }
}
