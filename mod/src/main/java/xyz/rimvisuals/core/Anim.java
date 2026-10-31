package xyz.rimvisuals.core;

/** Smoothly moves a value toward a target (frame-rate independent). */
public final class Anim {
    public float value;
    private long last = System.nanoTime();

    public Anim(float start) { value = start; }

    public float to(float target, float speed) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        float k = 1f - (float) Math.exp(-speed * Modules.UI.animSpeed() * dt);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.002f) value = target;
        return value;
    }
}
