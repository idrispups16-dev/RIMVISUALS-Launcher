package xyz.rimvisuals.mixin;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.rimvisuals.core.Modules;
@Mixin(SoundSystem.class)
public class SoundSystemMixin {
 @Inject(method="getAdjustedVolume(FLnet/minecraft/sound/SoundCategory;)F",at=@At("RETURN"),cancellable=true)
 private void rimVolume(float volume,SoundCategory category,CallbackInfoReturnable<Float> cir){if(Modules.SOUND_CONTROLLER.isActive())cir.setReturnValue(Modules.SOUND_CONTROLLER.scale(cir.getReturnValueF(),category));}
}
