package xyz.rimvisuals.mixin;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.*;
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
 @Unique private boolean rimPushed;
 @Inject(method="updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",at=@At("TAIL"))
 private void rimState(ItemEntity e,ItemEntityRenderState state,float tick,CallbackInfo ci){if(Modules.ITEM_PHYSICS.isActive())((ItemPose)(Object)state).rimPose(e.isOnGround(),e.age+tick);}
 @Inject(method="render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("HEAD"))
 private void rimStart(ItemEntityRenderState state,MatrixStack m,OrderedRenderCommandQueue q,CameraRenderState camera,CallbackInfo ci){rimPushed=Modules.ITEM_PHYSICS.isActive();if(!rimPushed)return;m.push();var pose=(ItemPose)(Object)state;var settings=Modules.ITEM_PHYSICS;float s=(float)settings.scale.value;m.scale(s,s,s);if(pose.rimGround()){m.translate(0,settings.lift.value,0);m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees((float)settings.groundTilt.value));}else if(!Modules.PERFORMANCE.reducedMotion.value)m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)(pose.rimAge()*settings.spin.value)));}
 @Inject(method="render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("RETURN"))
 private void rimEnd(ItemEntityRenderState state,MatrixStack m,OrderedRenderCommandQueue q,CameraRenderState camera,CallbackInfo ci){if(rimPushed)m.pop();}
}
