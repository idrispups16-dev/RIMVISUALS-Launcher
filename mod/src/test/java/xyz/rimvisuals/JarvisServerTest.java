package xyz.rimvisuals;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import xyz.rimvisuals.core.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real loopback socket round trip: client line -> JarvisServer -> JarvisCommands -> Modules state. */
class JarvisServerTest {
    Path dir;
    @BeforeEach void up() throws Exception { dir = Files.createTempDirectory("jarvis"); JarvisCommands.access = () -> true; JarvisServer.start(dir); }
    @AfterEach void down() { JarvisServer.stop(); Modules.reset(Modules.HIT_PARTICLES); }

    private Socket connect(String token) throws Exception {
        var s = new Socket(InetAddress.getLoopbackAddress(), JarvisServer.port()); s.setSoTimeout(5000);
        s.getOutputStream().write(("{\"auth\":\"" + token + "\"}\n").getBytes(StandardCharsets.UTF_8)); return s;
    }
    private static String readLine(Socket s) throws Exception {
        var in = s.getInputStream(); var sb = new StringBuilder(); int c; while ((c = in.read()) != -1 && c != '\n') sb.append((char) c); return sb.toString();
    }

    @Test void ipcFileHasLoopbackPortAndToken() throws Exception {
        var o = JsonParser.parseString(Files.readString(dir.resolve("jarvis-ipc.json"))).getAsJsonObject();
        assertEquals(JarvisServer.port(), o.get("port").getAsInt());
        assertEquals(64, o.get("token").getAsString().length());
    }
    @Test void wrongTokenIsRejectedAndCommandsDoNothing() throws Exception {
        try (var s = connect("nope")) {
            assertTrue(readLine(s).contains("\"auth\""));
            s.getOutputStream().write("{\"action\":\"set_module\",\"module\":\"hit_particles\",\"enabled\":false}\n".getBytes());
            assertEquals(-1, s.getInputStream().read());
        }
    }
    @Test void authorizedCommandChangesRealSetting() throws Exception {
        try (var s = connect(JarvisServer.token())) {
            assertTrue(readLine(s).contains("hello"));
            s.getOutputStream().write("{\"action\":\"set_setting\",\"module\":\"hit_particles\",\"setting\":\"color\",\"value\":\"#A855F7\"}\n".getBytes());
            var r = JsonParser.parseString(readLine(s)).getAsJsonObject();
            assertTrue(r.get("ok").getAsBoolean(), r.toString());
            assertEquals(0xA855F7, Modules.HIT_PARTICLES.color.rgb);
            s.getOutputStream().write("{\"action\":\"rm -rf\"}\n".getBytes());
            assertEquals("unknown_action", JsonParser.parseString(readLine(s)).getAsJsonObject().get("error").getAsString());
        }
        assertTrue(Files.readString(dir.resolve("jarvis.log")).contains("set_setting"));
    }
    @Test void stopRemovesIpcFileAndClosesPort() {
        JarvisServer.stop();
        assertFalse(Files.exists(dir.resolve("jarvis-ipc.json")));
        assertFalse(JarvisServer.running());
    }
}
