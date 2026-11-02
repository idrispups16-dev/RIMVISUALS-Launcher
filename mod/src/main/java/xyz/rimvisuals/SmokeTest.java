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
   var chat=new net.minecraft.client.gui.screen.ChatScreen("RIM draft \u03a9",false);
   client.setScreen(chat);
   RimVisualsClient.openHudFromChat(client,chat);
   if(!(client.currentScreen instanceof xyz.rimvisuals.gui.HudEditorScreen editor))throw new IllegalStateException("HUD editor not opened");
   if(net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(editor).size()!=12)throw new IllegalStateException("HUD toolbar missing");
   editor.close();
   var field=((xyz.rimvisuals.mixin.ChatScreenAccessor)chat).rimChatField();
   if(client.currentScreen!=chat||!field.getText().equals("RIM draft \u03a9")||!field.isFocused())throw new IllegalStateException("Chat draft/focus lost");
   LoggerFactory.getLogger("rimvisuals").info("RIM_HUD_CHAT_OK: editor toolbar, chat draft and focus return; not a world interaction test");var saved=xyz.rimvisuals.core.Modules.snapshot();try{
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
