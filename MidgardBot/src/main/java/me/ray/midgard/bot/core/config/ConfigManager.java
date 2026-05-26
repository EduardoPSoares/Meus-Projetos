package me.ray.midgard.bot.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private final Path configDir;
    private final Map<String, JsonConfig> configs = new ConcurrentHashMap<>();

    public ConfigManager(Path configDir) {
        this.configDir = configDir;
        configDir.toFile().mkdirs();
    }

    public JsonConfig getConfig(String name) {
        return configs.computeIfAbsent(name, n -> {
            String fileName = n.endsWith(".json") ? n : n + ".json";
            JsonConfig config = new JsonConfig(configDir.resolve(fileName));
            logger.info("Loaded config: {}", fileName);
            return config;
        });
    }

    public JsonConfig getOrCreate(String name, Runnable defaultsInitializer) {
        boolean isNew = !configs.containsKey(name);
        JsonConfig config = getConfig(name);
        if (isNew && config.getRoot().size() == 0) {
            defaultsInitializer.run();
            config.save();
        }
        return config;
    }

    public void saveAll() {
        // Configs are read-only - never overwrite files edited manually
    }

    public void reloadAll() {
        for (var entry : configs.entrySet()) {
            try {
                entry.getValue().reload();
                logger.info("Reloaded config: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to reload config: {}", entry.getKey(), e);
            }
        }
    }

    public void unload(String name) {
        configs.remove(name);
    }

    public Path getConfigDir() { return configDir; }
}
