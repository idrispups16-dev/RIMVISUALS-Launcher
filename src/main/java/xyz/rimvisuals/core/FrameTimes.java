package xyz.rimvisuals.core;
/** Bounded HUD callback interval histogram; not a controlled benchmark or GPU timer. */
public final class FrameTimes {
 private static final long[] bins=new long[251];private static long last,count,total,worst;
 public static void reset(){java.util.Arrays.fill(bins,0);last=count=total=worst=0;}
 public static void record(long now){if(last!=0){long ns=now-last;if(ns>0&&ns<1_000_000_000L){bins[(int)Math.min(250,ns/1_000_000)]++;count++;total+=ns;worst=Math.max(worst,ns);}}last=now;}
 public static int percentile(double fraction){if(count==0)return 0;long rank=(long)Math.ceil(count*fraction),n=0;for(int i=0;i<bins.length;i++){n+=bins[i];if(n>=rank)return i==250?1000:i+1;}return 251;}
 public static String report(){return String.format(java.util.Locale.ROOT,"HUD intervals n=%d mean=%.2fms p95<%dms worst=%.2fms (1ms bins below 250ms, overflow <1000ms; long pauses excluded)",count,count==0?0:total/1e6/count,percentile(.95),worst/1e6);}
}
