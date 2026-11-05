package xyz.rimvisuals.core;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HolyFeatureIntegrationTest {
 @Test void payloadUsesRawUtf8WithoutStringLengthPrefix(){
  var buffer=new net.minecraft.network.RegistryByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.registry.DynamicRegistryManager.EMPTY);
  try{
   String json="{\"client\":\"rimvisuals\",\"note\":\"проверка\"}";
   HolyFeatureControl.Payload.CODEC.encode(buffer,new HolyFeatureControl.Payload(json));
   assertEquals('{',buffer.getUnsignedByte(0));
   assertEquals(json,HolyFeatureControl.Payload.CODEC.decode(buffer).json());
   buffer.clear();buffer.writeZero(32769);
   assertThrows(io.netty.handler.codec.DecoderException.class,()->HolyFeatureControl.Payload.CODEC.decode(buffer));
  }finally{buffer.release();}
 }
 @Test void exportsActualCatalogueAndChecksModuleGatingWithoutChangingSavedSettings() throws Exception {
  var ids=HolyFeatureControl.catalogue();assertTrue(ids.size()>Modules.ALL.size());
  var p=new HolyFeaturePolicy(ids);p.connect("holyworld.me");
  var field=HolyFeatureControl.class.getDeclaredField("policy");field.setAccessible(true);Object previous=field.get(null);
  var access=xyz.rimvisuals.auth.AccessGate.class.getDeclaredField("access");access.setAccessible(true);Object previousAccess=access.get(null);
  var saved=Modules.snapshot();
  try{
   xyz.rimvisuals.auth.AccessGate.configure(()->true);field.set(null,p);
   Modules.ZOOM.enabled=true;assertFalse(Modules.ZOOM.isActive());
   UUID id=p.request(0);assertTrue(p.receive("{\"id\":\""+id+"\",\"ok\":true,\"payload\":{\"blocklist\":[\"zoom\",\"hw_helper.auction_price\"]}}"));
   assertFalse(Modules.ZOOM.available());assertFalse(Modules.ZOOM.isActive());assertTrue(Modules.ZOOM.enabled);
   assertFalse(Modules.HW_HELPER.available());assertFalse(Modules.HW_EVENTS.available());
   Modules.apply(saved);Modules.ZOOM.enabled=true;assertFalse(Modules.ZOOM.isActive());
   p.disconnect();assertTrue(Modules.ZOOM.isActive());
  }finally{field.set(null,previous);access.set(null,previousAccess);Modules.apply(saved);}
  var rows=new JsonArray();
  for(var m:Modules.ALL){var row=new JsonObject();row.addProperty("id",m.id);row.addProperty("name",m.name);row.addProperty("description",m.description);rows.add(row);
   for(var setting:m.settings)if(setting instanceof Setting.Bool){var option=new JsonObject();option.addProperty("id",m.id+"."+setting.key().replaceAll("[^a-z0-9_]","_"));option.addProperty("name",m.name+" / "+setting.name);option.addProperty("description","Boolean sub-option of "+m.id+"; if blocked, the complete parent module is disabled and hidden.");rows.add(option);}
  }
  for(var id:List.of("keybinds","profiles","hud_editor","client_settings")){var row=new JsonObject();row.addProperty("id",id);row.addProperty("name",id);row.addProperty("description",switch(id){case "keybinds"->"RIMVISUALS keyboard and mouse binding manager and dispatch.";case "profiles"->"Local saved settings, presets, backups, import/export and server profile binding.";case "hud_editor"->"Move, resize, recolour and align visible permitted HUD elements.";default->"Legacy local launcher settings import; no executable downloads in this mod.";});rows.add(row);}
  assertEquals(ids.size(),rows.size());
  Path output=Path.of("build/holyworld-features.json");Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(rows));
 }
}
