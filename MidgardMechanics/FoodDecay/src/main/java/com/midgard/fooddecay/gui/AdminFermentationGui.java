package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayConfig.FermentRecipe;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Admin GUI for the fermentation/drinks system.
 */
public class AdminFermentationGui extends AdminBaseGui {

    public AdminFermentationGui(FoodDecayModule module) {
        super("&8\uD83C\uDF7A Fermentação", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminFermentationGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.BREWING_STAND)
                .name(sc("&5&lSistema de Fermentação"))
                .lore(sc("&7Receitas de bebidas"),
                      sc("&7fermentadas."))
                .build());

        // ── Row 1 ──
        setItem(11, toggle(Material.BREWING_STAND, "&5&lFermentação Ativada",
                config.isFermentationEnabled(),
                "&7Habilita o sistema de",
                "&7fermentação de líquidos."),
                e -> { config.saveValue("fermentation.enabled", !config.isFermentationEnabled()); reopen.run(); });

        // ── Row 2: Fermentation recipes ──
        Collection<FermentRecipe> recipes = config.getFermentRecipes();
        int[] recipeSlots = {19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (FermentRecipe recipe : recipes) {
            if (idx >= recipeSlots.length) break;
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(sc("&7Líquido: &f" + recipe.inputLiquid()));
            lore.add(sc("&7Necessário: &f" + recipe.requiredMb() + " mB"));
            lore.add(sc("&7Tempo: &f" + recipe.timeMinutes() + " min"));
            lore.add(sc("&7Resultado: &f" + recipe.resultMaterial().name()));
            if (!recipe.effects().isEmpty()) {
                lore.add("");
                lore.add(sc("&7Efeitos: &f" + recipe.effects().size()));
            }
            lore.add("");
            lore.add(sc("&8Editar via config.yml"));

            setItem(recipeSlots[idx], new ItemBuilder(recipe.resultMaterial())
                    .name(sc("&d&l" + recipe.displayName()))
                    .lore(lore)
                    .build());
            idx++;
        }

        if (recipes.isEmpty()) {
            setItem(22, new ItemBuilder(Material.GRAY_DYE)
                    .name(sc("&8Nenhuma receita"))
                    .lore("", sc("&7Adicione receitas no"),
                          sc("&7config.yml em fermentation.recipes"))
                    .build());
        }

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
