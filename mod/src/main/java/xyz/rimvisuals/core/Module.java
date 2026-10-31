package xyz.rimvisuals.core;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum Category { HUD, VISUALS, INTERFACE, OPTIMIZATION }

    public final String id, name, description;
    public final Category category;
    public boolean enabled = true;
    public boolean toggleable = true;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String id, String name, String description, Category category) {
        this.id = id; this.name = name; this.description = description; this.category = category;
    }

    public <T extends Setting> T add(T setting) { settings.add(setting); return setting; }
}
