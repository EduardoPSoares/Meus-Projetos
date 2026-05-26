package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.NutritionManager;
import com.midgard.fooddecay.NutritionManager.FoodGroup;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.midgard.core.utils.MessageUtils.sc;

/**
 * Cleaner nutrition dashboard focused on decision-making:
 * current status, what is missing, what each group grants, and what to eat next.
 */
public class NutritionGui extends GuiMenu {

    private static final int[] GROUP_SLOTS = {20, 21, 22, 23, 24};

    private final NutritionManager nutritionManager;
    private final FoodDecayConfig config;

    public NutritionGui(NutritionManager nutritionManager, FoodDecayConfig config) {
        super(sc(config.getGuiTitleNutrition()), 6);
        this.nutritionManager = nutritionManager;
        this.config = config;
    }

    @Override
    public void setup(Player player) {
        fillBorder(ItemBuilder.placeholder());

        GroupSnapshot[] snapshots = buildSnapshots(player);

        setItem(4, buildTitleItem(snapshots));
        setItem(11, buildRhythmItem());
        setItem(13, buildHealthBonusItem(snapshots));
        setItem(15, buildPriorityItem(snapshots));

        for (int i = 0; i < snapshots.length && i < GROUP_SLOTS.length; i++) {
            setItem(GROUP_SLOTS[i], buildGroupItem(snapshots[i]));
        }

        setItem(31, buildBalanceItem(snapshots));
        setItem(49, new ItemBuilder(Material.BARRIER)
                .name(sc(config.msg("gui-nutrition-close")))
                .build(), e -> e.getWhoClicked().closeInventory());
        setItem(51, buildInfoItem());
    }

    private GroupSnapshot[] buildSnapshots(Player player) {
        double[] nutrition = nutritionManager.getNutrition(player);
        FoodGroup[] groups = FoodGroup.values();
        GroupSnapshot[] snapshots = new GroupSnapshot[groups.length];
        double activationThreshold = config.getNutritionActivationThreshold();
        Map<String, FoodDecayConfig.GroupBonus> groupBonuses = config.getNutritionGroupBonuses();

        for (int i = 0; i < groups.length; i++) {
            FoodGroup group = groups[i];
            double value = i < nutrition.length ? nutrition[i] : 0;
            FoodDecayConfig.GroupBonus bonus = groupBonuses.get(group.name());
            double groupThreshold = bonus != null && bonus.activationThreshold() != null
                    ? bonus.activationThreshold()
                    : activationThreshold;
            boolean active = value >= groupThreshold;
            double missing = Math.max(0, groupThreshold - value);
            double healthBonus = bonus != null
                    ? bonus.healthBonus()
                    : config.getNutritionHealthBonusPerGroup();
            List<String> effects = bonus != null ? bonus.effects() : List.of();
            Map<Attribute, Double> attributes = bonus != null ? bonus.attributes() : Map.of();
            Map<String, Double> mmocoreStats = bonus != null ? bonus.mmocoreStats() : Map.of();
            List<Material> sampleFoods = nutritionManager.getFoodsForGroup(group);

            snapshots[i] = new GroupSnapshot(
                    group,
                    value,
                    missing,
                    active,
                    groupThreshold,
                    healthBonus,
                    effects,
                    attributes,
                    mmocoreStats,
                    sampleFoods
            );
        }

        return snapshots;
    }

    private ItemStack buildTitleItem(GroupSnapshot[] snapshots) {
        int activeGroups = countActiveGroups(snapshots);

        ItemBuilder builder = new ItemBuilder(Material.GOLDEN_APPLE)
                .name(sc(config.msg("gui-nutrition-title-item")))
                .lore(
                        sc("&7Veja quanto falta para ativar"),
                        sc("&7cada grupo alimentar e qual"),
                        sc("&7bonus ele libera."),
                        "",
                        sc("&7Ativacao: &f" + formatNumber(config.getNutritionActivationThreshold()) + "%"),
                        sc("&7Grupos ativos: &f" + activeGroups + "&8/&f" + snapshots.length)
                );

        if (activeGroups > 0) {
            builder.glow();
        }
        return builder.build();
    }

