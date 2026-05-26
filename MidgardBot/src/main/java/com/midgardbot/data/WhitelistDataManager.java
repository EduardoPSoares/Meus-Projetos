package com.midgardbot.data;

import com.google.gson.reflect.TypeToken;
import com.midgardbot.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.midgardbot.data.DataPersistence.*;

/**
 * Gerencia todos os dados relacionados a whitelist: status, pendências,
 * tentativas, cooldowns e histórico de ações.
 */
public final class WhitelistDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistDataManager.class);

    static final File STATUS_FILE = new File(D + "whitelist_status.json");
    static final File PENDING_FILE = new File(D + "pending_whitelists.json");
    static final File ATTEMPTS_FILE = new File(D + "user_limits.json");
    static final File COOLDOWNS_FILE = new File(D + "whitelist_cooldowns.json");
    static final File HISTORY_FILE = new File(D + "whitelist_history.json");

    private static Map<String, WhitelistStatusInfo> userStatus = new ConcurrentHashMap<>();
    private static Map<String, Map<String, String>> pendingWhitelistsData = new ConcurrentHashMap<>();
    private static Map<String, UserLimit> userLimits = new ConcurrentHashMap<>();
    private static Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static Map<String, java.util.List<WhitelistHistoryEntry>> whitelistHistory = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = Constants.MAX_WHITELIST_ATTEMPTS;
    private static final long REFILL_TIME_MS = Constants.WHITELIST_REFILL_MS;
    private static boolean limitEnabled = true;

    private WhitelistDataManager() {}

    // ===== Inicialização =====

    static void init() {
        backupFile(STATUS_FILE);
        backupFile(PENDING_FILE);
        backupFile(ATTEMPTS_FILE);
        backupFile(COOLDOWNS_FILE);
        loadStatus();
        loadPending();
        loadAttempts();
        loadCooldowns();
        loadHistory();
    }

    static void saveAllSync() {
        saveSync(STATUS_FILE, userStatus);
        saveSync(PENDING_FILE, pendingWhitelistsData);
        saveSync(ATTEMPTS_FILE, userLimits);
        saveSync(COOLDOWNS_FILE, cooldowns);
    }

    // ===== Status =====

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted, String staffId) {
        if (DatabaseManager.isConnected()) {
            try (java.sql.Connection conn = DatabaseManager.getConnection()) {
                boolean isSQLite = conn.getMetaData().getDriverName().toLowerCase().contains("sqlite");
                String sql;
                if (isSQLite) {
                    sql = "INSERT INTO midgard_whitelist (discord_id, status, nickname, answers, terms_accepted, reason, staff_id) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                          "ON CONFLICT(discord_id) DO UPDATE SET status = excluded.status, nickname = excluded.nickname, answers = excluded.answers, terms_accepted = excluded.terms_accepted, reason = excluded.reason, staff_id = excluded.staff_id";
                } else {
                    sql = "INSERT INTO midgard_whitelist (discord_id, status, nickname, answers, terms_accepted, reason, staff_id) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE status = ?, nickname = ?, answers = ?, terms_accepted = ?, reason = ?, staff_id = ?";
                }
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, userId);
                    ps.setString(2, status.name());
                    ps.setString(3, nickname);
                    ps.setString(4, answers);
                    ps.setBoolean(5, termsAccepted);
                    ps.setString(6, reason);
                    ps.setString(7, staffId);
                    if (!isSQLite) {
                        ps.setString(8, status.name());
                        ps.setString(9, nickname);
                        ps.setString(10, answers);
                        ps.setBoolean(11, termsAccepted);
                        ps.setString(12, reason);
                        ps.setString(13, staffId);
                    }
                    ps.executeUpdate();
                }
                userStatus.put(userId, new WhitelistStatusInfo(status, reason, nickname, answers, termsAccepted, staffId));
                LOGGER.info("Status de whitelist atualizado para {} (User: {}, Staff: {})", status, userId, staffId);
            } catch (Exception e) {
                LOGGER.error("Erro ao salvar status de whitelist no banco para user " + userId, e);
                userStatus.put(userId, new WhitelistStatusInfo(status, reason, nickname, answers, termsAccepted, staffId));
                saveStatus();
            }
        } else {
            userStatus.put(userId, new WhitelistStatusInfo(status, reason, nickname, answers, termsAccepted, staffId));
            saveStatus();
            LOGGER.info("Status de whitelist salvo em arquivo para {} (User: {}, Staff: {})", status, userId, staffId);
        }
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted) {
        setStatus(userId, status, reason, nickname, answers, termsAccepted, null);
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers) {
        WhitelistStatusInfo current = getStatus(userId);
        boolean terms = current != null && current.termsAccepted;
        setStatus(userId, status, reason, nickname, answers, terms);
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname) {
        setStatus(userId, status, reason, nickname, null);
    }

    public static void removeWhitelistStatus(String userId) {
        if (DatabaseManager.isConnected()) {
            try (java.sql.Connection conn = DatabaseManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_whitelist WHERE discord_id = ?")) {
                ps.setString(1, userId);
                ps.executeUpdate();
                userStatus.remove(userId);
            } catch (java.sql.SQLException e) {
                LOGGER.error("Erro ao remover whitelist do banco", e);
            }
        } else {
            userStatus.remove(userId);
            saveStatus();
        }
    }

    public static void invalidateCache(String userId) {
        userStatus.remove(userId);
    }

    public static WhitelistStatusInfo getStatus(String userId) {
        if (DatabaseManager.isConnected()) {
            if (userStatus.containsKey(userId)) {
                return userStatus.get(userId);
            }
            try (java.sql.Connection conn = DatabaseManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT status, nickname, answers, terms_accepted, reason, staff_id FROM midgard_whitelist WHERE discord_id = ?")) {
                ps.setString(1, userId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String statusStr = rs.getString("status");
                        String nickname = rs.getString("nickname");
                        String answers = rs.getString("answers");
                        boolean termsAccepted = rs.getBoolean("terms_accepted");
                        String reason = rs.getString("reason");
                        String staffId = null;
                        try { staffId = rs.getString("staff_id"); } catch (java.sql.SQLException e) { /* Coluna pode não existir */ }
                        try {
                            WhitelistStatus status = WhitelistStatus.valueOf(statusStr);
                            WhitelistStatusInfo info = new WhitelistStatusInfo(status, reason, nickname, answers, termsAccepted, staffId);
                            userStatus.put(userId, info);
                            return info;
                        } catch (IllegalArgumentException e) { /* Status inválido */ }
                    }
                }
            } catch (java.sql.SQLException e) {
                LOGGER.error("Erro ao obter status de whitelist do banco", e);
            }
            return null;
        } else {
            return userStatus.get(userId);
        }
    }

    public static Map<String, WhitelistStatusInfo> getAllStatus() {
        return new HashMap<>(userStatus);
    }

    // ===== Cooldowns =====

    public static void setCooldown(String userId, long durationMillis) {
        cooldowns.put(userId, System.currentTimeMillis() + durationMillis);
        saveCooldowns();
    }

    public static boolean isOnCooldown(String userId) {
        Long expiry = cooldowns.get(userId);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public static long getCooldownRemaining(String userId) {
        Long expiry = cooldowns.get(userId);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public static void removeCooldown(String userId) {
        cooldowns.remove(userId);
        saveCooldowns();
    }

    // ===== Limite de Tentativas =====

    public static void setLimitEnabled(boolean enabled) { limitEnabled = enabled; }
    public static boolean isLimitEnabled() { return limitEnabled; }

    public static boolean canAttemptWhitelist(String userId) {
        if (!limitEnabled) return true;
        UserLimit limit = userLimits.get(userId);
        if (limit == null) return true;
        if (limit.resetTime > 0 && System.currentTimeMillis() > limit.resetTime) {
            limit.attempts = 0;
            limit.resetTime = 0;
            saveAttempts();
            return true;
        }
        return limit.attempts < MAX_ATTEMPTS;
    }

    public static int getRemainingAttempts(String userId) {
        UserLimit limit = userLimits.get(userId);
        if (limit == null) return MAX_ATTEMPTS;
        if (limit.resetTime > 0 && System.currentTimeMillis() > limit.resetTime) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - limit.attempts);
    }

    public static void addAttempts(String userId, int amount) {
        UserLimit limit = userLimits.get(userId);
        if (limit == null) {
            userLimits.put(userId, new UserLimit(-amount, 0));
        } else {
            limit.attempts -= amount;
        }
        saveAttempts();
    }

    public static void removeAttempts(String userId, int amount) {
        UserLimit limit = userLimits.get(userId);
        if (limit == null) {
            userLimits.put(userId, new UserLimit(amount, 0));
        } else {
            limit.attempts += amount;
        }
        saveAttempts();
    }

    public static void resetAttempts(String userId) {
        userLimits.remove(userId);
        saveAttempts();
    }

    public static void registerAttempt(String userId) {
        UserLimit limit = userLimits.get(userId);
        if (limit == null) {
            limit = new UserLimit(0, 0);
            userLimits.put(userId, limit);
        }
        if (limit.resetTime > 0 && System.currentTimeMillis() > limit.resetTime) {
            limit.attempts = 0;
            limit.resetTime = 0;
        }
        limit.attempts++;
        if (limit.attempts >= MAX_ATTEMPTS) {
            limit.resetTime = System.currentTimeMillis() + REFILL_TIME_MS;
        }
        saveAttempts();
    }

    public static long getNextAttemptTime(String userId) {
        UserLimit limit = userLimits.get(userId);
        if (limit != null && limit.resetTime > 0 && limit.attempts >= MAX_ATTEMPTS) {
            if (System.currentTimeMillis() < limit.resetTime) {
                return limit.resetTime;
            }
        }
        return 0;
    }

    // ===== Pending Whitelists =====

    public static void addPendingWhitelist(String userId, Map<String, String> answers) {
        pendingWhitelistsData.put(userId, answers);
        savePending();
    }

    public static void updatePendingWhitelistMessageId(String userId, String messageId) {
        Map<String, String> data = pendingWhitelistsData.get(userId);
        if (data != null) {
            data.put("_staff_message_id", messageId);
            savePending();
        }
    }

    public static Map<String, String> getPendingWhitelist(String userId) {
        return pendingWhitelistsData.get(userId);
    }

    public static void removePendingWhitelist(String userId) {
        if (pendingWhitelistsData.remove(userId) != null) {
            savePending();
        }
    }

    public static Map<String, Map<String, String>> getAllPendingWhitelists() {
        return new HashMap<>(pendingWhitelistsData);
    }

    // ===== Histórico =====

    public static void addHistory(String userId, String staffId, String staffName, String action, String details) {
        WhitelistHistoryEntry entry = new WhitelistHistoryEntry(userId, staffId, staffName, action, details);
        whitelistHistory.computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(entry);
        saveHistory();
    }

    public static java.util.List<WhitelistHistoryEntry> getHistory(String userId) {
        return whitelistHistory.getOrDefault(userId, java.util.Collections.emptyList());
    }

    public static Map<String, java.util.List<WhitelistHistoryEntry>> getAllHistory() {
        return new HashMap<>(whitelistHistory);
    }

    // ===== Sincronização com Banco =====

    public static void syncPendingFromDatabase() {
        if (!DatabaseManager.isConnected()) return;
        LOGGER.info("Sincronizando whitelists pendentes do banco de dados...");
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT discord_id, answers FROM midgard_whitelist WHERE status = 'PENDING'")) {
            int synced = 0;
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("discord_id");
                    if (!pendingWhitelistsData.containsKey(userId)) {
                        String answersJson = rs.getString("answers");
                        if (answersJson != null && !answersJson.isEmpty()) {
                            Type type = new TypeToken<Map<String, String>>(){}.getType();
                            Map<String, String> answers = GSON.fromJson(answersJson, type);
                            pendingWhitelistsData.put(userId, answers);
                            synced++;
                        }
                    }
                }
            }
            if (synced > 0) {
                LOGGER.info("Sincronizacao concluida: " + synced + " whitelists recuperadas do banco.");
                savePending();
            } else {
                LOGGER.info("Nenhuma whitelist pendente para sincronizar.");
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao sincronizar whitelists pendentes do banco", e);
        }
    }

    public static void syncStatusFromDatabase() {
        if (!DatabaseManager.isConnected()) return;
        LOGGER.info("Sincronizando status de whitelist do banco de dados...");
        Map<String, WhitelistStatusInfo> dbStatus = new HashMap<>();
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT discord_id, status, nickname, answers, terms_accepted, reason, staff_id FROM midgard_whitelist WHERE status != 'PENDING'")) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("discord_id");
                    String statusStr = rs.getString("status");
                    String nickname = rs.getString("nickname");
                    String answers = rs.getString("answers");
                    boolean termsAccepted = rs.getBoolean("terms_accepted");
                    String reason = rs.getString("reason");
                    String staffId = rs.getString("staff_id");
                    try {
                        WhitelistStatus status = WhitelistStatus.valueOf(statusStr);
                        dbStatus.put(userId, new WhitelistStatusInfo(status, reason, nickname, answers, termsAccepted, staffId));
                    } catch (IllegalArgumentException e) { /* Status inválido no banco */ }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao ler status do banco para sincronizacao", e);
            return;
        }
        if (!dbStatus.isEmpty()) {
            userStatus.putAll(dbStatus);
            LOGGER.info("Sincronizacao de status concluida: " + dbStatus.size() + " registros atualizados do banco.");
            saveStatus();
        } else {
            LOGGER.info("Nenhum registro de whitelist encontrado no banco.");
        }
    }

    // ===== Limpeza =====

    static void clearData() {
        userStatus.clear();
        pendingWhitelistsData.clear();
        saveStatus();
        savePending();
    }

    // ===== Persistência Privada =====

    private static void loadStatus() {
        if (!STATUS_FILE.exists()) {
            LOGGER.info("Arquivo de status não encontrado. Um novo será criado ao salvar.");
            return;
        }
        Type type = new TypeToken<Map<String, WhitelistStatusInfo>>(){}.getType();
        Map<String, WhitelistStatusInfo> loaded = loadFromFile(STATUS_FILE, type);
        if (loaded != null) {
            userStatus = new ConcurrentHashMap<>(loaded);
            LOGGER.info("✅ Status carregado com sucesso. Total de registros: {}", userStatus.size());
        } else {
            LOGGER.warn("Arquivo de status vazio ou invalido.");
        }
    }

    private static void saveStatus() { persistAsync(STATUS_FILE, userStatus); }

    private static void loadPending() {
        Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
        Map<String, Map<String, String>> loaded = loadFromFile(PENDING_FILE, type);
        if (loaded != null) pendingWhitelistsData = new ConcurrentHashMap<>(loaded);
    }

    private static void savePending() { persistAsync(PENDING_FILE, pendingWhitelistsData); }

    private static void loadAttempts() {
        Type type = new TypeToken<HashMap<String, UserLimit>>(){}.getType();
        Map<String, UserLimit> loaded = loadFromFile(ATTEMPTS_FILE, type);
        userLimits = loaded != null ? new ConcurrentHashMap<>(loaded) : new ConcurrentHashMap<>();
    }

    private static void saveAttempts() { persistAsync(ATTEMPTS_FILE, userLimits); }

    private static void loadCooldowns() {
        Type type = new TypeToken<Map<String, Long>>(){}.getType();
        Map<String, Long> loaded = loadFromFile(COOLDOWNS_FILE, type);
        if (loaded != null) cooldowns = new ConcurrentHashMap<>(loaded);
    }

    private static void saveCooldowns() { persistAsync(COOLDOWNS_FILE, cooldowns); }

    private static void loadHistory() {
        Type type = new TypeToken<Map<String, java.util.List<WhitelistHistoryEntry>>>(){}.getType();
        Map<String, java.util.List<WhitelistHistoryEntry>> loaded = loadFromFile(HISTORY_FILE, type);
        if (loaded != null) whitelistHistory = new ConcurrentHashMap<>(loaded);
    }

    private static void saveHistory() { persistAsync(HISTORY_FILE, whitelistHistory); }
}
