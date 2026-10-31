package xyz.rimvisuals.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.PerformanceMonitor;
import xyz.rimvisuals.core.RimSpawnAge;

@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V", at = @At("RETURN"))
    private void rimAge(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo ci) {
        ((RimSpawnAge) (Object) state).rimSetSpawnAge(entity.age + tickProgress);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    private void rimScaleStart(EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        matrices.push();
        long start = PerformanceMonitor.begin();
        float f = Modules.CRYSTAL_SCALE.factor(((RimSpawnAge) (Object) state).rimGetSpawnAge());
        PerformanceMonitor.end("Crystal Scale", start);
        if (f != 1f) matrices.scale(f, f, f);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("RETURN"))
    private void rimScaleEnd(EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        matrices.pop();
    }
}
