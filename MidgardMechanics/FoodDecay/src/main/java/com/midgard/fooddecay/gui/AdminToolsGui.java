package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Admin GUI for utility actions: give items, manage players, toggle GUI/vinegar settings.
 */
public class AdminToolsGui extends AdminBaseGui {

    public AdminToolsGui(FoodDecayModule module) {
        super("&8\u2699 Utilitários", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminToolsGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.CHEST)
                .name(sc("&e&lUtilitários & Ferramentas"))
                .lore(sc("&7Dar itens, gerenciar jogadores"),
                      sc("&7e configurar GUI/receitas."))
                .build());

        // ── Row 1: Give items ──
        setItem(11, new ItemBuilder(Material.PAPER)
                .name(sc("&3&lDar Planta (Blueprint)"))
                .lore("", sc("&7Selecione a maquina e o tier"),
                      sc("&7para receber a planta."),
                      "", sc("&eClique para abrir seletor"))
                .glow().build(),
                e -> new BlueprintSelectorGui(module, player).open(player));

        setItem(15, new ItemBuilder(Material.BARREL)
                .name(sc("&5&lDar Barril de Fermentacao"))
                .lore("", sc("&7Receba um barril de fermentação"),
                      sc("&7marcado para colocar no mundo."),
                      "", sc("&eClique para receber"))
                .glow().build(),
                e -> {
                    ItemStack barrel = module.getFermentationManager().createFermentBarrelItem();
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(barrel);
                        player.sendMessage(MessageUtils.toComponent(
                                sc("&aBarril de fermentação adicionado!")));
                    } else {
                        player.sendMessage(MessageUtils.toComponent(sc("&cInventário cheio!")));
                    }
                });

        // ── Row 2: Player management ──
        setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                .name(sc("&c&lResetar Nutrição"))
                .lore("", sc("&7Reseta a nutrição de um jogador"),
                      sc("&7(remove bônus de vida)."),
                      "", sc("&eClique para selecionar jogador"))
                .build(),
                e -> {
                    player.closeInventory();
                    ChatInput.request(player, sc("&eDigite o nome do jogador:"), input -> {
                        Player target = Bukkit.getPlayerExact(input.trim());
                        if (target != null) {
                            module.getNutritionManager().resetNutrition(target);
                            player.sendMessage(MessageUtils.toComponent(
                                    sc("&aNutrição de &f" + target.getName() + " &aresetada!")));
                        } else {
                            player.sendMessage(MessageUtils.toComponent(
                                    sc("&cJogador não encontrado: " + input.trim())));
                        }
                        reopen.run();
                    }, reopen);
                });

        // ── Row 3: System toggles ──
        setItem(29, toggle(Material.PAINTING, "&e&lGUI Ativada",
                config.isGuiEnabled(),
                "&7Ativa/desativa o sistema", "&7de menus visuais."),
                e -> { config.saveValue("gui.enabled", !config.isGuiEnabled()); reopen.run(); });

        setItem(31, toggle(Material.SPYGLASS, "&f&lInspecionar com Shift+Click",
                config.isInspectOnShiftClick(),
                "&7Shift + clique direito com comida",
                "&7abre a GUI de inspeção."),
                e -> { config.saveValue("gui.inspect-on-shift-click", !config.isInspectOnShiftClick()); reopen.run(); });

        setItem(33, toggle(Material.HONEY_BOTTLE, "&e&lReceita de Vinagre",
                config.isVinegarRecipeEnabled(),
                "&7Receita shapeless para criar", "&7vinagre para conservas."),
                e -> { config.saveValue("vinegar.enabled", !config.isVinegarRecipeEnabled()); reopen.run(); });

        setItem(35, toggle(Material.CAULDRON, "&6&lReceitas de Caldeirao",
                config.isCauldronRecipesEnabled(),
                "&7Receitas de ensopado no", "&7caldeirão com água."),
                e -> { config.saveValue("cauldron-recipes.enabled", !config.isCauldronRecipesEnabled()); reopen.run(); });

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }

    /**
     * Small selector GUI for choosing machine type + tier to give a blueprint.
     */
    private static class BlueprintSelectorGui extends AdminBaseGui {

        private final Player target;

        BlueprintSelectorGui(FoodDecayModule module, Player target) {
            super("&8\u2699 Selecionar Planta", 6, module);
            this.target = target;
        }

        @Override
        public void setup(Player player) {
            fillBorder(ItemBuilder.placeholder());

            setItem(4, new ItemBuilder(Material.PAPER)
                    .name(sc("&3&lSelecione a Maquina"))
                    .lore(sc("&7Clique em uma máquina para"),
                          sc("&7receber suas plantas (todos os tiers)."))
                    .build());

            int slot = 19;
            for (MultiblockType type : MultiblockType.values()) {
                if (slot > 25) break;
                String displayName = config.getMultiblockDisplayName(type);
                int maxTier = type.getMaxTier();

                setItem(slot, new ItemBuilder(type.getIcon())
                        .name(sc("&a&l" + displayName))
                        .lore("", sc("&7Tiers: &f1 - " + maxTier),
                              "", sc("&eClique para receber todos os tiers"))
                        .build(),
                        e -> {
                            int given = 0;
                            for (int t = 1; t <= maxTier; t++) {
                                ItemStack bp = module.getMultiblockManager().createBlueprintItem(type, t);
                                if (target.getInventory().firstEmpty() != -1) {
                                    target.getInventory().addItem(bp);
                                    given++;
                                }
                            }
                            target.sendMessage(MessageUtils.toComponent(
                                    sc("&a" + given + " plantas de " + displayName + " adicionadas!")));
                        });
                slot += 2;
            }

            // Back to tools
            setItem(45, new ItemBuilder(Material.ARROW)
                    .name(sc("&f&l\u2190 Voltar"))
                    .lore("", sc("&7Voltar aos utilitários"))
                    .build(), e -> new AdminToolsGui(module).open(player));

            close(player, 49);
        }
    }
}
