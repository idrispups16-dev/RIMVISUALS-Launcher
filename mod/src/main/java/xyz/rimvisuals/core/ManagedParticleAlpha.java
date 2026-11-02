package xyz.rimvisuals.core;
/** Marks only particles emitted by RIMVISUALS; vanilla/other mods are left unchanged. */
public interface ManagedParticleAlpha {
 void rimOpacity(float value);
 float rimOpacity();
 boolean rimManaged();
}
