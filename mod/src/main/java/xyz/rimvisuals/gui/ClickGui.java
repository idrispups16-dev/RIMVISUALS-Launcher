package xyz.rimvisuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import xyz.rimvisuals.RimPlatform;
import xyz.rimvisuals.core.Anim;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;
import xyz.rimvisuals.core.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;

/** RIMVISUALS menu: tabs, search, module cards with settings, color editor, profiles. */
public class ClickGui extends Screen {
    private static final String[] TABS = {"HUD", "Visuals", "Interface", "Optimization", "Viewmodel", "Swing", "Shader Hands", "Profiles"};
    private static final Module.Category[] CATS = {Module.Category.HUD, Module.Category.VISUALS, Module.Category.INTERFACE, Module.Category.OPTIMIZATION, Module.Category.VIEWMODEL, Module.Category.SWING, Module.Category.SHADER_HANDS};
    private static final int NONE = Integer.MIN_VALUE;

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        final DoubleConsumer drag;
        Hit(int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.click = click; this.drag = drag;
        }
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Anim> anims = new HashMap<>();
    private final Set<Module> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Setting> colorOpen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Setting.Color, TextInput> hexInputs = new IdentityHashMap<>();
    private final Anim open = new Anim(0), tabPos = new Anim(0), scrollA = new Anim(0);
    private final TextInput search = new TextInput("Search...", 24);
    private final TextInput profileName = new TextInput("Profile name", 20);
    private TextInput focus;
    private Hit dragging;
    private boolean scrollDrag;
    private int tab;
    private float targetScroll, maxScroll;
    private List<String> profiles = Modules.listProfiles();
    private String status = "";

    private boolean mob;
    private float s, fade;
    private int vw, vh, wx, wy, ww, wh, cx, cw, ctop, cbot, cy;

