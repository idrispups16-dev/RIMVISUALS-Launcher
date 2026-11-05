package xyz.rimvisuals.core;

import xyz.rimvisuals.modules.HitColor;

/** Keeps the entity hurt-overlay texture in sync with the Hit Color module (upload only on change). */
public final class RimOverlays {
    public static RimOverlayRecolor instance;
    private static int applied = HitColor.VANILLA_ARGB;
    private RimOverlays() {}

    public static void tick() {
        RimOverlayRecolor tex = instance;
        if (tex == null) return;
        int want = Modules.HIT_COLOR.argb();
        if (want != applied) { tex.rimRecolor(want); applied = want; }
    }

    /** Called when the overlay texture is rebuilt (e.g. resource reload), so we reapply our colour. */
    public static void textureRecreated(RimOverlayRecolor tex) { instance = tex; applied = HitColor.VANILLA_ARGB; }
}
