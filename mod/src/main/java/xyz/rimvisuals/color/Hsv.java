package xyz.rimvisuals.color;

/** Allocation-free HSV <-> RGB. Hue is 0..1 (wraps), saturation/value 0..1, RGB is 0xRRGGBB. */
public final class Hsv {
    private Hsv() {}

    public static int toRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        s = clamp01(s); v = clamp01(v);
        float h6 = h * 6f; int i = (int) h6; float f = h6 - i;
        float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    public static float[] fromRgb(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min, h;
        if (d == 0f) h = 0f;
        else if (max == r) h = (((g - b) / d) % 6f) / 6f;
        else if (max == g) h = (((b - r) / d) + 2f) / 6f;
        else h = (((r - g) / d) + 4f) / 6f;
        if (h < 0f) h += 1f;
        return new float[]{h, max == 0f ? 0f : d / max, max};
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
