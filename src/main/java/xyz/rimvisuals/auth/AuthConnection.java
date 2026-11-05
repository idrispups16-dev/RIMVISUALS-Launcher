package xyz.rimvisuals.auth;
import com.google.gson.*;
import java.net.*;
import java.nio.channels.UnresolvedAddressException;
import java.net.http.*;
import java.time.Duration;
import javax.net.ssl.SSLException;
/** Bounded auth transport. Never include bodies, credentials or raw exception messages in diagnostics. */
public final class AuthConnection {
 private AuthConnection(){}
 public static HttpClient client(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();}
 public static final class Failure extends java.io.IOException {
  public final String code;
  public Failure(String code){super(code);this.code=code;}
 }
 public static JsonObject request(HttpClient client,HttpRequest request) throws Exception {
  var response=client.send(request,HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
  {
   if(response.statusCode()!=200)throw new Failure("HTTP_"+response.statusCode());
   byte[] bytes=response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8);
   if(bytes.length>65536)throw new Failure("RESPONSE_TOO_LARGE");
   try{
    var value=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
    if(!value.isJsonObject())throw new Failure("INVALID_RESPONSE");
    return value.getAsJsonObject();
   }catch(JsonParseException e){throw new Failure("INVALID_RESPONSE");}
  }
 }
 public static String code(Throwable error){
  for(Throwable e=error;e!=null;e=e.getCause()){
   if(e instanceof Failure f)return f.code;
   if(e instanceof UnknownHostException||e instanceof UnresolvedAddressException)return "DNS";
   if(e instanceof SSLException)return "TLS";
   if(e instanceof HttpTimeoutException||e instanceof java.net.SocketTimeoutException)return "TIMEOUT";
  }
  for(Throwable e=error;e!=null;e=e.getCause())if(e instanceof ConnectException)return "CONNECT";
  return error instanceof java.io.IOException?"NETWORK":"INVALID_RESPONSE";
 }
 public static String message(Throwable e){return switch(code(e)){
  case "DNS"->"DNS: не найден адрес сервера входа.";
  case "TLS"->"TLS: не удалось проверить защищённое соединение.";
  case "TIMEOUT"->"Сервер входа не ответил вовремя. Повторите попытку.";
  case "CONNECT","NETWORK"->"Не удалось подключиться к серверу входа.";
  case "INVALID_RESPONSE","RESPONSE_TOO_LARGE"->"Сервер входа вернул неожиданный ответ.";
  default->"Ошибка сервера входа: "+code(e)+". Повторите попытку.";
 };}
}


