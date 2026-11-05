package xyz.rimvisuals.mixin;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.rimvisuals.core.Keybinds;
/** Remove restricted RIM entries from vanilla Controls as well as the custom key manager. */
@Mixin(ControlsListWidget.class)
public abstract class HolyControlsListMixin {
 @Redirect(method="<init>",at=@At(value="FIELD",target="Lnet/minecraft/client/option/GameOptions;allKeys:[Lnet/minecraft/client/option/KeyBinding;"))
 private KeyBinding[] rimAllowedKeys(GameOptions options){return java.util.Arrays.stream(options.allKeys).filter(Keybinds::visibleKey).toArray(KeyBinding[]::new);}
}
