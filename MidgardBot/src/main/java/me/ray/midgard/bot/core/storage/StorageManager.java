package me.ray.midgard.bot.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {

    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final Path dataDir;
    private final Map<String, JsonStorage> stores = new ConcurrentHashMap<>();

    public StorageManager(Path dataDir) {
        this.dataDir = dataDir;
        dataDir.toFile().mkdirs();
    }

    public JsonStorage getStorage(String name) {
        return stores.computeIfAbsent(name, n -> {
            String fileName = n.endsWith(".json") ? n : n + ".json";
            JsonStorage storage = new JsonStorage(dataDir.resolve(fileName));
            logger.info("Loaded storage: {}", fileName);
            return storage;
        });
    }

    public void saveAll() {
        for (var entry : stores.entrySet()) {
            try {
                entry.getValue().save();
            } catch (Exception e) {
                logger.error("Failed to save storage: {}", entry.getKey(), e);
            }
        }
    }

    public void unload(String name) {
        JsonStorage storage = stores.remove(name);
        if (storage != null) {
            storage.save();
        }
    }

    public Path getDataDir() { return dataDir; }
}
