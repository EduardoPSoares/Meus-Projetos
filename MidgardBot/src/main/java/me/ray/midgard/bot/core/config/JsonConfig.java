package me.ray.midgard.bot.core.config;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JsonConfig {

    private static final Logger logger = LoggerFactory.getLogger(JsonConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path filePath;
    private JsonObject root;
    private boolean readOnly = false;

    public JsonConfig(Path filePath) {
        this.filePath = filePath;
        this.root = new JsonObject();
        load();
    }

    public JsonConfig(String fileName) {
        this(Path.of(fileName));
    }

    // ==================== IO ====================

    public void load() {
        if (!Files.exists(filePath)) {
            root = new JsonObject();
            return;
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(filePath), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            root = element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
            logger.debug("Loaded config: {}", filePath.getFileName());
        } catch (Exception e) {
            logger.error("Failed to load config: {}", filePath, e);
            root = new JsonObject();
        }
    }

    public void save() {
        if (readOnly) return;
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(filePath), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            logger.debug("Saved config: {}", filePath.getFileName());
        } catch (Exception e) {
            logger.error("Failed to save config: {}", filePath, e);
        }
    }

    public void reload() {
        load();
    }

    // ==================== Getters ====================

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String defaultValue) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : defaultValue;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int defaultValue) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonPrimitive() ? el.getAsInt() : defaultValue;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long defaultValue) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonPrimitive() ? el.getAsLong() : defaultValue;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double defaultValue) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonPrimitive() ? el.getAsDouble() : defaultValue;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonPrimitive() ? el.getAsBoolean() : defaultValue;
    }

    public List<String> getStringList(String path) {
        JsonElement el = resolve(path);
        if (el == null || !el.isJsonArray()) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (JsonElement item : el.getAsJsonArray()) {
            list.add(item.getAsString());
        }
        return list;
    }

    public JsonObject getObject(String path) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    public JsonArray getArray(String path) {
        JsonElement el = resolve(path);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    public <T> T get(String path, Class<T> type) {
        JsonElement el = resolve(path);
        return el != null ? GSON.fromJson(el, type) : null;
    }

    public boolean has(String path) {
        return resolve(path) != null;
    }

    // ==================== Setters ====================

    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                current.add(parts[i], new JsonObject());
            }
            current = current.getAsJsonObject(parts[i]);
        }

        String key = parts[parts.length - 1];
        current.add(key, GSON.toJsonTree(value));
    }

    public void setDefault(String path, Object value) {
        if (!has(path)) {
            set(path, value);
        }
    }

    public void remove(String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) return;
            current = current.getAsJsonObject(parts[i]);
        }

        current.remove(parts[parts.length - 1]);
    }

    // ==================== Path Resolution ====================

    private JsonElement resolve(String path) {
        String[] parts = path.split("\\.");
        JsonElement current = root;

        for (String part : parts) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }

        return current;
    }

    // ==================== Accessors ====================

    public JsonObject getRoot() { return root; }
    public Path getFilePath() { return filePath; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    @Override
    public String toString() {
        return GSON.toJson(root);
    }
}
