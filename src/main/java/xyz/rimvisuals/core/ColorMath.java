package xyz.rimvisuals.core;

/** Pure colour helpers for animated tints (unit tested). All colours are 0xRRGGBB unless stated. */
public final class ColorMath {
    private ColorMath() {}

    /** Fully saturated rainbow colour; {@code phase} wraps every 1.0. */
    public static int rainbow(double phase) {
        double h = phase - Math.floor(phase);
        return java.awt.Color.HSBtoRGB((float) h, .85f, 1f) & 0xFFFFFF;
    }

    /** Smooth ping-pong between two colours; {@code phase} wraps every 1.0 (0 -> a, .5 -> b). */
    public static int gradient(int a, int b, double phase) {
        double t = .5 - .5 * Math.cos(2 * Math.PI * (phase - Math.floor(phase)));
        return lerp(a, b, (float) t);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = Math.round(((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = Math.round((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    public static int argb(int rgb, float alpha) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    /** Mixes an opaque ARGB colour with an RGB override; strength 0 keeps the original. */
    public static int mixArgb(int originalArgb, int rgb, float strength) {
        return (originalArgb & 0xFF000000) | lerp(originalArgb & 0xFFFFFF, rgb & 0xFFFFFF, strength);
    }

    /** Scales a positive finite fog distance; negative/zero/infinite sentinels are left alone. */
    public static float scaleDistance(float value, float factor) {
        if (!(value > 0f) || Float.isInfinite(value) || Float.isNaN(value)) return value;
        return value * Math.max(.05f, factor);
    }
}
