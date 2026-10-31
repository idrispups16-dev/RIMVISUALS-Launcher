package xyz.rimvisuals.mixin;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.rimvisuals.core.Modules;
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
 @org.spongepowered.asm.mixin.Unique private boolean rimMain=true;
 @Inject(method="renderFirstPersonItem",at=@At("HEAD"))
 private void rimContext(AbstractClientPlayerEntity player,float tick,float pitch,Hand hand,float swing,ItemStack item,float equip,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){rimMain=hand==Hand.MAIN_HAND;}
 @Inject(method="renderFirstPersonItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/util/math/MatrixStack;push()V",shift=At.Shift.AFTER,ordinal=0))
 private void rimPose(AbstractClientPlayerEntity player,float tick,float pitch,Hand hand,float swing,ItemStack item,float equip,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){Modules.VIEWMODEL.apply(matrices,hand==Hand.MAIN_HAND,item);}
 @Inject(method="applySwingOffset",at=@At("HEAD"),cancellable=true)
 private void rimSwing(MatrixStack matrices,Arm arm,float progress,CallbackInfo ci){if(Modules.SWING.apply(matrices,arm==Arm.RIGHT?1:-1,progress,rimMain))ci.cancel();}
 @ModifyArgs(method="renderFirstPersonItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
 private void rimLighting(Args args){args.set(5,Modules.HAND_LIGHTING.light(args.get(5),rimMain));}
}
