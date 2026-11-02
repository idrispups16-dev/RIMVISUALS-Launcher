package xyz.rimvisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.particle.BillboardParticle;
@Mixin(BillboardParticle.class)
public interface ParticleOpacityAccessor {@Accessor("alpha")void rimAlpha(float value);}
