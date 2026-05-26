package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Admin GUI for selecting which machine recipes to manage.
 */
public class RecipeManagerGui extends GuiMenu {

    private static final ItemStack FRAME_ITEM = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(" ")
            .hideFlags()
            .build();
    private static final ItemStack FILL_ITEM = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .hideFlags()
            .build();

    private final FoodDecayModule module;

    public RecipeManagerGui(FoodDecayModule module) {
        super(sc("&8Receitas"), 6);
        this.module = module;
    }

    @Override
    public void setup(Player player) {
        FoodDecayConfig config = module.getDecayConfig();

        fill(FILL_ITEM);
        fillBorder(FRAME_ITEM);

        setItem(4, new ItemBuilder(Material.CRAFTING_TABLE)
                .name(sc("&fReceitas admin"))
                .lore(sc("&7Escolha uma maquina"),
                        sc("&7para criar ou editar receitas"))
                .build());

        MultiblockType[] types = MultiblockType.values();
        for (int i = 0; i < types.length && i < 5; i++) {
            MultiblockType type = types[i];
            int slot = 20 + i;
            int recipeCount = config.getRecipes(type).size();

            setItem(slot, new ItemBuilder(type.getIcon())
                    .name(sc("&f" + config.getMultiblockDisplayName(type)))
                    .lore(sc("&7Receitas: &f" + recipeCount),
                            sc("&7Clique para abrir"))
                    .build(), e -> new RecipeListGui(module, type).open(player));
        }

        setItem(45, new ItemBuilder(Material.ARROW)
                .name(sc("&fVoltar"))
                .lore(sc("&7Retorna ao painel admin"))
                .build(), e -> new AdminMainGui(module).open(player));

        setItem(49, new ItemBuilder(Material.BARRIER)
                .name(sc("&cFechar"))
                .lore(sc("&7Fecha este menu"))
                .build(), e -> player.closeInventory());
    }
}
