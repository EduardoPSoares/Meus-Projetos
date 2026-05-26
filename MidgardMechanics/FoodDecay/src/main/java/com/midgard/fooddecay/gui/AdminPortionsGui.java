package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for the portions system.
 */
public class AdminPortionsGui extends AdminBaseGui {

    public AdminPortionsGui(FoodDecayModule module) {
        super("&8\uD83C\uDF56 Porções", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminPortionsGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.COOKED_BEEF)
                .name(sc("&6&lSistema de Porções"))
                .lore(sc("&7Divida alimentos em"),
                      sc("&7múltiplas porções."))
                .build());

        // ── Row 1 ──
        setItem(20, toggle(Material.COOKED_BEEF, "&6&lPorções Ativadas",
                config.isPortionsEnabled(),
                "&7Habilita o sistema de",
                "&7porções de comida."),
                e -> { config.saveValue("portions.enabled", !config.isPortionsEnabled()); reopen.run(); });

        setItem(22, val(Material.CAKE, "&e&lPorções Padrão",
                config.getPortionsDefault(),
                "&eClique para editar"),
                e -> editInt(player, "&eDigite o número padrão de porções:",
                        "portions.default-portions", 1, reopen));

        List<String> foodLines = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Material, Integer> entry : config.getPortionsPerFood().entrySet()) {
            if (count++ >= 15) { foodLines.add("&8... e mais"); break; }
            foodLines.add("&7" + entry.getKey().name() + "&8: &f" + entry.getValue());
        }
        if (foodLines.isEmpty()) foodLines.add("&8Usando padrão para todos");
        foodLines.add("");
        foodLines.add("&8Editar via config.yml");
        setItem(24, info(Material.PAPER, "&e&lPorções por Alimento", foodLines));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
