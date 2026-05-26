package com.midgardbot.data;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Fachada central para acesso a dados do bot.
 * Delega para sub-managers especializados:
 * <ul>
 *   <li>{@link WhitelistDataManager} — status, pendências, tentativas, cooldowns, histórico</li>
 *   <li>{@link StaffDataManager} — estatísticas e feedback de staff</li>
 *   <li>{@link ModerationDataManager} — flagged, blacklist, anti-fake, alertas</li>
 *   <li>{@link BotStateManager} — config, manutenção</li>
 *   <li>{@link BackupService} — backups automáticos</li>
 * </ul>
 */
public class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataManager.class);
    private static final File LINKED_ACCOUNTS_FILE = new File(DataPersistence.D + "linked_accounts.json");

    static {
        WhitelistDataManager.init();
        StaffDataManager.init();
        ModerationDataManager.init();
        BotStateManager.init();
        BackupService.startAutoBackup();
    }

    public static void setJDA(JDA jda) {
        BackupService.setJDA(jda);
    }

    // ===== Salvamento Sincronizado =====

    public static void saveAllSync() {
        LOGGER.info("Salvando todos os dados (Sincronizado)...");
        WhitelistDataManager.saveAllSync();
        StaffDataManager.saveAllSync();
        ModerationDataManager.saveAllSync();
        BotStateManager.saveAllSync();
        LOGGER.info("Dados salvos com sucesso.");
    }

    // ===== Limpeza Total de Whitelist =====

    public static void clearAllWhitelistData() {
        LOGGER.warn("INICIANDO LIMPEZA TOTAL DE DADOS DE WHITELIST...");
        WhitelistDataManager.clearData();
        if (LINKED_ACCOUNTS_FILE.exists()) {
            try (Writer writer = new FileWriter(LINKED_ACCOUNTS_FILE)) {
                writer.write("{}");
                LOGGER.info("Arquivo de contas vinculadas resetado.");
            } catch (IOException e) {
                LOGGER.error("Erro ao limpar contas vinculadas", e);
            }
        }
        LOGGER.info("Todos os dados de whitelist e vinculos foram apagados.");
    }

    // ===== Delegação: Whitelist Status =====

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted, String staffId) {
        WhitelistDataManager.setStatus(userId, status, reason, nickname, answers, termsAccepted, staffId);
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted) {
        WhitelistDataManager.setStatus(userId, status, reason, nickname, answers, termsAccepted);
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname, String answers) {
        WhitelistDataManager.setStatus(userId, status, reason, nickname, answers);
    }

    public static void setStatus(String userId, WhitelistStatus status, String reason, String nickname) {
        WhitelistDataManager.setStatus(userId, status, reason, nickname);
    }

    public static WhitelistStatusInfo getStatus(String userId) {
        return WhitelistDataManager.getStatus(userId);
    }

    public static void removeWhitelistStatus(String userId) {
        WhitelistDataManager.removeWhitelistStatus(userId);
    }

    public static void invalidateCache(String userId) {
        WhitelistDataManager.invalidateCache(userId);
    }

    public static Map<String, WhitelistStatusInfo> getAllStatus() {
        return WhitelistDataManager.getAllStatus();
    }

    // ===== Delegação: Cooldowns =====

    public static void setCooldown(String userId, long durationMillis) {
        WhitelistDataManager.setCooldown(userId, durationMillis);
    }

    public static boolean isOnCooldown(String userId) {
        return WhitelistDataManager.isOnCooldown(userId);
    }

    public static long getCooldownRemaining(String userId) {
        return WhitelistDataManager.getCooldownRemaining(userId);
    }

    public static void removeCooldown(String userId) {
        WhitelistDataManager.removeCooldown(userId);
    }

    // ===== Delegação: Limites =====

    public static void setLimitEnabled(boolean enabled) {
        WhitelistDataManager.setLimitEnabled(enabled);
    }

    public static boolean isLimitEnabled() {
        return WhitelistDataManager.isLimitEnabled();
    }

    public static boolean canAttemptWhitelist(String userId) {
        return WhitelistDataManager.canAttemptWhitelist(userId);
    }

    public static int getRemainingAttempts(String userId) {
        return WhitelistDataManager.getRemainingAttempts(userId);
    }

    public static void addAttempts(String userId, int amount) {
        WhitelistDataManager.addAttempts(userId, amount);
    }

    public static void removeAttempts(String userId, int amount) {
        WhitelistDataManager.removeAttempts(userId, amount);
    }

    public static void resetAttempts(String userId) {
        WhitelistDataManager.resetAttempts(userId);
    }

    public static void registerAttempt(String userId) {
        WhitelistDataManager.registerAttempt(userId);
    }

    public static long getNextAttemptTime(String userId) {
        return WhitelistDataManager.getNextAttemptTime(userId);
    }

    // ===== Delegação: Pending Whitelists =====

    public static void addPendingWhitelist(String userId, Map<String, String> answers) {
        WhitelistDataManager.addPendingWhitelist(userId, answers);
    }

    public static void updatePendingWhitelistMessageId(String userId, String messageId) {
        WhitelistDataManager.updatePendingWhitelistMessageId(userId, messageId);
    }

    public static Map<String, String> getPendingWhitelist(String userId) {
        return WhitelistDataManager.getPendingWhitelist(userId);
    }

    public static void removePendingWhitelist(String userId) {
        WhitelistDataManager.removePendingWhitelist(userId);
    }

    public static Map<String, Map<String, String>> getAllPendingWhitelists() {
        return WhitelistDataManager.getAllPendingWhitelists();
    }

    // ===== Delegação: Histórico =====

    public static void addHistory(String userId, String staffId, String staffName, String action, String details) {
        WhitelistDataManager.addHistory(userId, staffId, staffName, action, details);
    }

    public static java.util.List<WhitelistHistoryEntry> getHistory(String userId) {
        return WhitelistDataManager.getHistory(userId);
    }

    public static Map<String, java.util.List<WhitelistHistoryEntry>> getAllHistory() {
        return WhitelistDataManager.getAllHistory();
    }

    // ===== Delegação: Sync =====

    public static void syncPendingFromDatabase() {
        WhitelistDataManager.syncPendingFromDatabase();
    }

    public static void syncStatusFromDatabase() {
        WhitelistDataManager.syncStatusFromDatabase();
    }

    // ===== Delegação: Staff =====

    public static void incrementStaffApproval(String staffId) {
        StaffDataManager.incrementStaffApproval(staffId);
    }

    public static void incrementStaffRejection(String staffId) {
        StaffDataManager.incrementStaffRejection(staffId);
    }

    public static void incrementTicketStats(String staffId, boolean claimed) {
        StaffDataManager.incrementTicketStats(staffId, claimed);
    }

    public static Map<String, StaffStats> getStaffStats() {
        return StaffDataManager.getStaffStats();
    }

    public static void addStaffFeedback(String staffId, String userId, int rating, String comment) {
        StaffDataManager.addStaffFeedback(staffId, userId, rating, comment);
    }

    public static java.util.List<StaffFeedback> getFeedbacksForStaff(String staffId) {
        return StaffDataManager.getFeedbacksForStaff(staffId);
    }

    public static Map<String, java.util.List<StaffFeedback>> getAllStaffFeedbacks() {
        return StaffDataManager.getAllStaffFeedbacks();
    }

    // ===== Delegação: Moderação =====

    public static void flagUser(String userId) {
        ModerationDataManager.flagUser(userId);
    }

    public static void unflagUser(String userId) {
        ModerationDataManager.unflagUser(userId);
    }

    public static boolean isFlagged(String userId) {
        return ModerationDataManager.isFlagged(userId);
    }

    public static void addToBlacklist(String userId) {
        ModerationDataManager.addToBlacklist(userId);
    }

    public static void removeFromBlacklist(String userId) {
        ModerationDataManager.removeFromBlacklist(userId);
    }

    public static boolean isBlacklisted(String userId) {
        return ModerationDataManager.isBlacklisted(userId);
    }

    public static void addAntiFakeBypass(String userId) {
        ModerationDataManager.addAntiFakeBypass(userId);
    }

    public static void removeAntiFakeBypass(String userId) {
        ModerationDataManager.removeAntiFakeBypass(userId);
    }

    public static boolean isAntiFakeBypass(String userId) {
        return ModerationDataManager.isAntiFakeBypass(userId);
    }

    public static void addAlert(String userId, String type, String severity, String message, String relatedUserId) {
        ModerationDataManager.addAlert(userId, type, severity, message, relatedUserId);
    }

    public static java.util.List<PatternAlert> getAlerts(String userId) {
        return ModerationDataManager.getAlerts(userId);
    }

    public static Map<String, java.util.List<PatternAlert>> getAllAlerts() {
        return ModerationDataManager.getAllAlerts();
    }

    public static void clearAlerts(String userId) {
        ModerationDataManager.clearAlerts(userId);
    }

    // ===== Delegação: Estado do Bot =====

    public static void setMaintenanceMode(boolean enabled) {
        BotStateManager.setMaintenanceMode(enabled);
    }

    public static boolean isMaintenanceMode() {
        return BotStateManager.isMaintenanceMode();
    }

    public static void setWhitelistEnabled(boolean enabled) {
        BotStateManager.setWhitelistEnabled(enabled);
    }

    public static boolean isWhitelistEnabled() {
        return BotStateManager.isWhitelistEnabled();
    }

    public static void setMaintenance(String server, boolean enabled, String user) {
        BotStateManager.setMaintenance(server, enabled, user);
    }

    public static boolean isMaintenance(String server) {
        return BotStateManager.isMaintenance(server);
    }

    // ===== Delegação: Tickets (direto ao DatabaseManager) =====

    public static int createTicket(String userId, String channelName) {
        return DatabaseManager.createTicket(userId, channelName);
    }

    public static int createTicket(String userId, String channelName, String categoryName) {
        return DatabaseManager.createTicket(userId, channelName, categoryName);
    }

    public static void updateTicket(int ticketId, String content, String priority) {
        DatabaseManager.updateTicket(ticketId, content, priority);
    }

    public static void updateTicketChannel(int ticketId, String channelName) {
        DatabaseManager.updateTicketChannel(ticketId, channelName);
    }

    public static void updateTicketCategory(int ticketId, String categoryName) {
        DatabaseManager.updateTicketCategory(ticketId, categoryName);
    }

    public static void deleteTicket(int ticketId) {
        DatabaseManager.deleteTicket(ticketId);
    }

    public static int cleanupGhostTickets() {
        return DatabaseManager.cleanupGhostTickets();
    }
}
