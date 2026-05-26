package me.ray.midgard.modules.professions;

import org.bukkit.Material;

import java.util.Optional;

/**
 * Tipos de profissão disponíveis no sistema.
 * Cada jogador pode ter progresso em múltiplas profissões.
 */
public enum ProfessionType {

    BLACKSMITH("Ferreiro", "ferreiro", Material.ANVIL, "⚒"),
    FARMER("Agricultor", "agricultor", Material.DIAMOND_HOE, "🌾"),
    ARCANIST("Arcanista", "arcanista", Material.ENCHANTING_TABLE, "✦"),
    CARPENTER("Carpinteiro", "carpinteiro", Material.CRAFTING_TABLE, "🪓"),
    MINER("Minerador", "minerador", Material.DIAMOND_PICKAXE, "⛏"),
    ALCHEMIST("Alquimista", "alquimista", Material.BREWING_STAND, "⚗"),
    FISHER("Pescador", "pescador", Material.FISHING_ROD, "🎣"),
    COOK("Cozinheiro", "cozinheiro", Material.FURNACE, "🍳"),
    MEDIC("Médico", "medico", Material.GOLDEN_APPLE, "✚"),
    CARTOGRAPHER("Cartógrafo", "cartografo", Material.MAP, "🗺");

    private final String displayName;
    private final String id;
    private final Material icon;
    private final String symbol;

    ProfessionType(String displayName, String id, Material icon, String symbol) {
        this.displayName = displayName;
        this.id = id;
        this.icon = icon;
        this.symbol = symbol;
    }

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
    public Material getIcon() { return icon; }
    public String getSymbol() { return symbol; }

    /**
     * Busca um ProfessionType pelo ID (case-insensitive).
     */
    public static Optional<ProfessionType> fromId(String id) {
        if (id == null || id.isBlank()) { return Optional.empty(); }
        String lower = id.toLowerCase();
        for (ProfessionType type : values()) {
            if (type.id.equals(lower) || type.name().equalsIgnoreCase(lower)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
