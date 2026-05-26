package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;

import java.util.UUID;

/**
 * Represents a forge template — a blueprint that players can obtain and build.
 * Created by admins, templates define the name, tier, required profession level,
 * and schematic (block layout) for a type of forge.
 */
public class ForgeTemplate {

    private final UUID templateId;
    private String name;
    private ForgeTier tier;
    private int requiredLevel;
    private long createdAt;
    private boolean active;

    private transient ForgeSchematic schematic;

    public ForgeTemplate(UUID templateId, String name, ForgeTier tier, int requiredLevel) {
        this.templateId = templateId;
        this.name = name;
        this.tier = tier;
        this.requiredLevel = requiredLevel;
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }

    /** Full constructor for loading from DB. */
    public ForgeTemplate(UUID templateId, String name, ForgeTier tier, int requiredLevel,
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
    public ForgeTier getTier() { return tier; }
    public int getRequiredLevel() { return requiredLevel; }
    public long getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public ForgeSchematic getSchematic() { return schematic; }

    public void setName(String name) { this.name = name; }
    public void setTier(ForgeTier tier) { this.tier = tier; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }
    public void setActive(boolean active) { this.active = active; }
    public void setSchematic(ForgeSchematic schematic) { this.schematic = schematic; }
}
