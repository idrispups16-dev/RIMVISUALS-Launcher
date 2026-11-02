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
 public final Setting.Text auctionTitle=add(new Setting.Text("Auction title prefix",""));
 public final Setting.Text auctionFilter=add(new Setting.Text("Auction item name contains",""));
 public final Setting.Bool auctionNotify=add(new Setting.Bool("Auction match notification",false));
 public final Setting.Bool health=add(new Setting.Bool("Low HP warning",true));
 public final Setting.Num hp=add(new Setting.Num("HP threshold",8,1,20,1));
 public final Setting.Bool armor=add(new Setting.Bool("Armor warning",true));
 public final Setting.Num durability=add(new Setting.Num("Armor percent",15,1,50,1));
 public final Setting.Bool events=add(new Setting.Bool("Track events",true));
 public final Setting.Bool publicApi=add(new Setting.Bool("HolyWorld public API",true));
 public final Setting.Num interval=add(new Setting.Num("API interval seconds",20,20,300,5));
 public final Setting.Text apiServer=add(new Setting.Text("API server filter",""));
 public final Setting.Text eventNames=add(new Setting.Text("Message event names","Вулкан,Гейзер,Маяк убийца,Метеорит,Горящий череп,Адская резня,Бикини Боттом,Дракон,Сундук смерти,Куб,Посылка,Шахта"));
 public final Setting.Text interests=add(new Setting.Text("Interested events contains",""));
 public final Setting.Bool cooldowns=add(new Setting.Bool("Track PvP cooldowns",true));
 public final Setting.Text trapName=add(new Setting.Text("Trapka item name contains","трапка"));
 public final Setting.Text plastName=add(new Setting.Text("Plast item name contains","пласт"));
 public final Setting.Text trapPrefix=add(new Setting.Text("Trapka server CD prefix",""));
 public final Setting.Text plastPrefix=add(new Setting.Text("Plast server CD prefix",""));
 public final Setting.Bool estimates=add(new Setting.Bool("Estimated use-attempt timers",false));
 public final Setting.Num trapSeconds=add(new Setting.Num("Estimated Trapka seconds",0,0,600,1));
 public final Setting.Num plastSeconds=add(new Setting.Num("Estimated Plast seconds",0,0,600,1));
 public final Setting.Bool trapVisible=add(new Setting.Bool("Show Trapka timer",true));
 public final Setting.Bool plastVisible=add(new Setting.Bool("Show Plast timer",true));
 public final Setting.Color trapColor=add(new Setting.Color("Trapka color",0xAD88FF));
 public final Setting.Color plastColor=add(new Setting.Color("Plast color",0x55C9FF));
 public final Setting.Bool sounds=add(new Setting.Bool("Helper sounds",true));
 public final Setting.Num soundVolume=add(new Setting.Num("Helper sound volume",.5,0,1,.05));
 public final Setting.Bool readyNotify=add(new Setting.Bool("Cooldown READY notifications",true));
 public final HelperTracker tracker=new HelperTracker();
 public final HelperData.Session session=new HelperData.Session();
 public ServerHelper(boolean ft){super(ft?"ft_helper":"hw_helper",ft?"FT Helper":"HW Helper","Own server HUD and literal parsing rules. Blank rules do nothing; timer syntax: prefix + seconds or mm:ss. No commands.",Category.UTILITIES);this.ft=ft;publicApi.hidden=ft;interval.hidden=ft;apiServer.hidden=ft;enabled=false;}
 @Override public String statusNote(){return ServerHelpers.active()==this?"Active: "+ServerHelpers.address():"Inactive outside "+(ft?"FunTime":"HolyWorld")+". Configure literal prefixes from received messages.";}
}