    public ClickGui() {
        super(Text.literal("RIMVISUALS"));
        for (Module m : Modules.ALL) if (!m.toggleable) expanded.add(m);
        search.onChange = () -> targetScroll = 0;
        profileName.filter = ch -> ch < 128 && (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-');
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Modules.save(); }

    // ---------- helpers ----------
    private int f(int color) { return Gfx.alpha(color, fade); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(int x, int y, int w, int h, Runnable r, DoubleConsumer d) { hits.add(new Hit(x, y, w, h, r, d)); }
    private void txt(DrawContext c, String t, int x, int y, int col) { c.drawText(textRenderer, t, x, y, f(col), false); }
    private void txtC(DrawContext c, String t, int x, int y, int w, int h, int col) {
        c.drawText(textRenderer, t, x + (w - textRenderer.getWidth(t)) / 2, y + (h - 8) / 2, f(col), false);
    }
    private float anim(Object key, float target, float speed) {
        return anims.computeIfAbsent(key, k -> new Anim(target)).to(target, speed);
    }
    private int begin(int h) {
        int y = cy;
        cy += h;
        return (y >= ctop && y + h <= cbot) ? y : NONE;
    }
    private int rowH() { return mob ? 30 : 22; }
    private int sliderH() { return rowH() + (mob ? 12 : 8); }

    private void button(DrawContext c, int x, int y, int w, int h, String label, double mx, double my, Runnable r, boolean primary) {
        boolean hov = in(mx, my, x, y, w, h);
        int bg = primary ? Gfx.mix(Theme.card(), Theme.accent(), hov ? 0.6f : 0.4f) : (hov ? Theme.cardHover() : Theme.card());
        Gfx.roundBorder(c, x, y, w, h, 4, f(bg), f(Gfx.alpha(Theme.accent(), hov ? 0.9f : 0.4f)));
        txtC(c, label, x, y, w, h, Theme.text());
        hit(x, y, w, h, r, null);
    }

    private void drawSwitch(DrawContext c, int x, int y, int w, int h, Object key, boolean on) {
        float t = anim(key, on ? 1f : 0f, 14f);
        Gfx.round(c, x, y, w, h, h / 2, f(Gfx.mix(0xFF2A2740, Theme.accent(), t)));
        int k = h - 4;
        Gfx.round(c, x + 2 + Math.round((w - k - 4) * t), y + 2, k, k, k / 2, f(0xFFFFFFFF));
    }

    private void drawInput(DrawContext c, TextInput in, int x, int y, int w, int h) {
        boolean foc = focus == in;
        Gfx.roundBorder(c, x, y, w, h, 4, f(Theme.bg()), f(foc ? Theme.accent() : Gfx.alpha(Theme.accent(), 0.35f)));
        String shown = in.text;
        while (!shown.isEmpty() && textRenderer.getWidth(shown) > w - 10) shown = shown.substring(1);
        if (in.text.isEmpty() && !foc) txt(c, in.placeholder, x + 5, y + (h - 8) / 2, Theme.muted());
        else txt(c, shown, x + 5, y + (h - 8) / 2, Theme.text());
        if (foc && (System.currentTimeMillis() / 500) % 2 == 0) {
            int px = x + 5 + textRenderer.getWidth(shown);
            c.fill(px, y + 3, px + 1, y + h - 3, f(Theme.accent()));
        }
        hit(x, y, w, h, () -> focus = in, null);
    }

    // ---------- main render ----------
    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        mob = RimPlatform.mobile();
        s = (float) Modules.UI.guiScale.value;
        s = Math.max(0.3f, Math.min(s, Math.min(width / 320f, height / 190f)));
        vw = Math.round(width / s);
        vh = Math.round(height / s);
        double mx = mouseX / s, my = mouseY / s;
        hits.clear();
        fade = Math.max(0.02f, open.to(1f, 9f));
        int acc = Theme.accent();

        c.fill(0, 0, width, height, Gfx.withAlpha(0x000000, 0.7f * fade));
        if (Modules.PARTICLES.enabled && Modules.PARTICLES.menu.value) Modules.PARTICLES.drawMenu(c, width, height, fade);

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.scale(s, s);

        int hdr = mob ? 32 : 26, sideW = mob ? 86 : 72, pad = 6;
        ww = Math.min(vw - (mob ? 8 : 24), 440);
        wh = Math.min(vh - (mob ? 8 : 24), 270);
        wx = (vw - ww) / 2;
        wy = (vh - wh) / 2 + Math.round((1f - fade) * 12);

        Gfx.glow(c, wx, wy, ww, wh, 8, f(acc), 3);
        Gfx.roundBorder(c, wx, wy, ww, wh, 8, f(Theme.panel()), f(Gfx.alpha(acc, 0.55f)));
        Gfx.round(c, wx + 3, wy + hdr, sideW - 3, wh - hdr - 3, 6, f(Theme.bg()));

        // header
        Gfx.star(c, wx + 16, wy + hdr / 2, 7, f(acc));
        txt(c, "RIMVISUALS", wx + 28, wy + (hdr - 8) / 2, Theme.text());
        int bs = hdr - 8, bx = wx + ww - bs - 6, by = wy + 4;
        button(c, bx, by, bs, bs, "x", mx, my, this::close, false);
        String hudLabel = "Edit HUD";
        int hw = textRenderer.getWidth(hudLabel) + 14, hx = bx - 6 - hw;
        button(c, hx, by, hw, bs, hudLabel, mx, my, () -> client.setScreen(new HudEditorScreen(this)), true);
        int sx = wx + 96, sw = hx - 6 - sx;
        if (sw >= 40) drawInput(c, search, sx, by, sw, bs);

        // tabs
        int th = mob ? 32 : 24, ty = wy + hdr + 4;
        float sel = tabPos.to(tab, 14f);
        int hy = ty + Math.round(sel * (th + 3));
        Gfx.round(c, wx + 8, hy, sideW - 14, th, 5, f(Gfx.alpha(acc, 0.25f)));
        c.fill(wx + 8, hy + 5, wx + 10, hy + th - 5, f(acc));
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int y = ty + i * (th + 3);
            txt(c, TABS[i], wx + 16, y + (th - 8) / 2, i == tab ? Theme.text() : Theme.muted());
            hit(wx + 8, y, sideW - 14, th, () -> { tab = idx; targetScroll = 0; scrollA.value = 0; search.text = ""; }, null);
        }

        // content
        cx = wx + sideW + pad;
        cw = ww - sideW - pad * 2;
        ctop = wy + hdr + pad;
        cbot = wy + wh - pad;
        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        int scroll = Math.round(scrollA.to(targetScroll, 16f));
        cy = ctop - scroll;

        String q = search.text.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            boolean any = false;
            for (Module m : Modules.ALL) if (matches(m, q)) { moduleCard(c, m, mx, my); any = true; }
            if (!any) infoRow(c, "No results");
        } else if (tab < CATS.length) {
            for (Module m : Modules.ALL) if (m.category == CATS[tab]) moduleCard(c, m, mx, my);
        } else {
            profilesTab(c, mx, my);
        }
        int total = cy - (ctop - scroll);
        maxScroll = Math.max(0, total - (cbot - ctop));

