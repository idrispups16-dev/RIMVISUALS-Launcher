package xyz.rimvisuals.color;

import java.util.Arrays;
import java.util.Locale;

/**
 * What colour an effect uses: a fixed colour or an animated one. Immutable and allocation-free to resolve, so it is safe
 * to call per vertex per frame. {@code resolve(time, pos)} returns 0xAARRGGBB where {@code pos} (0..1) is the place along
 * a trail/ribbon/gradient and {@code time} is seconds from {@code UiMotion.time()}.
 */
public final class ColorSpec {
    public enum Mode { SOLID, RAINBOW, GRADIENT, PULSE, FADE, CYCLE }

    public static final int MAX_STOPS = 8;
    public static final String PREFIX = "c2|";

    public final Mode mode;
    private final int[] stops;          // 0xRRGGBB, 1..8
    public final float alpha;           // 0..1, multiplied into every result
    public final float speed;           // cycles per second (0 = static)
    public final float spread;          // how much pos shifts the phase (0 = same colour everywhere, 1 = full cycle along pos)
    public final float depth;           // PULSE/FADE amplitude 0..1
    public final float saturation, brightness;   // RAINBOW
    public final boolean loop;          // GRADIENT: wrap last->first (otherwise clamp)

    private ColorSpec(Mode mode, int[] stops, float alpha, float speed, float spread, float depth, float sat, float bri, boolean loop) {
        this.mode = mode; this.stops = stops; this.alpha = c01(alpha); this.speed = clamp(speed, 0f, 8f); this.spread = clamp(spread, 0f, 4f);
        this.depth = c01(depth); this.saturation = c01(sat); this.brightness = c01(bri); this.loop = loop;
    }

    private static float c01(float v) { return Float.isNaN(v) ? 0f : v < 0f ? 0f : Math.min(v, 1f); }
    private static float clamp(float v, float lo, float hi) { return Float.isNaN(v) ? lo : v < lo ? lo : Math.min(v, hi); }

    // ---------------------------------------------------------------- factories
    public static ColorSpec solid(int rgb) { return new ColorSpec(Mode.SOLID, new int[]{rgb & 0xFFFFFF}, 1f, 0f, 0f, 0f, 1f, 1f, false); }
    public static ColorSpec solid(int rgb, float alpha) { return new ColorSpec(Mode.SOLID, new int[]{rgb & 0xFFFFFF}, alpha, 0f, 0f, 0f, 1f, 1f, false); }
    public static ColorSpec rainbow(float speed, float spread, float saturation, float brightness) { return new ColorSpec(Mode.RAINBOW, new int[]{0xFF0000}, 1f, speed, spread, 0f, saturation, brightness, true); }
    public static ColorSpec gradient(float speed, float spread, boolean loop, int... stops) { return new ColorSpec(Mode.GRADIENT, norm(stops), 1f, speed, spread, 0f, 1f, 1f, loop); }
    public static ColorSpec pulse(int rgb, float speed, float depth) { return new ColorSpec(Mode.PULSE, new int[]{rgb & 0xFFFFFF}, 1f, speed, 0f, depth, 1f, 1f, false); }
    public static ColorSpec fade(int rgb, float speed, float depth) { return new ColorSpec(Mode.FADE, new int[]{rgb & 0xFFFFFF}, 1f, speed, 0f, depth, 1f, 1f, false); }
    public static ColorSpec cycle(float speed, int... stops) { return new ColorSpec(Mode.CYCLE, norm(stops), 1f, speed, 0f, 0f, 1f, 1f, true); }

    private static int[] norm(int[] s) {
        if (s == null || s.length == 0) return new int[]{0xFFFFFF};
        int n = Math.min(s.length, MAX_STOPS); int[] o = new int[n];
        for (int i = 0; i < n; i++) o[i] = s[i] & 0xFFFFFF;
        return o;
    }

    public ColorSpec withAlpha(float a) { return new ColorSpec(mode, stops, a, speed, spread, depth, saturation, brightness, loop); }
    public ColorSpec withStops(int... s) { return new ColorSpec(mode, norm(s), alpha, speed, spread, depth, saturation, brightness, loop); }
    public ColorSpec withSpeed(float v) { return new ColorSpec(mode, stops, alpha, v, spread, depth, saturation, brightness, loop); }
    public ColorSpec withSpread(float v) { return new ColorSpec(mode, stops, alpha, speed, v, depth, saturation, brightness, loop); }

    // ---------------------------------------------------------------- query
    public int baseRgb() { return stops[0]; }
    public int stopCount() { return stops.length; }
    public int stop(int i) { return stops[Math.max(0, Math.min(i, stops.length - 1))]; }
    public boolean animated() { return mode != Mode.SOLID && speed > 0f || mode == Mode.GRADIENT && spread > 0f || mode == Mode.RAINBOW && spread > 0f; }
    public boolean isPlainSolid() { return mode == Mode.SOLID && alpha >= 1f; }

