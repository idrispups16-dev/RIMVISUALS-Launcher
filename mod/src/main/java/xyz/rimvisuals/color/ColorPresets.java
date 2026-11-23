package xyz.rimvisuals.color;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Built-in palettes (plain generic colour sets) and the user's own presets, stored one per line as name=spec. */
public final class ColorPresets {
    private ColorPresets() {}

    public static final LinkedHashMap<String, ColorSpec> BUILTIN = new LinkedHashMap<>();
    static {
        BUILTIN.put("Violet", ColorSpec.solid(0x8B5CF6));
        BUILTIN.put("Red", ColorSpec.solid(0xEF4444));
        BUILTIN.put("Orange", ColorSpec.solid(0xF97316));
        BUILTIN.put("Yellow", ColorSpec.solid(0xFACC15));
        BUILTIN.put("Green", ColorSpec.solid(0x22C55E));
        BUILTIN.put("Cyan", ColorSpec.solid(0x22D3EE));
        BUILTIN.put("Blue", ColorSpec.solid(0x3B82F6));
        BUILTIN.put("Pink", ColorSpec.solid(0xEC4899));
        BUILTIN.put("White", ColorSpec.solid(0xFFFFFF));
        BUILTIN.put("Warm gradient", ColorSpec.gradient(.15f, 1f, true, 0xF97316, 0xEC4899, 0x8B5CF6));
        BUILTIN.put("Cool gradient", ColorSpec.gradient(.15f, 1f, true, 0x22D3EE, 0x3B82F6, 0x8B5CF6));
        BUILTIN.put("Rainbow", ColorSpec.rainbow(.2f, 1f, .85f, 1f));
        BUILTIN.put("Violet pulse", ColorSpec.pulse(0x8B5CF6, .6f, .55f));
        BUILTIN.put("Neon cycle", ColorSpec.cycle(.25f, 0x22D3EE, 0xEC4899, 0xFACC15));
    }

    /** User presets saved atomically in a text file (name=spec). Names are trimmed and limited to 32 safe characters. */
    public static final class User {
        private final Path file;
        private final LinkedHashMap<String, ColorSpec> items = new LinkedHashMap<>();

        public User(Path file) { this.file = file; load(); }

        public static String cleanName(String n) {
            if (n == null) return "";
            String s = n.replaceAll("[^\\p{L}\\p{N} _\\-]", "").trim();
            return s.length() > 32 ? s.substring(0, 32).trim() : s;
        }

        public Map<String, ColorSpec> all() { return Collections.unmodifiableMap(items); }

        public boolean put(String name, ColorSpec spec) {
            String n = cleanName(name);
            if (n.isEmpty() || spec == null || BUILTIN.containsKey(n) || (items.size() >= 64 && !items.containsKey(n))) return false;
            items.put(n, spec);
            return save();
        }

        public boolean remove(String name) { boolean r = items.remove(cleanName(name)) != null; if (r) save(); return r; }

        private void load() {
            items.clear();
            if (!Files.isRegularFile(file)) return;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    int eq = line.indexOf('='); if (eq <= 0) continue;
                    String n = cleanName(line.substring(0, eq)); ColorSpec s = ColorSpec.parse(line.substring(eq + 1));
                    if (!n.isEmpty() && s != null && items.size() < 64) items.put(n, s);
                }
            } catch (IOException | RuntimeException ignored) { /* a damaged presets file must never break the game */ }
        }

        private boolean save() {
            try {
                Files.createDirectories(file.toAbsolutePath().getParent());
                StringBuilder sb = new StringBuilder();
                items.forEach((k, v) -> sb.append(k).append('=').append(v.serialize()).append('\n'));
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) { return false; }
        }
    }
}
