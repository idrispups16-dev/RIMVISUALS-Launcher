package xyz.rimvisuals.gui;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.*;
import java.util.*;
public final class LocalDataScreen extends Screen {
 private final Screen parent;private final boolean markers;
 private TextFieldWidget query,name;
 private String status="Local only — stored on this device";
 private int scroll;
 private record Row(String id,String label){}
 private List<Row> rows=List.of(),filteredRows=List.of();
 public LocalDataScreen(Screen parent,boolean markers){super(Text.literal(markers?"RIMVISUALS / Local markers":"RIMVISUALS / Local friends"));this.parent=parent;this.markers=markers;}
 @Override protected void init(){
  query=addDrawableChild(new TextFieldWidget(textRenderer,12,34,Math.max(60,width/3-18),20,Text.literal("Search")));query.setPlaceholder(Text.literal("Search"));query.setChangedListener(q->filter());
  name=addDrawableChild(new TextFieldWidget(textRenderer,width/3,34,Math.max(50,width*2/3-108),20,Text.literal("Name")));name.setMaxLength(markers?40:16);name.setPlaceholder(Text.literal(markers?"Marker name":"Minecraft name"));
  addDrawableChild(ButtonWidget.builder(Text.literal("Add"),b->add()).dimensions(width-96,34,84,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Back"),b->close()).dimensions(width-88,8,76,20).build());refresh();
 }
 private void refresh(){if(LocalData.store()==null){status=LocalData.error;return;}var list=new ArrayList<Row>();
  if(markers)for(var m:LocalData.store().marks())list.add(new Row(m.id(),m.name()+" | "+Math.round(m.x())+" "+Math.round(m.y())+" "+Math.round(m.z())+" | "+m.dimension()));
  else for(String n:LocalData.store().friends())list.add(new Row(n,n));rows=List.copyOf(list);filter();LocalData.refresh();
 }
 private void filter(){String q=query.getText().toLowerCase(Locale.ROOT);filteredRows=rows.stream().filter(r->r.label.toLowerCase(Locale.ROOT).contains(q)).toList();scroll=0;}
 private void add(){if(LocalData.store()==null)return;try{
  if(markers){if(client.player==null||client.world==null){status="Enter a world to save your current position";return;}
   LocalData.store().addMark(name.getText(),LocalData.world(client),LocalData.dimension(client),client.player.getX(),client.player.getY(),client.player.getZ());Modules.LOCAL_MARKS.enabled=true;Modules.save();status="Current position saved";
  }else status=LocalData.store().addFriend(name.getText())?"Local name saved":"Use a unique Minecraft name (3–16 letters, digits, _)";
  name.setText("");refresh();
 }catch(java.io.IOException e){status="Save failed: "+e.getMessage();}}
 @Override public boolean shouldPause(){return false;}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  c.fill(0,0,width,height,0xee0b0912);super.render(c,mx,my,delta);c.drawText(textRenderer,title,12,12,0xffbba9ff,false);
  var filtered=filteredRows;
  int count=Math.max(1,(height-108)/30);scroll=Math.max(0,Math.min(scroll,Math.max(0,filtered.size()-count)));
  for(int i=scroll;i<Math.min(filtered.size(),scroll+count);i++){var row=filtered.get(i);int y=64+(i-scroll)*30;
   Gfx.roundBorder(c,12,y,width-24,26,5,0xff171321,0xff302b43);c.drawText(textRenderer,textRenderer.trimToWidth(row.label,width-95),20,y+9,0xffeeeaff,false);
   c.drawText(textRenderer,"Delete",width-62,y+9,0xff88caff,false);
  }
  if(filtered.isEmpty())c.drawCenteredTextWithShadow(textRenderer,Text.literal("No local entries"),width/2,90,0xff858195);
  c.drawText(textRenderer,textRenderer.trimToWidth(status,width-24),12,height-24,0xffaaa6bb,false);
 }
 @Override public boolean mouseClicked(Click click,boolean doubled){
  if(click.button()==0&&click.x()>=width-75&&click.y()>=64&&click.y()<height-44){
   var filtered=filteredRows;int i=scroll+(int)(click.y()-64)/30;
   if(i<filtered.size()){var row=filtered.get(i);client.setScreen(new ConfirmScreen(yes->{client.setScreen(this);if(yes)try{if(markers)LocalData.store().removeMark(row.id);else LocalData.store().removeFriend(row.id);refresh();status="Deleted";}catch(java.io.IOException e){status=e.getMessage();}},Text.literal("Delete local entry?"),Text.literal(row.label)));return true;}
  }return super.mouseClicked(click,doubled);
 }
 @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll-=Math.round(v*2);return true;}
 @Override public void close(){client.setScreen(parent);}
}
