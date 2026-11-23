package xyz.rimvisuals.color;

/** RGB / ARGB / HEX helpers. Packed colours are ints: 0xRRGGBB or 0xAARRGGBB. */
public final class Rgb {
    private Rgb() {}

    public static int lerp(int a, int b, float t) {
        t = t < 0f ? 0f : Math.min(t, 1f);
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Lerp of full ARGB values (alpha included). */
    public static int lerpArgb(int a, int b, float t) {
        int al = Math.round(((a >>> 24)) + ((b >>> 24) - (a >>> 24)) * (t < 0f ? 0f : Math.min(t, 1f)));
        return (al << 24) | lerp(a, b, t);
    }

    public static int withAlpha(int rgb, float alpha) {
        return (Math.round((alpha < 0f ? 0f : Math.min(alpha, 1f)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    public static int scale(int rgb, float k) {
        k = k < 0f ? 0f : k;
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * k)), b = Math.min(255, Math.round((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /** Parses "RRGGBB", "#RRGGBB", "RGB" or "#RRGGBBAA"; returns the packed 0xAARRGGBB or -1L-style failure via {@code OptionalLong}-free sentinel {@link #INVALID}. */
    public static final long INVALID = Long.MIN_VALUE;

    public static long parse(String s) {
        if (s == null) return INVALID;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        if (s.length() != 6 && s.length() != 8) return INVALID;
        for (int i = 0; i < s.length(); i++) if (Character.digit(s.charAt(i), 16) < 0) return INVALID;
        long v = Long.parseLong(s, 16);
        return s.length() == 6 ? (0xFFL << 24) | v : ((v & 0xFF) << 24) | (v >> 8);
    }

    public static String hex(int rgb) { return String.format(java.util.Locale.ROOT, "%06X", rgb & 0xFFFFFF); }
}
