package xyz.rimvisuals.mixin;

import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import xyz.rimvisuals.core.RimSpawnAge;

@Mixin(EndCrystalEntityRenderState.class)
public class EndCrystalEntityRenderStateMixin implements RimSpawnAge {
    @org.spongepowered.asm.mixin.Unique private float rimSpawnAge = 1000f;
    @Override public float rimGetSpawnAge() { return rimSpawnAge; }
    @Override public void rimSetSpawnAge(float ticks) { rimSpawnAge = ticks; }
}
