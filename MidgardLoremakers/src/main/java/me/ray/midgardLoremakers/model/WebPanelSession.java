package me.ray.midgardLoremakers.model;

import java.util.UUID;

public record WebPanelSession(UUID playerUuid, String playerName, long createdAt, long lastSeenAt) {
}
