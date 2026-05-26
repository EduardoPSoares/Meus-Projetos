package com.midgardbot.data;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;
import static com.midgardbot.data.DataPersistence.D;
import static com.midgardbot.data.DataPersistence.backupFile;
import static com.midgardbot.data.DataPersistence.loadFromFile;
import static com.midgardbot.data.DataPersistence.persistAsync;
import static com.midgardbot.data.DataPersistence.saveSync;

/**
 * Gerencia o estado de configuração do bot: modo manutenção,
 * whitelist habilitada e manutenção por servidor.
 */
public final class BotStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotStateManager.class);

    static final File CONFIG_FILE = new File(D + "bot_config.json");
    static final File MAINTENANCE_FILE = new File(D + "maintenance_status.json");

    private static volatile boolean maintenanceMode = false;
    private static volatile boolean whitelistEnabled = true;
    private static Map<String, MaintenanceInfo> maintenanceStatus = new ConcurrentHashMap<>();

    private BotStateManager() {}

    static void init() {
        backupFile(CONFIG_FILE);
        loadConfig();
        loadMaintenance();
    }

    static void saveAllSync() {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("maintenanceMode", maintenanceMode);
        config.put("whitelistEnabled", whitelistEnabled);
        saveSync(CONFIG_FILE, config);
    }

    // ===== Maintenance Mode (Global) =====

    public static void setMaintenanceMode(boolean enabled) {
        maintenanceMode = enabled;
        saveConfig();
    }

    public static boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    // ===== Whitelist Toggle =====

    public static void setWhitelistEnabled(boolean enabled) {
        whitelistEnabled = enabled;
        saveConfig();
    }

    public static boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    // ===== Maintenance Per-Server =====

    public static void setMaintenance(String server, boolean enabled, String user) {
        maintenanceStatus.put(server.toLowerCase(), new MaintenanceInfo(enabled, user));
        saveMaintenance();
    }

    public static boolean isMaintenance(String server) {
        MaintenanceInfo info = maintenanceStatus.get(server.toLowerCase());
        return info != null && info.enabled;
    }

    // ===== Persistência Privada =====

    private static void loadConfig() {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> loaded = loadFromFile(CONFIG_FILE, type);
        if (loaded != null) {
            if (loaded.containsKey("maintenanceMode")) {
                maintenanceMode = (boolean) loaded.get("maintenanceMode");
            }
            if (loaded.containsKey("whitelistEnabled")) {
                whitelistEnabled = (boolean) loaded.get("whitelistEnabled");
            }
        }
    }

    static void saveConfig() {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("maintenanceMode", maintenanceMode);
        config.put("whitelistEnabled", whitelistEnabled);
        persistAsync(CONFIG_FILE, config);
    }

    private static void loadMaintenance() {
        Type type = new TypeToken<Map<String, MaintenanceInfo>>(){}.getType();
        Map<String, MaintenanceInfo> loaded = loadFromFile(MAINTENANCE_FILE, type);
        if (loaded != null) maintenanceStatus = new ConcurrentHashMap<>(loaded);
    }

    private static void saveMaintenance() { persistAsync(MAINTENANCE_FILE, maintenanceStatus); }
}
