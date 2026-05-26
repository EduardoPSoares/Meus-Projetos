package com.midgard.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple YAML configuration files.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, CustomConfig> configs = new ConcurrentHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CustomConfig getConfig(String name) {
        return configs.computeIfAbsent(name, n -> new CustomConfig(plugin, n));
    }

    public void reloadAll() {
        configs.values().forEach(CustomConfig::reload);
    }

    public void saveAll() {
        configs.values().forEach(CustomConfig::save);
    }

    public static class CustomConfig {
        private final JavaPlugin plugin;
        private final String name;
        private final File file;
        private FileConfiguration config;

        public CustomConfig(JavaPlugin plugin, String name) {
            this.plugin = plugin;
            this.name = name.endsWith(".yml") ? name : name + ".yml";
            this.file = new File(plugin.getDataFolder(), this.name);
            reload();
        }

        public FileConfiguration get() {
            return config;
        }

        public void reload() {
            if (!file.exists()) {
                if (plugin.getResource(name) != null) {
                    plugin.saveResource(name, false);
                } else {
                    try {
                        if (file.getParentFile() != null) {
                            file.getParentFile().mkdirs();
                        }
                        file.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().severe("Could not create " + name + ": " + e.getMessage());
                    }
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
        }

        public void save() {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + name + ": " + e.getMessage());
            }
        }

        public File getFile() {
            return file;
        }
    }
}
