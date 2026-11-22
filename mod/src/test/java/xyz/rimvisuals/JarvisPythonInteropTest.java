package xyz.rimvisuals;

import org.junit.jupiter.api.*;
import xyz.rimvisuals.core.*;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Cross-language proof: the REAL Python launcher client talks to the REAL Java bridge over a loopback socket, and the
 * mod's Modules state changes. (No Minecraft window; the in-game effect itself is checked by hand.)
 */
class JarvisPythonInteropTest {
    @Test void pythonAssistantChangesModuleState() throws Exception {
        Path py = Path.of("..", "python-launcher").toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(py.resolve("rimvisuals_launcher")), "python-launcher not found");
        String exe = null;
        for (String c : new String[]{"python3", "python"}) {
            try { if (new ProcessBuilder(c, "--version").start().waitFor(10, TimeUnit.SECONDS)) { exe = c; break; } } catch (Exception ignored) {}
        }
        assumeTrue(exe != null, "no python on PATH");
        Path game = Files.createTempDirectory("jarvis-interop");
        Modules.CRYSTAL_FADE.enabled = false;
        JarvisCommands.access = () -> true;
        JarvisServer.start(game.resolve("config/rimvisuals"));
        try {
            String code = "import sys; sys.path.insert(0, r'" + py + "')\n"
                    + "from rimvisuals_launcher.jarvis.assistant import Assistant\n"
                    + "from rimvisuals_launcher.jarvis.ipc import JarvisClient\n"
                    + "a = Assistant(JarvisClient(r'" + game + "'))\n"
                    + "r1 = a.ask('Джарвис, включи Crystal Fade')\n"
                    + "r2 = a.ask('Джарвис, поставь фиолетовые частицы')\n"
                    + "print('R1', r1.executed, r1.text)\nprint('R2', r2.executed, r2.text)\n";
            var pb = new ProcessBuilder(exe, "-I", "-c", code).redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            var p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(p.waitFor(60, TimeUnit.SECONDS), out);
            assertTrue(out.contains("R1 True"), out);
            assertTrue(out.contains("R2 True"), out);
            assertTrue(Modules.CRYSTAL_FADE.enabled, "Crystal Fade must be enabled by the Python command");
            assertEquals(0x8B5CF6, Modules.HIT_PARTICLES.color.rgb);
            assertEquals(0x8B5CF6, Modules.CRIT_PARTICLES.color.rgb);
        } finally { JarvisServer.stop(); Modules.reset(Modules.CRYSTAL_FADE); Modules.reset(Modules.HIT_PARTICLES); Modules.reset(Modules.CRIT_PARTICLES); }
    }
}
