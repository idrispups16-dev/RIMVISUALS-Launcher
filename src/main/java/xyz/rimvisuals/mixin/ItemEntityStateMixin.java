package xyz.rimvisuals.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
@Mixin(ItemEntityRenderState.class)
public class ItemEntityStateMixin implements xyz.rimvisuals.core.ItemPose {
 @Unique private boolean rimGround;@Unique private float rimAge;
 public boolean rimGround(){return rimGround;}public float rimAge(){return rimAge;}public void rimPose(boolean ground,float age){rimGround=ground;rimAge=age;}
}
