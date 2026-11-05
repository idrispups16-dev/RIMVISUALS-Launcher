package xyz.rimvisuals.core;

import com.google.gson.*;
import java.util.*;

/** Connection-scoped, fail-closed implementation of HolyWorld's Feature Control contract. */
public final class HolyFeaturePolicy {
 public enum State { OUTSIDE, PENDING, READY, FAILED }
 private final Set<String> catalogue;
 private Set<String> blocked=Set.of();
 private State state=State.OUTSIDE;
 private UUID pending;
 private long lastRequest,revision;
 private boolean sent;
 public HolyFeaturePolicy(Collection<String> ids){
  var copy=new TreeSet<String>();
  for(String id:ids)if(id==null||!id.matches("[a-z0-9_.-]{1,128}")||!copy.add(id))throw new IllegalArgumentException("Invalid feature catalogue");
  catalogue=Collections.unmodifiableSet(copy);
 }
 public static boolean holyWorld(String address){return HelperData.matches(address,false,"");}
 public State state(){return state;}
 public long revision(){return revision;}
 public int blockedCount(){return blocked.size();}
 public Set<String> catalogue(){return catalogue;}
 public void connect(String address){connect(address,false);}
 public void connect(String address,boolean apiAdvertised){disconnect();if(holyWorld(address)||apiAdvertised){state=State.PENDING;revision++;}}
 public void disconnect(){state=State.OUTSIDE;pending=null;blocked=Set.of();sent=false;revision++;}
 public UUID request(long now){
  if(state==State.OUTSIDE||sent&&now-lastRequest<10_000)return null;
  sent=true;lastRequest=now;pending=UUID.randomUUID();state=State.PENDING;revision++;return pending;
 }
 public String requestJson(UUID id){
  var root=new JsonObject();root.addProperty("id",id.toString());root.addProperty("method","checkFeatures");
  var data=new JsonObject();data.addProperty("client","rimvisuals");var features=new JsonArray();catalogue.forEach(features::add);data.add("features",features);root.add("payload",data);return root.toString();
 }
 public void timeout(long now){if(pending!=null&&now-lastRequest>=5_000)fail();}
 public void fail(){pending=null;state=State.FAILED;revision++;}
 public boolean receive(String json){
  if(pending==null||state==State.OUTSIDE)return false;
  try{
   if(json==null||json.length()>32768)return false;
   var root=JsonParser.parseString(json).getAsJsonObject();
   if(!string(root.get("id"))||!pending.toString().equals(root.get("id").getAsString()))return false;
   var ok=root.get("ok");
   if(ok==null||!ok.isJsonPrimitive()||!ok.getAsJsonPrimitive().isBoolean()||!ok.getAsBoolean()){fail();return false;}
   var data=root.getAsJsonObject("payload");var values=data.getAsJsonArray("blocklist");
   if(values.size()>catalogue.size())throw new IllegalArgumentException();
   var next=new HashSet<String>();
   for(var value:values){if(!string(value)||!catalogue.contains(value.getAsString()))throw new IllegalArgumentException();next.add(value.getAsString());}
   blocked=Set.copyOf(next);pending=null;state=State.READY;revision++;return true;
  }catch(RuntimeException ex){fail();return false;}
 }
 private static boolean string(JsonElement value){return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString();}
 public boolean allowed(String id){
  if(state==State.OUTSIDE)return true;
  if(state!=State.READY||!catalogue.contains(id))return false;
  if((id.equals("profiles")||id.equals("performance_hud"))&&!blocked.isEmpty())return false;
  if((id.equals("local_marks")||id.startsWith("friends_options"))&&(groupBlocked("local_marks")||groupBlocked("friends_options")))return false;
  // A restricted preferences module affects multiple raw configuration consumers: disable the
  // whole client on this connection instead of silently leaving some preference effect active.
  if(groupBlocked("interface")||groupBlocked("performance")||groupBlocked("client_settings"))return false;
  String parent=id.contains(".")?id.substring(0,id.indexOf('.')):id;
  if(groupBlocked(parent))return false;
  if(id.startsWith("hw_helper_")&&groupBlocked("hw_helper"))return false;
  if(id.startsWith("ft_helper_")&&groupBlocked("ft_helper"))return false;
  return true;
 }
 private boolean groupBlocked(String parent){for(String id:blocked)if(id.equals(parent)||id.startsWith(parent+"."))return true;return false;}
}
