package xyz.rimvisuals.mixin;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.rimvisuals.core.Gfx;
import xyz.rimvisuals.core.ItemOpacity;
@Mixin(ItemGuiElementRenderState.class)
public class ItemGuiOpacityMixin implements ItemOpacity {
 @Unique private float rimAlpha=1;
 @Inject(method="<init>",at=@At("TAIL"))
 private void rimCapture(CallbackInfo ci){rimAlpha=Gfx.hudAlpha;}
 @Override public float rimOpacity(){return rimAlpha;}
}
