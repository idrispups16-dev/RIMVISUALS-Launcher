package xyz.rimvisuals;

import xyz.rimvisuals.core.Modules;

/** Decides whether to use the touch-friendly layout. Auto is a heuristic; users can force On/Off. */
public final class RimPlatform {
    private static final boolean AUTO = detect();

    private RimPlatform() {}

    private static boolean detect() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return os.contains("android") || (os.contains("linux") && (arch.contains("aarch64") || arch.contains("arm")));
    }

    public static boolean mobile() {
        int m = Modules.UI.mobile.index;
        return m == 1 || (m == 0 && AUTO);
    }
}
