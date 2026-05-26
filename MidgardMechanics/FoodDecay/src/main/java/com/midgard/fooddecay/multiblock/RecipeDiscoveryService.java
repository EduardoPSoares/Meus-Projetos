package com.midgard.fooddecay.multiblock;

import com.midgard.core.utils.MessageUtils;
import com.midgard.fooddecay.FoodDecayConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.midgard.core.utils.MessageUtils.sc;

final class RecipeDiscoveryService {

    static final String DISCOVERY_KEY_NAME = "discovered_recipes";

    private final FoodDecayConfig config;
    private final NamespacedKey discoveryKey;
    private final RecipeDiscoveryCodec codec = new RecipeDiscoveryCodec();
    private final RecipeDiscoveryEngine engine = new RecipeDiscoveryEngine();

    RecipeDiscoveryService(FoodDecayConfig config, NamespacedKey discoveryKey) {
        this.config = config;
        this.discoveryKey = discoveryKey;
    }

    Set<String> getDiscoveredRecipes(Player player) {
        return getDiscoveryProgress(player).entrySet().stream()
                .filter(entry -> entry.getValue().stage().isCatalogued())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    Map<String, RecipeDiscoveryProgress> getDiscoveryProgress(Player player) {
        String data = player.getPersistentDataContainer().get(discoveryKey, PersistentDataType.STRING);
        return new LinkedHashMap<>(codec.decode(data));
    }

    RecipeDiscoveryProgress getDiscoveryProgress(Player player, String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return RecipeDiscoveryProgress.UNKNOWN;
        }
        return getDiscoveryProgress(player).getOrDefault(recipeId, RecipeDiscoveryProgress.UNKNOWN);
    }

    RecipeDiscoveryStage getDiscoveryStage(Player player, String recipeId) {
        return getDiscoveryProgress(player, recipeId).stage();
    }

    boolean registerAttempt(Player player, MultiblockRecipe recipe) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isBlank()) {
            return false;
        }

        Map<String, RecipeDiscoveryProgress> progressByRecipe = getDiscoveryProgress(player);
        RecipeDiscoveryChange change = engine.registerAttempt(progressByRecipe, recipe.getId());
        if (!change.changed()) {
            return false;
        }

        save(player, progressByRecipe);
        notifyAttempt(player, recipe, change);
        return true;
    }

    boolean registerCollection(Player player, MultiblockRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        return registerCollection(player, recipe, config.getRecipes(recipe.getMachineType()));
    }

    boolean registerCollection(Player player, MultiblockRecipe recipe, Collection<MultiblockRecipe> machineRecipes) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isBlank()) {
            return false;
        }

        Map<String, RecipeDiscoveryProgress> progressByRecipe = getDiscoveryProgress(player);
        RecipeDiscoveryChange change = engine.registerCollection(progressByRecipe, recipe, machineRecipes);
        if (!change.changed()) {
            return false;
        }

        save(player, progressByRecipe);
        notifyCollection(player, recipe, change);
        return true;
    }

    private void save(Player player, Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        String encoded = codec.encode(progressByRecipe);
        if (encoded.isEmpty()) {
            player.getPersistentDataContainer().remove(discoveryKey);
            return;
        }

        player.getPersistentDataContainer().set(
                discoveryKey,
                PersistentDataType.STRING,
                encoded
        );
    }

    private void notifyAttempt(Player player, MultiblockRecipe recipe, RecipeDiscoveryChange change) {
        if (change.currentStage() != RecipeDiscoveryStage.SUSPECTED) {
            return;
        }

        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("recipe-discovery-suspected")
                        .replace("{name}", recipe.getOutputDisplayName())
                        .replace("{machine}", config.getMultiblockDisplayName(recipe.getMachineType())))));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    private void notifyCollection(Player player, MultiblockRecipe recipe, RecipeDiscoveryChange change) {
        if (change.previousStage() != RecipeDiscoveryStage.MASTERED
                && change.currentStage() == RecipeDiscoveryStage.MASTERED) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("recipe-discovery-mastered")
                            .replace("{name}", recipe.getOutputDisplayName())
                            .replace("{machine}", config.getMultiblockDisplayName(recipe.getMachineType())))));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.65f);
        } else if (change.previousStage() != RecipeDiscoveryStage.TESTED
                && change.currentStage() == RecipeDiscoveryStage.TESTED) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("recipe-discovery-tested")
                            .replace("{name}", recipe.getOutputDisplayName())
                            .replace("{machine}", config.getMultiblockDisplayName(recipe.getMachineType())))));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1.5f);
        }

        if (change.familyHintsUnlocked() > 0) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("recipe-discovery-family")
                            .replace("{machine}", config.getMultiblockDisplayName(recipe.getMachineType()))
                            .replace("{count}", String.valueOf(change.familyHintsUnlocked())))));
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.35f);
        }
    }
}
