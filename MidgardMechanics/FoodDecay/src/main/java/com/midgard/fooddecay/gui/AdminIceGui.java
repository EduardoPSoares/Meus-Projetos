package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Admin GUI for ice conservation settings: multiplier, warming/freezing duration, ice blocks.
 */
public class AdminIceGui extends AdminBaseGui {

    public AdminIceGui(FoodDecayModule module) {
        super("&8\u2699 Gelo & Conservação", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminIceGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.BLUE_ICE)
                .name(sc("&b&lConservação por Gelo"))
                .lore(sc("&7Blocos de gelo perto de comida"),
                      sc("&7reduzem a velocidade de decay."))
                .build());

        // ── Row 1: Main settings ──
        setItem(10, toggle(Material.BLUE_ICE, "&b&lGelo Ativado",
                config.isIceConservationEnabled(),
                "&7Ativa/desativa o sistema de", "&7conservação por blocos de gelo."),
                e -> { config.saveValue("ice-conservation.enabled", !config.isIceConservationEnabled()); reopen.run(); });

        setItem(12, val(Material.DIAMOND, "&b&lMultiplicador",
                config.getIceMultiplier() + "x",
                "&eClique para editar (ex: 0.3)"),
                e -> editDouble(player, "&eDigite o multiplicador de gelo (ex: 0.3 = 30% da velocidade):",
                        "ice-conservation.multiplier", 0.0, reopen));

        setItem(14, val(Material.SNOWBALL, "&f&lDuração de Congelamento",
                config.getIceFreezingDuration() + " min",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite a duração de congelamento (minutos):",
                        "ice-conservation.freezing-duration", 1, reopen));

        setItem(16, val(Material.MAGMA_CREAM, "&c&lDuração de Aquecimento",
                config.getIceWarmingDuration() + " min",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite a duração de aquecimento (minutos):",
                        "ice-conservation.warming-duration", 1, reopen));

        // ── Row 2: Ice blocks info ──
        setItem(22, info(Material.ICE, "&b&lBlocos de Gelo Aceitos",
                config.getIceBlocks().stream()
                        .map(m -> "&8\u25B8 &f" + m.name())
                        .toList()));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
