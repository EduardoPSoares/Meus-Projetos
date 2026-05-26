package com.midgard.fooddecay.multiblock;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class RecipeDiscoveryCodec {

    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = "\\|";

    Map<String, RecipeDiscoveryProgress> decode(String rawData) {
        Map<String, RecipeDiscoveryProgress> progressByRecipe = new LinkedHashMap<>();
        if (rawData == null || rawData.isBlank()) {
            return progressByRecipe;
        }

        if (!rawData.contains(ENTRY_SEPARATOR) && !rawData.contains("|")) {
            decodeLegacyList(rawData, progressByRecipe);
            return progressByRecipe;
        }

        for (String entry : rawData.split(ENTRY_SEPARATOR)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] fields = entry.split(FIELD_SEPARATOR, -1);
            if (fields.length < 3) {
                decodeLegacyList(entry, progressByRecipe);
                continue;
            }

            String recipeId = fields[0].trim();
            if (recipeId.isEmpty()) {
                continue;
            }

            boolean attempted = "1".equals(fields[1].trim()) || Boolean.parseBoolean(fields[1].trim());
            int successfulCollections;
            try {
                successfulCollections = Math.max(0, Integer.parseInt(fields[2].trim()));
            } catch (NumberFormatException ignored) {
                successfulCollections = attempted ? 0 : 1;
            }

            RecipeDiscoveryProgress progress = new RecipeDiscoveryProgress(attempted, successfulCollections);
            mergeProgress(progressByRecipe, recipeId, progress);
        }

        return progressByRecipe;
    }

    String encode(Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        if (progressByRecipe == null || progressByRecipe.isEmpty()) {
            return "";
        }

        return progressByRecipe.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && entry.getValue().stage().hasAnyClue())
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey().trim()
                        + "|" + (entry.getValue().attempted() ? "1" : "0")
                        + "|" + entry.getValue().successfulCollections())
                .collect(Collectors.joining(ENTRY_SEPARATOR));
    }

    private void decodeLegacyList(String rawData, Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        Arrays.stream(rawData.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(recipeId -> mergeProgress(progressByRecipe, recipeId,
                        new RecipeDiscoveryProgress(true, 1)));
    }

    private void mergeProgress(Map<String, RecipeDiscoveryProgress> progressByRecipe,
                               String recipeId,
                               RecipeDiscoveryProgress candidate) {
        RecipeDiscoveryProgress existing = progressByRecipe.get(recipeId);
        if (existing == null) {
            progressByRecipe.put(recipeId, candidate);
            return;
        }

        boolean attempted = existing.attempted() || candidate.attempted();
        int collections = Math.max(existing.successfulCollections(), candidate.successfulCollections());
        progressByRecipe.put(recipeId, new RecipeDiscoveryProgress(attempted, collections));
    }
}
