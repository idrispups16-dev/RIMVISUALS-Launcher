package xyz.rimvisuals.mixin;

import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.WorldColors;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin {
    /** Vanilla draws lava with the constant 0xFFFFFF (no tint); this is the only such constant in render(). */
    @ModifyConstant(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)V",
            constant = @Constant(intValue = 16777215))
    private int rimLava(int white) {
        var m = Modules.LAVA_COLOR;
        return m.enabled ? WorldColors.tint(white, m.color.rgb, m.strength.value) : white;
    }
}
