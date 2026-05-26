package com.midgard.fooddecay.gui;

import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayConfig.TemperatureZone;
import com.midgard.fooddecay.FoodDecayConfig.DepthZone;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for temperature, depth-temperature, and season settings.
 */
public class AdminEnvironmentGui extends AdminBaseGui {

    public AdminEnvironmentGui(FoodDecayModule module) {
        super("&8\uD83C\uDF21 Ambiente", 6, module);
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());
        Runnable reopen = () -> new AdminEnvironmentGui(module).open(player);

        // Title
        setItem(4, new ItemBuilder(Material.MAGMA_CREAM)
                .name(sc("&c&lConfigurações de Ambiente"))
                .lore(sc("&7Temperatura, profundidade"),
                      sc("&7e estações do ano."))
                .build());

        // ── Temperature ──
        setItem(10, toggle(Material.LAVA_BUCKET, "&c&lTemperatura",
                config.isTemperatureEnabled(),
                "&7Multiplica o decay baseado",
                "&7na temperatura do bioma."),
                e -> { config.saveValue("temperature.enabled", !config.isTemperatureEnabled()); reopen.run(); });

        List<String> zoneLines = new ArrayList<>();
        for (TemperatureZone z : config.getTemperatureZones()) {
            zoneLines.add("&7" + z.name() + "&8: &f≤" + z.maxTemp() + "°C &8→ &fx" + z.multiplier());
        }
        if (zoneLines.isEmpty()) zoneLines.add("&8Nenhuma zona configurada");
        zoneLines.add("");
        zoneLines.add("&8Editar zonas via config.yml");
        setItem(12, info(Material.PAPER, "&e&lZonas de Temperatura", zoneLines));

        // ── Depth ──
        setItem(19, toggle(Material.DIAMOND_PICKAXE, "&b&lProfundidade",
                config.isDepthTemperatureEnabled(),
                "&7Aplica offset de temperatura",
                "&7baseado no Y-level."),
                e -> { config.saveValue("depth-temperature.enabled", !config.isDepthTemperatureEnabled()); reopen.run(); });

        List<String> depthLines = new ArrayList<>();
        for (DepthZone d : config.getDepthZones()) {
            String sign = d.temperatureOffset() >= 0 ? "+" : "";
            depthLines.add("&7" + d.name() + "&8: &fY≤" + d.maxY()
                    + " &8→ &f" + sign + d.temperatureOffset() + "°C");
        }
        if (depthLines.isEmpty()) depthLines.add("&8Nenhuma zona configurada");
        depthLines.add("");
        depthLines.add("&8Editar zonas via config.yml");
        setItem(21, info(Material.PAPER, "&b&lZonas de Profundidade", depthLines));

        // ── Seasons ──
        setItem(28, toggle(Material.OAK_LEAVES, "&a&lEstações",
                config.isSeasonEnabled(),
                "&7Multiplica o decay baseado",
                "&7na estação atual."),
                e -> { config.saveValue("season.enabled", !config.isSeasonEnabled()); reopen.run(); });

        List<String> seasonLines = new ArrayList<>();
        for (Map.Entry<String, Double> entry : config.getSeasonMultipliers().entrySet()) {
            seasonLines.add("&7" + entry.getKey() + "&8: &fx" + entry.getValue());
        }
        if (seasonLines.isEmpty()) seasonLines.add("&8Nenhuma estação configurada");
        seasonLines.add("");
        seasonLines.add("&8Editar via config.yml");
        setItem(30, info(Material.PAPER, "&a&lMultiplicadores de Estação", seasonLines));

        // ── Bottom ──
        back(player, 45);
        close(player, 49);
    }
}
