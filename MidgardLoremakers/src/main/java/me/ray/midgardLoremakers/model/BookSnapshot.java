package me.ray.midgardLoremakers.model;

import java.util.List;

public record BookSnapshot(long id, long bookId, String title, String category, List<String> pages, long snapshotAt) {
}
