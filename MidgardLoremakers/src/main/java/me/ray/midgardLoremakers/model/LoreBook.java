package me.ray.midgardLoremakers.model;

import java.util.List;
import java.util.UUID;

public record LoreBook(long id, UUID ownerUuid, String title, String category, List<String> tags, List<String> pages, long createdAt, long updatedAt) {

    public int pageCount() {
        return pages.size();
    }
}
