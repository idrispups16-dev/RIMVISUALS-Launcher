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
 private void rimContext(AbstractClientPlayerEntity player,float tick,float pitch,Hand hand,float swing,ItemStack item,float equip,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){rimMain=hand==Hand.MAIN_HAND;rimProgress=swing;rimItem=item;}
 @org.spongepowered.asm.mixin.Unique private float rimProgress;
 @org.spongepowered.asm.mixin.Unique private ItemStack rimItem=ItemStack.EMPTY;
 @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("HEAD"))
 private void rimSubmit(net.minecraft.entity.LivingEntity entity,ItemStack item,net.minecraft.item.ItemDisplayContext mode,MatrixStack m,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){
  if(mode!=net.minecraft.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && mode!=net.minecraft.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND)return;
  m.push();Modules.VIEWMODEL.apply(m,rimMain,item);Modules.SWING.apply(m,mode==net.minecraft.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND?1:-1,rimProgress,rimMain);
 }
 @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("RETURN"))
 private void rimSubmitEnd(net.minecraft.entity.LivingEntity entity,ItemStack item,net.minecraft.item.ItemDisplayContext mode,MatrixStack m,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){if(mode==net.minecraft.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || mode==net.minecraft.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND)m.pop();}
 @Inject(method="renderArmHoldingItem",at=@At("HEAD"))
 private void rimArm(MatrixStack m,OrderedRenderCommandQueue queue,int light,float equip,float swing,Arm arm,CallbackInfo ci){m.push();Modules.VIEWMODEL.apply(m,rimMain,ItemStack.EMPTY);Modules.SWING.apply(m,arm==Arm.RIGHT?1:-1,swing,rimMain);}
 @Inject(method="renderArmHoldingItem",at=@At("RETURN"))
 private void rimArmEnd(MatrixStack m,OrderedRenderCommandQueue queue,int light,float equip,float swing,Arm arm,CallbackInfo ci){m.pop();}
 @ModifyArgs(method="renderFirstPersonItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
 private void rimLighting(Args args){args.set(5,Modules.HAND_LIGHTING.light(args.get(5),rimMain));}
}
