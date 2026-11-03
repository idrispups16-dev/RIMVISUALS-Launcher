package xyz.rimvisuals.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V", at = @At("HEAD"))
    private void rimBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        Modules.ANCHOR_FADE.onBlockUpdate((ClientWorld) (Object) this, pos, state);
    }
}
