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
   RimVisualsClient.openMenu(client);
   if(!(client.currentScreen instanceof ClickGui))throw new IllegalStateException("Menu entry did not open ClickGUI");
   if(!RimVisualsClient.menuKey.getDefaultKey().getTranslationKey().equals("key.keyboard.right.shift"))throw new IllegalStateException("Unexpected default menu bind");
   var bindSnapshot=xyz.rimvisuals.core.Modules.snapshot();
   try {
    var action=xyz.rimvisuals.core.Keybinds.actions().getFirst();
    xyz.rimvisuals.core.Keybinds.bind(action,net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_F8));
    if(!xyz.rimvisuals.core.Modules.snapshot().getProperty("keybind.menu").equals("key.keyboard.f8"))throw new IllegalStateException("Keyboard bind not persisted");
    xyz.rimvisuals.core.Keybinds.bind(action,net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(3));
    var mouse=xyz.rimvisuals.core.Modules.snapshot();
    xyz.rimvisuals.core.Modules.apply(bindSnapshot);xyz.rimvisuals.core.Modules.apply(mouse);
    if(!action.key().getBoundKeyTranslationKey().equals("key.mouse.4"))throw new IllegalStateException("Mouse bind not restored");
    if(!client.options.sneakKey.getBoundKeyTranslationKey().equals("key.keyboard.left.shift")||!client.options.chatKey.getBoundKeyTranslationKey().equals("key.keyboard.t"))throw new IllegalStateException("Vanilla control defaults changed");
    LoggerFactory.getLogger("rimvisuals").info("RIM_KEYBINDS_OK: keyboard/mouse round-trip; vanilla Left Shift/T defaults unchanged");
   }finally{xyz.rimvisuals.core.Modules.apply(bindSnapshot);client.options.write();}
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
  if(ticks>40 && stage==0 && client.getOverlay()==null && !(client.currentScreen instanceof ClickGui)){var gui=new ClickGui();gui.showCategoryForTest(0);client.setScreen(gui);LoggerFactory.getLogger("rimvisuals").info("SMOKE: opening GUI after loading overlay");}
  if(ticks>nextShotAt&&frames>5&&!shotRequested){shotRequested=true;String[] shots={"rimvisuals-ui.png","rimvisuals-settings.png","rimvisuals-keybinds.png","rimvisuals-friends.png","rimvisuals-markers.png"};net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(client.runDirectory,shots[stage],client.getFramebuffer(),1,message->{shotDone=true;});}
  if(shotDone){
   if(stage==4){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI, keyboard/mouse manager and local screens; no world test");client.scheduleStop();return;}
   stage++;shotDone=shotRequested=false;nextShotAt=ticks+12;
   if(stage==1){var gui=new ClickGui();gui.showCategoryForTest(4);client.setScreen(gui);}
   else if(stage==2)client.setScreen(new xyz.rimvisuals.gui.KeybindManagerScreen(new ClickGui()));
   else client.setScreen(new xyz.rimvisuals.gui.LocalDataScreen(new ClickGui(),stage==4));
  }
 }
 private static int stage,nextShotAt=100;
}
