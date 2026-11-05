package xyz.rimvisuals;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.slf4j.LoggerFactory;
import xyz.rimvisuals.gui.ClickGui;
/** Opt-in development startup/GUI smoke test. Never active in normal installations. */
public final class SmokeTest {
 private static net.minecraft.client.gui.tooltip.TooltipComponent shulkerFixture;
 public static int frames;private static int ticks;private static boolean audited;private static boolean shotRequested,shotDone;
 public static void tick(MinecraftClient client){if(!Boolean.getBoolean("rimvisuals.smoke"))return;
  if(!audited){audited=true;MixinEnvironment.getCurrentEnvironment().audit();}
  ++ticks;
  if(ticks==41){IntegrationChecks.run();PvpRenderChecks.run();HelperChecks.run();HwIntegrationChecks.run();
   RimVisualsClient.openMenu(client);
   if(!(client.currentScreen instanceof ClickGui))throw new IllegalStateException("Menu entry did not open ClickGUI");
   if(!RimVisualsClient.menuKey.getDefaultKey().getTranslationKey().equals("key.keyboard.right.shift"))throw new IllegalStateException("Unexpected default menu bind");
   var pingAction=xyz.rimvisuals.core.Keybinds.actions().stream().filter(a->a.id().equals("friend_ping")).findFirst().orElseThrow();
   if(!pingAction.key().getDefaultKey().getTranslationKey().equals("key.keyboard.n"))throw new IllegalStateException("N marker default missing");
   if(xyz.rimvisuals.core.Keybinds.actions().stream().anyMatch(a->a.id().matches("(hw|ft)_use_.*")||a.id().equals("item_swap_action")))throw new IllegalStateException("Retired item-use bind was registered");
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
   var previewSaved=xyz.rimvisuals.core.Modules.snapshot();
   try{xyz.rimvisuals.core.Modules.SHULKER_PREVIEW.enabled=true;
    var box=new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHULKER_BOX);
    box.set(net.minecraft.component.DataComponentTypes.CONTAINER,net.minecraft.component.type.ContainerComponent.fromStacks(java.util.List.of(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND,3),net.minecraft.item.ItemStack.EMPTY,new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_INGOT,4))));
    var tooltip=box.getTooltipData().orElseThrow();
    if(!(tooltip instanceof xyz.rimvisuals.modules.ShulkerPreview.Data data)||data.stacks().size()!=3||!data.stacks().get(1).isEmpty()||data.stacks().get(2).getCount()!=4)throw new IllegalStateException("Shulker slots changed");
    shulkerFixture=net.minecraft.client.gui.tooltip.TooltipComponent.of(tooltip);
    if(!(shulkerFixture instanceof xyz.rimvisuals.gui.ShulkerTooltip))throw new IllegalStateException("Shulker tooltip renderer missing");
    org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_SHULKER_OK: real container component slots, tooltip mixin and callback; no server inventory request");
   }finally{xyz.rimvisuals.core.Modules.apply(previewSaved);}
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
  if(ticks>nextShotAt&&frames>5&&!shotRequested){shotRequested=true;String[] shots={"rimvisuals-ui.png","rimvisuals-settings.png","rimvisuals-keybinds.png","rimvisuals-friends.png","rimvisuals-markers.png","rimvisuals-shulker-preview.png","rimvisuals-helper-settings.png","rimvisuals-helper-connect.png","rimvisuals-helper-events.png","rimvisuals-helper-cooldowns.png","rimvisuals-account.png"};net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(client.runDirectory,shots[stage],client.getFramebuffer(),1,message->{shotDone=true;});}
  if(shotDone){
   if(stage==10){LoggerFactory.getLogger("rimvisuals").info("RIMVISUALS_SMOKE_OK: startup, mixin audit, ClickGUI, keyboard/mouse manager and local screens; no world test");client.scheduleStop();return;}
   stage++;shotDone=shotRequested=false;nextShotAt=ticks+12;
   if(stage==1){var gui=new ClickGui();gui.showCategoryForTest(4);client.setScreen(gui);}
   else if(stage==2)client.setScreen(new xyz.rimvisuals.gui.KeybindManagerScreen(new ClickGui()));
   else if(stage==3)client.setScreen(new xyz.rimvisuals.gui.FriendsScreen(new ClickGui()));
   else if(stage==4)client.setScreen(new xyz.rimvisuals.gui.LocalDataScreen(new ClickGui(),true));
   else if(stage==5)client.setScreen(new PreviewScreen());
   else if(stage==6){var gui=new ClickGui();gui.showHelperForTest();client.setScreen(gui);}
   else if(stage==7)client.setScreen(new xyz.rimvisuals.gui.HelperConnectScreen(new ClickGui(),false));
   else if(stage==10)client.setScreen(new xyz.rimvisuals.gui.AccountScreen(null));
   else {var settings=new xyz.rimvisuals.gui.HelperSettingsScreen(new ClickGui(),false);settings.tabForTest(stage==8?0:1);client.setScreen(settings);}
  }
 }
 private static int stage,nextShotAt=100;
 private static final class PreviewScreen extends net.minecraft.client.gui.screen.Screen {
  PreviewScreen(){super(net.minecraft.text.Text.literal("RIM Shulker Preview fixture"));}
  @Override public void render(net.minecraft.client.gui.DrawContext c,int x,int y,float delta){
   c.fill(0,0,width,height,0xff0c0915);
   c.drawCenteredTextWithShadow(textRenderer,net.minecraft.text.Text.literal("Shulker Preview: 3 diamonds / empty / 4 gold"),width/2,height/2-48,0xffc4b5fd);
   shulkerFixture.drawItems(textRenderer,width/2-85,height/2-25,170,62,c);
  }
 }

}

