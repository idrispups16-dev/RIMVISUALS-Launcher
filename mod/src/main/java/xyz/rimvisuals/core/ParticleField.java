package xyz.rimvisuals.core;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Drifting glow dots. Cheap: one small fill per particle. */
public final class ParticleField {
    private final float[] x, y, vx, vy, ph;
    private long last = System.nanoTime();

    public ParticleField(int max) {
        x = new float[max]; y = new float[max]; vx = new float[max]; vy = new float[max]; ph = new float[max];
        Random r = new Random();
        for (int i = 0; i < max; i++) {
            x[i] = r.nextFloat(); y[i] = r.nextFloat();
            vx[i] = (r.nextFloat() - 0.5f) * 0.02f; vy[i] = -(0.015f + r.nextFloat() * 0.04f);
            ph[i] = r.nextFloat() * 6.28f;
        }
    }

    public void render(DrawContext c, int w, int h, int count, int size, int rgb, float alpha) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        count = Math.min(count, x.length);
        for (int i = 0; i < count; i++) {
            x[i] += vx[i] * dt; y[i] += vy[i] * dt; ph[i] += dt * 1.5f;
            if (y[i] < -0.02f) { y[i] = 1.02f; }
            if (x[i] < -0.02f) x[i] = 1.02f; else if (x[i] > 1.02f) x[i] = -0.02f;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(ph[i]));
            int a = Math.max(5, Math.round(255 * alpha * tw));
            int px = Math.round(x[i] * w), py = Math.round(y[i] * h);
            c.fill(px, py, px + size, py + size, (a << 24) | (rgb & 0xFFFFFF));
        }
    }
}
