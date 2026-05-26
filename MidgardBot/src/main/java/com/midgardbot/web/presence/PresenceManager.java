package com.midgardbot.web.presence;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia a presença de usuários online no painel administrativo.
 * Usa heartbeats periódicos para rastrear quem está ativo.
 */
public class PresenceManager {

    private static final long TIMEOUT_MS = 60_000; // 60s sem heartbeat = offline

    private static final ConcurrentHashMap<String, OnlineUser> onlineUsers = new ConcurrentHashMap<>();

    public static void heartbeat(String userId, String username, String avatarUrl, String currentPage) {
        onlineUsers.put(userId, new OnlineUser(userId, username, avatarUrl, currentPage, System.currentTimeMillis()));
    }

    public static void remove(String userId) {
        onlineUsers.remove(userId);
    }

    public static List<OnlineUser> getOnlineUsers() {
        long now = System.currentTimeMillis();
        // Remove expirados e retorna ativos
        onlineUsers.entrySet().removeIf(e -> (now - e.getValue().lastSeen()) > TIMEOUT_MS);
        return List.copyOf(onlineUsers.values());
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(e -> (now - e.getValue().lastSeen()) > TIMEOUT_MS * 2);
    }

    public record OnlineUser(String userId, String username, String avatarUrl, String currentPage, long lastSeen) {}
}
