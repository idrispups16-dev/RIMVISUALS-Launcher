package xyz.rimvisuals.modules;
import xyz.rimvisuals.core.*;
import xyz.rimvisuals.core.Module;

/** Own local-information helper; no server commands or guessed event durations. */
public final class ServerHelper extends Module {
 public final boolean ft;
 public final Setting.Text aliases=add(new Setting.Text("Extra exact hosts",""));
 public final Setting.Bool chat=add(new Setting.Bool("Read system chat",true));
 public final Setting.Bool action=add(new Setting.Bool("Read action bar",true));
 public final Setting.Bool scoreboard=add(new Setting.Bool("Read sidebar",true));
 public final Setting.Bool notifications=add(new Setting.Bool("Event notifications",true));
 public final Setting.Text eventPrefix=add(new Setting.Text("Event prefix",""));
 public final Setting.Text timerPrefix=add(new Setting.Text("Timer prefix",""));
 public final Setting.Text regionPrefix=add(new Setting.Text("Region prefix",""));
 public final Setting.Bool health=add(new Setting.Bool("Low HP warning",true));
 public final Setting.Num hp=add(new Setting.Num("HP threshold",8,1,20,1));
 public final Setting.Bool armor=add(new Setting.Bool("Armor warning",true));
 public final Setting.Num durability=add(new Setting.Num("Armor percent",15,1,50,1));
 public final HelperData.Session session=new HelperData.Session();
 public ServerHelper(boolean ft){super(ft?"ft_helper":"hw_helper",ft?"FT Helper":"HW Helper","Own server HUD and literal parsing rules. Blank rules do nothing; timer syntax: prefix + seconds or mm:ss. No commands.",Category.UTILITIES);this.ft=ft;enabled=false;}
 @Override public String statusNote(){return ServerHelpers.active()==this?"Active: "+ServerHelpers.address():"Inactive outside "+(ft?"FunTime":"HolyWorld")+". Configure literal prefixes from received messages.";}
}
