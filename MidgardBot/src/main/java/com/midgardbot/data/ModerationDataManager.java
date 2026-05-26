package com.midgardbot.data;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
 * Gerencia dados de moderação: usuários sinalizados, blacklist,
 * bypass anti-fake e alertas de padrões.
 */
public final class ModerationDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationDataManager.class);

    static final File FLAGGED_FILE = new File(D + "flagged_users.json");
    static final File BLACKLIST_FILE = new File(D + "blacklist.json");
    static final File ANTIFAKE_BYPASS_FILE = new File(D + "antifake_bypass.json");
    static final File ALERTS_FILE = new File(D + "pattern_alerts.json");

    private static Set<String> flaggedUsers = ConcurrentHashMap.newKeySet();
    private static Set<String> blacklist = ConcurrentHashMap.newKeySet();
    private static Set<String> antiFakeBypass = ConcurrentHashMap.newKeySet();
    private static Map<String, java.util.List<PatternAlert>> patternAlerts = new ConcurrentHashMap<>();

    private ModerationDataManager() {}

    static void init() {
        backupFile(FLAGGED_FILE);
        backupFile(BLACKLIST_FILE);
        loadFlagged();
        loadBlacklist();
        loadAntiFakeBypass();
        loadAlerts();
    }

    static void saveAllSync() {
        saveSync(FLAGGED_FILE, flaggedUsers);
        saveSync(BLACKLIST_FILE, blacklist);
        saveSync(ANTIFAKE_BYPASS_FILE, antiFakeBypass);
        saveSync(ALERTS_FILE, patternAlerts);
    }

    // ===== Flagged Users =====

    public static void flagUser(String userId) {
        flaggedUsers.add(userId);
        saveFlagged();
    }

    public static void unflagUser(String userId) {
        flaggedUsers.remove(userId);
        saveFlagged();
    }

    public static boolean isFlagged(String userId) {
        return flaggedUsers.contains(userId);
    }

    // ===== Blacklist =====

    public static void addToBlacklist(String userId) {
        blacklist.add(userId);
        saveBlacklist();
    }

    public static void removeFromBlacklist(String userId) {
        blacklist.remove(userId);
        saveBlacklist();
    }

    public static boolean isBlacklisted(String userId) {
        return blacklist.contains(userId);
    }

    // ===== Anti-Fake Bypass =====

    public static void addAntiFakeBypass(String userId) {
        antiFakeBypass.add(userId);
        saveAntiFakeBypass();
    }

    public static void removeAntiFakeBypass(String userId) {
        antiFakeBypass.remove(userId);
        saveAntiFakeBypass();
    }

    public static boolean isAntiFakeBypass(String userId) {
        return antiFakeBypass.contains(userId);
    }

    // ===== Pattern Alerts =====

    public static void addAlert(String userId, String type, String severity, String message, String relatedUserId) {
        PatternAlert alert = new PatternAlert(userId, type, severity, message, relatedUserId);
        patternAlerts.computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(alert);
        saveAlerts();
    }

    public static java.util.List<PatternAlert> getAlerts(String userId) {
        return patternAlerts.getOrDefault(userId, java.util.Collections.emptyList());
    }

    public static Map<String, java.util.List<PatternAlert>> getAllAlerts() {
        return new HashMap<>(patternAlerts);
    }

    public static void clearAlerts(String userId) {
        patternAlerts.remove(userId);
        saveAlerts();
    }

    // ===== Persistência Privada =====

    private static void loadFlagged() {
        Type type = new TypeToken<java.util.HashSet<String>>(){}.getType();
        Set<String> loaded = loadFromFile(FLAGGED_FILE, type);
        if (loaded != null) {
            flaggedUsers = ConcurrentHashMap.newKeySet();
            flaggedUsers.addAll(loaded);
        } else {
            flaggedUsers = ConcurrentHashMap.newKeySet();
        }
    }

    private static void saveFlagged() { persistAsync(FLAGGED_FILE, flaggedUsers); }

    private static void loadBlacklist() {
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> loaded = loadFromFile(BLACKLIST_FILE, type);
        if (loaded != null) {
            blacklist = ConcurrentHashMap.newKeySet();
            blacklist.addAll(loaded);
        }
    }

    private static void saveBlacklist() { persistAsync(BLACKLIST_FILE, blacklist); }

    private static void loadAntiFakeBypass() {
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> loaded = loadFromFile(ANTIFAKE_BYPASS_FILE, type);
        if (loaded != null) {
            antiFakeBypass = ConcurrentHashMap.newKeySet(loaded.size());
            antiFakeBypass.addAll(loaded);
        }
    }

    private static void saveAntiFakeBypass() { persistAsync(ANTIFAKE_BYPASS_FILE, antiFakeBypass); }

    private static void loadAlerts() {
        Type type = new TypeToken<Map<String, java.util.List<PatternAlert>>>(){}.getType();
        Map<String, java.util.List<PatternAlert>> loaded = loadFromFile(ALERTS_FILE, type);
        if (loaded != null) patternAlerts = new ConcurrentHashMap<>(loaded);
    }

    private static void saveAlerts() { persistAsync(ALERTS_FILE, patternAlerts); }
}
