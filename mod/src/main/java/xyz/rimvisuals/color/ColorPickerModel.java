package xyz.rimvisuals.color;

/** State of a colour picker (HSV + alpha) with the maths for its square, hue strip and alpha strip; the GUI only draws and forwards mouse positions. */
public final class ColorPickerModel {
    public float h, s = 1f, v = 1f, a = 1f;

    public ColorPickerModel() {}
    public ColorPickerModel(int rgb, float alpha) { setRgb(rgb); a = c01(alpha); }

    private static float c01(float x) { return Float.isNaN(x) ? 0f : x < 0f ? 0f : Math.min(x, 1f); }

    public void setRgb(int rgb) { float[] x = Hsv.fromRgb(rgb); if (x[1] > 0f && x[2] > 0f) h = x[0]; s = x[1]; v = x[2]; }
    public int rgb() { return Hsv.toRgb(h, s, v); }
    public int argb() { return Rgb.withAlpha(rgb(), a); }
    public String hex() { return Rgb.hex(rgb()); }

    /** Applies user text ("#FF8800", "ff8800", "#F80"); returns false and changes nothing when invalid. */
    public boolean setHex(String text) {
        long p = Rgb.parse(text); if (p == Rgb.INVALID) return false;
        setRgb((int) (p & 0xFFFFFF)); a = ((p >>> 24) & 255) / 255f; return true;
    }

    /** Saturation/value square: x right = saturation, y down = darker (both 0..1 of the square). */
    public void dragSquare(float x01, float y01) { s = c01(x01); v = 1f - c01(y01); }
    public void dragHue(float x01) { h = c01(x01) * .9999f; }
    public void dragAlpha(float x01) { a = c01(x01); }
    public void setRgbChannels(int r, int g, int b) { setRgb(((r & 255) << 16) | ((g & 255) << 8) | (b & 255)); }
}
