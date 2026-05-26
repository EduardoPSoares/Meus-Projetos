package me.ray.midgard.modules.professions.blacksmith.forge.session;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages all active forge sessions across the server.
 */
public class ForgeSessionManager {

    // Active sessions: playerId → ForgeSession
    private final Map<UUID, ForgeSession> activeSessions = new ConcurrentHashMap<>();

    // Callback when a session completes
    private Consumer<ForgeSession> onSessionComplete;
    // Callback when a session fails/expires
    private Consumer<ForgeSession> onSessionFail;

    // Timeout checker task
    private BukkitTask timeoutTask;

    public ForgeSessionManager() {}

    /**
     * Starts the timeout checking task.
     */
    public void start() {
        timeoutTask = Task.syncTimer(() -> {
            Iterator<Map.Entry<UUID, ForgeSession>> it = activeSessions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ForgeSession> entry = it.next();
                ForgeSession session = entry.getValue();

                if (session.isExpired()) {
                    session.setCurrentStage(ForgeStage.EXPIRED);
                    it.remove();
                    if (onSessionFail != null) { onSessionFail.accept(session); }
                } else if (!session.isActive()) {
                    it.remove();
                }
            }
        }, 100L, 100L); // Check every 5 seconds
    }

    /**
     * Stops the timeout task.
     */
    public void shutdown() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        // End all active sessions
        for (ForgeSession session : activeSessions.values()) {
            if (session.isActive()) {
                session.setCurrentStage(ForgeStage.EXPIRED);
                if (onSessionFail != null) { onSessionFail.accept(session); }
            }
        }
        activeSessions.clear();
    }

    /**
     * Starts a new forge session for a player.
     * Returns null if the player already has an active session.
     */
    public ForgeSession startSession(ForgeSession session) {
        UUID playerId = session.getPlayerId();
        if (activeSessions.containsKey(playerId)) {
            return null; // Player already has an active session
        }
        activeSessions.put(playerId, session);
        return session;
    }

    /**
     * Gets the active session for a player.
     */
    public ForgeSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    /**
     * Checks if a player has an active session.
     */
    public boolean hasActiveSession(UUID playerId) {
        ForgeSession session = activeSessions.get(playerId);
        return session != null && session.isActive();
    }

    /**
     * Completes a session successfully.
     */
    public void completeSession(UUID playerId) {
        ForgeSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.setCurrentStage(ForgeStage.COMPLETED);
            if (onSessionComplete != null) { onSessionComplete.accept(session); }
        }
    }

    /**
     * Fails a session.
     */
    public void failSession(UUID playerId) {
        ForgeSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.setCurrentStage(ForgeStage.FAILED);
            if (onSessionFail != null) { onSessionFail.accept(session); }
        }
    }

    /**
     * Cancels a session (player left, etc.).
     */
    public void cancelSession(UUID playerId) {
        ForgeSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.setCurrentStage(ForgeStage.EXPIRED);
        }
    }

    /**
     * Gets the session associated with a specific forge.
     */
    public ForgeSession getSessionForForge(UUID forgeId) {
        for (ForgeSession session : activeSessions.values()) {
            if (session.getForgeId().equals(forgeId)) { return session; }
        }
        return null;
    }

    /**
     * Checks if a forge is currently in use.
     */
    public boolean isForgeInUse(UUID forgeId) {
        return getSessionForForge(forgeId) != null;
    }

    public void setOnSessionComplete(Consumer<ForgeSession> callback) {
        this.onSessionComplete = callback;
    }

    public void setOnSessionFail(Consumer<ForgeSession> callback) {
        this.onSessionFail = callback;
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public Collection<ForgeSession> getAllSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }
}
