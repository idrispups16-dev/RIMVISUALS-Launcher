package xyz.rimvisuals.core;
import java.util.LinkedHashMap;
import java.util.Map;
/** CPU submission times only. Deferred GPU work is not measured here. */
public final class PerformanceMonitor {
 public static final class Sample { public double average;public long peak;public long calls;public long seen;void add(long ns){seen=System.nanoTime();average=calls++==0?ns:average*.95+ns*.05;peak=Math.max(peak,ns);} }
 private static final Map<String,Sample> samples=new LinkedHashMap<>();
 private static final Map<String,String> labels=new LinkedHashMap<>();
 private static long nextLabel;
 public static long begin(){return System.nanoTime();}
 public static void end(String id,long start){samples.computeIfAbsent(id,k->new Sample()).add(System.nanoTime()-start);}
 public static Map<String,String> labels(){long now=System.nanoTime();if(now>=nextLabel){nextLabel=now+500_000_000;labels.clear();for(var e:samples.entrySet()){var s=e.getValue();labels.put(e.getKey(),String.format(java.util.Locale.ROOT,"%s %.3f ms (peak %.3f)",e.getKey(),s.average/1e6,s.peak/1e6)+(now-s.seen>2_000_000_000L?" [inactive]":""));}}return labels;}
 /** The {@code n} heaviest modules by smoothed CPU time, refreshed twice a second. */
 public static java.util.List<String> top(int n){long now=System.nanoTime();if(now>=nextTop){nextTop=now+500_000_000;topCache.clear();samples.entrySet().stream().filter(e->now-e.getValue().seen<=2_000_000_000L).sorted((a,b)->Double.compare(b.getValue().average,a.getValue().average)).limit(n).forEach(e->topCache.add(String.format(java.util.Locale.ROOT,"%s %.2f ms",e.getKey(),e.getValue().average/1e6)));}return topCache;}
 private static long nextTop;private static final java.util.List<String> topCache=new java.util.ArrayList<>();
 public static void reset(){samples.clear();labels.clear();nextLabel=0;}
}
