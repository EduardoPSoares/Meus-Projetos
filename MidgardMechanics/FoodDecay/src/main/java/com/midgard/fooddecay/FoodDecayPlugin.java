package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bootstrap plugin for the MidgardCooking module.
 * Registers the module with MidgardCore on enable.
 */
public final class FoodDecayPlugin extends JavaPlugin {

    private static FoodDecayPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        MidgardCore core = MidgardCore.getInstance();
        if (core == null) {
            getLogger().severe("MidgardCore not found! Disabling MidgardCooking...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        migrateLegacyDataFolder();

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("recipes.yml", false);

        core.getModuleManager().registerModule(new FoodDecayModule());
        getLogger().info("MidgardCooking module registered.");
    }

    @Override
    public void onDisable() {
        MidgardCore core = MidgardCore.getInstance();
        if (core != null) {
            core.getModuleManager().unregisterModule("MidgardCooking");
        }
        instance = null;
    }

    public static FoodDecayPlugin getInstance() {
        return instance;
    }

    private void migrateLegacyDataFolder() {
        Path currentFolder = getDataFolder().toPath();
        Path parent = currentFolder.getParent();
        if (parent == null) {
            return;
        }

        Path legacyFolder = parent.resolve("FoodDecay");
        if (!Files.exists(legacyFolder) || legacyFolder.equals(currentFolder)) {
            return;
        }

        int[] copiedFiles = {0};
        int[] copiedDirs = {0};

        try {
            Files.createDirectories(currentFolder);
            try (var paths = Files.walk(legacyFolder)) {
                paths.forEach(source -> {
                    try {
                        Path relative = legacyFolder.relativize(source);
                        if (relative.toString().isEmpty()) {
                            return;
                        }

                        Path target = currentFolder.resolve(relative);
                        if (Files.isDirectory(source)) {
                            if (!Files.exists(target)) {
                                Files.createDirectories(target);
                                copiedDirs[0]++;
                            }
                            return;
                        }

                        Path targetParent = target.getParent();
                        if (targetParent != null) {
                            Files.createDirectories(targetParent);
                        }
                        if (!Files.exists(target)) {
                            Files.copy(source, target);
                            copiedFiles[0]++;
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }

            if (copiedFiles[0] > 0 || copiedDirs[0] > 0) {
                getLogger().info("Migrated legacy data from plugins/FoodDecay to plugins/MidgardCooking"
                        + " (" + copiedFiles[0] + " files, " + copiedDirs[0] + " directories).");
            }
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            getLogger().warning("Failed to migrate legacy FoodDecay data folder: " + cause.getMessage());
        } catch (IOException ex) {
            getLogger().warning("Failed to prepare MidgardCooking data folder: " + ex.getMessage());
        }
    }
}
