package xyz.rimvisuals.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HelperDataTest {
 @Test void hostBoundaries(){assertTrue(HelperData.matches("HUB.HOLYWORLD.RU:25565",false,""));assertTrue(HelperData.matches("hub.holyworld.me.",false,""));assertTrue(HelperData.matches("play.funtime.su",true,""));assertFalse(HelperData.matches("evilfuntime.su",true,""));assertFalse(HelperData.matches("funtime.su.evil.org",true,""));assertFalse(HelperData.matches("funtime.su:wrong",true,""));assertTrue(HelperData.matches("other.example:25565",true,"other.example"));assertFalse(HelperData.matches("sub.other.example",true,"other.example"));}
 @Test void boundedInput(){assertEquals(192,HelperData.clean("a".repeat(10000)).length());assertEquals("Hello",HelperData.clean("§aHello\n"));assertEquals("",HelperData.clean(null));}
 @Test void timersRequireData(){assertEquals(-1,HelperData.seconds("Event",""));assertEquals(90,HelperData.seconds("Timer: 1:30 next event","Timer:"));assertEquals(42,HelperData.seconds("Timer: 42","Timer:"));for(String s:new String[]{"1:99","-2","999999999999","1:2:3","unknown","999999"})assertEquals(-1,HelperData.seconds("Timer: "+s,"Timer:"));}
 @Test void unknownMessagesAndDisconnect(){var s=new HelperData.Session();assertFalse(s.accept("unknown","Event:","Timer:","Region:",100));assertTrue(s.accept("Timer: 10","Event:","Timer:","Region:",100));assertEquals(10,s.remaining(100));assertFalse(s.accept("Timer: 10","Event:","Timer:","Region:",100));s.accept("Region: home","Event:","Timer:","Region:",100);assertEquals("home",s.region);s.clear();assertEquals(0,s.remaining(100));assertEquals("",s.region);assertEquals("",s.timerSource);}
 @Test void textConfigRoundtrip(){var t=new Setting.Text("Rule","Timer:");t.load(t.save());assertEquals("Timer:",t.value);t.load("x".repeat(999));assertEquals(192,t.value.length());}
}
