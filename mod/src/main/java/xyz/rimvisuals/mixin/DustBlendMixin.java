package xyz.rimvisuals.mixin;
import net.minecraft.client.particle.AbstractDustParticle;
import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.rimvisuals.core.ManagedParticleAlpha;
@Mixin(AbstractDustParticle.class)
public class DustBlendMixin {
 @Inject(method="getRenderType",at=@At("RETURN"),cancellable=true)
 private void rimBlend(CallbackInfoReturnable<BillboardParticle.RenderType> cir){
  var p=(ManagedParticleAlpha)(Object)this;
  if(p.rimManaged()&&p.rimOpacity()<.999f)cir.setReturnValue(BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT);
 }
}
