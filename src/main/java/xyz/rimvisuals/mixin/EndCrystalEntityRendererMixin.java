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
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.util.Identifier;
import xyz.rimvisuals.core.ScaleMath;
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

    private static RenderLayer rimTranslucent;

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private void rimSubmit(OrderedRenderCommandQueue queue, Model model, Object state, MatrixStack matrices, RenderLayer layer, int light, int overlay, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        float alpha = Modules.CRYSTAL_FADE.alpha(((RimSpawnAge) state).rimGetSpawnAge());
        if (alpha >= 0.999f) {
            queue.submitModel(model, state, matrices, layer, light, overlay, outline, crumbling);
            return;
        }
        if (rimTranslucent == null) rimTranslucent = RenderLayers.entityTranslucent(Identifier.ofVanilla("textures/entity/end_crystal/end_crystal.png"));
        queue.submitModel(model, state, matrices, rimTranslucent, light, overlay, ScaleMath.whiteTint(alpha), null, outline, crumbling);
    }
}
