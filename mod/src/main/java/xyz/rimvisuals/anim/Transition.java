package xyz.rimvisuals.anim;

/**
 * Show/hide transition of one UI element: opacity, scale and position offset move together on one progress value, so
 * reversing mid-way is smooth (it continues from the current progress). Endpoints and curve are configuration, not a look.
 */
public final class Transition {
    private float progress;                  // 0 hidden .. 1 shown
    private boolean shown;
    private float inSec, outSec, delay, delayLeft;
    private Interpolator curve = Curve.CUBIC_OUT;
    public float opacityHidden = 0f, scaleHidden = 1f, offsetXHidden = 0f, offsetYHidden = 0f;

    public Transition(float inSec, float outSec) { this.inSec = Math.max(0f, inSec); this.outSec = Math.max(0f, outSec); }

    public Transition curve(Interpolator c) { this.curve = c == null ? Curve.LINEAR : c; return this; }
    public Transition delay(float sec) { this.delay = Math.max(0f, sec); return this; }
    public Transition hiddenState(float opacity, float scale, float offX, float offY) { opacityHidden = opacity; scaleHidden = scale; offsetXHidden = offX; offsetYHidden = offY; return this; }
    public Transition durations(float inSec, float outSec) { this.inSec = Math.max(0f, inSec); this.outSec = Math.max(0f, outSec); return this; }

    public void show() { if (!shown) { shown = true; delayLeft = delay; } }
    public void hide() { shown = false; delayLeft = 0f; }
    public void set(boolean visible) { if (visible) show(); else hide(); }
    public void snap(boolean visible) { shown = visible; progress = visible ? 1f : 0f; delayLeft = 0f; }

    public void advance(float dt) {
        if (!(dt > 0f)) return;
        if (shown && delayLeft > 0f) {
            if (dt <= delayLeft) { delayLeft -= dt; return; }
            dt -= delayLeft; delayLeft = 0f;          // the rest of this frame already counts as movement
        }
        float dur = shown ? inSec : outSec;
        float step = dur <= 0f ? 1f : dt / dur;
        progress = shown ? Math.min(1f, progress + step) : Math.max(0f, progress - step);
    }

    private float eased() { return curve.at(progress); }
    public float raw() { return progress; }
    public boolean isShown() { return shown; }
    public boolean visible() { return progress > 0f || shown; }
    public boolean settled() { return shown ? progress >= 1f : progress <= 0f; }
    public float opacity() { return clamp01(opacityHidden + (1f - opacityHidden) * eased()); }
    public float scale() { return scaleHidden + (1f - scaleHidden) * eased(); }
    public float offsetX() { return offsetXHidden * (1f - eased()); }
    public float offsetY() { return offsetYHidden * (1f - eased()); }
    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
