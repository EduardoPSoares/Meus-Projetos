package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.modules.professions.ProfessionsModule;

/**
 * Tiers da Smeltery que definem tamanho, capacidade e temperaturas máximas.
 */
public enum SmelteryTier {

    SMALL(1,
            "Fundição Pequena", "<gray>Fundição Pequena",
            3, 3, 3,    // 3x3x3 interior
            4320,        // 4320mb = 30 ingots
            1200,        // temp máxima 1200°C
            1, 0),       // 1 drain, level 0

    MEDIUM(2,
            "Fundição Média", "<yellow>Fundição Média",
            3, 4, 3,    // 3x4x3 interior (mais alto)
            8640,        // 60 ingots
            1800,
            2, 15),

    LARGE(3,
            "Fundição Grande", "<green>Fundição Grande",
            5, 5, 5,    // 5x5x5 interior
            20736,       // 144 ingots
            2200,
            3, 35),

    MASTER(4,
            "Fundição de Mestre", "<blue>Fundição de Mestre",
            5, 6, 5,    // 5x6x5 interior
            34560,       // 240 ingots
            2800,
            4, 60),

    LEGENDARY(5,
            "Fundição Lendária", "<light_purple>Fundição Lendária",
            7, 7, 7,    // 7x7x7 interior
            57600,       // 400 ingots
            3500,
            6, 85);

    private final int level;
    private final String name;
    private final String formattedName;
    private final int interiorWidth;
    private final int interiorHeight;
    private final int interiorDepth;
    private final int tankCapacity; // em mb
    private final int maxTemperature; // em °C
    private final int maxDrains;
    private final int requiredProfessionLevel;

    SmelteryTier(int level, String name, String formattedName,
                 int interiorWidth, int interiorHeight, int interiorDepth,
                 int tankCapacity, int maxTemperature,
                 int maxDrains, int requiredProfessionLevel) {
        this.level = level;
        this.name = name;
        this.formattedName = formattedName;
        this.interiorWidth = interiorWidth;
        this.interiorHeight = interiorHeight;
        this.interiorDepth = interiorDepth;
        this.tankCapacity = tankCapacity;
        this.maxTemperature = maxTemperature;
        this.maxDrains = maxDrains;
        this.requiredProfessionLevel = requiredProfessionLevel;
    }

    public int getLevel() { return level; }
    public String getName() {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("smeltery_tier." + name().toLowerCase()) : name;
    }
    public String getFormattedName() {
        String prefix = formattedName.substring(0, formattedName.length() - name.length());
        return prefix + getName();
    }
    public int getInteriorWidth() { return interiorWidth; }
    public int getInteriorHeight() { return interiorHeight; }
    public int getInteriorDepth() { return interiorDepth; }
    public int getTankCapacity() { return tankCapacity; }
    public int getMaxTemperature() { return maxTemperature; }
    public int getMaxDrains() { return maxDrains; }
    public int getRequiredProfessionLevel() { return requiredProfessionLevel; }

    /**
     * Largura total do multibloco (interior + 2 paredes).
     */
    public int getTotalWidth() { return interiorWidth + 2; }

    /**
     * Altura total do multibloco (interior + base + lava).
     */
    public int getTotalHeight() { return interiorHeight + 2; }

    /**
     * Profundidade total do multibloco.
     */
    public int getTotalDepth() { return interiorDepth + 2; }

    public static SmelteryTier fromLevel(int level) {
        for (SmelteryTier tier : values()) {
            if (tier.level == level) { return tier; }
        }
        return null;
    }
}