    private ItemStack buildRhythmItem() {
        return new ItemBuilder(Material.CLOCK)
                .name(sc("&e&lLimiar e Ritmo"))
                .lore(
                        sc("&7Cada grupo ativa ao atingir"),
                        sc("&f" + formatNumber(config.getNutritionActivationThreshold()) + "% &7de nutricao."),
                        "",
                        sc("&7Ganho: &fvaria pelo alimento"),
                        sc("&7Refeicoes fortes: &aate +" + formatNumber(config.getNutritionGainPerFood()) + "%"),
                        sc("&7Porcoes: &fdividem esse ganho"),
                        sc("&7Decaimento: &c-" + formatNumber(config.getNutritionDecayPerMinute()) + "% &7por minuto"),
                        "",
                        sc("&7Mais nutricao e saturacao"),
                        sc("&7geram avancos maiores.")
                )
                .build();
    }

    private ItemStack buildHealthBonusItem(GroupSnapshot[] snapshots) {
        int activeGroups = countActiveGroups(snapshots);
        double currentBonus = totalActiveBonus(snapshots);
        double maxBonus = totalPossibleBonus(snapshots);
        GroupSnapshot nextGroup = lowestInactiveGroup(snapshots);

        List<String> lore = new ArrayList<>();
        lore.add(sc("&7Grupos ativos: &f" + activeGroups + "&8/&f" + snapshots.length));
        lore.add(sc("&7Bonus atual: &c+" + formatNumber(currentBonus) + " ❤"));
        lore.add(sc("&7Bonus maximo: &c+" + formatNumber(maxBonus) + " ❤"));

        if (nextGroup != null) {
            lore.add("");
            lore.add(sc("&7Proximo desbloqueio: " + colorizeGroup(nextGroup)));
            lore.add(sc("&7Faltam: &f" + formatNumber(nextGroup.missingToActivate()) + "%"));
        }

        ItemBuilder builder = new ItemBuilder(Material.NETHER_STAR)
                .name(sc(config.msg("gui-nutrition-health-title")))
                .lore(lore);

        if (activeGroups > 0) {
            builder.glow();
        }
        return builder.build();
    }

    private ItemStack buildPriorityItem(GroupSnapshot[] snapshots) {
        List<GroupSnapshot> sorted = sortedByValueAscending(snapshots);
        List<GroupSnapshot> priorities = sorted.stream()
                .filter(snapshot -> !snapshot.active())
                .limit(2)
                .toList();

        List<String> lore = new ArrayList<>();
        if (priorities.isEmpty()) {
            lore.add(sc("&aTodos os grupos ja estao ativos."));
            lore.add(sc("&7Agora o foco e manter variedade"));
            lore.add(sc("&7para nao deixar nenhum cair."));
        } else {
            for (int i = 0; i < priorities.size(); i++) {
                GroupSnapshot snapshot = priorities.get(i);
                lore.add(sc("&e" + (i + 1) + ". " + colorizeGroup(snapshot) + " &8- &f"
                        + formatNumber(snapshot.value()) + "%"));
                lore.add(sc("&7Faltam: &f" + formatNumber(snapshot.missingToActivate()) + "%"));
                lore.add(sc("&7Coma: &f" + formatFoodExamples(snapshot.sampleFoods())));
                if (i < priorities.size() - 1) {
                    lore.add("");
                }
            }
        }

        ItemBuilder builder = new ItemBuilder(priorities.isEmpty() ? Material.EMERALD : Material.COMPASS)
                .name(sc(priorities.isEmpty() ? "&a&lTudo em dia" : "&6&lComa Agora"))
                .lore(lore);

        if (!priorities.isEmpty()) {
            builder.glow();
        }
        return builder.build();
    }

