package me.ray.midgard.modules.spells.obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.ray.midgard.modules.spells.data.SpellMilestone;
import me.ray.midgard.modules.spells.data.SpellSound;
import me.ray.midgard.modules.spells.requirement.SpellRequirement;

public class Spell {

    private final String id;
    private final String mythicSkillName;
    private final String displayName;
    private final SpellType spellType;
    private final List<String> lore;
    private final List<String> lockedLore;
    private final ScalableAttribute cooldown;
    private final ScalableAttribute manaCost;
    private final ScalableAttribute staminaCost;
    private final Map<String, Object> variables;
    private final List<SpellRequirement> requirements;
    private final double castTime;
    private final boolean interruptible;
    private final int maxLevel;

    // Icon configuration
    private final String iconMaterial;
    private final String iconMaterialLocked;
    private final int iconModelData;
    private final int iconModelDataLocked;

    // Feature 2: Milestones
    private final List<SpellMilestone> milestones;

    // Feature 3: Mastery bonuses (attributeId -> bonus value)
    private final Map<String, Double> masteryBonuses;

    // Feature 8: Interrupt threshold (0.0 = always interrupt, 0.15 = 15% HP)
    private final double interruptThreshold;

    // Feature 12: Custom sounds per spell
    private final SpellSound castStartSound;
    private final SpellSound castFinishSound;
    private final SpellSound castFailSound;

    public Spell(String id, String mythicSkillName, String displayName, SpellType spellType,
                 List<String> lore, List<String> lockedLore,
                 ScalableAttribute cooldown, ScalableAttribute manaCost, ScalableAttribute staminaCost,
                 Map<String, Object> variables, List<SpellRequirement> requirements,
                 double castTime, boolean interruptible,
                 String iconMaterial, String iconMaterialLocked, int iconModelData, int iconModelDataLocked,
                 int maxLevel,
                 List<SpellMilestone> milestones, Map<String, Double> masteryBonuses,
                 double interruptThreshold,
                 SpellSound castStartSound, SpellSound castFinishSound, SpellSound castFailSound) {
        this.id = id;
        this.mythicSkillName = mythicSkillName;
        this.displayName = displayName;
        this.spellType = spellType;
        this.lore = lore;
        this.lockedLore = lockedLore;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.staminaCost = staminaCost;
        this.variables = variables != null ? variables : new HashMap<>();
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        this.castTime = castTime;
        this.interruptible = interruptible;
        this.iconMaterial = iconMaterial;
        this.iconMaterialLocked = iconMaterialLocked;
        this.iconModelData = iconModelData;
        this.iconModelDataLocked = iconModelDataLocked;
        this.maxLevel = maxLevel > 0 ? maxLevel : 10;
        this.milestones = milestones != null ? milestones : new ArrayList<>();
        this.masteryBonuses = masteryBonuses != null ? masteryBonuses : new HashMap<>();
        this.interruptThreshold = interruptThreshold;
        this.castStartSound = castStartSound != null ? castStartSound : SpellSound.DEFAULT_CAST_START;
        this.castFinishSound = castFinishSound != null ? castFinishSound : SpellSound.DEFAULT_CAST_FINISH;
        this.castFailSound = castFailSound != null ? castFailSound : SpellSound.DEFAULT_CAST_FAIL;
    }

    public String getId() {
        return id;
    }

    public String getMythicSkillName() {
        return mythicSkillName;
    }

    /**
     * Returns the effective MythicMobs skill name based on current level and milestones.
     * If a milestone with a mechanic override has been reached, uses that skill instead.
     */
    public String getEffectiveSkillName(int level) {
        for (int i = milestones.size() - 1; i >= 0; i--) {
            SpellMilestone m = milestones.get(i);
            if (level >= m.level() && m.mechanicSkillOverride() != null) {
                return m.mechanicSkillOverride();
            }
        }
        return mythicSkillName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SpellType getSpellType() {
        return spellType;
    }

    public boolean isPassive() {
        return spellType == SpellType.PASSIVE;
    }

    public boolean isUltimate() {
        return spellType == SpellType.ULTIMATE;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getLockedLore() {
        return (lockedLore != null && !lockedLore.isEmpty()) ? lockedLore : lore;
    }

    public List<String> getLore(boolean isLocked) {
        return isLocked ? getLockedLore() : getLore();
    }

    public ScalableAttribute getCooldown() {
        return cooldown;
    }

    public ScalableAttribute getManaCost() {
        return manaCost;
    }

    public ScalableAttribute getStaminaCost() {
        return staminaCost;
    }

    public List<SpellRequirement> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public double getCastTime() {
        return castTime;
    }

    public boolean isInterruptible() {
        return interruptible;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    // ==================== MILESTONES ====================

    public List<SpellMilestone> getMilestones() {
        return Collections.unmodifiableList(milestones);
    }

    public SpellMilestone getMilestoneForLevel(int level) {
        for (SpellMilestone m : milestones) {
            if (m.level() == level) { return m; }
        }
        return null;
    }

    // ==================== MASTERY ====================

    public Map<String, Double> getMasteryBonuses() {
        return Collections.unmodifiableMap(masteryBonuses);
    }

    // ==================== INTERRUPT THRESHOLD ====================

    public double getInterruptThreshold() {
        return interruptThreshold;
    }

    // ==================== SOUNDS ====================

    public SpellSound getCastStartSound() {
        return castStartSound;
    }

    public SpellSound getCastFinishSound() {
        return castFinishSound;
    }

    public SpellSound getCastFailSound() {
        return castFailSound;
    }

    // ==================== ICON METHODS ====================

    public String getIconMaterial() {
        return iconMaterial;
    }

    public String getIconMaterialLocked() {
        return (iconMaterialLocked != null && !iconMaterialLocked.isEmpty()) ? iconMaterialLocked : iconMaterial;
    }

    public String getIconMaterial(boolean isLocked) {
        return isLocked ? getIconMaterialLocked() : getIconMaterial();
    }

    public int getIconModelData() {
        return iconModelData;
    }

    public int getIconModelDataLocked() {
        return iconModelDataLocked > 0 ? iconModelDataLocked : iconModelData;
    }

    public int getIconModelData(boolean isLocked) {
        return isLocked ? getIconModelDataLocked() : getIconModelData();
    }

    public boolean hasCustomIcon() {
        return (iconMaterial != null && !iconMaterial.isEmpty()) || iconModelData > 0;
    }
}
