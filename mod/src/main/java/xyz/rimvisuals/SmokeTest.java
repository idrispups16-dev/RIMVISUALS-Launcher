package xyz.rimvisuals;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.slf4j.LoggerFactory;
import xyz.rimvisuals.gui.ClickGui;
/** Opt-in development startup/GUI smoke test. Never active in normal installations. */
public final class SmokeTest {
 public static int frames;private static int ticks;private static boolean audited;
 public static void tick(MinecraftClient client){if(!Boolean.getBoolean("rimvisuals.smoke"))return;
  if(!audited){audited=true;MixinEnvironment.getCurrentEnvironment().audit();}
  ++ticks;
  if(ticks==41){var saved=xyz.rimvisuals.core.Modules.snapshot();try{
   var modules=xyz.rimvisuals.core.Modules.SWING;modules.enabled=true;xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value=false;
   for(int style=1;style<=9;style++){modules.style.index=style;for(boolean main:new boolean[]{true,false}){var matrix=new net.minecraft.client.util.math.MatrixStack();modules.apply(matrix,main?1:-1,.4f,main);if(matrix.peek().getPositionMatrix().equals(new org.joml.Matrix4f(),.000001f))throw new IllegalStateException("Swing matrix unchanged: "+style);}}
   var view=xyz.rimvisuals.core.Modules.VIEWMODEL;view.enabled=true;view.smooth.value=false;view.main.x.value=.2;view.off.x.value=-.2;for(boolean main:new boolean[]{true,false}){var matrix=new net.minecraft.client.util.math.MatrixStack();view.apply(matrix,main,net.minecraft.item.ItemStack.EMPTY);if(matrix.peek().getPositionMatrix().equals(new org.joml.Matrix4f(),.000001f))throw new IllegalStateException("Viewmodel unchanged");}
   LoggerFactory.getLogger("rimvisuals").info("RIM_TRANSFORM_TEST_OK: nine styles, both hands, viewmodel matrices changed; not a world visual test");
  }finally{xyz.rimvisuals.core.Modules.apply(saved);}}
  if(ticks>40 && client.getOverlay()==null && !(client.currentScreen instanceof ClickGui)){client.setScreen(new ClickGui());LoggerFactory.getLogger("rimvisuals").info("SMOKE: opening GUI after loading overlay");}
  if(ticks>100&&frames>5){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI rendering; no world test");client.scheduleStop();}
 }
}
