package com.midgardbot.data;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
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
 * Gerencia estatísticas e feedbacks de staff.
 */
public final class StaffDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaffDataManager.class);

    static final File STAFF_STATS_FILE = new File(D + "staff_stats.json");
    static final File STAFF_FEEDBACK_FILE = new File(D + "staff_feedback.json");

    private static Map<String, StaffStats> staffStats = new ConcurrentHashMap<>();
    private static Map<String, java.util.List<StaffFeedback>> staffFeedbacks = new ConcurrentHashMap<>();

    private StaffDataManager() {}

    static void init() {
        backupFile(STAFF_STATS_FILE);
        loadStaffStats();
        loadStaffFeedbacks();
    }

    static void saveAllSync() {
        saveSync(STAFF_STATS_FILE, staffStats);
        saveSync(STAFF_FEEDBACK_FILE, staffFeedbacks);
    }

    // ===== Estatísticas =====

    public static void incrementStaffApproval(String staffId) {
        staffStats.compute(staffId, (k, v) -> {
            if (v == null) v = new StaffStats();
            v.approved++;
            return v;
        });
        saveStaffStats();
    }

    public static void incrementStaffRejection(String staffId) {
        staffStats.compute(staffId, (k, v) -> {
            if (v == null) v = new StaffStats();
            v.rejected++;
            return v;
        });
        saveStaffStats();
    }

    public static void incrementTicketStats(String staffId, boolean claimed) {
        staffStats.compute(staffId, (k, v) -> {
            if (v == null) v = new StaffStats();
            if (claimed) v.ticketsClaimed++;
            else v.ticketsClosed++;
            return v;
        });
        saveStaffStats();
    }

    public static Map<String, StaffStats> getStaffStats() {
        return new HashMap<>(staffStats);
    }

    // ===== Feedback =====

    public static void addStaffFeedback(String staffId, String userId, int rating, String comment) {
        StaffFeedback feedback = new StaffFeedback(staffId, userId, rating, comment);
        staffFeedbacks.computeIfAbsent(staffId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(feedback);
        saveStaffFeedbacks();
    }

    public static java.util.List<StaffFeedback> getFeedbacksForStaff(String staffId) {
        return staffFeedbacks.getOrDefault(staffId, java.util.Collections.emptyList());
    }

    public static Map<String, java.util.List<StaffFeedback>> getAllStaffFeedbacks() {
        return new HashMap<>(staffFeedbacks);
    }

    // ===== Persistência Privada =====

    private static void loadStaffStats() {
        Type type = new TypeToken<Map<String, StaffStats>>(){}.getType();
        Map<String, StaffStats> loaded = loadFromFile(STAFF_STATS_FILE, type);
        if (loaded != null) staffStats = new ConcurrentHashMap<>(loaded);
    }

    private static void saveStaffStats() { persistAsync(STAFF_STATS_FILE, staffStats); }

    private static void loadStaffFeedbacks() {
        Type type = new TypeToken<Map<String, java.util.List<StaffFeedback>>>(){}.getType();
        Map<String, java.util.List<StaffFeedback>> loaded = loadFromFile(STAFF_FEEDBACK_FILE, type);
        if (loaded != null) staffFeedbacks = new ConcurrentHashMap<>(loaded);
    }

    private static void saveStaffFeedbacks() { persistAsync(STAFF_FEEDBACK_FILE, staffFeedbacks); }
}
