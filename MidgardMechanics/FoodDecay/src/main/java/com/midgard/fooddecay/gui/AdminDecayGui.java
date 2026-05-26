package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Admin GUI for general decay settings, behavior stamps, and colors.
 */
public class AdminDecayGui extends AdminBaseGui {

    public AdminDecayGui(FoodDecayModule module) {
        super("&8⚙ Decay Geral", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminDecayGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.CLOCK)
                .name(sc("&e&lConfigurações Gerais de Decay"))
                .lore(sc("&7Ajuste os parâmetros"),
                      sc("&7principais de decaimento."))
                .build());

        // ── Row 1: Main decay settings ──
        setItem(10, val(Material.CLOCK, "&e&lExpiração Padrão",
                config.getDefaultExpiration() + " min",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite a expiração padrão (minutos):",
                        "default-expiration-minutes", 1, reopen));

        setItem(12, val(Material.REPEATER, "&6&lIntervalo de Verificação",
                config.getDecayCheckInterval() + " ticks",
                "&eClique para editar"),
                e -> editLong(player, "&eDigite o intervalo (ticks):",
                        "decay-check-interval-ticks", 1, reopen));

        setItem(14, val(Material.SUNFLOWER, "&e&lLimite de Aviso",
                (int) (config.getWarningThreshold() * 100) + "%",
                "&eClique para editar (0.0 - 1.0)"),
                e -> editDouble(player, "&eDigite o threshold (ex: 0.25):",
                        "warning-threshold", 0.0, reopen));

        setItem(16, toggle(Material.NAME_TAG, "&f&lMostrar Timer na Lore",
                config.showLoreTimer(),
                "&7Exibe o timer de validade", "&7na lore do item."),
                e -> { config.saveValue("show-lore-timer", !config.showLoreTimer()); reopen.run(); });

        // ── Row 2: More settings ──
        setItem(19, toggle(Material.COMPASS, "&f&lRastrear Toda Comida",
                config.trackAllFood(),
                "&7Aplica validade automática", "&7a toda comida do jogo."),
                e -> { config.saveValue("track-all-food", !config.trackAllFood()); reopen.run(); });

        setItem(21, toggle(Material.ROTTEN_FLESH, "&c&lBloquear Consumo Expirado",
                config.blockExpiredConsume(),
                "&7Impede que jogadores comam", "&7alimentos expirados."),
                e -> { config.saveValue("behavior.block-expired-consume", !config.blockExpiredConsume()); reopen.run(); });

        // Separator
        setItem(23, new ItemBuilder(Material.PAPER)
                .name(sc("&8&l━━ Stamps ━━"))
                .lore("", sc("&7Quando marcar a validade"),
                      sc("&7nos alimentos:"))
                .build());

        // ── Row 3: Stamp toggles ──
        setItem(28, toggle(Material.HOPPER, "&7Pickup",
                config.stampOnPickup(), "&7Ao pegar do chão"),
                e -> { config.saveValue("behavior.stamp-on-pickup", !config.stampOnPickup()); reopen.run(); });

        setItem(29, toggle(Material.CRAFTING_TABLE, "&7Craft",
                config.stampOnCraft(), "&7Ao craftar"),
                e -> { config.saveValue("behavior.stamp-on-craft", !config.stampOnCraft()); reopen.run(); });

        setItem(30, toggle(Material.FURNACE, "&7Furnace",
                config.stampOnFurnace(), "&7Ao sair da fornalha"),
                e -> { config.saveValue("behavior.stamp-on-furnace", !config.stampOnFurnace()); reopen.run(); });

        setItem(31, toggle(Material.CHEST, "&7Inv Click",
                config.stampOnInventoryClick(), "&7Ao clicar no inventário"),
                e -> { config.saveValue("behavior.stamp-on-inventory-click", !config.stampOnInventoryClick()); reopen.run(); });

        setItem(32, toggle(Material.ENDER_CHEST, "&7Inv Open",
                config.stampOnInventoryOpen(), "&7Ao abrir inventário"),
                e -> { config.saveValue("behavior.stamp-on-inventory-open", !config.stampOnInventoryOpen()); reopen.run(); });

        setItem(33, toggle(Material.OAK_DOOR, "&7Join",
                config.stampOnJoin(), "&7Ao entrar no servidor"),
                e -> { config.saveValue("behavior.stamp-on-join", !config.stampOnJoin()); reopen.run(); });

        setItem(34, toggle(Material.EGG, "&7Item Spawn",
                config.isStampOnItemSpawn(), "&7Ao spawnar no mundo"),
                e -> { config.saveValue("behavior.stamp-on-item-spawn", !config.isStampOnItemSpawn()); reopen.run(); });

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
