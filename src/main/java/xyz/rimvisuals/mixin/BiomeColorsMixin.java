package xyz.rimvisuals.mixin;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.WorldColors;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getGrassColor(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void rimGrass(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        var m = Modules.BIOME_COLORS;
        if (m.isActive() && m.grass.value) cir.setReturnValue(WorldColors.tint(cir.getReturnValueI(), m.grassColor.rgb, m.strength.value));
    }

    @Inject(method = "getFoliageColor(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void rimFoliage(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        var m = Modules.BIOME_COLORS;
        if (m.isActive() && m.foliage.value) cir.setReturnValue(WorldColors.tint(cir.getReturnValueI(), m.foliageColor.rgb, m.strength.value));
    }

    @Inject(method = "getWaterColor(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void rimWater(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        var m = Modules.WATER_COLOR;
        if (m.isActive()) cir.setReturnValue(WorldColors.tint(cir.getReturnValueI(), m.color.rgb, m.strength.value));
    }
}
