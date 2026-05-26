package com.midgard.fooddecay.gui;

import com.midgard.core.gui.PaginatedMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated admin list for machine recipes.
 */
public class RecipeListGui extends PaginatedMenu {

    private static final ItemStack FRAME_ITEM = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(" ")
            .hideFlags()
            .build();
    private static final ItemStack FILL_ITEM = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .hideFlags()
            .build();

    private final FoodDecayModule module;
    private final MultiblockType machineType;

    public RecipeListGui(FoodDecayModule module, MultiblockType machineType) {
        super(sc("&8" + module.getDecayConfig().getMultiblockDisplayName(machineType) + " : receitas"), 6);
        this.module = module;
        this.machineType = machineType;
    }

    @Override
    public ItemStack getPreviousPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&fAnterior"))
                .lore(sc("&7Pagina anterior"))
                .build();
    }

    @Override
    public ItemStack getNextPageItem() {
        return new ItemBuilder(Material.ARROW)
                .name(sc("&fProxima"))
                .lore(sc("&7Proxima pagina"))
                .build();
    }

    @Override
    public void setupDecoration(Player player) {
        FoodDecayConfig config = module.getDecayConfig();
        List<MultiblockRecipe> recipes = config.getRecipes(machineType);

        fill(FILL_ITEM);
        fillBorder(FRAME_ITEM);

        setItem(4, new ItemBuilder(machineType.getIcon())
                .name(sc("&f" + config.getMultiblockDisplayName(machineType)))
                .lore(sc("&7Receitas: &f" + recipes.size()),
                        sc("&7Clique para editar"),
                        sc("&7Shift para excluir"))
                .build());

        clearPageItems();

        addPageItem(new ItemBuilder(Material.EMERALD)
                .name(sc("&aNova receita"))
                .lore(sc("&7Cria uma receita vazia"),
                        sc("&7para esta maquina"))
                .glow()
                .build(), e -> {
            ChatInput.request(player, "&eDigite o ID da nova receita:", id -> {
                String cleanId = id.trim().toLowerCase().replace(" ", "-");
                if (cleanId.isEmpty()) {
                    player.sendMessage(MessageUtils.toComponent(sc("&cID invalido.")));
                    new RecipeListGui(module, machineType).open(player);
                    return;
                }

                for (MultiblockRecipe existing : config.getRecipes(machineType)) {
                    if (existing.getId().equalsIgnoreCase(cleanId)) {
                        player.sendMessage(MessageUtils.toComponent(sc("&cJa existe uma receita com esse ID.")));
                        new RecipeListGui(module, machineType).open(player);
                        return;
                    }
                }

                MultiblockRecipe blank = new MultiblockRecipe(cleanId, machineType,
                        Material.STONE, null, null, null, 0,
                        Material.STONE, null, null, null,
                        null, null, 0,
                        0, null,
                        machineType.getDefaultProcessingMinutes(), null,
                        null, null, List.of());
                new RecipeEditorGui(module, blank, true).open(player);
            }, () -> new RecipeListGui(module, machineType).open(player));
        });

        for (MultiblockRecipe recipe : recipes) {
            addPageItem(new ItemBuilder(recipe.createOutputPreview())
                    .name(sc("&f" + recipe.getId()))
                    .lore(buildRecipeLore(recipe, config))
                    .build(), e -> {
                if (e.isShiftClick()) {
                    config.deleteRecipe(machineType, recipe.getId());
                    player.sendMessage(MessageUtils.toComponent(sc("&cReceita &e" + recipe.getId() + " &cremovida.")));
                    new RecipeListGui(module, machineType).open(player);
                } else {
                    new RecipeEditorGui(module, recipe, false).open(player);
                }
            });
        }

        setItem(49, new ItemBuilder(Material.ARROW)
                .name(sc("&fVoltar"))
                .lore(sc("&7Volta para maquinas"))
                .build(), e -> new RecipeManagerGui(module).open(player));
    }

    private List<String> buildRecipeLore(MultiblockRecipe recipe, FoodDecayConfig config) {
        List<String> lore = new ArrayList<>();
        lore.add(sc("&7Input: &f" + recipe.getInputReferenceLabel()));
        lore.add(sc("&7Output: &f" + recipe.getOutputReferenceLabel()));
        lore.add(sc("&7Tempo: &f" + recipe.getTimeMinutes() + " min"));
        if (!recipe.getExtraIngredients().isEmpty()) {
            List<String> extras = new ArrayList<>();
            for (var ingredient : recipe.getExtraIngredients()) {
                extras.add(ingredient.getReferenceLabel());
                if (extras.size() >= 3) {
                    break;
                }
            }
            if (recipe.getExtraIngredients().size() > extras.size()) {
                extras.add("+" + (recipe.getExtraIngredients().size() - extras.size()) + " item(ns)");
            }
            lore.add(sc("&7Extras: &f" + String.join(", ", extras)));
        }

        if (recipe.getTrait() != null) {
            lore.add(sc("&7Trait: &f" + config.getTraitDisplayName(recipe.getTrait())));
        }
        if (recipe.getRequiresTrait() != null) {
            lore.add(sc("&7Req. trait: &f" + config.getTraitDisplayName(recipe.getRequiresTrait())));
        }
        if (recipe.getRequiresRecipe() != null) {
            lore.add(sc("&7Req. receita: &f" + recipe.getRequiresRecipe()));
        }
        if (!recipe.getNutritionGroups().isEmpty()) {
            List<String> nutritionLabels = new ArrayList<>();
            for (String groupName : recipe.getNutritionGroups()) {
                try {
                    nutritionLabels.add(module.getNutritionManager().getGroupDisplayName(
                            com.midgard.fooddecay.NutritionManager.FoodGroup.valueOf(groupName)));
                } catch (IllegalArgumentException ignored) {
                    nutritionLabels.add(groupName);
                }
            }
            lore.add(sc("&7Nutricao: &f" + String.join(", ", nutritionLabels)));
        }
        if (recipe.getProfession() != null) {
            lore.add(sc("&7Profissao: &f" + recipe.getProfession() + " Lv." + recipe.getProfessionLevel()));
        }
        if (recipe.getExperienceProfession() != null) {
            lore.add(sc("&7Exp: &f" + recipe.getExperienceReward() + " (" + recipe.getExperienceProfession() + ")"));
        }

        lore.add("");
        lore.add(sc("&8Clique &7editar"));
        lore.add(sc("&8Shift &7excluir"));
        return lore;
    }
}
