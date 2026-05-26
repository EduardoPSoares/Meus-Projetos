package de.maxhenkel.voicechat.voice.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AudioPriorityTracker {

    private final Map<UUID, ActiveSpeaker> activeSpeakers = new ConcurrentHashMap<>();

    private static final long ACTIVE_THRESHOLD_MS = 500;

    /**
     * Returns true if this sender's audio should be suppressed for the given receiver
     * because the receiver is currently hearing a higher-priority speaker.
     */
    public boolean shouldSuppress(UUID receiverUuid, UUID senderUuid, int senderPriority) {
        ActiveSpeaker active = activeSpeakers.get(receiverUuid);
        if (active == null) return false;
        if (System.currentTimeMillis() - active.timestamp > ACTIVE_THRESHOLD_MS) {
            activeSpeakers.remove(receiverUuid);
            return false;
        }
        if (active.speakerUuid.equals(senderUuid)) return false;
        return senderPriority < active.priority;
    }

    /**
     * Records that a receiver is hearing a speaker with the given priority.
     * Only updates if this speaker has equal or higher priority than the current active speaker.
     */
    public void recordSpeaker(UUID receiverUuid, UUID senderUuid, int senderPriority) {
        ActiveSpeaker current = activeSpeakers.get(receiverUuid);
        long now = System.currentTimeMillis();
        if (current == null || now - current.timestamp > ACTIVE_THRESHOLD_MS || senderPriority >= current.priority) {
            activeSpeakers.put(receiverUuid, new ActiveSpeaker(senderUuid, senderPriority, now));
        }
    }

    public void clearPlayer(UUID playerUuid) {
        activeSpeakers.remove(playerUuid);
    }

    private static class ActiveSpeaker {
        final UUID speakerUuid;
        final int priority;
        final long timestamp;

        ActiveSpeaker(UUID speakerUuid, int priority, long timestamp) {
            this.speakerUuid = speakerUuid;
            this.priority = priority;
            this.timestamp = timestamp;
        }
    }
}
