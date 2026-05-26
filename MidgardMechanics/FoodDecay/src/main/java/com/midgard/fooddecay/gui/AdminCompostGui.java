package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Admin GUI for the composting and ambient smoke systems.
 */
public class AdminCompostGui extends AdminBaseGui {

    public AdminCompostGui(FoodDecayModule module) {
        super("&8\u267B Compostagem & Fumaça", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminCompostGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.BONE_MEAL)
                .name(sc("&2&lCompostagem & Fumaça Ambiente"))
                .lore(sc("&7Compostagem de comida estragada"),
                      sc("&7e partículas de fumaça."))
                .build());

        // ── Row 1: Composting ──
        setItem(10, toggle(Material.BONE_MEAL, "&2&lCompostagem Ativada",
                config.isCompostingEnabled(),
                "&7Permite compostar comida",
                "&7estragada em fertilizante."),
                e -> { config.saveValue("composting.enabled", !config.isCompostingEnabled()); reopen.run(); });

        setItem(12, toggle(Material.ROTTEN_FLESH, "&e&lDecay Parcial",
                config.isCompostPartialDecay(),
                "&7Permite compostar comida",
                "&7parcialmente estragada."),
                e -> { config.saveValue("composting.allow-partial-decay", !config.isCompostPartialDecay()); reopen.run(); });

        setItem(14, val(Material.SUNFLOWER, "&e&lDecay Minimo",
                (int) (config.getCompostMinDecay() * 100) + "%",
                "&eClique para editar (0.0 - 1.0)"),
                e -> editDouble(player, "&eDigite o decay mínimo (ex: 0.5):",
                        "composting.partial-decay-threshold", 0.0, reopen));

        setItem(16, new ItemBuilder(Material.BONE_MEAL)
                .name(sc("&f&lResultado"))
                .lore("",
                      sc("&7Material: &f" + config.getCompostResultMaterial()),
                      sc("&7Quantidade: &f" + config.getCompostResultAmount()),
                      "",
                      sc("&8Editar via config.yml"))
                .build());

        // ── Row 2: Separator ──
        setItem(22, new ItemBuilder(Material.PAPER)
                .name(sc("&8&l━━ Fumaça Ambiente ━━"))
                .lore("", sc("&7Partículas de fumaça"),
                      sc("&7decorativas para comidas."))
                .build());

        // ── Row 3: Ambient smoke ──
        setItem(28, toggle(Material.CAMPFIRE, "&6&lFumaça Ativada",
                config.isAmbientSmokeEnabled(),
                "&7Gera partículas de fumaça",
                "&7em comidas defumadas."),
                e -> { config.saveValue("ambient-smoke.enabled", !config.isAmbientSmokeEnabled()); reopen.run(); });

        setItem(30, val(Material.REPEATER, "&e&lIntervalo",
                config.getAmbientSmokeInterval() + " ticks",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o intervalo (ticks):",
                        "ambient-smoke.interval-ticks", 1, reopen));

        setItem(32, val(Material.BLAZE_POWDER, "&6&lQuantidade",
                config.getAmbientSmokeCount() + " partículas",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite a quantidade de partículas:",
                        "ambient-smoke.particle-count", 0, reopen));

        setItem(34, val(Material.ARROW, "&f&lAltura",
                config.getAmbientSmokeHeight() + " blocos",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite a altura máxima (blocos):",
                        "ambient-smoke.height", 0.0, reopen));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
