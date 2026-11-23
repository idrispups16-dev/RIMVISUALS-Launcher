package xyz.rimvisuals.gui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.rimvisuals.core.*;
public final class FriendsScreen extends Screen {
 private final Screen parent;private TextFieldWidget code;private int page;private String signature="";
 public FriendsScreen(Screen parent){super(Text.literal("RIMVISUALS / Друзья"));this.parent=parent;}
 private void button(int x,int y,int w,String label,Runnable action){addDrawableChild(new RimButton(x,y,w,20,Text.literal(label),b->action.run()));}
 @Override protected void init(){
  code=addDrawableChild(new TextFieldWidget(textRenderer,12,65,Math.max(100,width-130),20,Text.literal("Код друга")));code.setMaxLength(12);code.setPlaceholder(Text.literal("Код аккаунта друга"));
  button(width-110,65,98,"Добавить",()->SocialClient.action("request",code.getText()));
  button(12,36,170,"Скопировать мой код",()->client.keyboard.setClipboard(SocialClient.code()));
  button(width-100,10,88,"Назад",this::close);
  button(12,height-58,90,"Статус: "+(Modules.FRIENDS.presence.value?"ON":"OFF"),()->{Modules.FRIENDS.presence.value=!Modules.FRIENDS.presence.value;rebuild();});
  button(110,height-58,110,"Метки: "+(Modules.FRIENDS.pings.value?"ON":"OFF"),()->{Modules.FRIENDS.pings.value=!Modules.FRIENDS.pings.value;rebuild();});
  button(228,height-58,85,"<",()->{page=Math.max(0,page-1);rebuild();});button(320,height-58,85,">",()->{page++;rebuild();});
  var rows=new java.util.ArrayList<com.google.gson.JsonObject>();rows.addAll(SocialClient.rows("incoming"));rows.addAll(SocialClient.rows("friends"));rows.addAll(SocialClient.rows("sent"));
  int count=Math.max(1,(height-165)/28);page=Math.min(page,Math.max(0,(rows.size()-1)/count));
  for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var row=rows.get(i);String kind=SocialClient.text(row,"kind"),friend=SocialClient.text(row,"code");int y=100+(i-page*count)*28;
   String text=SocialClient.text(row,"nickname")+" ["+friend+"] "+(kind.equals("incoming")?"Запрос":kind.equals("sent")?"Ожидание":row.get("online").getAsBoolean()?"RIM / онлайн":"офлайн");
   button(12,y,Math.max(100,width-140),text,()->code.setText(friend));
   button(width-120,y,108,kind.equals("incoming")?"Принять":"Удалить",()->SocialClient.action(kind.equals("incoming")?"accept":"remove",friend));
  }
 }
 private void rebuild(){String value=code==null?"":code.getText();clearChildren();init();code.setText(value);}
 @Override public void tick(){String s=SocialClient.rows("friends").toString()+SocialClient.rows("incoming")+SocialClient.rows("sent");if(!s.equals(signature)){signature=s;rebuild();}}
 @Override public void render(DrawContext c,int x,int y,float d){c.fill(0,0,width,height,0xf0121212);super.render(c,x,y,d);c.drawText(textRenderer,title,12,14,0xffeeeeee,false);c.drawText(textRenderer,textRenderer.trimToWidth(SocialClient.status(),width-24),12,height-27,0xffbbbbbb,false);}
 @Override public boolean shouldPause(){return false;}
 @Override public void close(){Modules.save();client.setScreen(parent);}
}
