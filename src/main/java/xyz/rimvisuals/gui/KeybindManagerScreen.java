package xyz.rimvisuals.gui;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import xyz.rimvisuals.core.*;
import java.util.*;
/** Original keyboard/mouse map. All actions use Minecraft's normal key binding registry. */
public final class KeybindManagerScreen extends Screen {
 private record Area(int x,int y,int w,int h,Runnable action){boolean hit(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
 private final Screen parent;
 private final List<Area> areas=new ArrayList<>();
 private TextFieldWidget search;
 private Keybinds.Action selected;
 private boolean capture;
 private List<String> conflictLabels=List.of();
 private List<Keybinds.Action> visibleActions=List.of();
 private int scroll; private long policyRevision=-1;
 private float scale=1;
 private String initialSearch="";
 public KeybindManagerScreen(Screen parent,String filter){this(parent);initialSearch=filter;}
 private String status="Select an action, then click a key or Capture";
 public KeybindManagerScreen(Screen parent){super(Text.literal("RIMVISUALS Keybind Manager"));this.parent=parent;}
 @Override protected void init(){search=addDrawableChild(new TextFieldWidget(textRenderer,8,8,Math.max(80,width-100),20,Text.literal("Search actions")));search.setPlaceholder(Text.literal("Search actions"));search.setText(initialSearch);search.setChangedListener(q->filter());filter();if(selected==null&&!Keybinds.actions().isEmpty())selected=Keybinds.actions().getFirst();if(selected!=null)conflictLabels=Keybinds.conflicts(selected,InputUtil.fromTranslationKey(selected.key().getBoundKeyTranslationKey()));addDrawableChild(new RimButton(width-84,8,76,20,Text.literal("Back"),b->close()));}
 @Override public boolean shouldPause(){return false;}
 private void box(DrawContext c,int x,int y,int w,int h,String label,boolean active,Runnable action){
  Gfx.roundBorder(c,x,y,w,h,5,active?0xff505050:0xff202020,active?0xffb0b0b0:0xff454545);
  c.drawCenteredTextWithShadow(textRenderer,Text.literal(label),x+w/2,y+(h-8)/2,0xffeeeeee);areas.add(new Area(x,y,w,h,action));
 }
 @Override public void render(DrawContext c,int mx,int my,float delta){
  if(policyRevision!=HolyFeatureControl.revision()){policyRevision=HolyFeatureControl.revision();filter();}
  c.fill(0,0,width,height,0xe8101010);super.render(c,mx,my,delta);
  scale=Math.min(width/900f,(height-38)/480f);var mat=c.getMatrices();mat.pushMatrix();mat.translate(0,34);mat.scale(scale,scale);areas.clear();
  c.drawText(textRenderer,"RIMVISUALS / INPUT",12,4,0xffcccccc,false);
  var visible=visibleActions;
  int shown=12;scroll=Math.max(0,Math.min(scroll,Math.max(0,visible.size()-shown)));
  for(int i=scroll;i<Math.min(visible.size(),scroll+shown);i++){
   var a=visible.get(i);int y=25+(i-scroll)*29;
   String label=textRenderer.trimToWidth(a.label(),166);
   box(c,10,y,215,26,label,selected==a,()->{selected=a;capture=false;conflictLabels=Keybinds.conflicts(a,InputUtil.fromTranslationKey(a.key().getBoundKeyTranslationKey()));});
   c.drawText(textRenderer,textRenderer.trimToWidth(a.key().getBoundKeyLocalizedText().getString(),46),171,y+17,0xffbbbbbb,false);
  }
  keyboard(c);
  if(selected!=null){
   box(c,245,305,130,24,capture?"Press a key...":"Capture",capture,()->capture=true);
   box(c,385,305,105,24,"Unbind",false,()->assign(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_UNKNOWN)));
   box(c,500,305,120,24,"Default",false,()->assign(selected.key().getDefaultKey()));
   c.drawText(textRenderer,textRenderer.trimToWidth(selected.label(),620),245,340,0xffdddddd,false);
   var conflict=conflictLabels;
   if(!conflict.isEmpty())c.drawText(textRenderer,textRenderer.trimToWidth("Conflict: "+String.join(", ",conflict),630),245,358,0xffffbb77,false);
  }
  c.drawText(textRenderer,textRenderer.trimToWidth(status,865),12,405,0xffbbbbbb,false);
  c.drawText(textRenderer,"LShift / T keep vanilla defaults. Mouse 1-8 supported. Esc returns.",12,424,0xff888888,false);
  mat.popMatrix();
 }
 private void filter(){String q=search.getText().toLowerCase(Locale.ROOT);visibleActions=Keybinds.actions().stream().filter(a->a.label().toLowerCase(Locale.ROOT).contains(q)).toList();if(!visibleActions.contains(selected))selected=visibleActions.isEmpty()?null:visibleActions.getFirst();scroll=0;}
 private void keyboard(DrawContext c){
  int[][] codes={{256,290,291,292,293,294,295,296,297,298,299,300,301},{96,49,50,51,52,53,54,55,56,57,48,45,61,259},{258,81,87,69,82,84,89,85,73,79,80,91,93,92},{280,65,83,68,70,71,72,74,75,76,59,39,257},{340,90,88,67,86,66,78,77,44,46,47,344},{341,343,342,32,346,347,345}};
  String[][] labels={{"Esc","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"},{"`","1","2","3","4","5","6","7","8","9","0","-","=","Bk"},{"Tab","Q","W","E","R","T","Y","U","I","O","P","[","]","\\"},{"Caps","A","S","D","F","G","H","J","K","L",";","'","Enter"},{"LSh","Z","X","C","V","B","N","M",",",".","/","RSh"},{"Ctrl","Win","Alt","Space","Alt","Win","Ctrl"}};
  for(int row=0;row<codes.length;row++){int x=245;for(int col=0;col<codes[row].length;col++){
   int code=codes[row][col],w=code==32?160:code==340||code==344?52:35;
   var key=InputUtil.Type.KEYSYM.createFromCode(code);
   boolean active=selected!=null&&selected.key().getBoundKeyTranslationKey().equals(key.getTranslationKey());
   box(c,x,30+row*37,w,31,labels[row][col],active,()->assign(key));x+=w+3;
  }}
  int[] extras={265,263,264,262};String[] names={"Up","Left","Down","Right"};
  for(int i=0;i<extras.length;i++){var k=InputUtil.Type.KEYSYM.createFromCode(extras[i]);box(c,245+i*57,265,53,25,names[i],false,()->assign(k));}
  for(int i=0;i<8;i++){final int button=i;var k=InputUtil.Type.MOUSE.createFromCode(i);box(c,815+(i%2)*40,30+(i/2)*44,36,38,"M"+(i+1),selected!=null&&selected.key().getBoundKeyTranslationKey().equals(k.getTranslationKey()),()->assign(InputUtil.Type.MOUSE.createFromCode(button)));}
 }
 private void assign(InputUtil.Key key){
  if(selected==null||!HolyFeatureControl.actionAllowed(selected.id())){status="Select an action first";return;}
  var a=selected;capture=false;var conflicts=Keybinds.conflicts(a,key);
  Runnable apply=()->{Keybinds.bind(a,key);conflictLabels=Keybinds.conflicts(a,key);status="Saved: "+a.label()+" = "+key.getLocalizedText().getString();};
  if(conflicts.isEmpty())apply.run();
  else client.setScreen(new ConfirmScreen(yes->{client.setScreen(this);if(yes)apply.run();},Text.literal("Key conflict"),Text.literal("Already used by "+String.join(", ",conflicts)+". Conflicting actions may not both trigger. Bind anyway?")));
 }
 @Override public boolean mouseClicked(Click click,boolean doubled){
  if(capture){assign(InputUtil.Type.MOUSE.createFromCode(click.button()));return true;}
  if(click.y()<34)return super.mouseClicked(click,doubled);
  if(click.button()==0)for(var a:areas)if(a.hit(click.x()/scale,(click.y()-34)/scale)){assignFocus();a.action.run();return true;}
  return super.mouseClicked(click,doubled);
 }
 private void assignFocus(){setFocused(null);search.setFocused(false);}
 @Override public boolean keyPressed(KeyInput input){if(input.key()==GLFW.GLFW_KEY_ESCAPE){close();return true;}if(capture){assign(InputUtil.Type.KEYSYM.createFromCode(input.key()));return true;}return super.keyPressed(input);}
 @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll-=Math.round(v*2);return true;}
 @Override public void close(){capture=false;client.setScreen(parent);}
 @Override public void removed(){Modules.save();}
}
