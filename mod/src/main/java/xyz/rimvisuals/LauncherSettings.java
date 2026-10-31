package xyz.rimvisuals;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import xyz.rimvisuals.core.Modules;
import java.nio.file.*;
import java.util.Properties;
import org.slf4j.LoggerFactory;
public final class LauncherSettings {
 private static long last=-1; private static int ticks;
 public static void tick(){if(++ticks%40!=0)return;load();}
 public static void load(){
  Path p=FabricLoader.getInstance().getConfigDir().resolve("rimvisuals/launcher.json");
  try{if(!Files.exists(p))return;long stamp=Files.getLastModifiedTime(p).toMillis();if(stamp==last)return;
   JsonObject root=JsonParser.parseString(Files.readString(p)).getAsJsonObject();
   if(root.get("schemaVersion").getAsInt()!=1)return;
   JsonObject v=root.getAsJsonObject("visuals");Properties props=new Properties();
   String[][] keys={{"pulseGlow","pulse_glow"},{"particles","particles"},{"watermark","watermark"},{"infoHud","info_hud"}};
   for(String[] k:keys)if(v.has(k[0]))props.setProperty(k[1]+".enabled",String.valueOf(v.get(k[0]).getAsBoolean()));
   if(v.has("particles"))props.setProperty("particles.in-game_overlay",String.valueOf(v.get("particles").getAsBoolean()));
   if(v.has("accent")){String hex=v.get("accent").getAsString();if(hex.matches("#[0-9a-fA-F]{6}")){props.setProperty("interface.theme","3");props.setProperty("interface.custom_accent",hex.substring(1));}}
   if(v.has("hudScale"))props.setProperty("interface.hud_scale",String.valueOf(Math.max(.6,Math.min(2,v.get("hudScale").getAsDouble()/100))));
   Modules.apply(props);last=stamp;LoggerFactory.getLogger("rimvisuals").info("Applied launcher settings");
  }catch(Exception e){LoggerFactory.getLogger("rimvisuals").warn("Invalid launcher settings; keeping current settings",e);}
 }
}
