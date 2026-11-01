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
    private int bh, doneX, resetX, btnY, btnW;

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
            if (!m.enabled) continue;
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
        String hint = any ? "Drag to move  |  mouse wheel over an element: resize  |  right-click: reset element" : "No HUD element is enabled. Turn one on in the menu.";
        c.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 10, Theme.text(), false);
        bh = mob ? 32 : 22; btnW = mob ? 110 : 90; btnY = height - bh - 12;
        doneX = width / 2 - btnW - 6; resetX = width / 2 + 6;
        button(c, doneX, "Done", acc);
        button(c, resetX, "Reset", Theme.card());
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
            if (hit != null) { hit.posX.set(hit.defX); hit.posY.set(hit.defY); hit.scale.set(1); return true; }
            return super.mouseClicked(click, doubled);
        }
        if (button != 0) return super.mouseClicked(click, doubled);
        if (inBtn(mx, my, doneX)) { close(); return true; }
        if (inBtn(mx, my, resetX)) {
            for (HudModule m : Modules.HUD) { m.posX.set(m.defX); m.posY.set(m.defY); m.scale.set(1); }
            return true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        float hs = (float) Modules.UI.hudScale.value;
        int vw = Math.round(width / hs), vh = Math.round(height / hs);
        double x = mx / hs, y = my / hs;
        for (int i = Modules.HUD.size() - 1; i >= 0; i--) {
            HudModule m = Modules.HUD.get(i);
            if (!m.enabled) continue;
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
        grab.posX.set(Math.max(0, Math.min(vw - w, xyz.rimvisuals.core.HudMath.snap(mx / hs - offX, grid))));
        grab.posY.set(Math.max(0, Math.min(vh - h, xyz.rimvisuals.core.HudMath.snap(my / hs - offY, grid))));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mx=click.x(), my=click.y(); int button=click.button();
        grab = null;
        return super.mouseReleased(click);
    }

    private HudModule hitTest(MinecraftClient mc, double x, double y, int vw, int vh) {
        for (int i = Modules.HUD.size() - 1; i >= 0; i--) {
            HudModule m = Modules.HUD.get(i);
            if (!m.enabled) continue;
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
        m.scale.set(m.scale.value + Math.signum(verticalAmount) * .05);
        return true;
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
    @Override public void removed() { Modules.save(); }
}
