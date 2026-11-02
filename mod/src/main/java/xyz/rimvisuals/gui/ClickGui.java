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
import xyz.rimvisuals.core.ConfigStore;
import xyz.rimvisuals.core.Presets;
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
    private final Screen parentScreen;
    private static final String[] TABS = {"Visuals", "HUD", "Utilities", "Profiles", "Keybinds", "Friends", "Markers"};
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
    private final Map<Setting.Text, TextInput> ruleInputs = new IdentityHashMap<>();
    private final Map<Setting.Color, TextInput> hexInputs = new IdentityHashMap<>();
    private final Anim open = new Anim(0), tabPos = new Anim(0), scrollA = new Anim(0);
    private final TextInput search = new TextInput("Search...", 24);
    private final TextInput profileName = new TextInput("Profile name", 20);
    private Module selected;
    private List<Module> filtered=List.of();
    private String filterKey="";
    private float detailScroll, detailMax;
    private final Anim detailA=new Anim(0);
    private int detailX,detailW;
    private boolean detailVisible;
    private String tooltip="";
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

    public ClickGui() { this(null); }
    public ClickGui(Screen parent) {
        super(Text.literal("RIMVISUALS"));
        parentScreen=parent;
        for (Module m : Modules.ALL) if (!m.toggleable) expanded.add(m);
        search.onChange = () -> {targetScroll = 0;filterKey="";};
        profileName.filter = ch -> ch < 128 && (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-');
    }

    public void editHud(xyz.rimvisuals.core.HudModule m){tab=1;selected=m;colorOpen.add(m.accentColor);detailScroll=0;}
    @Override public void close(){client.setScreen(parentScreen);}
    public void showCategoryForTest(int index){tab=0;selected=index==4?Modules.VIEWMODEL:null;}
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
        if(Boolean.getBoolean("rimvisuals.smoke"))xyz.rimvisuals.SmokeTest.frames++;
        mob = RimPlatform.mobile();
        s = (float) Modules.UI.guiScale.value;
        s = Math.max(0.45f, Math.min(s, Math.min(width / 640f, height / 400f)));
        vw = Math.round(width / s);
        vh = Math.round(height / s);
        double mx = mouseX / s, my = mouseY / s;
        hits.clear();tooltip="";
        fade = Math.max(0.02f, open.to(1f, 9f));
        int acc = Theme.accent();

        c.fill(0, 0, width, height, Gfx.withAlpha(0x000000, (float)Modules.UI.backdrop.value * fade));
        if (Modules.PARTICLES.enabled && Modules.PARTICLES.menu.value) Modules.PARTICLES.drawMenu(c, width, height, fade);

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.scale(s, s);

        int hdr = mob ? 32 : 26, sideW = mob ? 112 : 94, pad = 6;
        ww = Math.min(vw - (mob ? 8 : 24), (int)Modules.UI.panelWidth.value);
        wh = Math.min(vh - (mob ? 8 : 24), (int)Modules.UI.panelHeight.value);
        wx = (vw - ww) / 2;
        wy = (vh - wh) / 2 + Math.round((1f - fade) * 12);

        Gfx.glow(c, wx, wy, ww, wh, 8, f(acc), 1);
        Gfx.roundBorder(c, wx, wy, ww, wh, 8, f(Theme.panel()), f(Gfx.alpha(acc, 0.55f)));
        Gfx.round(c, wx + 3, wy + hdr, sideW - 3, wh - hdr - 3, 6, f(Theme.bg()));

        for(int i=0;i<32;i++){int x1=wx+2+(ww-4)*i/32,x2=wx+2+(ww-4)*(i+1)/32;c.fill(x1,wy+hdr-2,x2,wy+hdr,Gfx.mix(Theme.accent(),Theme.accent2(),i/31f));}
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
            hit(wx + 8, y, sideW - 14, th, () -> selectTab(idx), null);
        }

        // content
        cx = wx + sideW + pad;
        cw = ww - sideW - pad * 2;
        ctop = wy + hdr + pad;
        cbot = wy + wh - pad;
        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        int scroll = Math.round(scrollA.to(targetScroll, 16f));
        cy = ctop - scroll;
        c.enableScissor(cx,ctop,cx+cw,cbot);

        detailVisible=false;
        if (tab == 3) profilesTab(c, mx, my);
        else renderModuleBrowser(c,mx,my,scroll);
        int total = cy - (ctop - scroll);
        maxScroll = Math.max(0, total - (cbot - ctop));

        c.disableScissor();
        if(detailVisible) renderDetails(c,mx,my);
        if(!tooltip.isEmpty()){var lines=textRenderer.wrapLines(Text.literal(tooltip),Math.max(120,ww-30));int tipY=wy+wh+3;if(tipY+lines.size()*10>vh)tipY=wy+wh-lines.size()*10-5;c.fill(wx+5,tipY-3,wx+ww-5,tipY+lines.size()*10+2,0xee080810);for(var line:lines){c.drawText(textRenderer,line,wx+10,tipY,Theme.text(),false);tipY+=10;}}
        ms.popMatrix();
    }

    private void selectTab(int index) {
        if(index==4){client.setScreen(new KeybindManagerScreen(this));return;}
        if(index==5||index==6){client.setScreen(new LocalDataScreen(this,index==6));return;}
        tab=index;targetScroll=0;scrollA.value=0;selected=null;search.text="";filterKey="";
    }

    private boolean categoryMatches(Module m) {
        if(tab==1)return m.category==Module.Category.HUD;
        if(tab==2)return m.category==Module.Category.INTERFACE||m.category==Module.Category.OPTIMIZATION||m.category==Module.Category.UTILITIES;
        return m.category==Module.Category.VISUALS||m.category==Module.Category.VIEWMODEL||m.category==Module.Category.SWING||m.category==Module.Category.SHADER_HANDS;
    }

    private void renderModuleBrowser(DrawContext c,double mx,double my,int scroll) {
        String q=search.text.trim().toLowerCase(Locale.ROOT),key=tab+":"+q;
        if(!filterKey.equals(key)) {
            filtered=Modules.ALL.stream().filter(m->categoryMatches(m)&&(q.isEmpty()||matches(m,q))).toList();filterKey=key;
        }
        int originalW=cw;
        boolean wide=ww>=780;
        if(selected!=null&&!wide){detailVisible=true;detailX=cx;detailW=cw;cy=ctop;return;}
        if(selected!=null){detailVisible=true;detailW=240;detailX=cx+cw-detailW;cw-=detailW+10;}
        int gap=8,columns=2,cardW=(cw-gap)/2,cardH=48;
        if(filtered.isEmpty())infoRow(c,"No modules match this search");
        for(int i=0;i<filtered.size();i++) {
            Module m=filtered.get(i);int x=cx+(i%columns)*(cardW+gap),y=ctop-scroll+(i/columns)*cardH;
            if(y<ctop||y+cardH>cbot)continue;
            boolean hover=in(mx,my,x,y,cardW,cardH-6);
            Gfx.roundBorder(c,x,y,cardW,cardH-6,7,f(hover?Theme.cardHover():Theme.card()),f(selected==m?Theme.accent2():0xff252438));
            if(hover)tooltip=m.name+": "+m.description+" "+m.statusNote();
            int sw=25;
            txt(c,textRenderer.trimToWidth(m.name,cardW-48),x+10,y+9,m.toggleable&&!m.enabled?Theme.muted():Theme.text());
            txt(c,!m.suppressionReason().isEmpty()?m.suppressionReason():m.toggleable?(m.enabled?"ENABLED":"DISABLED"):"SETTINGS",x+10,y+25,m.toggleable&&m.enabled?Theme.accent2():Theme.muted());
            hit(x,y,cardW-(m.toggleable?sw+12:0),cardH-6,()->{selected=m;detailScroll=0;detailA.value=0;focus=null;},null);
            if(m.toggleable){drawSwitch(c,x+cardW-sw-9,y+14,sw,14,m,m.enabled);hit(x+cardW-sw-12,y,sw+12,cardH-6,()->m.enabled=!m.enabled,null);}
        }
        cy=ctop-scroll+((filtered.size()+1)/2)*cardH;
        cw=originalW;
    }

    private void renderDetails(DrawContext c,double mx,double my) {
        int oldX=cx,oldW=cw,oldTop=ctop,oldCy=cy;
        cx=detailX;cw=detailW;
        Gfx.round(c,cx,ctop,cw,cbot-ctop,7,f(0xff12101e));
        button(c,cx+6,ctop+5,44,18,"Back",mx,my,()->{selected=null;detailScroll=0;},false);
        txt(c,textRenderer.trimToWidth(selected.name,cw-65),cx+58,ctop+10,Theme.text());
        ctop+=32;cy=ctop-Math.round(detailA.to(detailScroll,16));
        c.enableScissor(cx,ctop,cx+cw,cbot);
        if(!selected.statusNote().isEmpty())infoRow(c,textRenderer.trimToWidth(selected.statusNote(),cw-16));
        int resetY=begin(rowH());if(resetY!=NONE)button(c,cx+8,resetY,cw-16,rowH()-3,"Reset module",mx,my,()->Modules.reset(selected),false);
        if(selected instanceof xyz.rimvisuals.modules.ServerHelper helper){
            int joinY=begin(rowH());if(joinY!=NONE)button(c,cx+8,joinY,cw-16,rowH()-3,"Manual server connection",mx,my,()->client.setScreen(new HelperConnectScreen(this,helper.ft)),false);
            int cfgY=begin(rowH());if(cfgY!=NONE)button(c,cx+8,cfgY,cw-16,rowH()-3,"Save separate server CFG",mx,my,()->{String name=(helper.ft?"FunTime":"HolyWorld")+"_"+System.currentTimeMillis();Modules.createProfile(name);Modules.save();},false);
        }
        for(Setting st:selected.settings)if(!st.hidden)settingRow(c,st,mx,my,1);
        detailMax=Math.max(0,cy-(ctop-Math.round(detailA.value))-(cbot-ctop));
        detailScroll=Math.max(0,Math.min(detailMax,detailScroll));
        c.disableScissor();cx=oldX;cw=oldW;ctop=oldTop;cy=oldCy;
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
            boolean hov = in(mx, my, cx, y, cw, h - 3);if(hov)tooltip=m.name+": "+m.description+" "+m.statusNote();
            Gfx.round(c, cx, y, cw, h - 3, 5, f(hov ? Theme.cardHover() : Theme.card()));
            if (m.toggleable && m.enabled) c.fill(cx, y + 6, cx + 2, y + h - 9, f(Theme.accent()));
            txt(c, textRenderer.trimToWidth(m.name,cw-65), cx + 9, y + (mob ? 7 : 5), Theme.text());
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
            if(!selected.statusNote().isEmpty())infoRow(c,textRenderer.trimToWidth(selected.statusNote(),cw-16));
        int resetY=begin(rowH());if(resetY!=NONE)button(c,cx+8,resetY,cw-16,rowH()-3,"Reset module to defaults",mx,my,()->Modules.reset(m),false);
            for (Setting st : m.settings) if (!st.hidden) settingRow(c, st, mx, my, a);
            cy += 4;
        }
    }

    private void settingRow(DrawContext c, Setting st, double mx, double my, float a) {
        int x = cx + 8, w = cw - 16;
        if(st instanceof Setting.Text rule){
            int h=rowH()+18,y=begin(h);if(y==NONE)return;
            txt(c,textRenderer.trimToWidth(st.name,w-12),x+4,y+2,Theme.text());
            TextInput input=ruleInputs.computeIfAbsent(rule,k->{TextInput t=new TextInput("Not configured",192);t.text=k.value;t.onChange=()->k.value=t.text;return t;});
            if(focus!=input)input.text=rule.value;
            drawInput(c,input,x,y+13,w,h-16);
            if(in(mx,my,x,y,w,h))tooltip=st.name+": "+rule.value;
        } else if (st instanceof Setting.Bool b) {
            int h = rowH(), y = begin(h);
            if (y == NONE) return;
            boolean hov = in(mx, my, x, y, w, h - 2);
            Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
            txt(c, textRenderer.trimToWidth(st.name,w-64), x + 8, y + (h - 2 - 8) / 2, Theme.text());
            if(hov)tooltip=st.name;
            int sw = mob ? 30 : 24, swh = mob ? 16 : 12;
            drawSwitch(c, x + w - sw - 8, y + (h - 2 - swh) / 2, sw, swh, st, b.value);
            hit(x, y, w, h - 2, () -> b.value = !b.value, null);
        } else if (st instanceof Setting.Num n) {
            slider(c, st.name, n.display(), n.fraction(), x, w, a, px -> n.set(n.min + px * (n.max - n.min)));
        } else if (st instanceof Setting.Mode md) {
            int h = rowH()+12, y = begin(h);
            if (y == NONE) return;
            boolean hov = in(mx, my, x, y, w, h - 2);
            Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
            txt(c, textRenderer.trimToWidth(st.name,w-16), x + 8, y + 4, Theme.text());
            if(hov)tooltip=st.name;
            String label = textRenderer.trimToWidth(md.get(),Math.max(28,w/2-16));
            int bw = textRenderer.getWidth(label) + 14, bh = 16;
            Gfx.roundBorder(c, x + w - bw - 6, y + h - 19, bw, bh, 4, f(Gfx.mix(Theme.card(), Theme.accent(), 0.3f)), f(Gfx.alpha(Theme.accent(), 0.6f)));
            txtC(c, label, x + w - bw - 6, y + h - 19, bw, bh, Theme.text());
            hit(x, y, w, h - 2, md::next, null);
        } else if (st instanceof Setting.Color col) {
            int h = rowH(), y = begin(h);
            if (y != NONE) {
                boolean hov = in(mx, my, x, y, w, h - 2);
                Gfx.round(c, x, y, w, h - 2, 4, f(Gfx.alpha(hov ? Theme.cardHover() : Theme.bg(), a)));
                txt(c, textRenderer.trimToWidth(st.name,w-64), x + 8, y + (h - 2 - 8) / 2, Theme.text());
            if(hov)tooltip=st.name;
                int sw = mob ? 30 : 24, swh = mob ? 16 : 12;
                Gfx.roundBorder(c, x + w - sw - 8, y + (h - 2 - swh) / 2, sw, swh, 3, f(0xFF000000 | col.rgb), f(0xFFFFFFFF));
                hit(x, y, w, h - 2, () -> { if (!colorOpen.remove(col)) colorOpen.add(col); }, null);
            }
            if (colorOpen.contains(col)) {
                hsvChannel(c,col,0,a);hsvChannel(c,col,1,a);hsvChannel(c,col,2,a);
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
        txt(c, textRenderer.trimToWidth(label,w-24-textRenderer.getWidth(value)), x + 8, y + 4, Theme.text());
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
            col.rgb = (col.rgb & ~(255 << shift)) | (nv << shift);customTheme(col);
        });
    }

    private void customTheme(Setting.Color col){if(col==Modules.UI.custom||col==Modules.UI.secondary)Modules.UI.theme.index=3;}
    private void hsvChannel(DrawContext c,Setting.Color col,int channel,float a){float[] hsv=java.awt.Color.RGBtoHSB((col.rgb>>16)&255,(col.rgb>>8)&255,col.rgb&255,null);String label=new String[]{"Hue","Saturation","Value"}[channel];slider(c,label,String.valueOf(Math.round(hsv[channel]*(channel==0?360:100))),hsv[channel],cx+16,cw-16,a,p->{float[] current=java.awt.Color.RGBtoHSB((col.rgb>>16)&255,(col.rgb>>8)&255,col.rgb&255,null);current[channel]=(float)p;col.rgb=java.awt.Color.HSBtoRGB(current[0],current[1],current[2])&0xffffff;customTheme(col);});}

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
                    try { col.rgb = Integer.parseInt(t.text, 16) & 0xFFFFFF;customTheme(col); } catch (NumberFormatException ignored) {}
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
                Runnable save = () -> {
                    boolean ok=Modules.store().exists(n)?Modules.saveProfile(n):Modules.createProfile(n);
                    profiles=Modules.listProfiles();profileName.text="";status=ok?"Saved: "+n:"Save failed: "+n;
                };
                if(Modules.store().exists(n)) confirm("Replace CFG "+n+"?",save); else save.run();
            }, true);
        }
        // toolbar: presets and file actions
        int th = mob ? 34 : 26;
        String[] presetNames = Presets.ENABLED.keySet().toArray(new String[0]);
        int ty = begin(th);
        if (ty != NONE) {
            int bw = (cw - 4 * (presetNames.length - 1)) / presetNames.length;
            for (int k = 0; k < presetNames.length; k++) {
                String pn = presetNames[k];
                button(c, cx + k * (bw + 4), ty, bw, th - 4, pn.replace('_', ' '), mx, my,
                        () -> status = Modules.applyPreset(pn) ? "Preset applied: " + pn.replace('_', ' ') : "Unknown preset", false);
            }
        }
        int fy = begin(th);
        if (fy != NONE) {
            String[] labels = {"Backup", "Restore", "Import", "Server"};
            int bw = (cw - 4 * (labels.length - 1)) / labels.length;
            Runnable[] actions = {
                    () -> status = Modules.backupNow() ? "Backup saved (last " + ConfigStore.MAX_BACKUPS + " kept)" : "Backup failed",
                    () -> confirm("Restore latest backup? Current settings will be backed up.", () -> { status = Modules.restoreLatestBackup() ? "Restored latest backup" : "Restore failed / no backup"; profiles = Modules.listProfiles(); }),
                    () -> { var got = Modules.importProfiles(); profiles = Modules.listProfiles();
                            status = got.isEmpty() ? "Nothing to import: put .json files into config/rimvisuals/imports" : "Imported: " + String.join(", ", got); },
                    () -> { String a = serverAddress();
                            status = a == null ? "Join a server first" : Modules.serverProfile(a).map(n -> "This server uses: " + n).orElse("No configuration bound to " + a); }
            };
            for (int k = 0; k < labels.length; k++) button(c, cx + k * (bw + 4), fy, bw, th - 4, labels[k], mx, my, actions[k], false);
        }
        if (!status.isEmpty()) infoRow(c, status);
        if (profiles.isEmpty()) infoRow(c, "No profiles yet. Save your current setup above.");
        infoRow(c, "Name field: Save / Copy to / Rename to. Export writes config/rimvisuals/exports.");
        for (String name : profiles) {
            if(!search.text.isBlank()&&!name.toLowerCase(Locale.ROOT).contains(search.text.toLowerCase(Locale.ROOT)))continue;
            int ph = mob ? 74 : 58;
            int py = begin(ph);
            if (py == NONE) continue;
            Gfx.round(c, cx, py, cw, ph - 3, 5, f(Theme.card()));
            String bound = serverAddress() != null && Modules.serverProfile(serverAddress()).filter(name::equals).isPresent() ? "  [server]" : "";
            txt(c, textRenderer.trimToWidth(name + bound, cw - 20), cx + 9, py + 6, Theme.text());
            String[] labels = {"Load", "Copy", "Rename", "Export", "Server", "Delete"};
            int per = 3, bw = (cw - 18 - 4 * (per - 1)) / per, bh = (ph - 3 - 22) / 2 - 2;
            for (int k = 0; k < labels.length; k++) {
                int bx = cx + 9 + (k % per) * (bw + 4), by = py + 19 + (k / per) * (bh + 3);
                String label = labels[k];
                button(c, bx, by, bw, bh, label, mx, my, () -> profileAction(label, name), label.equals("Load"));
            }
        }
    }

    private String serverAddress() {
        var e = client == null ? null : client.getCurrentServerEntry();
        return e == null ? null : e.address;
    }

    private void confirm(String question, Runnable action) {
        client.setScreen(new net.minecraft.client.gui.screen.ConfirmScreen(yes -> {
            client.setScreen(this);
            if(yes) action.run();
        }, Text.literal("RIMVISUALS CFG"), Text.literal(question)));
    }

    private void profileAction(String action, String name) {
        String target = Modules.cleanName(profileName.text);
        switch (action) {
            case "Load" -> status = Modules.loadProfile(name) ? "Loaded: " + name : "Could not load " + name;
            case "Copy" -> {
                if (target.isEmpty()) status = "Type the new name in the field first";
                else status = Modules.duplicateProfile(name, target) ? "Copied to: " + target : "Could not copy (name taken?)";
            }
            case "Rename" -> {
                if (target.isEmpty()) status = "Type the new name in the field first";
                else confirm("Rename "+name+" to "+target+"?", () -> {status = Modules.renameProfile(name, target) ? "Renamed to: " + target : "Could not rename (name taken?)";profiles=Modules.listProfiles();});
            }
            case "Export" -> { String p = Modules.exportProfile(name); status = p == null ? "Export failed" : "Exported: " + p; }
            case "Server" -> {
                String a = serverAddress();
                if (a == null) status = "Join a server first";
                else if (Modules.serverProfile(a).filter(name::equals).isPresent()) status = Modules.unbindServer(a) ? "Unbound " + a : "Could not unbind";
                else status = Modules.bindServer(a, name) ? name + " will load on " + a : "Could not bind";
            }
            case "Delete" -> confirm("Delete CFG "+name+"?", () -> {status=Modules.deleteProfile(name)?"Deleted: "+name:"Delete failed";profiles=Modules.listProfiles();});
            default -> { }
        }
        profiles = Modules.listProfiles();
        if (!action.equals("Load") && !action.equals("Export") && !action.equals("Server")) profileName.text = "";
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
        if(detailVisible&&mouseX/s>=detailX&&mouseX/s<detailX+detailW)detailScroll=Math.max(0,Math.min(detailMax,detailScroll-(float)vertical*24f));
        else targetScroll = Math.max(0, Math.min(maxScroll, targetScroll - (float) vertical * 24f));
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
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (focus != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { focus.backspace(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) { focus = null; return true; }
        }
        return super.keyPressed(input);
    }
}
