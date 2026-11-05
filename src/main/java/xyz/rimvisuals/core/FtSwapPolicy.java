package xyz.rimvisuals.core;

import java.util.List;
import java.util.Locale;

/** Pure selection and pacing rules; never reads input or sends inventory actions. */
public final class FtSwapPolicy {
 private FtSwapPolicy() {}
 public static int choose(boolean offhandFirst,boolean offhandSecond,List<Boolean> targetSlots){
  if(offhandFirst==offhandSecond)return -1;
  for(int i=0;i<Math.min(36,targetSlots.size());i++)if(Boolean.TRUE.equals(targetSlots.get(i)))return i;
  return -1;
 }
 public static int screenSlot(int inventorySlot){return inventorySlot<0||inventorySlot>35?-1:inventorySlot<9?36+inventorySlot:inventorySlot;}
 public static boolean matches(int kind,boolean head,boolean totem,String displayName,String filter){
  String name=clean(displayName),wanted=clean(filter);
  if(!wanted.isEmpty()&&!name.contains(wanted))return false;
  return switch(kind){
   case 0->head&&(!wanted.isEmpty()||name.contains("сфер")||name.contains("sphere"));
   case 1->totem&&(!wanted.isEmpty()||name.contains("талисман")||name.contains("talisman"));
   case 2->totem&&!name.contains("талисман")&&!name.contains("talisman");
   default->false;
  };
 }
 private static String clean(String value){return value==null?"":value.replaceAll("§.","").strip().toLowerCase(Locale.ROOT);}
 public static final class Gate {
  private Object context;private int tick;private long nanos;
  public boolean ready(Object context,int tick,long now){return this.context!=context||(tick-this.tick>=4&&now-nanos>=200_000_000L);}
  public void record(Object context,int tick,long now){this.context=context;this.tick=tick;this.nanos=now;}
 }
}
