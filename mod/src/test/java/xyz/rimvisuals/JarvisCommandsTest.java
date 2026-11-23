package xyz.rimvisuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import xyz.rimvisuals.core.*;
import static org.junit.jupiter.api.Assertions.*;

class JarvisCommandsTest {
    private static JsonObject call(String json) { return JarvisCommands.handle(JsonParser.parseString(json).getAsJsonObject()); }
    @BeforeEach void open() { JarvisCommands.access = () -> true; }
    @AfterEach void restore() { Modules.reset(Modules.CRYSTAL_FADE); }

    @Test void unknownActionAndAccessGate() {
        assertEquals("unknown_action", call("{\"action\":\"exec\"}").get("error").getAsString());
        JarvisCommands.access = () -> false;
        assertEquals("access_denied", call("{\"action\":\"set_module\",\"module\":\"crystal_fade\",\"enabled\":true}").get("error").getAsString());
        assertTrue(call("{\"action\":\"ping\"}").get("ok").getAsBoolean());
    }
    @Test void setModuleReallyChangesState() {
        Modules.CRYSTAL_FADE.enabled = false;
        var r = call("{\"action\":\"set_module\",\"module\":\"crystal_fade\",\"enabled\":true}");
        assertTrue(r.get("ok").getAsBoolean());
        assertTrue(Modules.CRYSTAL_FADE.enabled);
        call("{\"action\":\"set_module\",\"module\":\"crystal_fade\",\"enabled\":false}");
        assertFalse(Modules.CRYSTAL_FADE.enabled);
    }
    @Test void colorNumberModeValidation() {
        var r = call("{\"action\":\"set_setting\",\"module\":\"hit_particles\",\"setting\":\"color\",\"value\":\"#A855F7\"}");
        assertTrue(r.get("ok").getAsBoolean(), r.toString());
        assertEquals(0xA855F7, Modules.HIT_PARTICLES.color.rgb);
        assertEquals("bad_value", call("{\"action\":\"set_setting\",\"module\":\"hit_particles\",\"setting\":\"color\",\"value\":\"red;rm\"}").get("error").getAsString());
        call("{\"action\":\"set_setting\",\"module\":\"hit_particles\",\"setting\":\"particles_per_hit\",\"value\":9999}");
        assertTrue(Modules.HIT_PARTICLES.count.value <= Modules.HIT_PARTICLES.count.max);
        assertEquals("no_setting", call("{\"action\":\"set_setting\",\"module\":\"hit_particles\",\"setting\":\"nope\",\"value\":1}").get("error").getAsString());
    }
    @Test void textSettingsAreNeverRemotelyWritable() {
        var before = Modules.HW_HELPER.eventPrefix.value;
        var r = call("{\"action\":\"set_setting\",\"module\":\"hw_helper\",\"setting\":\"event_prefix\",\"value\":\"x\"}");
        assertFalse(r.get("ok").getAsBoolean());
        assertEquals(before, Modules.HW_HELPER.eventPrefix.value);
    }
    @Test void dangerousNeedsConfirmation() {
        assertEquals("confirmation_required", call("{\"action\":\"apply_preset\",\"name\":\"PvP_Clean\"}").get("error").getAsString());
        assertEquals("confirmation_required", call("{\"action\":\"reset_module\",\"module\":\"crystal_fade\"}").get("error").getAsString());
        Modules.CRYSTAL_FADE.opacity.set(.5);
        assertTrue(call("{\"action\":\"reset_module\",\"module\":\"crystal_fade\",\"confirmed\":true}").get("ok").getAsBoolean());
        assertNotEquals(.5, Modules.CRYSTAL_FADE.opacity.value);
    }
    @Test void performanceModeAndQueries() {
        var r = call("{\"action\":\"performance_mode\",\"mode\":\"Balanced\"}");
        assertTrue(r.get("ok").getAsBoolean());
        assertEquals(1, Modules.PERFORMANCE.preset.index);
        JarvisCommands.fps = () -> 41;
        assertEquals(41, call("{\"action\":\"query\",\"what\":\"fps\"}").get("fps").getAsInt());
        assertTrue(call("{\"action\":\"query\",\"what\":\"cooldowns\"}").get("ok").getAsBoolean());
        assertEquals("bad_value", call("{\"action\":\"query\",\"what\":\"/etc/passwd\"}").get("error").getAsString());
        var mods = call("{\"action\":\"list_modules\"}").getAsJsonArray("modules");
        assertTrue(mods.size() > 30);
    }
}
