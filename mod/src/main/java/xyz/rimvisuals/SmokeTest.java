package xyz.rimvisuals;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.slf4j.LoggerFactory;
import xyz.rimvisuals.gui.ClickGui;
/** Opt-in development startup/GUI smoke test. Never active in normal installations. */
public final class SmokeTest {
 public static int frames;private static int ticks;private static boolean audited;private static boolean shotRequested,shotDone;
 public static void tick(MinecraftClient client){if(!Boolean.getBoolean("rimvisuals.smoke"))return;
  if(!audited){audited=true;MixinEnvironment.getCurrentEnvironment().audit();}
  ++ticks;
  if(ticks==41){IntegrationChecks.run();
   var hudParent=new net.minecraft.client.gui.screen.TitleScreen();
   var editor=new xyz.rimvisuals.gui.HudEditorScreen(hudParent);
   client.setScreen(editor);
   if(net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(editor).size()!=12)throw new IllegalStateException("HUD toolbar missing");
   float previousAlpha=xyz.rimvisuals.core.Gfx.hudAlpha;
   try{xyz.rimvisuals.core.Gfx.hudAlpha=.35f;
    var itemState=new net.minecraft.client.gui.render.state.ItemGuiElementRenderState("rim-alpha-test",new org.joml.Matrix3x2f(),new net.minecraft.client.render.item.KeyedItemRenderState(),0,0,null);
    if(Math.abs(((xyz.rimvisuals.core.ItemOpacity)(Object)itemState).rimOpacity()-.35f)>.00001)throw new IllegalStateException("Deferred item opacity was lost");
   }finally{xyz.rimvisuals.core.Gfx.hudAlpha=previousAlpha;}
   editor.close();
   if(client.currentScreen!=hudParent)throw new IllegalStateException("HUD editor did not return to parent");
   LoggerFactory.getLogger("rimvisuals").info("RIM_HUD_EDITOR_OK: toolbar initialization and close; chat input requires an in-world player");
   var saved=xyz.rimvisuals.core.Modules.snapshot();try{
   var modules=xyz.rimvisuals.core.Modules.SWING;modules.enabled=true;xyz.rimvisuals.core.Modules.PERFORMANCE.reducedMotion.value=false;
   for(int style=1;style<=9;style++){modules.style.index=style;for(boolean main:new boolean[]{true,false}){var matrix=new net.minecraft.client.util.math.MatrixStack();modules.apply(matrix,main?1:-1,.4f,main);if(matrix.peek().getPositionMatrix().equals(new org.joml.Matrix4f(),.000001f))throw new IllegalStateException("Swing matrix unchanged: "+style);}}
   var view=xyz.rimvisuals.core.Modules.VIEWMODEL;view.enabled=true;view.smooth.value=false;view.main.x.value=.2;view.off.x.value=-.2;for(boolean main:new boolean[]{true,false}){var matrix=new net.minecraft.client.util.math.MatrixStack();view.apply(matrix,main,net.minecraft.item.ItemStack.EMPTY);if(matrix.peek().getPositionMatrix().equals(new org.joml.Matrix4f(),.000001f))throw new IllegalStateException("Viewmodel unchanged");}
   LoggerFactory.getLogger("rimvisuals").info("RIM_TRANSFORM_TEST_OK: nine styles, both hands, viewmodel matrices changed; not a world visual test");
  }finally{xyz.rimvisuals.core.Modules.apply(saved);}}
  if(ticks>40 && client.getOverlay()==null && !(client.currentScreen instanceof ClickGui)){var gui=new ClickGui();gui.showCategoryForTest(4);client.setScreen(gui);LoggerFactory.getLogger("rimvisuals").info("SMOKE: opening GUI after loading overlay");}
  if(ticks>100&&frames>5&&!shotRequested){shotRequested=true;net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(client.runDirectory,"rimvisuals-viewmodel-gui.png",client.getFramebuffer(),1,message->{shotDone=true;});}
  if(ticks>100&&frames>5&&shotDone){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI rendering; no world test");client.scheduleStop();}
 }
}
