package com.midgardbot.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);
    private static final File FILE = new File("messages.json");
    private static final Gson GSON = new Gson();
    private static Map<String, Object> messages = new HashMap<>();

    static {
        load();
    }

    public static void load() {
        if (!FILE.exists()) {
            saveDefault();
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            messages = GSON.fromJson(reader, type);
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar messages.json", e);
        }
    }

    private static void saveDefault() {
        try (InputStream in = Messages.class.getResourceAsStream("/messages.json")) {
            if (in != null) {
                try (OutputStream out = new FileOutputStream(FILE)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao salvar messages.json padrao", e);
        }
    }

    public static String get(String path) {
        String[] parts = path.split("\\.");
        Object current = messages;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return "Missing: " + path;
            }
        }
        
        return current != null ? current.toString() : "Missing: " + path;
    }
    
    public static String get(String path, String... placeholders) {
        String msg = get(path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i+1]);
            }
        }
        return msg;
    }
}
