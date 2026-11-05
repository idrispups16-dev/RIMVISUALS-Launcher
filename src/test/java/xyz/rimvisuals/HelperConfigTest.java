package xyz.rimvisuals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.rimvisuals.core.*;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class HelperConfigTest {
 @Test void separateHelperSettingsSurviveJsonBackupReset(@TempDir Path dir)throws Exception{
  var saved=Modules.snapshot();try{
   Modules.HW_HELPER.enabled=true;Modules.HW_HELPER.eventPrefix.value="Событие:";Modules.HW_HELPER.timerPrefix.value="Осталось:";
   Modules.FT_HELPER.enabled=true;Modules.FT_HELPER.aliases.value="controlled.example";Modules.FT_HELPER.regionPrefix.value="Регион:";
   Modules.FT_HELPER.auctionTitle.value="Auction";Modules.FT_HELPER.auctionFilter.value="Sword";
   Modules.FT_HELPER_HUD.posX.set(123);Modules.FT_HELPER_HUD.scale.set(1.25);Modules.FT_HELPER_HUD.elementOpacity.set(.35);Modules.FT_HELPER_HUD.accentColor.rgb=0x123456;
   Modules.FT_HELPER.estimates.value=true;Modules.FT_HELPER.trapSeconds.set(37);Modules.FT_HELPER.sounds.value=false;Modules.FT_HELPER.eventMenu.value="Ивенты";Modules.HW_HELPER.apiServer.value="LITE_ANARCHY_1";Modules.FT_COOLDOWNS.posX.set(211);Modules.FT_EVENTS.elementOpacity.set(.4);
   var expected=Modules.snapshot();var store=new ConfigStore(dir);assertTrue(store.create("HolyWorld",expected));assertTrue(store.create("FunTime",expected));assertFalse(store.create("FunTime",new Properties()));
   store.backup(expected);String imported=store.importJson(store.exportJson("FunTime")).orElseThrow();assertEquals(expected,store.load(imported).orElseThrow());assertEquals(expected,store.latestBackup().orElseThrow());
   Modules.reset(Modules.HW_HELPER);Modules.reset(Modules.FT_HELPER);Modules.reset(Modules.FT_HELPER_HUD);assertFalse(Modules.HW_HELPER.enabled);assertEquals("",Modules.FT_HELPER.regionPrefix.value);
   Modules.apply(store.load(imported).orElseThrow());assertEquals("Событие:",Modules.HW_HELPER.eventPrefix.value);assertEquals("Регион:",Modules.FT_HELPER.regionPrefix.value);assertEquals(37,Modules.FT_HELPER.trapSeconds.value);assertFalse(Modules.FT_HELPER.sounds.value);assertEquals("LITE_ANARCHY_1",Modules.HW_HELPER.apiServer.value);assertEquals(211,Modules.FT_COOLDOWNS.posX.value);assertEquals(123,Modules.FT_HELPER_HUD.posX.value);assertEquals(.35,Modules.FT_HELPER_HUD.elementOpacity.value,1e-8);
  }finally{Modules.apply(saved);}
 }
 @Test void legacyConfigsDoNotEnableHelpers(){var saved=Modules.snapshot();try{Modules.reset(Modules.HW_HELPER);Modules.reset(Modules.FT_HELPER);Properties legacy=new Properties();legacy.setProperty("watermark.enabled","true");Modules.apply(legacy);assertFalse(Modules.HW_HELPER.enabled);assertFalse(Modules.FT_HELPER.enabled);}finally{Modules.apply(saved);}}
}
