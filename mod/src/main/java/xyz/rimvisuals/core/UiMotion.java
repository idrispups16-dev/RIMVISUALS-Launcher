package xyz.rimvisuals.core;

import xyz.rimvisuals.anim.FrameClock;
import xyz.rimvisuals.anim.Transition;

/**
 * The one place UI/effect code gets frame time from. It applies the user's animation-speed setting and honours
 * "Reduced motion" (animations jump to their end state instead of moving). Call {@link #frame()} once per rendered frame.
 */
public final class UiMotion {
    private static final FrameClock CLOCK = new FrameClock();
    private static float dt;
    private UiMotion() {}

    /** Advances the shared clock and returns this frame's delta in seconds, already scaled by the UI speed setting. */
    public static float frame() {
        float raw = CLOCK.tick(System.nanoTime());
        dt = raw * speed();
        return dt;
    }

    public static float dt() { return dt; }
    public static double time() { return CLOCK.time(); }
    public static boolean reduced() { return Modules.PERFORMANCE.reducedMotion.value; }
    private static float speed() { return Math.max(.1f, Modules.UI.animSpeed()); }

    /** Advances a transition by the current frame (or snaps it when reduced motion is on). */
    public static void advance(Transition t) {
        if (reduced()) t.snap(t.isShown()); else t.advance(dt);
    }
}
