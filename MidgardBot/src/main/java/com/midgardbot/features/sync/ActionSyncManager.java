package com.midgardbot.features.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ActionSyncManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionSyncManager.class);
    private static final File QUEUE_FOLDER = new File("data/sync_queue");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        if (!QUEUE_FOLDER.exists()) QUEUE_FOLDER.mkdirs();
    }

    public static class ActionItem {
        public String type; // KICK, BAN, WARN, MESSAGE
        public String uuid;
        public String reason;
        public String moderator;
        public long timestamp;

        public ActionItem(String type, String uuid, String reason, String moderator) {
            this.type = type;
            this.uuid = uuid;
            this.reason = reason;
            this.moderator = moderator;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static void queueAction(String uuid, String type, String reason, String moderator) {
        ActionItem item = new ActionItem(type, uuid, reason, moderator);
        String fileName = "action_" + System.currentTimeMillis() + "_" + uuid + ".json";
        File file = new File(QUEUE_FOLDER, fileName);
        
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(item, writer);
            LOGGER.info("Ação enfileirada: " + type + " para " + uuid);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar ação de sync", e);
        }
    }
}
