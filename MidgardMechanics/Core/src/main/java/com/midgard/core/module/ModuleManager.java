package com.midgard.core.module;

import com.midgard.core.MidgardCore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class ModuleManager {

    private final MidgardCore core;
    private final Map<String, MidgardModule> modules = new LinkedHashMap<>();

    public ModuleManager(MidgardCore core) {
        this.core = core;
    }

    public void registerModule(MidgardModule module) {
        if (modules.containsKey(module.getName())) {
            core.getLogger().warning("Module " + module.getName() + " is already registered!");
            return;
        }
        modules.put(module.getName(), module);
        try {
            module.enable();
        } catch (Exception e) {
            core.getLogger().severe("Failed to enable module " + module.getName() + ": " + e.getMessage());
            core.getLogger().log(Level.SEVERE, "Stack trace:", e);
            modules.remove(module.getName());
        }
    }

    public void unregisterModule(String name) {
        MidgardModule module = modules.remove(name);
        if (module != null) {
            try {
                module.disable();
            } catch (Exception e) {
                core.getLogger().severe("Failed to disable module " + name + ": " + e.getMessage());
                core.getLogger().log(Level.SEVERE, "Stack trace:", e);
            }
        }
    }

    public MidgardModule getModule(String name) {
        return modules.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends MidgardModule> T getModule(String name, Class<T> type) {
        MidgardModule module = modules.get(name);
        if (module != null && type.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    public boolean isModuleEnabled(String name) {
        MidgardModule module = modules.get(name);
        return module != null && module.isEnabled();
    }

    public void disableAll() {
        modules.values().forEach(module -> {
            try {
                module.disable();
            } catch (Exception e) {
                core.getLogger().severe("Failed to disable module " + module.getName() + ": " + e.getMessage());
                core.getLogger().log(Level.SEVERE, "Stack trace:", e);
            }
        });
    }

    public Map<String, MidgardModule> getModules() {
        return Collections.unmodifiableMap(modules);
    }
}
