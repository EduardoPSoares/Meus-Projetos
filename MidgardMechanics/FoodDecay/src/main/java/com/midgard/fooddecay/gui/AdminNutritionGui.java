package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for the nutrition system.
 */
public class AdminNutritionGui extends AdminBaseGui {

    public AdminNutritionGui(FoodDecayModule module) {
        super("&8\uD83C\uDF4E Nutrição", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminNutritionGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.GOLDEN_APPLE)
                .name(sc("&a&lSistema de Nutrição"))
                .lore(sc("&7Grupos alimentares e"),
                      sc("&7bônus de vida."))
                .build());

        // ── Row 1 ──
        setItem(10, toggle(Material.GOLDEN_APPLE, "&a&lNutrição Ativada",
                config.isNutritionEnabled(),
                "&7Habilita o sistema de",
                "&7nutrição e grupos."),
                e -> { config.saveValue("nutrition.enabled", !config.isNutritionEnabled()); reopen.run(); });

        setItem(12, new ItemBuilder(Material.APPLE)
                .name(sc("&a&lEscala Maxima de Ganho"))
                .lore(
                        "",
                        sc("&7Atual: &f" + config.getNutritionGainPerFood() + "% max"),
                        "",
                        sc("&7Comidas fracas sobem menos."),
                        sc("&7Refeicoes fortes chegam"),
                        sc("&7perto deste teto."),
                        "",
                        sc("&eClique para editar")
                )
                .build(),
                e -> editDouble(player, "&eDigite a escala maxima de ganho (%):",
                        "nutrition.gain-per-food", 0.1, reopen));

        setItem(14, val(Material.CLOCK, "&e&lDecay por Minuto",
                config.getNutritionDecayPerMinute() + " pts/min",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o decay por minuto (pts):",
                        "nutrition.decay-per-minute", 0.0, reopen));

        setItem(16, val(Material.NETHER_STAR, "&c&lBônus de Vida por Grupo",
                config.getNutritionHealthBonusPerGroup() + " HP",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o bônus por grupo (HP):",
                        "nutrition.health-bonus-per-group", 0.0, reopen));

        // ── Row 2 ──
        setItem(19, val(Material.TARGET, "&e&lLimite de Ativação",
                config.getNutritionActivationThreshold() + "%",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o threshold de ativação (%):",
                        "nutrition.activation-threshold", 0.0, reopen));

        setItem(21, toggle(Material.TOTEM_OF_UNDYING, "&c&lReset ao Morrer",
                config.isNutritionResetOnDeath(),
                "&7Zera a nutrição quando",
                "&7o jogador morre."),
                e -> { config.saveValue("nutrition.reset-on-death", !config.isNutritionResetOnDeath()); reopen.run(); });

        // ── Row 3: Nutrition groups ──
        Map<String, String> groupNames = config.getNutritionGroupDisplayNames();
        Map<String, String> groupColors = config.getNutritionGroupColors();
        Map<String, Material> groupIcons = config.getNutritionGroupIcons();

        int[] groupSlots = {28, 29, 30, 31, 32, 33, 34};
        int idx = 0;
        for (Map.Entry<String, String> entry : groupNames.entrySet()) {
            if (idx >= groupSlots.length) break;
            String key = entry.getKey();
            String displayName = entry.getValue();
            String color = groupColors.getOrDefault(key, "&7");
            Material icon = groupIcons.getOrDefault(key, Material.STONE);

            FoodDecayConfig.GroupBonus bonus = config.getNutritionGroupBonuses().get(key);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(sc("&7ID: &f" + key));
            if (bonus != null) {
                lore.add(sc("&7Bônus vida: &c+" + bonus.healthBonus()));
                if (!bonus.effects().isEmpty()) {
                    lore.add(sc("&7Efeitos: &f" + bonus.effects().size()));
                }
            }
            lore.add("");
            lore.add(sc("&8Editar via config.yml"));

            setItem(groupSlots[idx], new ItemBuilder(icon)
                    .name(sc(color + "&l" + displayName))
                    .lore(lore)
                    .build());
            idx++;
        }

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
