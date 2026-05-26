package me.ray.midgardDiscord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private static final int CONFIG_VERSION = 3;

    private final Logger logger;
    private final File file;
    private final Gson gson;
    private Map<String, Object> messages;
    private final MiniMessage miniMessage;

    public MessagesManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.file = new File(dataDirectory.toFile(), "messages.json");
        this.gson = new Gson();
        this.miniMessage = MiniMessage.miniMessage();
        load();
    }

    public void load() {
        if (!file.exists()) {
            saveDefault();
        } else {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> existing = gson.fromJson(reader, type);
                int version = 1;
                if (existing != null && existing.containsKey("config_version")) {
                    version = ((Number) existing.get("config_version")).intValue();
                }
                if (version < CONFIG_VERSION) {
                    file.delete();
                    saveDefault();
                }
            } catch (Exception e) {
                logger.warn("Erro ao verificar versao do messages.json, regenerando...", e);
                file.delete();
                saveDefault();
            }
        }
        
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            messages = gson.fromJson(reader, type);
        } catch (Exception e) {
            logger.error("Erro ao carregar messages.json", e);
            messages = new HashMap<>();
        }
    }

    private void saveDefault() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            try (InputStream in = getClass().getResourceAsStream("/messages.json")) {
                if (in != null) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Fallback default
                    Map<String, Object> defaultMap = new HashMap<>();
                    Map<String, String> maint = new HashMap<>();
                    maint.put("kick-title", "<gold><bold>MIDGARD RPG");
                    defaultMap.put("maintenance", maint);
                    
                    try (Writer writer = new FileWriter(file)) {
                        gson.toJson(defaultMap, writer);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao salvar messages.json padrao", e);
        }
    }

    public String getRaw(String path) {
        String[] parts = path.split("\\.");
        Object current = messages;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current != null ? current.toString() : null;
    }

    public Component get(String path) {
        String raw = getRaw(path);
        if (raw == null) return Component.text("Missing message: " + path);
        return miniMessage.deserialize(raw);
    }
    
    public Component get(String path, String... placeholders) {
        String raw = getRaw(path);
        if (raw == null) return Component.text("Missing message: " + path);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                raw = raw.replace("{" + placeholders[i] + "}", placeholders[i+1]);
            }
        }
        
        return miniMessage.deserialize(raw);
    }
}
