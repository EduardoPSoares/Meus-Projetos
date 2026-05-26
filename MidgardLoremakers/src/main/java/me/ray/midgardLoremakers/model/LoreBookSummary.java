package me.ray.midgardLoremakers.model;

import java.util.List;

public record LoreBookSummary(long id, String title, String category, List<String> tags, int pageCount, long createdAt, long updatedAt, boolean shared, boolean favorite) {
}
