package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FermentationManager;
import com.midgard.fooddecay.FermentationManager.FermentEntry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.midgard.core.utils.MessageUtils.sc;

/**
 * GUI that opens when right-clicking a dedicated fermentation barrel.
 * Shows idle, fermenting progress, or ready-to-collect states.
 */
public class FermentationBarrelGui extends GuiMenu {

    private final Location barrelLocation;
    private final FermentationManager fermentManager;
    private final FoodDecayConfig config;

    public FermentationBarrelGui(Location barrelLocation, FermentationManager fermentManager,
                                  FoodDecayConfig config) {
        super(sc(config.msg("gui-ferment-title")), 6);
        this.barrelLocation = barrelLocation;
        this.fermentManager = fermentManager;
        this.config = config;
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());

        // Barrel icon in top center
        setItem(4, new ItemBuilder(Material.BARREL)
                .name(sc(config.msg("gui-ferment-barrel-name")))
                .lore(sc(config.msg("gui-ferment-barrel-lore")))
                .build());

        // Liquid level info (slot 10 — left side)
        setupLiquidInfo();

        FermentEntry entry = fermentManager.getActiveFermentation(barrelLocation);

        if (entry == null) {
            // Idle — no active fermentation
            setupIdle(player);
        } else if (entry.isReady()) {
            // Ready to collect
            setupReady(player, entry);
        } else {
            // Fermenting in progress
            setupProgress(player, entry);
        }
    }

    private void setupLiquidInfo() {
        org.bukkit.block.Block block = barrelLocation.getBlock();
        String liquidType = fermentManager.getBarrelLiquidType(block);
        int amount = fermentManager.getBarrelLiquidAmount(block);
        int capacity = fermentManager.getBarrelLiquidCapacity();

        Material icon;
        String name;
        String loreAmount;

        if (liquidType == null || liquidType.isEmpty() || amount <= 0) {
            icon = Material.BUCKET;
            name = config.msg("gui-ferment-liquid-empty-name");
            loreAmount = config.msg("gui-ferment-liquid-empty-lore");
        } else {
            icon = Material.WATER_BUCKET;
            String displayName = fermentManager.getLiquidDisplayName(liquidType);
            name = config.msg("gui-ferment-liquid-name")
                    .replace("{liquid}", displayName);
            loreAmount = config.msg("gui-ferment-liquid-lore")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{capacity}", String.valueOf(capacity));
        }

        setItem(19, new ItemBuilder(icon)
                .name(sc(name))
                .lore(sc(loreAmount))
                .build());
    }

    private void setupIdle(Player player) {
        org.bukkit.block.Block block = barrelLocation.getBlock();
        String liquidType = fermentManager.getBarrelLiquidType(block);
        int amount = fermentManager.getBarrelLiquidAmount(block);

        if (liquidType != null && !liquidType.isEmpty() && amount > 0) {
            List<FoodDecayConfig.FermentRecipe> recipes = config.findAllFermentRecipes(liquidType);
            if (!recipes.isEmpty()) {
                // Show clickable recipe buttons in bottom row
                setupRecipeButtons(player, recipes, liquidType, amount);
            } else {
                String liquidName = fermentManager.getLiquidDisplayName(liquidType);
                String penalty = String.valueOf(fermentManager.getInvalidRecipePenaltyPercent());
                String minimum = String.valueOf(fermentManager.getInvalidRecipeMinimumLossMb());

                setItem(22, new ItemBuilder(Material.RED_TERRACOTTA)
                        .name(sc(config.msg("gui-ferment-no-recipe-name")))
                        .lore(
                                sc(config.msg("gui-ferment-no-recipe-lore-1")
                                        .replace("{liquid}", liquidName)),
                                "",
                                sc(config.msg("gui-ferment-no-recipe-lore-2")
                                        .replace("{penalty}", penalty)),
                                sc(config.msg("gui-ferment-no-recipe-lore-3")
                                        .replace("{minimum}", minimum)),
                                "",
                                sc(config.msg("gui-ferment-no-recipe-lore-4")),
                                sc(config.msg("gui-ferment-no-recipe-lore-5"))
                        )
                        .build());
            }
        } else {
            setItem(22, new ItemBuilder(Material.LIGHT_GRAY_CONCRETE)
                    .name(sc(config.msg("gui-ferment-idle-name")))
                    .lore(sc(config.msg("gui-ferment-idle-lore-1")),
                            "", sc(config.msg("gui-ferment-idle-lore-2")))
                    .build());
        }
    }

    private void setupRecipeButtons(Player player, List<FoodDecayConfig.FermentRecipe> recipes,
                                     String liquidType, int amount) {
        // Info item in center slot
        String liquidName = fermentManager.getLiquidDisplayName(liquidType);
        setItem(22, new ItemBuilder(Material.BREWING_STAND)
                .name(sc("&e⚖ Escolha a Receita"))
                .lore(sc("&7Líquido: &b" + liquidName),
                        sc("&7Disponível: &f" + amount + " mB"),
                        "",
                        sc("&eClique em uma receita abaixo para iniciar."))
                .build());

        // Place recipe buttons in row 3, centered
        int maxButtons = Math.min(recipes.size(), 7);
        int startSlot = 28 + (7 - maxButtons) / 2;

        for (int i = 0; i < maxButtons; i++) {
            FoodDecayConfig.FermentRecipe recipe = recipes.get(i);
            boolean canCraft = amount >= recipe.requiredMb();

            List<String> lore = new ArrayList<>();
            lore.add("&7Líquido: &b" + liquidName + " &7(" + recipe.requiredMb() + " mB)");
            lore.add("&7Tempo: &e" + recipe.timeMinutes() + " min");
            lore.add("");
            if (canCraft) {
                lore.add("&a✔ Clique para iniciar!");
            } else {
                lore.add("&c✖ Falta &f" + (recipe.requiredMb() - amount) + " mB");
            }

            Material icon = canCraft ? recipe.resultMaterial() : Material.GRAY_DYE;
            ItemBuilder builder = new ItemBuilder(icon)
                    .name(sc((canCraft ? "&a" : "&c") + recipe.displayName()))
                    .lore(lore);
            if (canCraft) builder.glow();

            final String recipeId = recipe.id();
            if (canCraft) {
                setItem(startSlot + i, builder.build(), click -> {
                    if (fermentManager.startFermentation(player, barrelLocation, recipeId)) {
                        player.closeInventory();
                    }
                });
            } else {
                setItem(startSlot + i, builder.build());
            }
        }
    }

    private void setupReady(Player player, FermentEntry entry) {
        FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(entry.recipeId());
        String drinkName = recipe != null ? recipe.displayName() : entry.recipeId();
        Material resultMat = recipe != null ? recipe.resultMaterial() : Material.POTION;

        List<String> lore = new ArrayList<>();
        lore.add(config.msg("gui-ferment-ready-lore"));
        lore.add("");
        lore.add("&7Líquido: &b" + fermentManager.getLiquidDisplayName(entry.liquidType())
                + " &7(" + entry.liquidAmount() + " mB)");
        lore.add("&7Tempo total: &f" + formatTime(entry.durationMs()));

        setItem(22, new ItemBuilder(resultMat)
                .name(sc(config.msg("gui-ferment-ready-name")
                        .replace("{drink}", drinkName)))
                .lore(lore)
                .glow()
                .build(), click -> {
            fermentManager.collectFromGui(player, barrelLocation);
            player.closeInventory();
        });

        // Full progress bar
        for (int i = 0; i < 7; i++) {
            setItem(28 + i, new ItemBuilder(Material.LIME_CONCRETE)
                    .name(sc("&a100%")).build());
        }
    }

    private void setupProgress(Player player, FermentEntry entry) {
        FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(entry.recipeId());
        String drinkName = recipe != null ? recipe.displayName() : entry.recipeId();

        float pct = entry.progress() * 100;
        long remaining = Math.max(0, entry.durationMs() - entry.elapsed());
        long elapsed = entry.elapsed();

        List<String> lore = new ArrayList<>();
        lore.add(config.msg("gui-ferment-progress-drink").replace("{drink}", drinkName));
        lore.add("");
        lore.add("&7Progresso: &f" + String.format("%.1f", pct) + "%");
        lore.add("&7Tempo decorrido: &f" + formatTime(elapsed));
        lore.add("&7Tempo restante: &f" + formatTime(remaining));
        if (recipe != null) {
            lore.add("");
            lore.add("&7Líquido usado: &b" + fermentManager.getLiquidDisplayName(entry.liquidType())
                    + " &7(" + entry.liquidAmount() + " mB)");
        }

        setItem(22, new ItemBuilder(Material.CLOCK)
                .name(sc(config.msg("gui-ferment-progress-name")
                        .replace("{percent}", String.format("%.0f", pct))))
                .lore(lore)
                .glow()
                .build());

        // Progress bar
        int bars = 7;
        int filled = (int) (pct * bars / 100);
        for (int i = 0; i < bars; i++) {
            Material g;
            String color;
            if (i < filled) {
                g = Material.LIME_CONCRETE;
                color = "&a";
            } else if (i == filled) {
                g = Material.YELLOW_CONCRETE;
                color = "&e";
            } else {
                g = Material.RED_CONCRETE;
                color = "&c";
            }
            int barPct = (int) ((i + 1) * 100.0 / bars);
            setItem(28 + i, new ItemBuilder(g)
                    .name(sc(color + barPct + "%")).build());
        }
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        if (mins > 0) return mins + "m " + secs + "s";
        return secs + "s";
    }
}
