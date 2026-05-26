package com.midgard.fooddecay.gui;

import com.midgard.core.gui.PaginatedMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.RecipeDiscoveryProgress;
import com.midgard.fooddecay.multiblock.RecipeDiscoveryStage;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.midgard.core.utils.MessageUtils.sc;

public class CookbookMachineRecipesGui extends PaginatedMenu {

    private final FoodDecayModule module;
    private final FoodDecayConfig config;
    private final MultiblockType machineType;

    public CookbookMachineRecipesGui(FoodDecayModule module, MultiblockType machineType) {
        super(sc("&8Receitas - " + module.getDecayConfig().getMultiblockDisplayName(machineType)), 6);
        this.module = module;
        this.config = module.getDecayConfig();
        this.machineType = machineType;
    }

    @Override
    public ItemStack getPreviousPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&ePagina anterior"))
                .build();
    }

    @Override
    public ItemStack getNextPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&eProxima pagina"))
                .build();
    }

    @Override
    public void setupDecoration(Player player) {
        fillBorder(ItemBuilder.placeholder(Material.BROWN_STAINED_GLASS_PANE));

        List<MultiblockRecipe> recipes = config.getRecipes(machineType);
        Map<String, RecipeDiscoveryProgress> progressByRecipe = getDiscoveryProgress(player);
        DiscoveryCounts counts = countDiscovery(recipes, progressByRecipe);

        clearPageItems();
        if (recipes.isEmpty()) {
            addPageItem(new ItemBuilder(Material.BARRIER)
                    .name(sc("&cNenhuma receita cadastrada"))
                    .lore(
                            sc("&7Esta categoria ainda nao possui"),
                            sc("&7receitas configuradas.")
                    )
                    .build());
        } else {
            for (MultiblockRecipe recipe : recipes) {
                if (getStage(progressByRecipe, recipe) == RecipeDiscoveryStage.MASTERED) {
                    addPageItem(buildMasteredRecipe(recipe));
                }
            }

            for (MultiblockRecipe recipe : recipes) {
                if (getStage(progressByRecipe, recipe) == RecipeDiscoveryStage.TESTED) {
                    addPageItem(buildTestedRecipe(recipe, progressByRecipe.getOrDefault(
                            recipe.getId(),
                            RecipeDiscoveryProgress.UNKNOWN
                    )));
                }
            }

            for (MultiblockRecipe recipe : recipes) {
                if (getStage(progressByRecipe, recipe) == RecipeDiscoveryStage.SUSPECTED) {
                    addPageItem(buildSuspectedRecipe(recipe));
                }
            }

            for (MultiblockRecipe recipe : recipes) {
                if (getStage(progressByRecipe, recipe) == RecipeDiscoveryStage.UNKNOWN) {
                    addPageItem(buildUnknownRecipe());
                }
            }
        }

        ItemBuilder title = new ItemBuilder(machineType.getIcon())
                .name(sc("&6&l" + config.getMultiblockDisplayName(machineType)))
                .lore(buildTitleLore(counts, recipes.size()));
        if (counts.catalogued() > 0 || counts.suspected() > 0) {
            title.glow();
        }
        setItem(4, title.build());

        setItem(2, new ItemBuilder(Material.CLOCK)
                .name(sc("&e&lDetalhes"))
                .lore(
                        sc("&8| &7Tempo base: &f" + config.getMultiblockProcessingMinutes(machineType) + " min"),
                        sc("&8| &7Preservacao: " + config.getTraitDisplayName(machineType.getResultTrait())),
                        sc("&8| &7Pistas: &e" + counts.suspected()),
                        sc("&8| &7Registradas: &f" + counts.catalogued()),
                        sc("&8| &7Dominadas: &a" + counts.mastered())
                )
                .build());

        setItem(6, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(sc("&e&lLeitura do caderno"))
                .lore(
                        sc("&7Dominada: mostra todos os detalhes."),
                        sc("&7Testada: mostra o preparo confirmado."),
                        "",
                        sc("&7Suspeita: mostra pistas parciais."),
                        sc("&7Oculta: ainda sem pistas."),
                        "",
                        sc("&7Concluir e coletar registra."),
                        sc("&7Repetir domina a receita.")
                )
                .build());

        setItem(49, new ItemBuilder(Material.ARROW)
                .name(sc("&fVoltar"))
                .lore(sc("&7Retornar para as categorias"))
                .build(), e -> new CookbookGui(module).open(player));

        setItem(50, new ItemBuilder(Material.BARRIER)
                .name(sc("&cFechar"))
                .build(), e -> player.closeInventory());
    }

    private List<String> buildTitleLore(DiscoveryCounts counts, int totalRecipes) {
        List<String> lore = new ArrayList<>();
        for (String line : getDescriptionSummary(config.getMultiblockDescription(machineType))) {
            lore.add(sc(line));
        }

        if (!lore.isEmpty()) {
            lore.add("");
        }

        lore.add(sc("&7Pistas abertas: &e" + counts.suspected()));
        lore.add(sc("&7Registradas: &f" + counts.catalogued() + "&8/&f" + totalRecipes));
        lore.add(sc("&7Dominadas: &a" + counts.mastered()));
        lore.add(sc("&7Pagina atual: &f" + (getCurrentPage() + 1) + "&8/&f" + Math.max(1, getTotalPages())));
        return lore;
    }

    private ItemStack buildMasteredRecipe(MultiblockRecipe recipe) {
        ItemStack preview = recipe.createOutputPreview();
        String outputName = displayNameForMenu(recipe.getOutputDisplayName(), preview.getType());
        String inputName = displayNameForMenu(recipe.getInputDisplayName(), recipe.createInputPreview().getType());

        List<String> lore = new ArrayList<>();
        lore.add(sc("&8| &7Ingrediente: &f" + inputName));
        lore.add(sc("&8| &7Resultado: &f" + outputName));
        lore.add(sc("&8| &7Tempo: &f" + recipe.getTimeMinutes() + " min"));
        appendExtraIngredientLore(lore, recipe);

        if (recipe.getTrait() != null) {
            lore.add(sc("&8| &7Aplica: " + config.getTraitDisplayName(recipe.getTrait())));
        }
        if (recipe.getRequiresTrait() != null) {
            lore.add(sc("&8| &7Requer traco: " + config.getTraitDisplayName(recipe.getRequiresTrait())));
        }
        if (recipe.getRequiresRecipe() != null && !recipe.getRequiresRecipe().isBlank()) {
            lore.add(sc("&8| &7Requer receita: &f" + recipe.getRequiresRecipe()));
        }
        if (recipe.getProfession() != null && !recipe.getProfession().isBlank()) {
            lore.add(sc("&8| &7Profissao: &f" + recipe.getProfession() + " " + recipe.getProfessionLevel()));
        }

        lore.add("");
        lore.add(sc("&aStatus: Dominada"));
        lore.add(sc("&7Todos os detalhes desta receita"));
        lore.add(sc("&7ja estao registrados no caderno."));

        return new ItemBuilder(preview)
                .name(sc("&f" + outputName))
                .lore(lore)
                .glow()
                .build();
    }

    private ItemStack buildTestedRecipe(MultiblockRecipe recipe, RecipeDiscoveryProgress progress) {
        ItemStack preview = recipe.createOutputPreview();
        String outputName = displayNameForMenu(recipe.getOutputDisplayName(), preview.getType());
        String inputName = displayNameForMenu(recipe.getInputDisplayName(), recipe.createInputPreview().getType());
        int remaining = progress.collectionsRemainingForMastery();

        List<String> lore = new ArrayList<>();
        lore.add(sc("&8| &7Ingrediente: &f" + inputName));
        lore.add(sc("&8| &7Resultado: &f" + outputName));
        lore.add(sc("&8| &7Tempo: &f" + recipe.getTimeMinutes() + " min"));
        appendExtraIngredientLore(lore, recipe);

        String requirementHint = buildRequirementHint(recipe, false);
        if (requirementHint != null) {
            lore.add(sc("&8| &7Ponto-chave: &f" + requirementHint));
        }

        lore.add("");
        lore.add(sc("&eStatus: Testada"));
        if (remaining > 0) {
            lore.add(sc("&7Repita mais &f" + remaining + "x &7para dominar."));
        }

        return new ItemBuilder(preview)
                .name(sc("&e" + outputName))
                .lore(lore)
                .build();
    }

    private ItemStack buildSuspectedRecipe(MultiblockRecipe recipe) {
        return new ItemBuilder(Material.MAP)
                .name(sc("&eRascunho de preparo"))
                .lore(
                        sc("&8| &7Maquina: &f" + config.getMultiblockDisplayName(machineType)),
                        sc("&8| &7Ingrediente-base: &f" + classifyInputHint(recipe)),
                        sc("&8| &7Composicao: &f" + extraIngredientHint(recipe)),
                        sc("&8| &7Duracao: &f" + timeBand(recipe.getTimeMinutes())),
                        sc("&8| &7Pista: &f" + buildRequirementHint(recipe, true)),
                        "",
                        sc("&eStatus: Suspeita"),
                        sc("&7Conclua e colete uma vez"),
                        sc("&7para registrar a receita.")
                )
                .build();
    }

    private ItemStack buildUnknownRecipe() {
        return new ItemBuilder(Material.GRAY_DYE)
                .name(sc("&8Receita oculta"))
                .lore(
                        sc("&8| &7Categoria: &f" + config.getMultiblockDisplayName(machineType)),
                        sc("&8| &7Status: &8Sem pistas"),
                        "",
                        sc("&7Ainda nao houve nenhuma"),
                        sc("&7tentativa valida registrada."),
                        "",
                        sc("&7Acerte um preparo nesta"),
                        sc("&7maquina para abrir um rascunho.")
                )
                .build();
    }

    private Map<String, RecipeDiscoveryProgress> getDiscoveryProgress(Player player) {
        return module.getMultiblockManager() != null
                ? module.getMultiblockManager().getRecipeDiscoveryProgress(player)
                : Map.of();
    }

    private RecipeDiscoveryStage getStage(Map<String, RecipeDiscoveryProgress> progressByRecipe,
                                          MultiblockRecipe recipe) {
        return progressByRecipe.getOrDefault(recipe.getId(), RecipeDiscoveryProgress.UNKNOWN).stage();
    }

    private DiscoveryCounts countDiscovery(List<MultiblockRecipe> recipes,
                                           Map<String, RecipeDiscoveryProgress> progressByRecipe) {
        long suspected = 0;
        long catalogued = 0;
        long mastered = 0;

        for (MultiblockRecipe recipe : recipes) {
            RecipeDiscoveryStage stage = getStage(progressByRecipe, recipe);
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

    private String displayNameForMenu(String rawName, Material fallback) {
        if (rawName == null || rawName.isBlank()) {
            return fallback != null ? formatMaterial(fallback.name()) : "?";
        }

        String plain = rawName.replaceAll("(?i)&[0-9A-FK-OR]", "");
        plain = plain.replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
        if (!plain.isEmpty()) {
            return plain;
        }

        return fallback != null ? formatMaterial(fallback.name()) : "?";
    }

    private String formatMaterial(String materialName) {
        String[] parts = materialName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0))
                    .append(part.substring(1).toLowerCase());
        }
        return builder.toString();
    }

    private String classifyInputHint(MultiblockRecipe recipe) {
        if (recipe.getInputMaterial() == null) {
            return "item especial";
        }

        String materialName = recipe.getInputMaterial().name();
        if (containsAny(materialName, "COD", "SALMON", "PUFFERFISH", "TROPICAL_FISH")) {
            return "peixe";
        }
        if (containsAny(materialName, "BEEF", "PORK", "CHICKEN", "MUTTON", "RABBIT")) {
            return "carne";
        }
        if (containsAny(materialName, "APPLE", "BERRY", "MELON", "PUMPKIN")) {
            return "fruta";
        }
        if (containsAny(materialName, "CARROT", "POTATO", "BEETROOT", "KELP", "SEAWEED")) {
            return "vegetal";
        }
        if (containsAny(materialName, "WHEAT", "BREAD", "COOKIE", "CAKE")) {
            return "grao ou massa";
        }
        if (containsAny(materialName, "MILK", "EGG", "HONEY")) {
            return "ingrediente animal";
        }

        return formatMaterial(materialName).toLowerCase();
    }

    private String timeBand(int minutes) {
        if (minutes <= 5) {
            return "curta";
        }
        if (minutes <= 15) {
            return "media";
        }
        return "longa";
    }

    private String buildRequirementHint(MultiblockRecipe recipe, boolean vague) {
        if (recipe.getRequiresTrait() != null) {
            return vague
                    ? "precisa de um alimento previamente tratado"
                    : "precisa do traco " + plainTraitName(recipe.getRequiresTrait());
        }
        if (recipe.getRequiresRecipe() != null && !recipe.getRequiresRecipe().isBlank()) {
            return vague
                    ? "depende de um preparo anterior"
                    : "depende de uma receita anterior";
        }
        if (recipe.getProfession() != null && !recipe.getProfession().isBlank()) {
            return vague
                    ? "exige tecnica profissional"
                    : recipe.getProfession() + " " + recipe.getProfessionLevel();
        }
        if (!recipe.getExtraIngredients().isEmpty()) {
            return vague
                    ? "combina ingredientes adicionais"
                    : "usa " + recipe.getExtraIngredients().size() + " ingrediente(s) extra(s)";
        }
        if (recipe.getTrait() != null) {
            return vague
                    ? "gera um preparo conservado"
                    : "aplica " + plainTraitName(recipe.getTrait());
        }
        return vague ? "nenhuma exigencia especial percebida" : null;
    }

    private void appendExtraIngredientLore(List<String> lore, MultiblockRecipe recipe) {
        if (recipe.getExtraIngredients().isEmpty()) {
            return;
        }

        List<String> labels = new ArrayList<>();
        for (var ingredient : recipe.getExtraIngredients()) {
            labels.add(ingredient.getReferenceLabel());
            if (labels.size() >= 3) {
                break;
            }
        }
        if (recipe.getExtraIngredients().size() > labels.size()) {
            labels.add("+" + (recipe.getExtraIngredients().size() - labels.size()) + " item(ns)");
        }
        lore.add(sc("&8| &7Extras: &f" + String.join(", ", labels)));
    }

    private String extraIngredientHint(MultiblockRecipe recipe) {
        if (recipe.getExtraIngredients().isEmpty()) {
            return "ingrediente unico";
        }
        return recipe.getExtraIngredients().size() == 1
                ? "preparo composto com 1 complemento"
                : "preparo composto com " + recipe.getExtraIngredients().size() + " complementos";
    }

    private String plainTraitName(com.midgard.fooddecay.FoodTrait trait) {
        return config.getTraitDisplayName(trait).replaceAll("(?i)&[0-9A-FK-OR]", "");
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private record DiscoveryCounts(long suspected, long catalogued, long mastered) {
    }
}
