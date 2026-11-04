package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.*;
public final class FriendsOptions extends xyz.rimvisuals.core.Module {
 public final Setting.Bool presence=add(new Setting.Bool("Share RIMVISUALS status with friends",true));
 public final Setting.Bool pings=add(new Setting.Bool("Receive friend markers",true));
 public final Setting.Bool badges=add(new Setting.Bool("RIM friend name badges",true));
 public final Setting.Text room=add(new Setting.Text("Server room channel",""));
 public FriendsOptions(){super("friends_options","Friends & Markers","Mutual account friends, shared pings (N), local .way name X Y Z. Match the room on multi-server networks.",Category.VISUALS);toggleable=false;}
}