    private ItemStack buildGroupItem(GroupSnapshot snapshot) {
        List<String> lore = new ArrayList<>();
        lore.add(sc("&7Status: " + snapshot.statusColor() + snapshot.statusLabel()));
        lore.add(sc("&7Atual: &f" + formatNumber(snapshot.value()) + "%"));
        if (snapshot.active()) {
            lore.add(sc("&7Limiar: &aAtivo acima de "
                    + formatNumber(snapshot.activationThreshold()) + "%"));
        } else {
            lore.add(sc("&7Faltam: &f" + formatNumber(snapshot.missingToActivate()) + "% &7para ativar"));
        }
        lore.add(sc("&7Barra: " + buildBar(snapshot.value(), snapshot.color())));
        lore.add("");
        lore.add(sc("&7Bonus ao ativar: &c+" + formatNumber(snapshot.healthBonus()) + " ❤"));

        String effects = formatEffects(snapshot.effects());
        if (!effects.isEmpty()) {
            lore.add(sc("&7Efeitos: &f" + effects));
        }

        String attributes = formatAttributes(snapshot.attributes());
        if (!attributes.isEmpty()) {
            lore.add(sc("&7Atributos: &f" + attributes));
        }

        String stats = formatMmocoreStats(snapshot.mmocoreStats());
        if (!stats.isEmpty()) {
            lore.add(sc("&7MMOCore: &f" + stats));
        }

        lore.add("");
        lore.add(sc("&7Exemplos: &f" + formatFoodExamples(snapshot.sampleFoods())));

        ItemBuilder builder = new ItemBuilder(nutritionManager.getGroupIcon(snapshot.group()))
                .name(sc(snapshot.color() + "&l" + snapshot.displayName()))
                .lore(lore);

        if (snapshot.active()) {
            builder.glow();
        }
        return builder.build();
    }

    private ItemStack buildBalanceItem(GroupSnapshot[] snapshots) {
        List<GroupSnapshot> sorted = sortedByValueAscending(snapshots);
        GroupSnapshot lowest = sorted.getFirst();
        GroupSnapshot highest = sorted.getLast();
        List<GroupSnapshot> active = List.of(snapshots).stream()
                .filter(GroupSnapshot::active)
                .toList();

        List<String> lore = new ArrayList<>();
        lore.add(sc("&7Maior grupo: " + colorizeGroup(highest) + " &8- &f" + formatNumber(highest.value()) + "%"));
        lore.add(sc("&7Menor grupo: " + colorizeGroup(lowest) + " &8- &f" + formatNumber(lowest.value()) + "%"));
        lore.add(sc("&7Media geral: &f" + formatNumber(averageValue(snapshots)) + "%"));
        lore.add("");
        if (active.isEmpty()) {
            lore.add(sc("&7Ativos agora: &cNenhum grupo ativo"));
        } else {
            lore.add(sc("&7Ativos agora: &a" + active.stream()
                    .map(GroupSnapshot::displayName)
                    .collect(Collectors.joining("&7, &a"))));
        }
        lore.add(sc("&7Meta: manter todos acima de &f"
                + formatNumber(config.getNutritionActivationThreshold()) + "%"));

        return new ItemBuilder(Material.BOOK)
                .name(sc("&b&lPanorama do Dia"))
                .lore(lore)
                .build();
    }

    private ItemStack buildInfoItem() {
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .name(sc(config.msg("gui-nutrition-info-title")))
                .lore(
                        sc("&7Cada comida abastece um ou mais"),
                        sc("&7grupos alimentares diferentes."),
                        "",
                        sc("&7Snacks leves sobem menos."),
                        sc("&7Pratos completos rendem mais."),
                        sc("&7Itens em porcoes dividem o ganho."),
                        "",
                        sc("&7Quando um grupo passa de &f"
                                + formatNumber(config.getNutritionActivationThreshold()) + "%&7,"),
                        sc("&7ele libera o bonus configurado."),
                        "",
                        sc("&7Comer sempre o mesmo tipo"),
                        sc("&7nao sustenta o menu inteiro.")
                )
                .build();
    }

    private int countActiveGroups(GroupSnapshot[] snapshots) {
        int count = 0;
        for (GroupSnapshot snapshot : snapshots) {
            if (snapshot.active()) {
                count++;
            }
        }
        return count;
    }

