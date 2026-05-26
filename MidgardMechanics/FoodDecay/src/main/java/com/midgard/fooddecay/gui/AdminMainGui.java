package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Main admin hub GUI with sections for all FoodDecay subsystems.
 * Provides 100% control of the plugin without commands.
 */
public class AdminMainGui extends GuiMenu {

    private final FoodDecayModule module;
    private final FoodDecayConfig config;

    public AdminMainGui(FoodDecayModule module) {
        super(sc("&8⚙ Admin - FoodDecay"), 6);
        this.module = module;
        this.config = module.getDecayConfig();
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());

        // Title
        setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name(sc("&6&lPainel de Administração"))
                .lore(sc("&7Controle total do plugin FoodDecay."),
                      sc("&7Gerencie configurações, máquinas,"),
                      sc("&7itens e jogadores por aqui."),
                      "",
                      sc("&8Clique em uma seção para editar."))
                .build());

        // ── Row 1: Core Systems ──
        setItem(10, section(Material.CLOCK, "&e&lDecay Geral",
                "&7Decaimento, stamps e comportamento.", true),
                e -> new AdminDecayGui(module).open(player));

        setItem(12, section(Material.MAGMA_CREAM, "&c&lAmbiente",
                "&7Temperatura, profundidade e estações.",
                config.isTemperatureEnabled()),
                e -> new AdminEnvironmentGui(module).open(player));

        setItem(14, section(Material.SHIELD, "&d&lConservação",
                "&7Traços e modificadores de container.",
                config.isTraitsEnabled()),
                e -> new AdminTraitsGui(module).open(player));

        setItem(16, section(Material.COOKED_BEEF, "&6&lPorções",
                "&7Sistema de porções por alimento.",
                config.isPortionsEnabled()),
                e -> new AdminPortionsGui(module).open(player));

        // ── Row 2: Processing ──
        setItem(19, section(Material.CAMPFIRE, "&c&lCozimento",
                "&7Fogueiras e receitas de cozimento.",
                config.isCookingEnabled()),
                e -> new AdminCookingGui(module).open(player));

        setItem(21, section(Material.WATER_BUCKET, "&b&lLíquidos",
                "&7Containers e tipos de líquido.",
                config.isLiquidContainersEnabled()),
                e -> new AdminLiquidGui(module).open(player));

        setItem(23, section(Material.BREWING_STAND, "&5&lFermentação",
                "&7Receitas de bebidas fermentadas.",
                config.isFermentationEnabled()),
                e -> new AdminFermentationGui(module).open(player));

        setItem(25, section(Material.ANVIL, "&7&lPeso & Tamanho",
                "&7Peso, tamanho e restrições.",
                config.isWeightEnabled()),
                e -> new AdminWeightGui(module).open(player));

        // ── Row 3: Advanced ──
        setItem(28, section(Material.GOLDEN_APPLE, "&a&lNutrição",
                "&7Grupos alimentares e bônus de vida.",
                config.isNutritionEnabled()),
                e -> new AdminNutritionGui(module).open(player));

        setItem(30, section(Material.BONE_MEAL, "&2&lCompostagem & Fumaça",
                "&7Compostagem e partículas de fumaça.",
                config.isCompostingEnabled()),
                e -> new AdminCompostGui(module).open(player));

        setItem(32, section(Material.SMITHING_TABLE, "&3&lMáquinas",
                "&7Multiblocks, QTE e processamento.",
                config.isMultiblockEnabled()),
                e -> new AdminMultiblockGui(module).open(player));

        setItem(34, section(Material.CRAFTING_TABLE, "&6&lReceitas",
                "&7Criar, editar e deletar receitas.",
                config.isMultiblockEnabled()),
                e -> new RecipeManagerGui(module).open(player));

        // ── Row 4: Extra & Tools ──
        setItem(39, section(Material.BLUE_ICE, "&b&lGelo & Conservação",
                "&7Blocos de gelo e multiplicadores.",
                config.isIceConservationEnabled()),
                e -> new AdminIceGui(module).open(player));

        setItem(41, section(Material.CHEST, "&e&lUtilitários",
                "&7Dar itens, gerenciar jogadores.",
                true),
                e -> new AdminToolsGui(module).open(player));

        // ── Row 5: Actions ──
        setItem(45, new ItemBuilder(Material.EMERALD)
                .name(sc("&a&l\u27F3 Recarregar Config"))
                .lore("", sc("&7Recarrega config.yml"),
                      sc("&7e messages.yml."), "",
                      sc("&eClique para recarregar"))
                .glow().build(),
                e -> {
                    config.reload();
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.getMsgReloadSuccess())));
                    new AdminMainGui(module).open(player);
                });

        setItem(49, new ItemBuilder(Material.BARRIER)
                .name(sc("&c&lFechar"))
                .build(), e -> player.closeInventory());
    }

    private ItemStack section(Material mat, String name, String desc, boolean enabled) {
        ItemBuilder b = new ItemBuilder(mat)
                .name(sc(name))
                .lore("", sc(desc), "",
                       sc(enabled ? "&a\u2714 Ativado" : "&c\u2718 Desativado"),
                       "", sc("&eClique para configurar"));
        if (enabled) b.glow();
        return b.build();
    }
}