    // ---------------------------------------------------------------- resolve (hot path: no allocation)
    public int resolve(double timeSec, float pos) {
        double phase = timeSec * speed + pos * spread;
        phase -= Math.floor(phase);
        int rgb; float a = alpha;
        switch (mode) {
            case RAINBOW: rgb = Hsv.toRgb((float) phase, saturation, brightness); break;
            case GRADIENT: rgb = gradientAt(timeSec * speed + pos * spread); break;
            case CYCLE: rgb = cycleAt(phase); break;
            case PULSE: rgb = Rgb.scale(stops[0], 1f - depth * (float) (.5 - .5 * Math.cos(2 * Math.PI * phase))); break;
            case FADE: rgb = stops[0]; a *= 1f - depth * (float) (.5 - .5 * Math.cos(2 * Math.PI * phase)); break;
            default: rgb = stops[0];
        }
        return Rgb.withAlpha(rgb, a);
    }

    private int gradientAt(double raw) {
        int n = stops.length;
        if (n == 1) return stops[0];
        double x;
        if (loop) { raw -= Math.floor(raw); x = raw * n; }
        else { raw = raw < 0 ? 0 : Math.min(raw, 1.0); x = raw * (n - 1); }
        int i = (int) Math.floor(x); float f = (float) (x - i);
        int a = stops[Math.min(i, n - 1) % n], b = stops[loop ? (i + 1) % n : Math.min(i + 1, n - 1)];
        return Rgb.lerp(a, b, f * f * (3f - 2f * f));
    }

    private int cycleAt(double phase) {
        int n = stops.length;
        if (n == 1) return stops[0];
        double x = phase * n; int i = (int) Math.floor(x); float f = (float) (x - i);
        float blend = f < .75f ? 0f : (f - .75f) / .25f;            // hold, then a short smooth hand-over to the next colour
        return Rgb.lerp(stops[i % n], stops[(i + 1) % n], blend * blend * (3f - 2f * blend));
    }

    // ---------------------------------------------------------------- persistence
    /** Plain opaque solid colours are stored as the legacy 6-digit hex so old configs, old builds and exports stay readable. */
    public String serialize() {
        if (isPlainSolid()) return Rgb.hex(stops[0]);
        StringBuilder sb = new StringBuilder(PREFIX).append(mode.name().toLowerCase(Locale.ROOT)).append('|');
        for (int i = 0; i < stops.length; i++) { if (i > 0) sb.append(','); sb.append(Rgb.hex(stops[i])); }
        return sb.append("|a=").append(f(alpha)).append("|s=").append(f(speed)).append("|p=").append(f(spread)).append("|d=").append(f(depth))
                .append("|sat=").append(f(saturation)).append("|br=").append(f(brightness)).append("|l=").append(loop ? 1 : 0).toString();
    }

    private static String f(float v) { return String.format(Locale.ROOT, "%.3f", v); }

    /** Accepts legacy hex ("8B5CF6", "#8B5CF6", "#8B5CF680") and the "c2|..." form. Returns null on anything malformed. */
    public static ColorSpec parse(String s) {
        if (s == null) return null;
        s = s.trim();
        if (!s.startsWith(PREFIX)) {
            long v = Rgb.parse(s);
            return v == Rgb.INVALID ? null : solid((int) (v & 0xFFFFFF), ((v >>> 24) & 255) / 255f);
        }
        String[] p = s.substring(PREFIX.length()).split("\\|");
        if (p.length < 2 || s.length() > 400) return null;
        Mode mode;
        try { mode = Mode.valueOf(p[0].toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
        String[] hx = p[1].split(",");
        if (hx.length == 0 || hx.length > MAX_STOPS) return null;
        int[] st = new int[hx.length];
        for (int i = 0; i < hx.length; i++) { long v = Rgb.parse(hx[i]); if (v == Rgb.INVALID) return null; st[i] = (int) (v & 0xFFFFFF); }
        float a = 1f, sp = 0f, pr = 0f, d = 0f, sat = 1f, br = 1f; boolean loop = mode == Mode.RAINBOW || mode == Mode.CYCLE;
        for (int i = 2; i < p.length; i++) {
            int eq = p[i].indexOf('='); if (eq < 0) continue;
            String k = p[i].substring(0, eq), val = p[i].substring(eq + 1);
            try {
                float x = Float.parseFloat(val);
                switch (k) { case "a": a = x; break; case "s": sp = x; break; case "p": pr = x; break; case "d": d = x; break;
                    case "sat": sat = x; break; case "br": br = x; break; case "l": loop = x != 0f; break; default: break; }
            } catch (NumberFormatException e) { return null; }
        }
        return new ColorSpec(mode, st, a, sp, pr, d, sat, br, loop);
    }

    @Override public boolean equals(Object o) {
        return o instanceof ColorSpec c && mode == c.mode && Arrays.equals(stops, c.stops) && alpha == c.alpha && speed == c.speed
                && spread == c.spread && depth == c.depth && saturation == c.saturation && brightness == c.brightness && loop == c.loop;
    }
    @Override public int hashCode() { return Arrays.hashCode(stops) * 31 + mode.hashCode(); }
    @Override public String toString() { return serialize(); }
}
