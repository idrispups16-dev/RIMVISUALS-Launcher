package xyz.rimvisuals.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FtSwapPolicyTest {
 @Test void togglesOnlyTheTwoChosenOffhandTypes(){
  assertEquals(2,FtSwapPolicy.choose(true,false,List.of(false,false,true,false)));
  assertEquals(1,FtSwapPolicy.choose(false,true,List.of(false,true,false)));
  assertEquals(-1,FtSwapPolicy.choose(false,false,List.of(true,true)));
  assertEquals(-1,FtSwapPolicy.choose(true,true,List.of(true,true)));
  assertEquals(-1,FtSwapPolicy.choose(true,false,List.of(false,false)));
 }
 @Test void mapsOnlyStorageAndHotbarSlots(){
  assertEquals(36,FtSwapPolicy.screenSlot(0));assertEquals(44,FtSwapPolicy.screenSlot(8));
  assertEquals(9,FtSwapPolicy.screenSlot(9));assertEquals(35,FtSwapPolicy.screenSlot(35));
  assertEquals(-1,FtSwapPolicy.screenSlot(36));assertEquals(-1,FtSwapPolicy.screenSlot(-1));
 }
 @Test void fourTicksAndWallClockAreBothRequired(){
  var g=new FtSwapPolicy.Gate();Object world=new Object();
  assertTrue(g.ready(world,100,1_000_000_000L));g.record(world,100,1_000_000_000L);
  assertFalse(g.ready(world,103,1_500_000_000L));
  assertFalse(g.ready(world,104,1_199_999_999L));
  assertTrue(g.ready(world,104,1_200_000_000L));
  g.record(world,104,1_200_000_000L);assertFalse(g.ready(world,104,1_200_000_000L));
  assertTrue(g.ready(new Object(),0,1_200_000_001L));
 }
 @Test void itemKindsDistinguishNamedTalismansFromOrdinaryTotems(){
  assertTrue(FtSwapPolicy.matches(0,true,false,"Сфера Пандоры",""));
  assertFalse(FtSwapPolicy.matches(0,true,false,"Голова игрока",""));
  assertTrue(FtSwapPolicy.matches(1,false,true,"§6Талисман Грани",""));
  assertFalse(FtSwapPolicy.matches(2,false,true,"Талисман Грани",""));
  assertTrue(FtSwapPolicy.matches(2,false,true,"Тотем бессмертия",""));
  assertTrue(FtSwapPolicy.matches(1,false,true,"Особый амулет","амулет"));
  assertFalse(FtSwapPolicy.matches(2,false,true,"Тотем бессмертия","пандора"));
 }
}
