package xyz.rimvisuals;
import xyz.rimvisuals.core.*;
/** Client-loaded config/reset/disconnect checks; no connection to public servers. */
public final class HelperChecks {
 public static void run(){var saved=Modules.snapshot();try{
  var h=Modules.FT_HELPER;h.enabled=true;h.eventPrefix.value="Event:";h.timerPrefix.value="Timer:";h.regionPrefix.value="Region:";
  var hud=Modules.FT_HELPER_HUD;hud.posX.set(137);hud.scale.set(1.25);hud.elementOpacity.set(.35);hud.customAccent.value=true;hud.accentColor.rgb=0x112233;
  var expected=Modules.snapshot();Modules.reset(h);Modules.reset(hud);Modules.apply(expected);
  if(!h.enabled||!h.timerPrefix.value.equals("Timer:")||hud.posX.value!=137||Math.abs(hud.scale.value-1.25)>.001||Math.abs(hud.elementOpacity.value-.35)>.001||hud.accentColor.rgb!=0x112233)throw new IllegalStateException("Helper settings roundtrip failed");
  var s=h.session;s.accept("Timer: 30","Event:","Timer:","Region:",System.nanoTime());s.accept("Region: home","Event:","Timer:","Region:",System.nanoTime());ServerHelpers.clear();
  if(ServerHelpers.active()!=null||!s.region.isEmpty()||!s.timers.isEmpty()||s.until!=0||!ServerHelpers.address().isEmpty())throw new IllegalStateException("Helper disconnect cleanup failed");
  ServerHelpers.updateConnection("play.funtime.su:25565");
  if(ServerHelpers.active()!=h)throw new IllegalStateException("FunTime matching failed");
  ServerHelpers.receive("Timer: 10 trap",0);long deadline=s.until;
  ServerHelpers.receive("Region: base",1);
  ServerHelpers.receive("Timer: 10 trap",0);
  if(!s.region.equals("base")||s.timers.size()!=1||deadline!=s.until)throw new IllegalStateException("Received helper data routing/dedup failed");
  h.action.value=false;ServerHelpers.receive("Region: hidden",1);if(!s.region.equals("base"))throw new IllegalStateException("Disabled source was parsed");
  ServerHelpers.updateConnection("funtime.su.unrelated.example");if(ServerHelpers.active()!=null||!s.timers.isEmpty()||!s.region.isEmpty())throw new IllegalStateException("Wrong server retained helper data");
  Modules.HW_HELPER.enabled=true;ServerHelpers.updateConnection("hub.holyworld.me");if(ServerHelpers.active()!=Modules.HW_HELPER)throw new IllegalStateException("HolyWorld matching failed");
  Modules.HW_HELPER.enabled=false;ServerHelpers.updateConnection("hub.holyworld.me");if(ServerHelpers.active()!=null)throw new IllegalStateException("Disabled helper stayed active");
  org.slf4j.LoggerFactory.getLogger("rimvisuals").info("RIM_HELPERS_OK: config roundtrip/reset, synthetic receive routing/dedup, host gating and session cleanup, no public-server test");
 }finally{ServerHelpers.clear();Modules.apply(saved);}}
}
