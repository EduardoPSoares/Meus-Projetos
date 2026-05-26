package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.RecipeDiscoveryProgress;
import com.midgard.fooddecay.multiblock.RecipeDiscoveryStage;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.midgard.core.utils.MessageUtils.sc;

public class CookbookGui extends GuiMenu {

    private static final int[] CATEGORY_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};

    private final FoodDecayModule module;
    private final FoodDecayConfig config;

    public CookbookGui(FoodDecayModule module) {
        super(sc("&8Livro de Receitas"), 5);
        this.module = module;
        this.config = module.getDecayConfig();
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder(Material.BROWN_STAINED_GLASS_PANE));

        Map<String, RecipeDiscoveryProgress> progressByRecipe = getDiscoveryProgress(player);
        List<CategoryEntry> categories = buildCategories(player);
        DiscoveryCounts machineCounts = countMachineDiscovery(progressByRecipe);
        int totalMachineRecipes = countTotalMachineRecipes();
        int fermentationRecipes = hasFermentationSection() ? config.getFermentRecipes().size() : 0;
        int totalCatalogued = totalMachineRecipes + fermentationRecipes;
        long cataloguedRecipes = machineCounts.catalogued() + fermentationRecipes;

        ItemBuilder title = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(sc("&6&lLivro de Receitas"))
                .lore(
                        sc("&7Maquinas, bebidas e preparos"),
                        sc("&7organizados por categoria."),
                        "",
                        sc("&7Catalogadas: &f" + cataloguedRecipes + "&8/&f" + totalCatalogued),
                        sc("&7Pistas abertas: &e" + machineCounts.suspected()),
                        sc("&7Dominadas: &a" + machineCounts.mastered()),
                        sc("&7Categorias: &f" + categories.size()),
                        "",
                        sc("&eClique em uma categoria para abrir")
                );
        if (cataloguedRecipes > 0 || machineCounts.suspected() > 0) {
            title.glow();
        }
        setItem(4, title.build());

        setItem(22, new ItemBuilder(Material.COMPASS)
                .name(sc("&e&lComo navegar"))
                .lore(
                        sc("&7Cada maquina trabalha com"),
                        sc("&74 estados de descoberta."),
                        "",
                        sc("&8- &7Tentativa valida abre uma &esuspeita"),
                        sc("&8- &7Concluir e coletar registra como &ftestada"),
                        sc("&8- &7Repetir a receita torna ela &adominada"),
                        sc("&8- &7Dominar uma receita revela mais pistas")
                )
                .build());

        if (categories.isEmpty()) {
            setItem(13, new ItemBuilder(Material.BARRIER)
                    .name(sc("&cNenhuma categoria disponivel"))
                    .lore(
                            sc("&7Nao ha receitas visiveis no momento."),
                            sc("&7Ative as maquinas ou cadastre preparos"),
                            sc("&7para preencher este livro.")
                    )
                    .build());
        } else {
            for (int i = 0; i < categories.size() && i < CATEGORY_SLOTS.length; i++) {
                CategoryEntry entry = categories.get(i);
                setItem(CATEGORY_SLOTS[i], entry.item(), entry.action());
            }
        }

        setItem(40, new ItemBuilder(Material.BARRIER)
                .name(sc("&cFechar"))
                .build(), e -> player.closeInventory());
    }

    private List<CategoryEntry> buildCategories(Player player) {
        List<CategoryEntry> categories = new ArrayList<>();
        Map<String, RecipeDiscoveryProgress> progressByRecipe = getDiscoveryProgress(player);

        for (MultiblockType type : MultiblockType.values()) {
            if (!config.isMultiblockTypeEnabled(type)) {
                continue;
            }

            List<MultiblockRecipe> recipes = config.getRecipes(type);
            if (recipes.isEmpty()) {
                continue;
            }

            DiscoveryCounts counts = countDiscovery(recipes, progressByRecipe);

            categories.add(new CategoryEntry(
                    buildMachineCategory(type, counts, recipes.size()),
                    e -> new CookbookMachineRecipesGui(module, type).open(player)
            ));
        }

        if (hasFermentationSection()) {
            int totalRecipes = config.getFermentRecipes().size();
            categories.add(new CategoryEntry(
                    buildFermentationCategory(totalRecipes),
                    e -> new CookbookFermentationRecipesGui(module).open(player)
            ));
        }

        return categories;
    }

    private ItemStack buildMachineCategory(MultiblockType type, DiscoveryCounts counts, int totalRecipes) {
        List<String> lore = new ArrayList<>();
        lore.add(sc("&8| &7Pistas: &e" + counts.suspected()));
        lore.add(sc("&8| &7Registradas: &f" + counts.catalogued() + "&8/&f" + totalRecipes));
        lore.add(sc("&8| &7Dominadas: &a" + counts.mastered()));
        lore.add(sc("&8| &7Tempo base: &f" + config.getMultiblockProcessingMinutes(type) + " min"));
        lore.add(sc("&8| &7Preservacao: " + config.getTraitDisplayName(type.getResultTrait())));

        List<String> summary = getDescriptionSummary(config.getMultiblockDescription(type));
        if (!summary.isEmpty()) {
            lore.add("");
            for (String line : summary) {
                lore.add(sc(line));
            }
        }

        lore.add("");
        lore.add(sc("&eClique para abrir a categoria"));

        ItemBuilder item = new ItemBuilder(type.getIcon())
                .name(sc("&6&l" + config.getMultiblockDisplayName(type)))
                .lore(lore);
        if (counts.suspected() > 0 || counts.catalogued() > 0) {
            item.glow();
        }
        return item.build();
    }

    private ItemStack buildFermentationCategory(int totalRecipes) {
        ItemBuilder item = new ItemBuilder(Material.BARREL)
                .name(sc("&5&lFermentacao"))
                .lore(
                        sc("&8| &7Receitas: &f" + totalRecipes),
                        sc("&8| &7Estacao: &fBarril de fermentacao"),
                        sc("&8| &7Foco: &fBebidas e efeitos"),
                        "",
                        sc("&7Navegue por bebidas e veja"),
                        sc("&7os liquidos e efeitos de cada uma."),
                        "",
                        sc("&eClique para abrir a categoria")
                )
                .glow();
        return item.build();
    }

    private DiscoveryCounts countMachineDiscovery(Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        List<MultiblockRecipe> allRecipes = new ArrayList<>();

        for (MultiblockType type : MultiblockType.values()) {
            if (!config.isMultiblockTypeEnabled(type)) {
                continue;
            }
            allRecipes.addAll(config.getRecipes(type));
        }

        return countDiscovery(allRecipes, progressByRecipe);
    }

    private int countTotalMachineRecipes() {
        int total = 0;

        for (MultiblockType type : MultiblockType.values()) {
            if (!config.isMultiblockTypeEnabled(type)) {
                continue;
            }
            total += config.getRecipes(type).size();
        }

        return total;
    }

    private boolean hasFermentationSection() {
        return config.isFermentationEnabled() && !config.getFermentRecipes().isEmpty();
    }

    private Map<String, RecipeDiscoveryProgress> getDiscoveryProgress(Player player) {
        return module.getMultiblockManager() != null
                ? module.getMultiblockManager().getRecipeDiscoveryProgress(player)
                : Map.of();
    }

    private DiscoveryCounts countDiscovery(List<MultiblockRecipe> recipes,
                                           Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        long suspected = 0;
        long catalogued = 0;
        long mastered = 0;

        for (MultiblockRecipe recipe : recipes) {
            RecipeDiscoveryStage stage = progressByRecipe
                    .getOrDefault(recipe.getId(), RecipeDiscoveryProgress.UNKNOWN)
                    .stage();

            if (stage == RecipeDiscoveryStage.SUSPECTED) {
                suspected++;
            }
            if (stage.isCatalogued()) {
                catalogued++;
            }
            if (stage == RecipeDiscoveryStage.MASTERED) {
                mastered++;
            }
        }

        return new DiscoveryCounts(suspected, catalogued, mastered);
    }

    private List<String> getDescriptionSummary(List<String> description) {
        List<String> summary = new ArrayList<>();
        if (description == null) {
            return summary;
        }

        for (String line : description) {
            if (line == null || line.isBlank()) {
                if (!summary.isEmpty()) {
                    break;
                }
                continue;
            }

            summary.add(line);
            if (summary.size() >= 2) {
                break;
            }
        }

        return summary;
    }

    private record CategoryEntry(ItemStack item, Consumer<InventoryClickEvent> action) {
    }

    private record DiscoveryCounts(long suspected, long catalogued, long mastered) {
    }
}
