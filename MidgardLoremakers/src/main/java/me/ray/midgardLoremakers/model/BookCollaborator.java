package me.ray.midgardLoremakers.model;

import java.util.UUID;

public record BookCollaborator(long id, long bookId, UUID collaboratorUuid, String collaboratorName, long createdAt) {
}
