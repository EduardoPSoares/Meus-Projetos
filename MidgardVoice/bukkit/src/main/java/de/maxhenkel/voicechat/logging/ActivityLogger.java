package de.maxhenkel.voicechat.logging;

import de.maxhenkel.voicechat.Voicechat;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File logFile;

    public ActivityLogger() {
        this.logFile = new File(Voicechat.INSTANCE.getDataFolder(), "activity.log");
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = "[" + timestamp + "] " + message;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            Voicechat.LOGGER.error("Failed to write to activity log", e);
        }
    }

    public void logGlobalAdded(String adminName, String playerName) {
        log("GLOBAL_ADD: " + adminName + " adicionou " + playerName + " como voz global");
    }

    public void logGlobalRemoved(String adminName, String playerName) {
        log("GLOBAL_REMOVE: " + adminName + " removeu " + playerName + " da voz global");
    }

    public void logRangeSet(String adminName, String playerName, float distance) {
        log("RANGE_SET: " + adminName + " definiu range de " + playerName + " para " + distance + " blocos");
    }

    public void logRangeRemoved(String adminName, String playerName) {
        log("RANGE_REMOVE: " + adminName + " removeu range customizado de " + playerName);
    }

    public void logZoneCreated(String adminName, String zoneName) {
        log("ZONE_CREATE: " + adminName + " criou a zona '" + zoneName + "'");
    }

    public void logZoneDeleted(String adminName, String zoneName) {
        log("ZONE_DELETE: " + adminName + " deletou a zona '" + zoneName + "'");
    }

    public void logZoneVoiceToggled(String adminName, String zoneName, boolean enabled) {
        log("ZONE_VOICE_TOGGLE: " + adminName + " " + (enabled ? "ativou" : "desativou") + " voz na zona '" + zoneName + "'");
    }

    public void logZonePlayerAdded(String adminName, String playerName, String zoneName, String listType) {
        log("ZONE_PLAYER_ADD: " + adminName + " adicionou " + playerName + " a lista de " + listType + " da zona '" + zoneName + "'");
    }

    public void logZonePlayerRemoved(String adminName, String playerName, String zoneName, String listType) {
        log("ZONE_PLAYER_REMOVE: " + adminName + " removeu " + playerName + " da lista de " + listType + " da zona '" + zoneName + "'");
    }

}
