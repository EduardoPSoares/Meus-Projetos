package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.MultiblockType;
import com.midgard.fooddecay.multiblock.MultiblockType.TierDef;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Admin GUI for managing a specific machine type: enable/disable, processing time,
 * tier info, blueprint distribution.
 */
public class AdminMachineGui extends AdminBaseGui {

    private final MultiblockType type;

    public AdminMachineGui(FoodDecayModule module, MultiblockType type) {
        super("&8\u2699 " + module.getDecayConfig().getMultiblockDisplayName(type), 6, module);
        this.type = type;
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminMachineGui(module, type).open(player);
        String cfgKey = type.getConfigKey();

        boolean on = config.isMultiblockTypeEnabled(type);
        String displayName = config.getMultiblockDisplayName(type);
        int procMin = config.getMultiblockProcessingMinutes(type);

        // Title
        setItem(4, new ItemBuilder(type.getIcon())
                .name(sc("&3&l" + displayName))
                .lore(sc("&7Tipo: &f" + type.name()),
                      sc("&7Config key: &f" + cfgKey),
                      sc("&7Traço: &f" + config.getTraitDisplayName(type.getResultTrait())),
                      "", sc(on ? "&a\u2714 Ativado" : "&c\u2718 Desativado"))
                .build());

        // ── Row 1: Main settings ──
        setItem(10, toggle(type.getIcon(), "&3&lAtivado",
                on, "&7Ativa ou desativa esta", "&7máquina específica."),
                e -> {
                    config.saveValue("multiblock.structures." + cfgKey + ".enabled", !on);
                    reopen.run();
                });

        setItem(12, val(Material.CLOCK, "&e&lTempo de Processamento",
                procMin + " min",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o tempo de processamento (minutos):",
                        "multiblock.structures." + cfgKey + ".processing-minutes", 1, reopen));

        setItem(14, val(Material.NAME_TAG, "&f&lNome de Exibição",
                displayName,
                "&eClique para editar"),
                e -> {
                    player.closeInventory();
                    ChatInput.request(player, sc("&eDigite o novo nome de exibição:"), input -> {
                        config.saveValue("multiblock.structures." + cfgKey + ".display-name", input.trim());
                        reopen.run();
                    }, reopen);
                });

        // ── Row 2: Tier info ──
        int slot = 19;
        List<TierDef> tiers = type.getTiers();
        for (TierDef td : tiers) {
            if (slot > 25) break;
            setItem(slot, new ItemBuilder(td.icon())
                    .name(sc("&b&lTier " + td.tier() + " - " + td.displayName()))
                    .lore("",
                          sc("&7Bloco âncora: &f" + td.anchor().name()),
                          sc("&7Blocos totais: &f" + type.getTotalBlocks(td.tier())),
                          sc("&7Tempo padrão: &f" + td.processingMinutes() + " min"),
                          "",
                          sc("&eClique para receber a planta"))
                    .build(),
                    e -> {
                        ItemStack blueprint = module.getMultiblockManager()
                                .createBlueprintItem(type, td.tier());
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(blueprint);
                            player.sendMessage(MessageUtils.toComponent(
                                    sc("&aPlanta T" + td.tier() + " de " + displayName + " adicionada!")));
                        } else {
                            player.sendMessage(MessageUtils.toComponent(
                                    sc("&cInventário cheio!")));
                        }
                    });
            slot += 2;
        }

        // ── Row 3: Recipe info ──
        int recipeCount = config.hasRecipes(type) ? config.getRecipes(type).size() : 0;
        setItem(28, info(Material.BOOK, "&6&lReceitas (" + recipeCount + ")",
                recipeCount > 0
                        ? config.getRecipes(type).stream()
                            .map(r -> "&8\u25B8 &7" + r.getInputReferenceLabel() + " &8\u2192 &f" + r.getOutputReferenceLabel())
                            .limit(8)
                            .toList()
                        : List.of("&7Nenhuma receita configurada.", "&7Este tipo usa traço direto:", "&f" + config.getTraitDisplayName(type.getResultTrait()))));

        // Description
        setItem(30, info(Material.PAPER, "&7&lDescrição",
                config.getMultiblockDescription(type)));

        // ── Bottom ──
        setItem(45, new ItemBuilder(Material.ARROW)
                .name(sc("&f&l\u2190 Voltar"))
                .lore("", sc("&7Voltar ao menu de máquinas"))
                .build(), e -> new AdminMultiblockGui(module).open(player));

        close(player, 49);
    }
}
