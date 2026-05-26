package com.midgard.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Wrapper for type-safe config access with defaults.
 */
public class ConfigAccessor {

    private final FileConfiguration config;

    public ConfigAccessor(FileConfiguration config) {
        this.config = config;
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public List<Integer> getIntList(String path) {
        return config.getIntegerList(path);
    }

    public boolean has(String path) {
        return config.contains(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
