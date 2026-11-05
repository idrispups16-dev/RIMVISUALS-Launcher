package xyz.rimvisuals.gui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
/** Manual, confirmed connection only. No retries, commands, authentication or queue handling. */
public final class HelperConnectScreen extends Screen {
 private final Screen parent;private final boolean ft;
 public HelperConnectScreen(Screen parent,boolean ft){super(Text.literal(ft?"FunTime — manual join":"HolyWorld — manual join"));this.parent=parent;this.ft=ft;}
 @Override protected void init(){String[] addresses=ft?new String[]{"play.funtime.su"}:new String[]{"hub.holyworld.ru","hub.holyworld.me"};int y=height/2-40;
  for(String address:addresses){addDrawableChild(ButtonWidget.builder(Text.literal(address),b->client.setScreen(new ConfirmScreen(yes->{if(yes)ConnectScreen.connect(this,client,ServerAddress.parse(address),new ServerInfo(ft?"FunTime":"HolyWorld",address,ServerInfo.ServerType.OTHER),false,null);else client.setScreen(this);},Text.literal("Connect to "+address+"?"),Text.literal("Manual connection. Server version and rules still apply.")))).dimensions(width/2-115,y,230,20).build());y+=26;}
  addDrawableChild(ButtonWidget.builder(Text.literal("Back"),b->close()).dimensions(width/2-115,y+12,230,20).build());
 }
 @Override public void close(){client.setScreen(parent);}
}
