package xyz.rimvisuals.core;

import java.util.Locale;

/** A saved module option. Subclasses: Bool, Num, Mode, Color. */
public abstract class Setting {
    public final String name;
    public boolean hidden;

    protected Setting(String name) { this.name = name; }

    public String key() { return name.toLowerCase(Locale.ROOT).replace(' ', '_'); }
    public abstract String save();
    public abstract void load(String s);

    public static final class Text extends Setting {
        public String value;
        public Text(String name,String value){super(name);this.value=HelperData.clean(value);}
        @Override public String save(){return value;}
        @Override public void load(String s){value=HelperData.clean(s);}
    }

    public static final class Bool extends Setting {
        public boolean value;
        public Bool(String name, boolean value) { super(name); this.value = value; }
        @Override public String save() { return String.valueOf(value); }
        @Override public void load(String s) { value = Boolean.parseBoolean(s); }
    }

    public static final class Num extends Setting {
        public double value;
        public final double min, max, step;
        public Num(String name, double value, double min, double max, double step) {
            super(name); this.min = min; this.max = max; this.step = step; this.value = value;
        }
        public void set(double v) {
            if (!Double.isFinite(v)) return;
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }
        public double fraction() { return max == min ? 0 : (value - min) / (max - min); }
        public String display() {
            return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
        }
        @Override public String save() { return String.valueOf(value); }
        @Override public void load(String s) { try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {} }
    }

    public static final class Mode extends Setting {
        public final String[] modes;
        public int index;
        public Mode(String name, int index, String... modes) { super(name); this.index = index; this.modes = modes; }
        public String get() { return modes[index]; }
        public void next() { index = (index + 1) % modes.length; }
        @Override public String save() { return String.valueOf(index); }
        @Override public void load(String s) {
            try { int i = Integer.parseInt(s); if (i >= 0 && i < modes.length) index = i; } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * A colour option. {@code rgb} stays the plain colour every existing effect reads; {@code spec} (optional) adds
     * animation, alpha, gradients and so on for effects that call {@link #argb}. Plain solid colours still save as the old
     * 6-digit hex, so existing configs load unchanged and unchanged settings write the same text as before.
     */
    public static final class Color extends Setting {
        public int rgb;
        public xyz.rimvisuals.color.ColorSpec spec;
        public Color(String name, int rgb) { super(name); this.rgb = rgb & 0xFFFFFF; }
        public String hex() { return String.format("%06X", rgb & 0xFFFFFF); }

        /** Current spec; if some code assigned {@code rgb} directly after the spec was set, the direct value wins (spec cleared). */
        public xyz.rimvisuals.color.ColorSpec spec() {
            if (spec != null && spec.baseRgb() != rgb) spec = null;
            return spec != null ? spec : xyz.rimvisuals.color.ColorSpec.solid(rgb);
        }
        public void setSpec(xyz.rimvisuals.color.ColorSpec s) {
            if (s == null) return;
            rgb = s.baseRgb(); spec = s.isPlainSolid() ? null : s;
        }
        public void setSolid(int rgb) { this.rgb = rgb & 0xFFFFFF; this.spec = null; }
        /** 0xAARRGGBB for this frame. {@code pos} is the place along a trail/ribbon (0..1); pass 0 for single-colour effects. */
        public int argb(double timeSec, float pos) { return spec().resolve(timeSec, pos); }

        @Override public String save() { return spec().serialize(); }
        @Override public void load(String s) {
            xyz.rimvisuals.color.ColorSpec p = xyz.rimvisuals.color.ColorSpec.parse(s);
            if (p == null) return;                       // malformed text keeps the current colour (same as the old behaviour)
            setSpec(p);
        }
    }
}
