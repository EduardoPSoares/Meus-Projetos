package me.ray.midgardLoremakers.model;

import java.util.UUID;

public record AuthenticatedSession(UUID playerUuid, String playerName, long expiresAt) {
}
