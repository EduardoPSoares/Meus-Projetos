package me.ray.midgard.bot.core.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonStorage {

    private static final Logger logger = LoggerFactory.getLogger(JsonStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path filePath;
    private final Map<String, JsonElement> data = new ConcurrentHashMap<>();
    private boolean dirty = false;

    public JsonStorage(Path filePath) {
        this.filePath = filePath;
        load();
    }

    // ==================== IO ====================

    public void load() {
        if (!Files.exists(filePath)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(filePath), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                data.clear();
                for (var entry : element.getAsJsonObject().entrySet()) {
                    data.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load storage: {}", filePath, e);
        }
    }

    public void save() {
        if (!dirty) return;
        try {
            Files.createDirectories(filePath.getParent());
            JsonObject obj = new JsonObject();
            for (var entry : data.entrySet()) {
                obj.add(entry.getKey(), entry.getValue());
            }
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(filePath), StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
            dirty = false;
        } catch (Exception e) {
            logger.error("Failed to save storage: {}", filePath, e);
        }
    }

    public void forceSave() {
        dirty = true;
        save();
    }

    // ==================== Operations ====================

    public <T> void set(String key, T value) {
        data.put(key, GSON.toJsonTree(value));
        dirty = true;
    }

    public <T> T get(String key, Class<T> type) {
        JsonElement el = data.get(key);
        return el != null ? GSON.fromJson(el, type) : null;
    }

    public <T> T get(String key, Type type) {
        JsonElement el = data.get(key);
        return el != null ? GSON.fromJson(el, type) : null;
    }

    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }

    public <T> List<T> getList(String key, Class<T> elementType) {
        JsonElement el = data.get(key);
        if (el == null || !el.isJsonArray()) return new ArrayList<>();
        Type listType = TypeToken.getParameterized(List.class, elementType).getType();
        return GSON.fromJson(el, listType);
    }

    public <T> Map<String, T> getMap(String key, Class<T> valueType) {
        JsonElement el = data.get(key);
        if (el == null || !el.isJsonObject()) return new HashMap<>();
        Type mapType = TypeToken.getParameterized(Map.class, String.class, valueType).getType();
        return GSON.fromJson(el, mapType);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void remove(String key) {
        if (data.remove(key) != null) {
            dirty = true;
        }
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        data.clear();
        dirty = true;
    }

    public boolean isDirty() { return dirty; }
}
