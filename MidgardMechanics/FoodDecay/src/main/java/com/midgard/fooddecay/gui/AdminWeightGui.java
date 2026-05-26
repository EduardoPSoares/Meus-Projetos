package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.WeightFormatUtil;
import com.midgard.fooddecay.WeightManager.FoodSize;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for the weight/size system and decay-by-weight.
 */
public class AdminWeightGui extends AdminBaseGui {

    public AdminWeightGui(FoodDecayModule module) {
        super("&8\u2696 Peso & Tamanho", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminWeightGui(module).open(player);

        setItem(4, new ItemBuilder(Material.ANVIL)
                .name(sc("&7&lSistema de Peso & Tamanho"))
                .lore(sc("&7Peso, tamanho e restricoes"),
                        sc("&7de containers."))
                .build());

        setItem(10, toggle(Material.ANVIL, "&7&lPeso Ativado",
                config.isWeightEnabled(),
                "&7Habilita peso e tamanho",
                "&7nos alimentos."),
                e -> {
                    config.saveValue("weight-size.enabled", !config.isWeightEnabled());
                    reopen.run();
                });

        setItem(12, toggle(Material.BARREL, "&6&lRestricoes de Container",
                config.isWeightContainerRestrictionsEnabled(),
                "&7Bloqueia tamanhos incompativeis",
                "&7em certos containers."),
                e -> {
                    config.saveValue("weight-size.container-restrictions", !config.isWeightContainerRestrictionsEnabled());
                    reopen.run();
                });

        setItem(14, val(Material.IRON_INGOT, "&f&lMax Kg por Stack",
                WeightFormatUtil.formatKgDisplay(config.getWeightMaxKgPerStack()) + " kg",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o max kg por stack:",
                        "weight-size.max-kg-per-stack", 0.01, reopen));

        setItem(19, val(Material.GOLD_NUGGET, "&e&lTamanho Padrao",
                config.getWeightDefaultSize(),
                "&eClique para editar"),
                e -> ChatInput.request(player, sc("&eDigite o tamanho padrao (ex: SMALL, MEDIUM, LARGE):"),
                        input -> {
                            try {
                                FoodSize.valueOf(input.trim().toUpperCase());
                                config.saveValue("weight-size.default-size", input.trim().toUpperCase());
                            } catch (IllegalArgumentException ex) {
                                player.sendMessage(MessageUtils.toComponent(
                                        sc("&cTamanho invalido! Use: TINY, SMALL, MEDIUM, LARGE ou HUGE")));
                            }
                            reopen.run();
                        }, reopen));

        setItem(21, val(Material.IRON_NUGGET, "&7&lPeso Padrao",
                WeightFormatUtil.formatKgDisplay(config.getWeightDefaultKg()) + " kg",
                "&eClique para editar"),
                e -> editDouble(player, "&eDigite o peso padrao (kg):",
                        "weight-size.default-kg", 0.001, reopen));

        setItem(23, toggle(Material.FEATHER, "&e&lDecay por Peso",
                config.isDecayByWeightEnabled(),
                "&7Comida perde peso",
                "&7conforme decai."),
                e -> {
                    config.saveValue("decay-by-weight.enabled", !config.isDecayByWeightEnabled());
                    reopen.run();
                });

        setItem(25, val(Material.CLOCK, "&6&lIntervalo de Perda",
                (int) (config.getDecayByWeightLossInterval() * 100) + "% do decay",
                "&eClique para editar (0.01 - 1.0)"),
                e -> editDouble(player, "&eDigite o intervalo de perda (ex: 0.25 = a cada 25% de decay):",
                        "decay-by-weight.loss-interval", 0.01, reopen));

        List<String> foodLines = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Material, Double> entry : config.getWeightPerFoodKg().entrySet()) {
            if (count++ >= 15) {
                foodLines.add("&8... e mais");
                break;
            }
            foodLines.add("&7" + entry.getKey().name() + "&8: &f"
                    + WeightFormatUtil.formatKgDisplay(entry.getValue()) + " kg");
        }
        if (foodLines.isEmpty()) {
            foodLines.add("&8Usando padrao para todos");
        }
        foodLines.add("");
        foodLines.add("&8Editar via config.yml");
        setItem(30, info(Material.BOOK, "&7&lPesos por Alimento", foodLines));

        back(player, 45);
        close(player, 49);
    }
}
