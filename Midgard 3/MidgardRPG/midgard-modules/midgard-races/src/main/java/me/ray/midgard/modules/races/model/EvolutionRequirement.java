package me.ray.midgard.modules.races.model;

import java.util.List;
import java.util.Map;

/**
 * Representa um requisito de evolução de raça.
 * Tipos suportados:
 * - LEVEL: Nível mínimo
 * - KILLS: Kills totais de mobs
 * - KILL_TYPE: Kills de tipo específico de mob
 * - ITEM: Item específico no inventário (consumido ao evoluir)
 * - MONEY: Custo em dinheiro
 * - PERMISSION: Permissão necessária
 */
public record EvolutionRequirement(
        RequirementType type,
        String key,
        double value,
        String displayName
) {

    public enum RequirementType {
        LEVEL,
        KILLS,
        KILL_TYPE,
        ITEM,
        MONEY,
        PERMISSION
    }

    /**
     * Verifica se o requisito é atendido pelo jogador.
     */
    public boolean isMet(org.bukkit.entity.Player player, me.ray.midgard.modules.races.data.RaceData data) {
        return switch (type) {
            case LEVEL -> data.getLevel() >= (int) value;
            case KILLS -> data.getTotalKills() >= (int) value;
            case KILL_TYPE -> data.getKillsOf(key) >= (int) value;
            case ITEM -> hasItem(player);
            case MONEY -> hasMoney(player);
            case PERMISSION -> player.hasPermission(key);
        };
    }

    /**
     * Consome o requisito (itens, dinheiro).
     */
    public void consume(org.bukkit.entity.Player player) {
        switch (type) {
            case ITEM -> consumeItem(player);
            case MONEY -> consumeMoney(player);
            default -> { /* Outros tipos não consomem nada */ }
        }
    }

    private boolean hasItem(org.bukkit.entity.Player player) {
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(key);
        if (mat == null) { return false; }
        return player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(mat), (int) value);
    }

    private void consumeItem(org.bukkit.entity.Player player) {
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(key);
        if (mat == null) { return; }
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(mat, (int) value));
    }

    private boolean hasMoney(org.bukkit.entity.Player player) {
        me.ray.midgard.core.economy.EconomyProvider eco = me.ray.midgard.core.MidgardCore.getEconomyProvider();
        if (eco == null) { return true; }
        return eco.has(player.getUniqueId(), "default", value);
    }

    private void consumeMoney(org.bukkit.entity.Player player) {
        me.ray.midgard.core.economy.EconomyProvider eco = me.ray.midgard.core.MidgardCore.getEconomyProvider();
        if (eco == null) { return; }
        eco.withdraw(player.getUniqueId(), "default", value);
    }

    /**
     * Parseia uma seção YAML em EvolutionRequirement.
     * Formato:
     *   type: LEVEL
     *   key: ""
     *   value: 10
     *   display: "Nível 10"
     */
    public static EvolutionRequirement fromConfig(Map<String, Object> map) {
        if (map == null) { return null; }

        String typeName = "LEVEL";
        if (map.get("type") instanceof String s) { typeName = s.toUpperCase(); }

        RequirementType type;
        try {
            type = RequirementType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String key = "";
        if (map.get("key") instanceof String s) { key = s; }

        double value = 0;
        if (map.get("value") instanceof Number n) { value = n.doubleValue(); }

        String display = "";
        if (map.get("display") instanceof String s) { display = s; }

        return new EvolutionRequirement(type, key, value, display);
    }

    /**
     * Parseia lista de requisitos de um ConfigurationSection.
     */
    public static List<EvolutionRequirement> fromConfigList(List<Map<?, ?>> list) {
        if (list == null || list.isEmpty()) { return List.of(); }

        return list.stream()
                .map(raw -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) raw;
                    return fromConfig(map);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
