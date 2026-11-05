package xyz.rimvisuals;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import xyz.rimvisuals.auth.AuthConnection;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class AuthConnectionTest {
 @Test void followsHttpsCompatibleRedirectPolicyAndReadsJson() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/redirect",e->{e.getResponseHeaders().add("Location","/config");e.sendResponseHeaders(307,-1);e.close();});
  server.createContext("/config",e->{byte[] b="{\"authEnabled\":true}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});server.start();
  try{var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/redirect")).GET().build();assertTrue(AuthConnection.request(AuthConnection.client(),req).get("authEnabled").getAsBoolean());}finally{server.stop(0);}
 }
 @Test void distinguishesDnsTlsAndTimeoutWithoutExposingExceptionSecrets(){
  assertEquals("DNS",AuthConnection.code(new RuntimeException(new UnknownHostException("secret"))));
  assertEquals("TLS",AuthConnection.code(new javax.net.ssl.SSLHandshakeException("secret")));
  assertEquals("TIMEOUT",AuthConnection.code(new HttpTimeoutException("secret")));
  assertFalse(AuthConnection.message(new UnknownHostException("token=secret")).contains("secret"));
 }
 @Test void keepsHttpStatusSeparateFromNetworkFailure() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",e->{e.sendResponseHeaders(403,-1);e.close();});server.start();
  try{var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/")).GET().build();var ex=assertThrows(AuthConnection.Failure.class,()->AuthConnection.request(AuthConnection.client(),req));assertEquals("HTTP_403",ex.code);}finally{server.stop(0);}
 }
}
