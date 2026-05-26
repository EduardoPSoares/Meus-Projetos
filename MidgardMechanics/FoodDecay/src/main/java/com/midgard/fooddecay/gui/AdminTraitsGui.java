package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.FoodTrait;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for preservation traits and container modifiers.
 */
public class AdminTraitsGui extends AdminBaseGui {

    public AdminTraitsGui(FoodDecayModule module) {
        super("&8\uD83D\uDEE1 Conservação", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminTraitsGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.SHIELD)
                .name(sc("&d&lTraços de Conservação"))
                .lore(sc("&7Configurar traços e"),
                      sc("&7modificadores de container."))
                .build());

        // ── Row 1: Main toggles ──
        setItem(10, toggle(Material.SHIELD, "&d&lTraços Ativados",
                config.isTraitsEnabled(),
                "&7Sistema de preservação", "&7por traços."),
                e -> { config.saveValue("traits.enabled", !config.isTraitsEnabled()); reopen.run(); });

        setItem(12, toggle(Material.CAMPFIRE, "&6&lAuto-Smoke (Smoker)",
                config.isAutoSmokeFromSmoker(),
                "&7Aplica SMOKED automaticamente",
                "&7ao usar o smoker."),
                e -> { config.saveValue("traits.auto-smoke-from-smoker", !config.isAutoSmokeFromSmoker()); reopen.run(); });

        setItem(14, val(Material.IRON_BARS, "&f&lMax Traços por Item",
                config.getMaxTraitsPerItem(),
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o máximo de traços por item:",
                        "traits.max-per-item", 1, reopen));

        setItem(16, toggle(Material.BARREL, "&6&lContainers Ativados",
                config.isContainerModifiersEnabled(),
                "&7Modificadores de decay",
                "&7por tipo de container."),
                e -> { config.saveValue("containers.enabled", !config.isContainerModifiersEnabled()); reopen.run(); });

        // ── Row 2: Individual traits ──
        FoodTrait[] traits = FoodTrait.values();
        int[] traitSlots = {19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < traits.length && i < traitSlots.length; i++) {
            FoodTrait trait = traits[i];
            Material icon = config.getTraitIngredient(trait);
            if (icon == null) icon = Material.PAPER;
            double mult = config.getTraitMultiplier(trait);
            String displayName = config.getTraitDisplayName(trait);

            setItem(traitSlots[i], new ItemBuilder(icon)
                    .name(sc("&f&l" + displayName))
                    .lore("",
                           sc("&7Multiplicador: &f" + mult + "x"),
                           sc("&7Ingrediente: &f" + (config.getTraitIngredient(trait) != null
                                   ? config.getTraitIngredient(trait).name() : "Nenhum")),
                           "",
                           sc("&8Editar via config.yml"))
                    .build());
        }

        // ── Row 3: Container multipliers ──
        List<String> containerLines = new ArrayList<>();
        for (Map.Entry<Material, Double> entry : config.getContainerMultipliers().entrySet()) {
            containerLines.add("&7" + entry.getKey().name() + "&8: &fx" + entry.getValue());
        }
        if (containerLines.isEmpty()) containerLines.add("&8Nenhum container configurado");
        containerLines.add("");
        containerLines.add("&8Editar via config.yml");
        setItem(30, info(Material.BARREL, "&6&lMultiplicadores de Container", containerLines));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
