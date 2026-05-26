package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;

import java.util.UUID;

/**
 * Representa um template de smeltery — um blueprint que jogadores podem obter e construir.
 * Criado por admins, define nome, tier, nível necessário e esquemático (layout de blocos).
 */
public class SmelteryTemplate {

    private final UUID templateId;
    private String name;
    private SmelteryTier tier;
    private int requiredLevel;
    private long createdAt;
    private boolean active;

    private transient SmelterySchematic schematic;

    public SmelteryTemplate(UUID templateId, String name, SmelteryTier tier, int requiredLevel) {
        this.templateId = templateId;
        this.name = name;
        this.tier = tier;
        this.requiredLevel = requiredLevel;
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }

    /** Construtor completo para carregar do DB. */
    public SmelteryTemplate(UUID templateId, String name, SmelteryTier tier, int requiredLevel,
                            long createdAt, boolean active) {
        this.templateId = templateId;
        this.name = name;
        this.tier = tier;
        this.requiredLevel = requiredLevel;
        this.createdAt = createdAt;
        this.active = active;
    }

    public UUID getTemplateId() { return templateId; }
    public String getName() { return name; }
    public SmelteryTier getTier() { return tier; }
    public int getRequiredLevel() { return requiredLevel; }
    public long getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public SmelterySchematic getSchematic() { return schematic; }

    public void setName(String name) { this.name = name; }
    public void setTier(SmelteryTier tier) { this.tier = tier; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }
    public void setActive(boolean active) { this.active = active; }
    public void setSchematic(SmelterySchematic schematic) { this.schematic = schematic; }
}
