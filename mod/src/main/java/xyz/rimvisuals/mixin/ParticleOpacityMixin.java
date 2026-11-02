package xyz.rimvisuals.mixin;
import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.*;
import xyz.rimvisuals.core.ManagedParticleAlpha;
@Mixin(BillboardParticle.class)
public abstract class ParticleOpacityMixin implements ManagedParticleAlpha {
 @Shadow protected float alpha;
 @Unique private boolean rimManaged;
 public void rimOpacity(float value){alpha=Math.max(0,Math.min(1,value));rimManaged=true;}
 public float rimOpacity(){return alpha;}
 public boolean rimManaged(){return rimManaged;}
}