        ms.popMatrix();
    }

    private static boolean matches(Module m, String q) {
        if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (Setting st : m.settings) if (!st.hidden && st.name.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private void infoRow(DrawContext c, String text) {
        int y = begin(rowH());
        if (y != NONE) txt(c, text, cx + 8, y + 6, Theme.muted());
    }

    // ---------- module cards ----------
    private void moduleCard(DrawContext c, Module m, double mx, double my) {
        int h = mob ? 38 : 30;
        boolean isOpen = expanded.contains(m);
        boolean hasSettings = false;
        for (Setting st : m.settings) if (!st.hidden) hasSettings = true;
        float a = anim(m.id + "#fade", isOpen ? 1f : 0f, 10f);
        int y = begin(h);
        if (y != NONE) {
            int sw = mob ? 30 : 24, swh = mob ? 16 : 12;
            boolean hov = in(mx, my, cx, y, cw, h - 3);
            Gfx.round(c, cx, y, cw, h - 3, 5, f(hov ? Theme.cardHover() : Theme.card()));
            if (m.toggleable && m.enabled) c.fill(cx, y + 6, cx + 2, y + h - 9, f(Theme.accent()));
            txt(c, m.name, cx + 9, y + (mob ? 7 : 5), Theme.text());
            int maxW = cw - 30 - (m.toggleable ? sw + 12 : 0);
            txt(c, textRenderer.trimToWidth(m.description, maxW), cx + 9, y + (mob ? 21 : 16), Theme.muted());
            int rightEdge = cx + cw - 8;
            if (hasSettings) {
                final boolean wasOpen = isOpen;
                int ew = m.toggleable ? cw - sw - 16 : cw;
                hit(cx, y, ew, h - 3, () -> { if (wasOpen) expanded.remove(m); else expanded.add(m); }, null);
                if (!m.toggleable) { txt(c, isOpen ? "v" : ">", rightEdge - 6, y + (h - 3 - 8) / 2, Theme.muted()); }
            }
            if (m.toggleable) {
                int swx = rightEdge - sw, swy = y + (h - 3 - swh) / 2;
                drawSwitch(c, swx, swy, sw, swh, m, m.enabled);
                hit(swx - 4, y, sw + 12, h - 3, () -> m.enabled = !m.enabled, null);
            }
        }
        if (isOpen) {
            for (Setting st : m.settings) if (!st.hidden) settingRow(c, st, mx, my, a);
            cy += 4;
        }
    }

    private void settingRow(DrawContext c, Setting st, double mx, double my, float a) {
        int x = cx + 8, w = cw - 8;
        if (st instanceof Setting.Bool b) {
            int h = rowH(), y = begin(h);
            if (y == NONE) return;
            boolean hov = in(mx, my, x, y, w, h - 2);
            Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
            txt(c, st.name, x + 8, y + (h - 2 - 8) / 2, Theme.text());
            int sw = mob ? 30 : 24, swh = mob ? 16 : 12;
            drawSwitch(c, x + w - sw - 8, y + (h - 2 - swh) / 2, sw, swh, st, b.value);
            hit(x, y, w, h - 2, () -> b.value = !b.value, null);
        } else if (st instanceof Setting.Num n) {
            slider(c, st.name, n.display(), n.fraction(), x, w, a, px -> n.set(n.min + px * (n.max - n.min)));
        } else if (st instanceof Setting.Mode md) {
            int h = rowH(), y = begin(h);
            if (y == NONE) return;
            boolean hov = in(mx, my, x, y, w, h - 2);
            Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
            txt(c, st.name, x + 8, y + (h - 2 - 8) / 2, Theme.text());
            String label = md.get();
            int bw = textRenderer.getWidth(label) + 14, bh = h - 8;
            Gfx.roundBorder(c, x + w - bw - 6, y + 3, bw, bh, 4, f(Gfx.mix(Theme.card(), Theme.accent(), 0.3f)), f(Gfx.alpha(Theme.accent(), 0.6f)));
            txtC(c, label, x + w - bw - 6, y + 3, bw, bh, Theme.text());
            hit(x, y, w, h - 2, md::next, null);
        } else if (st instanceof Setting.Color col) {
            int h = rowH(), y = begin(h);
            if (y != NONE) {
                boolean hov = in(mx, my, x, y, w, h - 2);
                Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
                txt(c, st.name, x + 8, y + (h - 2 - 8) / 2, Theme.text());
                int sw = mob ? 30 : 24, swh = mob ? 16 : 12;
                Gfx.roundBorder(c, x + w - sw - 8, y + (h - 2 - swh) / 2, sw, swh, 3, f(0xFF000000 | col.rgb), f(0xFFFFFFFF));
                hit(x, y, w, h - 2, () -> { if (!colorOpen.remove(col)) colorOpen.add(col); }, null);
            }
            if (colorOpen.contains(col)) {
                channel(c, "R", col, 16, a);
                channel(c, "G", col, 8, a);
                channel(c, "B", col, 0, a);
                hexRow(c, col, a);
            }
        }
    }

    /** value01 callback receives the position along the track (0..1). */
    private void slider(DrawContext c, String label, String value, double frac, int x, int w, float a, DoubleConsumer set01) {
        int h = sliderH(), y = begin(h);
        if (y == NONE) return;
        Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(Theme.bg(), a)));
        txt(c, label, x + 8, y + 4, Theme.text());
        txt(c, value, x + w - 8 - textRenderer.getWidth(value), y + 4, Theme.muted());
        int tx = x + 8, tw = w - 16, th = mob ? 5 : 3, ty = y + h - (mob ? 13 : 10), kn = mob ? 11 : 7;
        Gfx.round(c, tx, ty, tw, th, th / 2, f(0xFF2A2740));
        int fw = (int) Math.round(tw * Math.max(0, Math.min(1, frac)));
        if (fw > 0) Gfx.round(c, tx, ty, Math.max(fw, th), th, th / 2, f(Theme.accent()));
        Gfx.round(c, tx + fw - kn / 2, ty + th / 2 - kn / 2, kn, kn, kn / 2, f(0xFFFFFFFF));
        hit(x, y, w, h - 2, null, px -> set01.accept(Math.max(0, Math.min(1, (px - tx) / (double) tw))));
    }

    private void channel(DrawContext c, String label, Setting.Color col, int shift, float a) {
        int v = (col.rgb >> shift) & 255;
        slider(c, label, String.valueOf(v), v / 255.0, cx + 16, cw - 16, a, p -> {
            int nv = (int) Math.round(p * 255);
            col.rgb = (col.rgb & ~(255 << shift)) | (nv << shift);
        });
    }

    private void hexRow(DrawContext c, Setting.Color col, float a) {
        int h = rowH(), y = begin(h);
        if (y == NONE) return;
        int x = cx + 16, w = cw - 16;
        Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(Theme.bg(), a)));
        txt(c, "HEX", x + 8, y + (h - 2 - 8) / 2, Theme.text());
        TextInput in = hexInputs.computeIfAbsent(col, k -> {
            TextInput t = new TextInput("RRGGBB", 6);
            t.filter = ch -> Character.digit(ch, 16) >= 0;
            t.onChange = () -> {
                if (t.text.length() == 6) {
                    try { col.rgb = Integer.parseInt(t.text, 16) & 0xFFFFFF; } catch (NumberFormatException ignored) {}
                }
            };
            t.text = col.hex();
            return t;
        });
        if (focus != in) in.text = col.hex();
        drawInput(c, in, x + w - 84, y + 2, 76, h - 6);
    }

    // ---------- profiles ----------
    private void profilesTab(DrawContext c, double mx, double my) {
        int h = mob ? 36 : 28;
        int y = begin(h);
        if (y != NONE) {
            int bw = mob ? 70 : 58;
            drawInput(c, profileName, cx, y, cw - bw - 6, h - 4);
            button(c, cx + cw - bw, y, bw, h - 4, "Save", mx, my, () -> {
                String n = Modules.cleanName(profileName.text);
                if (n.isEmpty()) { status = "Type a profile name first"; return; }
                Modules.saveProfile(n);
                profiles = Modules.listProfiles();
                profileName.text = "";
                status = "Saved: " + n;
            }, true);
        }
        if (!status.isEmpty()) infoRow(c, status);
        if (profiles.isEmpty()) infoRow(c, "No profiles yet. Save your current setup above.");
        for (String name : profiles) {
            int ph = mob ? 36 : 28;
            int py = begin(ph);
            if (py == NONE) continue;
            Gfx.round(c, cx, py, cw, ph - 3, 5, f(Theme.card()));
            txt(c, textRenderer.trimToWidth(name, cw - 140), cx + 9, py + (ph - 3 - 8) / 2, Theme.text());
            int bw1 = mob ? 54 : 46, bw2 = mob ? 60 : 52, bh = ph - 9;
            int dx = cx + cw - bw2 - 5, lx = dx - bw1 - 4;
            button(c, lx, py + 3, bw1, bh, "Load", mx, my, () -> status = Modules.loadProfile(name) ? "Loaded: " + name : "Could not load " + name, true);
            button(c, dx, py + 3, bw2, bh, "Delete", mx, my, () -> {
                Modules.deleteProfile(name);
                profiles = Modules.listProfiles();
                status = "Deleted: " + name;
            }, false);
        }
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX=click.x(), mouseY=click.y(); int button=click.button();
        if (button != 0) return super.mouseClicked(click, doubled);
        double x = mouseX / s, y = mouseY / s;
        focus = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(x, y)) continue;
            if (h.click != null) h.click.run();
            if (h.drag != null) { dragging = h; h.drag.accept(x); }
            return true;
        }
        scrollDrag = in(x, y, cx, ctop, cw, cbot - ctop);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mouseX=click.x(), mouseY=click.y(); int button=click.button();
        if (dragging != null) { dragging.drag.accept(mouseX / s); return true; }
        if (scrollDrag) {
            targetScroll = Math.max(0, Math.min(maxScroll, targetScroll - (float) (dy / s)));
            scrollA.value = targetScroll;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX=click.x(), mouseY=click.y(); int button=click.button();
        dragging = null;
        scrollDrag = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll - (float) vertical * 24f));
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr=(char)input.codepoint();
        if (focus != null && focus.type(chr)) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode=input.key();
        if (focus != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { focus.backspace(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) { focus = null; return true; }
        }
        return super.keyPressed(input);
    }
}
