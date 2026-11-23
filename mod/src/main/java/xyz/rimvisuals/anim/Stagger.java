package xyz.rimvisuals.anim;

/** Delay helper for sequential appearance of list items (categories, rows). */
public final class Stagger {
    private Stagger() {}

    /** Delay for item {@code index}: step each, but the whole sequence never exceeds {@code maxTotal} seconds. */
    public static float delay(int index, int count, float step, float maxTotal) {
        if (index <= 0 || count <= 1) return 0f;
        float total = Math.min(maxTotal, step * (count - 1));
        return total * Math.min(index, count - 1) / (count - 1);
    }
}
