package xyz.rimvisuals.gui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.*;
import xyz.rimvisuals.modules.ServerHelper;
import java.util.*;
/** Only HW/FT gameplay settings. Does not replace ClickGUI or the HUD editor. */
public final class HelperSettingsScreen extends Screen {
 private final Screen parent;private boolean ft;private int tab,page;private String status="";
 private static final String[] TABS={"Events","Cooldowns","Notifications","Regions","Keybinds","HUD Settings"};
 public HelperSettingsScreen(Screen parent,boolean ft){super(Text.literal("RIMVISUALS Server Helpers"));this.parent=parent;this.ft=ft;}
 private ServerHelper helper(){return ft?Modules.FT_HELPER:Modules.HW_HELPER;}
 private void button(int x,int y,int w,String text,Runnable action){addDrawableChild(new RimButton(x,y,w,20,Text.literal(text),b->action.run()));}
 private void rebuild(){clearChildren();init();}
 @Override protected void init(){
  ServerHelper h=helper();int half=(width-32)/2;
  button(12,10,half,"HolyWorld"+(ft?"":" selected"),()->{ft=false;page=0;rebuild();});button(20+half,10,half,"FunTime"+(ft?" selected":""),()->{ft=true;page=0;rebuild();});
  int tw=(width-34)/3;for(int i=0;i<TABS.length;i++){final int index=i;button(12+(i%3)*(tw+5),36+(i/3)*24,tw,(tab==i?"> ":"")+TABS[i],()->{tab=index;page=0;rebuild();});}
  int y=90;button(12,y,width-24,h.name+": "+(h.enabled?"ON":"OFF"),()->{h.enabled=!h.enabled;Modules.save();rebuild();});y+=24;
  if(tab==4){button(12,y,width-24,"Open Keybind Manager (helper actions are unbound by default)",()->client.setScreen(new KeybindManagerScreen(this)));y+=26;}
  if(tab==5){button(12,y,width-24,"Open existing HUD Editor",()->client.setScreen(new HudEditorScreen(this)));y+=24;}
  if(tab==2||tab==5){var hud=ft?Modules.FT_NOTICES:Modules.HW_NOTICES;button(12,y,width-24,"Helper notification HUD: "+(hud.enabled?"ON":"OFF"),()->{hud.enabled=!hud.enabled;Modules.save();rebuild();});y+=24;}
  if(tab==5){var huds=ft?java.util.List.of(Modules.FT_EVENTS,Modules.FT_COOLDOWNS,Modules.FT_REGIONS):java.util.List.of(Modules.HW_EVENTS,Modules.HW_COOLDOWNS,Modules.HW_REGIONS);for(var hud:huds){button(12,y,width-24,hud.name+": "+(hud.enabled?"ON":"OFF"),()->{hud.enabled=!hud.enabled;Modules.save();rebuild();});y+=24;}}
  if(tab==0){button(12,y,half,"Refresh events (manual)",()->{HelperRuntime.refresh();status=HelperRuntime.status();});button(20+half,y,half,ft?"Prepare /events in chat":"Manual server connection",()->{if(ft)client.setScreen(new net.minecraft.client.gui.screen.ChatScreen("/events",false));else client.setScreen(new HelperConnectScreen(this,false));});y+=26;}
  List<Setting> fields=fields(h);int count=Math.max(1,(height-y-75)/36);int pages=Math.max(1,(fields.size()+count-1)/count);page=Math.min(page,pages-1);
  for(int i=page*count;i<Math.min(fields.size(),(page+1)*count);i++){Setting s=fields.get(i);int yy=y+(i-page*count)*36;
   if(s instanceof Setting.Bool b)button(12,yy,width-24,s.name+": "+(b.value?"ON":"OFF"),()->{b.value=!b.value;Modules.save();rebuild();});
   else if(s instanceof Setting.Text t){labelField(s.name,t.value,yy,192,t::load);}
   else if(s instanceof Setting.Color col){labelField(s.name+" HEX",col.hex(),yy,6,col::load);}
   else if(s instanceof Setting.Num n){button(12,yy,width-104,s.name+": "+n.display(),()->{});button(width-86,yy,34,"-",()->{n.set(n.value-n.step);Modules.save();rebuild();});button(width-46,yy,34,"+",()->{n.set(n.value+n.step);Modules.save();rebuild();});}
  }
  button(12,height-52,65,"Previous",()->{page=Math.max(0,page-1);rebuild();});button(83,height-52,65,"Next",()->{page=Math.min(pages-1,page+1);rebuild();});
  button(width-166,height-52,74,"Save",()->{Modules.save();status="Settings saved to CFG";});button(width-86,height-52,74,"Back",this::close);
 }
 private void labelField(String label,String value,int y,int max,java.util.function.Consumer<String> change){var field=addDrawableChild(new TextFieldWidget(textRenderer,12,y+12,width-24,20,Text.literal(label)));field.setMaxLength(max);field.setText(value);field.setPlaceholder(Text.literal(label));field.setChangedListener(change);fieldLabels.add(new Label(label,y));}
 private record Label(String text,int y){}private final List<Label> fieldLabels=new ArrayList<>();
 @Override public void clearChildren(){super.clearChildren();fieldLabels.clear();}
 private List<Setting> fields(ServerHelper h){return switch(tab){
  case 0->h.ft?List.of(h.events,h.eventNames,h.eventMenu,h.interests,h.eventPrefix,h.timerPrefix,h.chat,h.action,h.scoreboard,h.aliases):List.of(h.events,h.publicApi,h.interval,h.apiServer,h.interests,h.eventNames,h.eventMenu,h.eventPrefix,h.timerPrefix,h.chat,h.action,h.scoreboard,h.aliases);
  case 1->List.of(h.cooldowns,h.trapName,h.plastName,h.trapPrefix,h.plastPrefix,h.estimates,h.trapSeconds,h.plastSeconds,h.trapVisible,h.plastVisible,h.trapColor,h.plastColor);
  case 2->List.of(h.notifications,h.sounds,h.soundVolume,h.readyNotify,h.health,h.hp,h.armor,h.durability);
  case 3->List.of(h.regionPrefix,h.scoreboard,h.action,h.chat);
  case 4->List.of();
  default->List.of();};}
 @Override public void render(DrawContext c,int mx,int my,float delta){c.fill(0,0,width,height,0xf00b0912);super.render(c,mx,my,delta);for(var label:fieldLabels)c.drawText(textRenderer,label.text,12,label.y,0xffbba9ff,false);String footer=status.isEmpty()?HelperRuntime.status():status;c.drawText(textRenderer,textRenderer.trimToWidth(footer,width-24),12,height-23,0xffaaa6bb,false);}
 @Override public boolean shouldPause(){return false;}
 @Override public void close(){Modules.save();client.setScreen(parent);}
 public void tabForTest(int tab){this.tab=tab;}
}
