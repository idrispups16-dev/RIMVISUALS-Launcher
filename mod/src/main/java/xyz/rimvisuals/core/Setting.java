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

    public static final class Color extends Setting {
        public int rgb;
        public Color(String name, int rgb) { super(name); this.rgb = rgb & 0xFFFFFF; }
        public String hex() { return String.format("%06X", rgb & 0xFFFFFF); }
        @Override public String save() { return hex(); }
        @Override public void load(String s) { try { rgb = Integer.parseInt(s, 16) & 0xFFFFFF; } catch (NumberFormatException ignored) {} }
    }
}
