package xyz.rimvisuals.core;
/** Cosmetic time only. Never changes player swing ticks, attack cooldown or packets. */
public final class AttackSwingClock {
 private boolean started;private long at,length;
 public void start(long now,long duration){
  if(started&&now-at>=0&&now-at<length/3)return;
  started=true;at=now;length=Math.max(80_000_000L,Math.min(4_500_000_000L,duration));
 }
 public float progress(long now){
  if(!started||now-at<0||now-at>=length)return -1;
  return (float)((now-at)/(double)length);
 }
 public void clear(){started=false;}
}
