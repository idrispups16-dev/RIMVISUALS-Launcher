package xyz.rimvisuals.core;

/** Duck interface added by a mixin to render states so the renderer knows how old the entity is. */
public interface RimSpawnAge {
    float rimGetSpawnAge();
    void rimSetSpawnAge(float ticks);
}
