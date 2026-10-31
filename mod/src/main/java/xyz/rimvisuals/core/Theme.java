package xyz.rimvisuals.core;

/** Interface themes. Index matches InterfaceModule.theme: Violet, Blue, Midnight, Custom. */
public final class Theme {
    private static final int[] ACC = {0xFFA855F7, 0xFF3B82F6, 0xFF6366F1};
    private static final int[] ACC2 = {0xFF3B82F6, 0xFF8B5CF6, 0xFFA855F7};
    private static final int[] BG = {0xF006060C, 0xF0050810, 0xF0030309, 0xF006060C};
    private static final int[] PANEL = {0xF00E0B18, 0xF00A1020, 0xF0090912, 0xF00E0B18};
    private static final int[] CARD = {0xFF171326, 0xFF121A30, 0xFF14142A, 0xFF171326};

    private Theme() {}

    private static int i() { return Modules.UI.theme.index; }

    public static int accent() { return i() < 3 ? ACC[i()] : 0xFF000000 | Modules.UI.custom.rgb; }
    public static int accent2() { return i() < 3 ? ACC2[i()] : Gfx.mix(accent(), 0xFF3B82F6, 0.5f); }
    public static int bg() { return BG[i()]; }
    public static int panel() { return PANEL[i()]; }
    public static int card() { return CARD[i()]; }
    public static int cardHover() { return Gfx.mix(card(), accent(), 0.14f); }
    public static int text() { return 0xFFEDEAF7; }
    public static int muted() { return 0xFF8E87A8; }
}
