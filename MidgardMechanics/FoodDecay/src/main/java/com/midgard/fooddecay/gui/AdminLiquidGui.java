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
 * Admin GUI for the liquid container system.
 */
public class AdminLiquidGui extends AdminBaseGui {

    public AdminLiquidGui(FoodDecayModule module) {
        super("&8\uD83D\uDCA7 Líquidos", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminLiquidGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.WATER_BUCKET)
                .name(sc("&b&lContainers de Líquido"))
                .lore(sc("&7Configurar containers,"),
                      sc("&7capacidades e tipos."))
                .build());

        // ── Row 1 ──
        setItem(11, toggle(Material.WATER_BUCKET, "&b&lLíquidos Ativados",
                config.isLiquidContainersEnabled(),
                "&7Habilita o sistema de",
                "&7líquidos em containers."),
                e -> { config.saveValue("liquid-containers.enabled", !config.isLiquidContainersEnabled()); reopen.run(); });

        setItem(13, val(Material.BUCKET, "&e&lQuantidade ao Despejar",
                config.getLiquidPourAmount() + " mB",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite a quantidade ao despejar (mB):",
                        "liquid-containers.pour-amount-mb", 1, reopen));

        setItem(15, val(Material.CAULDRON, "&6&lmB por Nível de Caldeira",
                config.getLiquidMbPerCauldronLevel() + " mB",
                "&eClique para editar"),
                e -> editInt(player, "&eDigite mB por nível de caldeira:",
                        "liquid-containers.mb-per-cauldron-level", 1, reopen));

        // ── Row 2: Info ──
        List<String> containerLines = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Material, Integer> entry : config.getLiquidContainerCapacities().entrySet()) {
            if (count++ >= 15) { containerLines.add("&8... e mais"); break; }
            containerLines.add("&7" + entry.getKey().name() + "&8: &f" + entry.getValue() + " mB");
        }
        if (containerLines.isEmpty()) containerLines.add("&8Nenhum container configurado");
        containerLines.add("");
        containerLines.add("&8Editar via config.yml");
        setItem(20, info(Material.GLASS_BOTTLE, "&b&lCapacidades de Container", containerLines));

        List<String> typeLines = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.getLiquidDisplayNames().entrySet()) {
            String color = config.getLiquidColor(entry.getKey());
            typeLines.add(color + entry.getValue() + " &8(" + entry.getKey() + ")");
        }
        if (typeLines.isEmpty()) typeLines.add("&8Nenhum tipo configurado");
        typeLines.add("");
        typeLines.add("&8Editar via config.yml");
        setItem(24, info(Material.POTION, "&b&lTipos de Líquido", typeLines));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