    private double totalActiveBonus(GroupSnapshot[] snapshots) {
        double total = 0;
        for (GroupSnapshot snapshot : snapshots) {
            if (snapshot.active()) {
                total += snapshot.healthBonus();
            }
        }
        return total;
    }

    private double totalPossibleBonus(GroupSnapshot[] snapshots) {
        double total = 0;
        for (GroupSnapshot snapshot : snapshots) {
            total += snapshot.healthBonus();
        }
        return total;
    }

    private double averageValue(GroupSnapshot[] snapshots) {
        if (snapshots.length == 0) {
            return 0;
        }

        double total = 0;
        for (GroupSnapshot snapshot : snapshots) {
            total += snapshot.value();
        }
        return total / snapshots.length;
    }

    private GroupSnapshot lowestInactiveGroup(GroupSnapshot[] snapshots) {
        return sortedByValueAscending(snapshots).stream()
                .filter(snapshot -> !snapshot.active())
                .findFirst()
                .orElse(null);
    }

    private List<GroupSnapshot> sortedByValueAscending(GroupSnapshot[] snapshots) {
        return List.of(snapshots).stream()
                .sorted(Comparator.comparingDouble(GroupSnapshot::value))
                .toList();
    }

    private String buildBar(double value, String color) {
        int totalSegments = 14;
        int filled = Math.max(0, Math.min(totalSegments,
                (int) Math.round((value / 100.0) * totalSegments)));
        int empty = totalSegments - filled;
        return color + "|".repeat(filled) + "&8" + "|".repeat(empty);
    }

    private String formatEffects(List<String> effects) {
        return effects.stream()
                .limit(2)
                .map(this::formatEffect)
                .filter(effect -> !effect.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String formatEffect(String rawEffect) {
        if (rawEffect == null || rawEffect.isBlank()) {
            return "";
        }

        String[] parts = rawEffect.split(":");
        String effectName = formatToken(parts[0]);
        int amplifier = 0;
        int seconds = 0;

        if (parts.length >= 2) {
            try {
                amplifier = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                amplifier = 0;
            }
        }
        if (parts.length >= 3) {
            try {
                seconds = Integer.parseInt(parts[2].trim()) / 20;
            } catch (NumberFormatException ignored) {
                seconds = 0;
            }
        }

        String suffix = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
        if (seconds > 0) {
            return effectName + suffix + " (" + seconds + "s)";
        }
        return effectName + suffix;
    }

    private String formatMmocoreStats(Map<String, Double> stats) {
        return stats.entrySet().stream()
                .limit(2)
                .map(entry -> formatToken(entry.getKey()) + " +" + formatNumber(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private String formatAttributes(Map<Attribute, Double> attributes) {
        return attributes.entrySet().stream()
                .limit(2)
                .map(entry -> formatToken(entry.getKey().name()) + " +" + formatNumber(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private String formatFoodExamples(List<Material> foods) {
        if (foods == null || foods.isEmpty()) {
            return "Sem exemplos";
        }

        return foods.stream()
                .limit(3)
                .map(material -> formatToken(material.name()))
                .collect(Collectors.joining(", "));
    }

    private String colorizeGroup(GroupSnapshot snapshot) {
        return snapshot.color() + snapshot.displayName();
    }

    private String formatToken(String raw) {
        String[] parts = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }

    private record GroupSnapshot(
            FoodGroup group,
            double value,
            double missingToActivate,
            boolean active,
            double activationThreshold,
            double healthBonus,
            List<String> effects,
            Map<Attribute, Double> attributes,
            Map<String, Double> mmocoreStats,
            List<Material> sampleFoods
    ) {
        String color() {
            return group.getColor();
        }

        String displayName() {
            return group.getDisplayName();
        }

        String statusLabel() {
            if (active) {
                return "Ativo";
            }
            if (value >= 15) {
                return "Quase ativo";
            }
            if (value > 0) {
                return "Baixo";
            }
            return "Vazio";
        }

        String statusColor() {
            if (active) {
                return "&a";
            }
            if (value >= 15) {
                return "&e";
            }
            if (value > 0) {
                return "&6";
            }
            return "&c";
        }
    }
}
