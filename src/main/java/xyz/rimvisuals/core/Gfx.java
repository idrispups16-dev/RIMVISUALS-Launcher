package xyz.rimvisuals.core;

import net.minecraft.client.gui.DrawContext;

/** Small drawing helpers (rounded rectangles, glow, RV logo). Only uses DrawContext.fill. */
public final class Gfx {
    private Gfx() {}

    /** Per-element HUD opacity multiplier; set by RimHud around each element and always restored to 1. */
    public static float hudAlpha = 1f;

    /** Applies the current HUD element opacity to an ARGB colour. */
    public static int ha(int argb) {
        return multiplyAlpha(argb,hudAlpha);
    }

    public static int multiplyAlpha(int argb,float opacity) {
        if (opacity >= 1f) return argb;
        int a = Math.round((argb >>> 24) * Math.max(0f, opacity));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static void fill(DrawContext c, int x1, int y1, int x2, int y2, int color) { c.fill(x1, y1, x2, y2, ha(color)); }

    public static void text(DrawContext c, net.minecraft.client.font.TextRenderer tr, String s, int x, int y, int color) {
        int col = ha(color);
        if ((col >>> 24) < 4) return; // fully transparent text would otherwise render opaque
        c.drawTextWithShadow(tr, s, x, y, col);
    }

    public static int alpha(int argb, float f) {
        int a = argb >>> 24;
        if (a == 0) return argb;
        a = Math.round(a * Math.max(0f, Math.min(1f, f)));
        return (Math.max(a, 5) << 24) | (argb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, float a01) {
        return (Math.round(255 * Math.max(0f, Math.min(1f, a01))) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = (a >>> sh) & 255, cb = (b >>> sh) & 255;
            out |= (Math.round(ca + (cb - ca) * t) & 255) << sh;
        }
        return out;
    }

    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        color = ha(color);
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }

    public static void roundBorder(DrawContext c, int x, int y, int w, int h, int r, int fill, int border) {
        round(c, x, y, w, h, r, border);
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int color, int layers) {
        for (int i = layers; i >= 1; i--) {
            float f = 0.04f + 0.03f * (layers - i);
            round(c, x - i * 2, y - i * 2, w + i * 4, h + i * 4, r + i * 2, alpha(color, f));
        }
    }

    /** Original compact RV monogram, drawn on a seven by five grid. */
    public static void rv(DrawContext c, int cx, int cy, int radius, int color) {
        String[] glyph={"1100101","1010101","1100101","1010101","1010010"};
        int cell=Math.max(1,(radius*2+1)/7), x=cx-7*cell/2,y=cy-5*cell/2;
        color=ha(color);
        for(int row=0;row<5;row++)for(int col=0;col<7;col++)if(glyph[row].charAt(col)=='1')
            c.fill(x+col*cell,y+row*cell,x+(col+1)*cell,y+(row+1)*cell,color);
    }
}
