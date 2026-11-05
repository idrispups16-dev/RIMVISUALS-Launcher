package xyz.rimvisuals.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import xyz.rimvisuals.core.HwIntegrations;
@Pseudo
@Mixin(targets="me.zyouime.zakohealthindicator.render.HealthHudRender",remap=false)
public class ZakoHealthControlMixin {
 @ModifyExpressionValue(method="render",at=@At(value="FIELD",target="Lme/zyouime/zakohealthindicator/config/ModConfig;enabled:Z",remap=false),remap=false)
 private boolean rimEnabled(boolean original){return HwIntegrations.health();}
}
