package xyz.rimvisuals.core;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
/** One bounded asynchronous request, fixed official HTTPS endpoint; no auth or redirects. */
public final class HolyEventsApi {
 public record Result(List<HelperTracker.Event> events,String status){}
 private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"rim-hw-events");t.setDaemon(true);return t;});
 private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public static CompletableFuture<Result> fetch(String server){return CompletableFuture.supplyAsync(()->{
  try{
   if(!server.isBlank()&&!server.matches("[a-zA-Z0-9_-]{1,64}"))return new Result(List.of(),"Invalid API server filter");
   var request=HttpRequest.newBuilder(URI.create("https://api.holyworld.me/v1/events"+(server.isBlank()?"":"?server="+server))).timeout(Duration.ofSeconds(10)).header("Accept","application/json").header("User-Agent","RIMVISUALS/0.4.2-helpers.2").GET().build();
   var response=CLIENT.send(request,HttpResponse.BodyHandlers.ofInputStream());
   try(var stream=response.body()){
    if(response.statusCode()!=200)return new Result(List.of(),"API HTTP "+response.statusCode()+"; no live event data");
    byte[] bytes=stream.readNBytes(262145);if(bytes.length>262144)return new Result(List.of(),"API response too large");
    String json=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
    // Filtered responses may be an array or {events:[...]}; preserve the known selected server.
    var root=com.google.gson.JsonParser.parseString(json);
    if(!server.isBlank()&&(root.isJsonArray()||root.isJsonObject()&&root.getAsJsonObject().has("events")&&root.getAsJsonObject().get("events").isJsonArray())){
     var map=new com.google.gson.JsonObject();map.add(server,root.isJsonArray()?root:root.getAsJsonObject().get("events"));json=map.toString();
    }
    return new Result(HelperTracker.api(json,System.currentTimeMillis()),"API OK");
   }
  }catch(Exception e){return new Result(List.of(),"API unavailable: "+e.getClass().getSimpleName());}
 },IO);}
 private HolyEventsApi(){}
}
