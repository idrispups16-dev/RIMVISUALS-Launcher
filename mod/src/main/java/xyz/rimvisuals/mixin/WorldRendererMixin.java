package xyz.rimvisuals.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "pushEntityRenders(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V", at = @At("TAIL"))
    private void rimGhosts(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        Modules.CHINA_HAT.draw(matrices,queue,renderStates.cameraRenderState.pos);
        Modules.BLOCK_OVERLAY.draw(matrices,queue,renderStates.cameraRenderState.pos);
        Modules.ANCHOR_FADE.render(matrices, queue, renderStates.cameraRenderState.pos);
    }
}
