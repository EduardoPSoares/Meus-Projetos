package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for the cooking system (campfire-based).
 */
public class AdminCookingGui extends AdminBaseGui {

    public AdminCookingGui(FoodDecayModule module) {
        super("&8\uD83D\uDD25 Cozimento", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminCookingGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.CAMPFIRE)
                .name(sc("&c&lSistema de Cozimento"))
                .lore(sc("&7Cozinhar em fogueiras"),
                      sc("&7com receitas e temperatura."))
                .build());

        // ── Row 1 ──
        setItem(10, toggle(Material.CAMPFIRE, "&c&lCozimento Ativado",
                config.isCookingEnabled(),
                "&7Habilita cozimento em",
                "&7fogueiras."),
                e -> { config.saveValue("cooking.enabled", !config.isCookingEnabled()); reopen.run(); });

        setItem(12, toggle(Material.COAL, "&8&lQueimar Comida",
                config.isCookingBurnEnabled(),
                "&7Comida queima se ficar",
                "&7tempo demais na fogueira."),
                e -> { config.saveValue("cooking.burn-enabled", !config.isCookingBurnEnabled()); reopen.run(); });

        setItem(14, val(Material.BLAZE_POWDER, "&6&lTempo p/ Queimar",
                config.getCookingBurnMinutes() + " min",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite os minutos para queimar:",
                        "cooking.burn-after-minutes", 1, reopen));

        setItem(16, val(Material.CLOCK, "&e&lTempo Padrão",
                config.getCookingDefaultTime() + " min",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite o tempo padrão (minutos):",
                        "cooking.default-cook-minutes", 1, reopen));

        // ── Row 2: Info ──
        List<String> heatLines = new ArrayList<>();
        for (Map.Entry<String, Double> entry : config.getCookingHeatMultipliers().entrySet()) {
            heatLines.add("&7" + entry.getKey() + "&8: &fx" + entry.getValue());
        }
        if (heatLines.isEmpty()) {
            heatLines.add("&7campfire&8: &fx1.0");
            heatLines.add("&7soul-campfire&8: &fx1.5");
        }
        heatLines.add("");
        heatLines.add("&8Editar via config.yml");
        setItem(19, info(Material.FIRE_CHARGE, "&6&lMultiplicadores de Calor", heatLines));

        List<String> recipeLines = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Material, Material> entry : config.getCookingRecipes().entrySet()) {
            if (count++ >= 15) { recipeLines.add("&8... e mais"); break; }
            recipeLines.add("&7" + entry.getKey().name() + " &8→ &f" + entry.getValue().name());
        }
        if (recipeLines.isEmpty()) recipeLines.add("&8Nenhuma receita configurada");
        recipeLines.add("");
        recipeLines.add("&8Editar via config.yml");
        setItem(21, info(Material.BOOK, "&e&lReceitas de Cozimento", recipeLines));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
