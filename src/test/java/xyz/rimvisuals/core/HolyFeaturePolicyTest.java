package xyz.rimvisuals.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HolyFeaturePolicyTest {
 private static final Set<String> IDS=Set.of("zoom","fullbright","hw_helper","hw_helper.auction_price","hw_helper.cooldowns","friends_options","friends_options.pings");
 private HolyFeaturePolicy policy(){return new HolyFeaturePolicy(IDS);}
 private String response(UUID id,String list){return "{\"id\":\""+id+"\",\"ok\":true,\"payload\":{\"blocklist\":"+list+"}}";}
 @Test void matchesOnlyOfficialDomainBoundaries(){
  assertTrue(HolyFeaturePolicy.holyWorld("hub.holyworld.me:25565"));assertTrue(HolyFeaturePolicy.holyWorld("HOLYWORLD.RU."));
  assertFalse(HolyFeaturePolicy.holyWorld("evilholyworld.me"));assertFalse(HolyFeaturePolicy.holyWorld("holyworld.me.evil.net"));assertFalse(HolyFeaturePolicy.holyWorld(""));
 }
 @Test void pendingFailsClosedWithoutDestroyingOtherServerAvailability(){
  var p=policy();assertTrue(p.allowed("zoom"));p.connect("holyworld.me");assertFalse(p.allowed("zoom"));assertFalse(p.allowed("not_declared"));
  p.connect("example.com");assertTrue(p.allowed("zoom"));
 }
 @Test void advertisedApiAlsoRestrictsAnIpOrCustomAlias(){var p=policy();p.connect("192.0.2.1",true);assertFalse(p.allowed("zoom"));assertEquals(HolyFeaturePolicy.State.PENDING,p.state());}
 @Test void declaresAllFunctionsAndCorrelatesResponse(){
  var p=policy();p.connect("holyworld.me");UUID id=p.request(0);
  var json=com.google.gson.JsonParser.parseString(p.requestJson(id)).getAsJsonObject();
  assertEquals("rimvisuals",json.getAsJsonObject("payload").get("client").getAsString());
  assertEquals(IDS.size(),json.getAsJsonObject("payload").getAsJsonArray("features").size());
  assertFalse(p.receive(response(UUID.randomUUID(),"[]")));assertFalse(p.allowed("zoom"));
  assertTrue(p.receive(response(id,"[\"fullbright\"]")));assertTrue(p.allowed("zoom"));assertFalse(p.allowed("fullbright"));
 }
 @Test void blocksWholeHelperWhenAnySubfeatureIsForbidden(){
  var p=policy();p.connect("holyworld.ru");UUID id=p.request(0);
  assertTrue(p.receive(response(id,"[\"hw_helper.auction_price\"]")));
  assertFalse(p.allowed("hw_helper"));assertFalse(p.allowed("hw_helper.cooldowns"));assertTrue(p.allowed("zoom"));
 }
 @Test void parentBlocksChildrenAndUnknownResponseIsFailClosed(){
  var p=policy();p.connect("holyworld.me");UUID id=p.request(0);
  assertTrue(p.receive(response(id,"[\"friends_options\"]")));assertFalse(p.allowed("friends_options.pings"));
  id=p.request(10_000);assertFalse(p.receive(response(id,"[\"mystery\"]")));assertFalse(p.allowed("zoom"));
 }
 @Test void errorsMalformedAndDuplicateRepliesCannotAuthorize(){
  for(String payload:List.of("null","{}","{\"ok\":true}","{\"blocklist\":[1]}","{\"blocklist\":\"zoom\"}")){
   var p=policy();p.connect("holyworld.me");UUID id=p.request(0);
   p.receive("{\"id\":\""+id+"\",\"ok\":true,\"payload\":"+payload+"}");assertFalse(p.allowed("zoom"));
  }
  var p=policy();p.connect("holyworld.me");UUID id=p.request(0);assertTrue(p.receive(response(id,"[\"zoom\"]")));
  assertFalse(p.receive(response(id,"[]")));assertFalse(p.allowed("zoom"));
 }
 @Test void rateLimitTimeoutAndReconnectAreConnectionScoped(){
  var p=policy();p.connect("holyworld.me");UUID old=p.request(0);assertNotNull(old);assertNull(p.request(9999));
  p.timeout(5000);assertFalse(p.allowed("zoom"));assertNotNull(p.request(10000));
  p.connect("holyworld.ru");assertFalse(p.receive(response(old,"[]")));assertFalse(p.allowed("zoom"));
  p.disconnect();assertTrue(p.allowed("zoom"));
 }
 @Test void hidesDependentMarkersAndPresetsAndProfileNames(){
  var ids=new HashSet<>(IDS);ids.addAll(List.of("local_marks","profiles","performance_hud"));
  var p=new HolyFeaturePolicy(ids);p.connect("holyworld.me");UUID id=p.request(0);
  assertTrue(p.receive(response(id,"[\"friends_options.pings\"]")));
  assertFalse(p.allowed("local_marks"));assertFalse(p.allowed("profiles"));assertFalse(p.allowed("performance_hud"));
 }
}
