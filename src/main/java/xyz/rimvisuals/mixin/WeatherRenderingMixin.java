package xyz.rimvisuals.mixin;

import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.WorldColors;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {
    @Redirect(method = "renderPieces", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ColorHelper;getWhite(F)I"))
    private int rimWeather(float alpha) {
        var m = Modules.WEATHER_COLOR;
        int white = ColorHelper.getWhite(m.isActive() ? alpha * (float) m.opacity.value : alpha);
        return m.isActive() ? WorldColors.tint(white, m.color.rgb, m.strength.value) : white;
    }
}
