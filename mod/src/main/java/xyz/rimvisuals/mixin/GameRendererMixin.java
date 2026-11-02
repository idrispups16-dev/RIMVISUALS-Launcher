package xyz.rimvisuals.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.rimvisuals.core.Modules;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="getFov",at=@org.spongepowered.asm.mixin.injection.At("RETURN"),cancellable=true)
    private void rimZoom(net.minecraft.client.render.Camera camera,float tick,boolean changingFov,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if(!changingFov)return;
        var mc=net.minecraft.client.MinecraftClient.getInstance();
        cir.setReturnValue((float)Modules.ZOOM.apply(cir.getReturnValueF(),mc.player!=null&&mc.currentScreen==null));
    }

    /** The 14.0 is the vanilla roll in degrees at full damage tilt. */
    @ModifyConstant(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", constant = @Constant(doubleValue = 14.0))
    private double rimHurtTilt(double vanilla) { return vanilla * Modules.HURT_CAMERA.scale(); }
}
