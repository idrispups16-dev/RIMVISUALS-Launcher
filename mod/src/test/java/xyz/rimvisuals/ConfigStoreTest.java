package xyz.rimvisuals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.rimvisuals.core.ConfigStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path dir;

    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) p.setProperty(kv[i], kv[i + 1]);
        return p;
    }

    @Test void saveLoadListDelete() {
        ConfigStore s = new ConfigStore(dir);
        assertTrue(s.save("PvP", props("a.b", "1")));
        assertEquals("1", s.load("PvP").orElseThrow().getProperty("a.b"));
        assertEquals(java.util.List.of("PvP"), s.list());
        assertTrue(s.delete("PvP"));
        assertTrue(s.list().isEmpty());
        assertFalse(s.save("../..", props("x", "1")), "empty after cleaning");
    }

    @Test void nameIsSanitisedAgainstTraversal() {
        assertEquals("evil", ConfigStore.clean("../evil"));
        assertEquals(24, ConfigStore.clean("a".repeat(40)).length());
        ConfigStore s = new ConfigStore(dir);
        s.save("../escape", props("x", "1"));
        assertTrue(Files.exists(dir.resolve("profiles").resolve("escape.properties")));
        assertFalse(Files.exists(dir.resolve("escape.properties")));
    }

    @Test void duplicateAndRename() {
        ConfigStore s = new ConfigStore(dir);
        s.save("one", props("k", "v"));
        assertTrue(s.duplicate("one", "two"));
        assertFalse(s.duplicate("one", "two"), "target exists");
        assertTrue(s.rename("two", "three"));
        assertFalse(s.exists("two"));
        assertEquals("v", s.load("three").orElseThrow().getProperty("k"));
        assertFalse(s.rename("one", "three"), "target exists");
        assertFalse(s.rename("missing", "x"));
    }

    @Test void jsonRoundTrip() throws Exception {
        ConfigStore s = new ConfigStore(dir);
        s.save("exp", props("hit_marker.enabled", "true", "hud.scale", "1.25"));
        Path json = s.exportJson("exp");
        assertTrue(Files.readString(json).contains("\"hit_marker.enabled\":\"true\""));
        s.delete("exp");
        String name = s.importJson(json).orElseThrow();
        assertEquals("exp", name);
        assertEquals("1.25", s.load(name).orElseThrow().getProperty("hud.scale"));
        assertEquals("exp_2", s.importJson(json).orElseThrow(), "import never overwrites");
    }

    @Test void importRejectsBadInput() throws Exception {
        ConfigStore s = new ConfigStore(dir);
        Path f = dir.resolve("bad.json");
        for (String body : new String[]{"not json", "[]", "{}", "{\"settings\":{}}", "{\"settings\":{\"Bad Key!\":\"1\"}}",
                "{\"settings\":{\"ok\":{\"nested\":1}}}", "{\"settings\":{\"ok\":\"" + "x".repeat(300) + "\"}}"}) {
            Files.writeString(f, body);
            assertTrue(s.importJson(f).isEmpty(), body);
        }
        assertTrue(s.importJson(dir.resolve("missing.json")).isEmpty());
        assertTrue(s.list().isEmpty());
    }

    @Test void backupsAreRotated() throws Exception {
        ConfigStore s = new ConfigStore(dir);
        for (int i = 0; i < ConfigStore.MAX_BACKUPS + 4; i++) { s.backup(props("n", "" + i)); Thread.sleep(3); }
        assertEquals(ConfigStore.MAX_BACKUPS, s.backupFiles().size());
        assertEquals("" + (ConfigStore.MAX_BACKUPS + 3), s.latestBackup().orElseThrow().getProperty("n"));
    }

    @Test void createNeverOverwritesAndRenamePreservesServerBinding() {
        var s=new ConfigStore(dir);
        assertTrue(s.create("old",props("k","first")));
        assertFalse(s.create("old",props("k","lost")));
        assertTrue(s.bindServer("example.com","old"));
        assertTrue(s.rename("old","new"));
        assertEquals("first",s.load("new").orElseThrow().getProperty("k"));
        assertEquals("new",s.serverProfile("example.com").orElseThrow());
    }

    @Test void rejectsUnknownSchemaButAcceptsLegacyJson() throws Exception {
        var s=new ConfigStore(dir);Path f=dir.resolve("old.json");
        Files.writeString(f,"{\"name\":\"old\",\"settings\":{\"hud.pos_x\":\"3\"}}");
        assertTrue(s.importJson(f).isPresent());
        Files.writeString(f,"{\"schema\":2,\"name\":\"next\",\"settings\":{\"hud.pos_x\":\"4\"}}");
        assertTrue(s.importJson(f).isEmpty());
    }

    @Test void atomicSaveLeavesNoTemporaryFiles() throws Exception {
        var s=new ConfigStore(dir);assertTrue(s.save("p",props("a","1")));assertTrue(s.save("p",props("a","2")));
        assertEquals("2",s.load("p").orElseThrow().getProperty("a"));
        try(var files=Files.list(s.profiles())){assertEquals(1,files.count());}
    }

    @Test void serverBindings() {
        ConfigStore s = new ConfigStore(dir);
        s.save("pvp", props("k", "v"));
        assertFalse(s.bindServer("play.example.com", "missing"));
        assertTrue(s.bindServer(" Play.Example.com ", "pvp"));
        assertEquals("pvp", s.serverProfile("play.example.com").orElseThrow());
        s.delete("pvp");
        assertTrue(s.serverProfile("play.example.com").isEmpty(), "binding to deleted profile is ignored");
        s.save("pvp", props("k", "v"));
        assertTrue(s.unbindServer("play.example.com"));
        assertTrue(s.serverProfile("play.example.com").isEmpty());
    }
}
