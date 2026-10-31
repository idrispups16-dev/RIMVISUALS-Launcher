package xyz.rimvisuals;
import xyz.rimvisuals.core.Modules;
import xyz.rimvisuals.core.Setting;
import java.util.HashSet;
import java.util.Properties;
/** Opt-in integration assertions run only by the development smoke test. */
public final class IntegrationChecks {
 public static void run(){
  var saved=Modules.snapshot();
  try{
   var keys=new HashSet<String>();
   for(var m:Modules.ALL){if(!keys.add(m.id))throw new IllegalStateException("Duplicate module: "+m.id);var settings=new HashSet<String>();for(var s:m.settings)if(!settings.add(s.key()))throw new IllegalStateException("Duplicate setting: "+m.id+"."+s.key());}
   Modules.CRYSTAL_FADE.enabled=true;Modules.CRYSTAL_FADE.opacity.set(.5);Modules.PEARL_TRAILS.enabled=true;
   var legacy=new Properties();legacy.setProperty("pulse_glow.enabled","false");legacy.setProperty("particles.in-game_overlay","true");legacy.setProperty("interface.hud_scale","1.2");Modules.apply(legacy);
   if(!Modules.CRYSTAL_FADE.enabled||Modules.CRYSTAL_FADE.opacity.value!=.5||!Modules.PEARL_TRAILS.enabled||Modules.PULSE.enabled||!Modules.PARTICLES.overlay.value||Math.abs(Modules.UI.hudScale.value-1.2)>.00001)throw new IllegalStateException("Legacy launcher configuration lost module settings");
   var expected=Modules.snapshot();Modules.apply(saved);Modules.apply(expected);var actual=Modules.snapshot();
   for(var key:expected.stringPropertyNames())if(!expected.getProperty(key).equals(actual.getProperty(key))){try{if(Math.abs(Double.parseDouble(expected.getProperty(key))-Double.parseDouble(actual.getProperty(key)))<.000001)continue;}catch(NumberFormatException ignored){}throw new IllegalStateException("Config round-trip failed: "+key);}
   org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_CONFIG_OK: unique module keys, legacy settings preserve new visuals, profile round-trip; no world test");
  }finally{Modules.apply(saved);}
 }
}
