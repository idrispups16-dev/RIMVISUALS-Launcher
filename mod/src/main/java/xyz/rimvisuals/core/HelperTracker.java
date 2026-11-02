package xyz.rimvisuals.core;

import com.google.gson.*;
import java.util.*;
import java.util.regex.*;

/** Pure bounded data model. No schedules or server cooldown constants. */
public final class HelperTracker {
 public enum State { UPCOMING, ACTIVE, FINISHED, UNKNOWN }
 public record Event(String id,String name,String server,String coordinates,State state,long start,long end,long seen,String source) {
  public String countdown(long now){long deadline=state==State.UPCOMING?start:state==State.ACTIVE?end:0;return deadline>now?((deadline-now+999)/1000)+"s":deadline==0?"time unknown":"awaiting server update";}
 }
 public record Cooldown(String name,long start,long end,boolean estimated,String source) {
  public long seconds(long now){return Math.max(0,(end-now+999)/1000);}
  public double progress(long now){return end<=start?1:Math.max(0,Math.min(1,(double)(now-start)/(end-start)));}
 }
 public final LinkedHashMap<String,Event> events=new LinkedHashMap<>();
 public final LinkedHashMap<String,Cooldown> cooldowns=new LinkedHashMap<>();
 private final Map<String,Long> ready=new HashMap<>();
 private final Map<String,Long> recent=new LinkedHashMap<>();
 public void clear(){events.clear();cooldowns.clear();ready.clear();recent.clear();}
 public static boolean interested(String name,String filter){if(filter==null||filter.isBlank())return true;for(String part:filter.toLowerCase(Locale.ROOT).split(",",-1))if(!part.isBlank()&&name.toLowerCase(Locale.ROOT).contains(part.strip()))return true;return false;}
 public void cooldown(String name,double seconds,boolean estimate,String source,long now){
  if(!Double.isFinite(seconds)||seconds<0||seconds>86400)return;
  if(cooldowns.size()>=8&&!cooldowns.containsKey(name))return;
  cooldowns.put(name,new Cooldown(name,now,now+Math.round(seconds*1000),estimate,source));ready.remove(name);
 }
 public List<String> ready(long now){List<String> out=new ArrayList<>();for(var c:cooldowns.values())if(c.end<=now&&!ready.containsKey(c.name)){ready.put(c.name,now);out.add(c.name+(c.estimated?" ~ READY (estimate)":" READY"));}return out;}
 public void trim(long now){cooldowns.values().removeIf(c->now-c.end>10000);events.values().removeIf(e->now-e.seen>300000);while(events.size()>64)events.remove(events.keySet().iterator().next());}
 private static String str(JsonObject o,String key){var v=o.get(key);return v!=null&&v.isJsonPrimitive()?HelperData.clean(v.getAsString()):"";}
 /** HWEvents' public wire model: server -> [{instanceId,id,metadata:{displayName,rare}}]. */
 public static List<Event> api(String json,long now){
  if(json==null||json.length()>262144)throw new IllegalArgumentException("API body limit");
  JsonElement root=JsonParser.parseString(json);if(!root.isJsonObject())throw new IllegalArgumentException("Unknown API shape");JsonObject map=root.getAsJsonObject();
  if(map.has("events")&&map.get("events").isJsonObject())map=map.getAsJsonObject("events");
  List<Event> out=new ArrayList<>();int servers=0;
  for(var group:map.entrySet()){
   if(++servers>256)break;if(!group.getValue().isJsonArray())continue;
   for(var raw:group.getValue().getAsJsonArray()){
    if(out.size()>=256)return List.copyOf(out);if(!raw.isJsonObject())continue;var o=raw.getAsJsonObject();
    String name="";if(o.has("metadata")&&o.get("metadata").isJsonObject())name=str(o.getAsJsonObject("metadata"),"displayName");
    String id=str(o,"instanceId");if(name.isBlank()||id.isBlank())continue;
    out.add(new Event(group.getKey()+":"+id,name,HelperData.clean(group.getKey()),"",State.ACTIVE,0,0,now,"HolyWorld API"));
   }
  }return List.copyOf(out);
 }
 public void replaceApi(List<Event> snapshot,long now){
  Set<String> ids=new HashSet<>();for(var e:snapshot){ids.add(e.id);events.put(e.id,e);}
  events.replaceAll((id,e)->e.source.equals("HolyWorld API")&&!ids.contains(id)?new Event(e.id,e.name,e.server,e.coordinates,State.UNKNOWN,0,0,e.seen,"API: no longer listed"):e);trim(now);
 }
 private static final Pattern CLOCK=Pattern.compile("(?<![0-9])([0-9]{1,3}):([0-9]{2})(?::([0-9]{2}))?(?![0-9])");
 private static final Pattern UNITS=Pattern.compile("([0-9]{1,4})\\s*(час(?:а|ов)?|ч\\b|мин(?:ут(?:а|ы)?)?|м\\b|сек(?:унд(?:а|ы)?)?|с\\b|seconds?|minutes?)",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
 private static final Pattern COORDS=Pattern.compile("(?i)(?:xyz|координаты|коорд)[ :]+(-?[0-9]{1,7})[ ,;]+(-?[0-9]{1,3})[ ,;]+(-?[0-9]{1,7})");
 private static final Pattern ANARCHY=Pattern.compile("(?iu)(?:анархия|анархии|анархию|сервер|анка)[ #№:]*([0-9]{1,4})");
 public static int duration(String raw){
  String s=HelperData.clean(raw).toLowerCase(Locale.ROOT);Matcher c=CLOCK.matcher(s);
  if(c.find()){int a=Integer.parseInt(c.group(1)),b=Integer.parseInt(c.group(2));if(b>59)return -1;int n;if(c.group(3)!=null){int sec=Integer.parseInt(c.group(3));if(sec>59)return -1;n=a*3600+b*60+sec;}else n=a*60+b;return n<=86400?n:-1;}
  Matcher u=UNITS.matcher(s);int n=0,count=0;while(u.find()&&count++<6){int v=Integer.parseInt(u.group(1));String k=u.group(2);n+=v*(k.startsWith("ч")?3600:k.startsWith("м")||k.startsWith("minute")?60:1);if(n>86400)return -1;}return count>0?n:-1;
 }
 /** Only explicit received event lines; unknown state/time is retained as unknown. */
 public boolean message(String raw,String names,String server,long now){
  String s=HelperData.clean(raw),l=s.toLowerCase(Locale.ROOT);String found="";
  for(String name:names.split(",",-1))if(!name.isBlank()&&l.contains(name.strip().toLowerCase(Locale.ROOT))){found=name.strip();break;}
  boolean delay=l.contains("до следующего ивента")||l.contains("до начала ивента")||l.contains("до следующего события");
  if(found.isBlank()&&!delay)return false;
  Long last=recent.get(s);if(last!=null&&now-last<2000)return false;recent.put(s,now);while(recent.size()>32)recent.remove(recent.keySet().iterator().next());
  State state=delay||l.contains("начнётся")||l.contains("начнется")||l.contains("появится")||l.contains("до начала")?State.UPCOMING:l.contains("завершён")||l.contains("завершен")||l.contains("закончился")?State.FINISHED:l.contains("начался")||l.contains("активен")||l.contains("появился")?State.ACTIVE:State.UNKNOWN;
  if(found.isBlank())found="Next event (server response)";String coords="";var c=COORDS.matcher(s);if(c.find())coords=c.group(1)+" "+c.group(2)+" "+c.group(3);
  var a=ANARCHY.matcher(s);if(a.find())server="Anarchy #"+a.group(1);
  int sec=duration(s);long start=state==State.UPCOMING&&sec>=0?now+sec*1000L:0;
  long end=state==State.ACTIVE&&sec>=0&&(l.contains("до конца")||l.contains("до завершения")||l.contains("осталось"))?now+sec*1000L:0;
  String id="message:"+server+":"+found;Event old=events.get(id);if(old!=null&&old.state==state&&start==0&&end==0&&coords.equals(old.coordinates))return false;
  events.put(id,new Event(id,found,server,coords,state,start,end,now,"received message"));trim(now);return true;
 }
}
