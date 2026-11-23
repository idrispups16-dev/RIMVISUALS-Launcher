package xyz.rimvisuals.anim;

/** Maps normalised progress 0..1 to eased progress (usually 0..1; Back/Spring curves may overshoot). */
@FunctionalInterface
public interface Interpolator {
    float at(float t);
}
