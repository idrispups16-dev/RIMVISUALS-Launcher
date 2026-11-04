package xyz.rimvisuals.core;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import xyz.rimvisuals.RimVisualsClient;
import java.util.*;
/** Uses vanilla dispatch and options; no raw global keyboard hooks or chat interception. */
public final class Keybinds {
 public record Action(String id,String label,KeyBinding key,Runnable run){}
 private static final List<Action> actions=new ArrayList<>();
 private static final Properties pending=new Properties();
 public static List<Action> actions(){return Collections.unmodifiableList(actions);}
 public static void init(){
  actions.clear();
  actions.add(new Action("menu","Open ClickGUI",RimVisualsClient.menuKey,()->RimVisualsClient.openMenu(MinecraftClient.getInstance())));
  actions.add(new Action("monitor","Performance Monitor",RimVisualsClient.monitorKey,()->Modules.PERFORMANCE.monitor.value=!Modules.PERFORMANCE.monitor.value));
  var hud=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.rimvisuals.hud",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,KeyBinding.Category.MISC));
  actions.add(new Action("hud","Edit HUD",hud,()->MinecraftClient.getInstance().setScreen(new xyz.rimvisuals.gui.HudEditorScreen(null))));
  for(Module m:Modules.ALL)if(m.toggleable){
   var key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.rimvisuals.toggle."+m.id,InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,KeyBinding.Category.MISC));
   actions.add(new Action(m.id,"Toggle "+m.name,key,()->m.enabled=!m.enabled));
  }
  var swap=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.rimvisuals.swap",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,KeyBinding.Category.MISC));
  actions.add(new Action("item_swap_action","Item Swap to offhand",swap,()->Modules.ITEM_SWAP.run()));
  helperAction("helper_events","Toggle Events HUD",()->toggleHelperHud(0));
  helperAction("helper_cooldowns","Toggle Cooldowns HUD",()->toggleHelperHud(1));
  helperAction("helper_regions","Toggle Regions HUD",()->toggleHelperHud(2));
  helperAction("helper_notifications","Show/Hide Helper Notifications",()->forHelpers(h->h.notifications.value=!h.notifications.value));
  helperAction("helper_settings","Open HW/FT Settings",()->MinecraftClient.getInstance().setScreen(new xyz.rimvisuals.gui.HelperSettingsScreen(null,ServerHelpers.active()==Modules.FT_HELPER)));
  helperAction("helper_refresh","Manual Refresh Events",HelperRuntime::refresh);
  helperAction("helper_trap_plast","Toggle Trapka/Plast Timer Visibility",()->forHelpers(h->{boolean show=!(h.trapVisible.value||h.plastVisible.value);h.trapVisible.value=show;h.plastVisible.value=show;}));
  for(var h:List.of(Modules.HW_HELPER,Modules.FT_HELPER)){
   String prefix=h.ft?"ft":"hw";
   helperAction(prefix+"_use_trap",h.name+" / Use Trapka",()->HelperItemBindings.use(h,0));
   helperAction(prefix+"_use_plast",h.name+" / Use Plast",()->HelperItemBindings.use(h,1));
   helperAction(prefix+"_use_custom",h.name+" / Use custom item",()->HelperItemBindings.use(h,2));
  }
  var ping=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.rimvisuals.friend_ping",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_N,KeyBinding.Category.MISC));
  actions.add(new Action("friend_ping","Friends / Share marker",ping,SocialClient::ping));
  load(pending);
 }
 private static void helperAction(String id,String label,Runnable run){var key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.rimvisuals."+id,InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,KeyBinding.Category.MISC));actions.add(new Action(id,label,key,run));}
 private static void forHelpers(java.util.function.Consumer<xyz.rimvisuals.modules.ServerHelper> action){var h=ServerHelpers.active();if(h!=null)action.accept(h);else{action.accept(Modules.HW_HELPER);action.accept(Modules.FT_HELPER);}}
 private static void toggleHelperHud(int kind){forHelpers(h->{var hud=kind==0?(h.ft?Modules.FT_EVENTS:Modules.HW_EVENTS):kind==1?(h.ft?Modules.FT_COOLDOWNS:Modules.HW_COOLDOWNS):(h.ft?Modules.FT_REGIONS:Modules.HW_REGIONS);hud.enabled=!hud.enabled;});}
 public static void tick(MinecraftClient mc){for(var a:actions)while(a.key.wasPressed())if(mc.currentScreen==null)a.run.run();}
 public static void snapshot(Properties p){p.putAll(pending);for(var a:actions)p.setProperty("keybind."+a.id,a.key.getBoundKeyTranslationKey());}
 public static void load(Properties p){
  for(String k:p.stringPropertyNames())if(k.startsWith("keybind.")&&p.getProperty(k).length()<80)pending.setProperty(k,p.getProperty(k));
  boolean changed=false;
  for(var a:actions){String value=pending.getProperty("keybind."+a.id);if(value!=null)try{a.key.setBoundKey(InputUtil.fromTranslationKey(value));changed=true;}catch(IllegalArgumentException ignored){}}
  if(changed)KeyBinding.updateKeysByCode();
 }
 public static void bind(Action a,InputUtil.Key key){a.key.setBoundKey(key);pending.setProperty("keybind."+a.id,key.getTranslationKey());KeyBinding.updateKeysByCode();MinecraftClient.getInstance().options.write();Modules.save();}
 public static List<String> conflicts(Action action,InputUtil.Key proposed){
  if(proposed.getCode()==GLFW.GLFW_KEY_UNKNOWN)return List.of();
  var out=new ArrayList<String>();
  if(proposed.getCategory()==InputUtil.Type.KEYSYM&&(proposed.getCode()==GLFW.GLFW_KEY_ESCAPE||proposed.getCode()==GLFW.GLFW_KEY_F3))out.add("Minecraft reserved Escape/debug control");
  for(var key:MinecraftClient.getInstance().options.allKeys)if(key!=action.key&&key.getBoundKeyTranslationKey().equals(proposed.getTranslationKey()))out.add(net.minecraft.text.Text.translatable(key.getId()).getString());
  return out;
 }
}
