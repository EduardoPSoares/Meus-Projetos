package me.ray.midgard.modules.spells.data;

import me.ray.midgard.core.profile.ModuleData;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpellProfile implements ModuleData {

    // Skill Bar Mapping: Slot (1-9) -> Spell ID
    private final Map<Integer, String> skillBar = new ConcurrentHashMap<>();

    // Combo Mapping: Slot (1-6) -> ComboBinding
    private final Map<Integer, ComboBinding> comboSlots = new ConcurrentHashMap<>();

    // Cooldowns: Spell ID -> Expiration Timestamp (System.currentTimeMillis)
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    // Spells Desbloqueados
    private final Set<String> unlockedSpells = ConcurrentHashMap.newKeySet();

    // Spell Levels: Spell ID -> Level
    private final Map<String, Integer> spellLevels = new ConcurrentHashMap<>();

    // Estilo de Casting
    private CastingStyle castingStyle = CastingStyle.SKILLBAR;

    // ====== NEW FIELDS ======

    // Feature 1: Spell XP acumulado por spell
    private final Map<String, Double> spellXP = new ConcurrentHashMap<>();

    // Feature 2: Milestones alcancados (formato "spellId:level")
    private final Set<String> achievedMilestones = ConcurrentHashMap.newKeySet();

    // Feature 3: Spells que atingiram nivel maximo (mastered)
    private final Set<String> masteredSpells = ConcurrentHashMap.newKeySet();

    // Feature 4: Spells travadas (requerem scroll para remover)
    private final Set<String> lockedSpells = ConcurrentHashMap.newKeySet();

    // Feature 6: Memoria de spells desaprendidas (spellId -> nivel lembrado)
    private final Map<String, Integer> spellMemory = new ConcurrentHashMap<>();

    // Ultimate equipada (null = nenhuma)
    private String equippedUltimate = null;

    // Feature 13: Estatisticas por spell
    private final Map<String, SpellStats> spellStatistics = new ConcurrentHashMap<>();

    public static class ComboBinding {
        private String sequence;
        private String spellId;

        public ComboBinding(String sequence, String spellId) {
            this.sequence = sequence;
            this.spellId = spellId;
        }

        public String getSequence() { return sequence; }
        public void setSequence(String sequence) { this.sequence = sequence; }
        public String getSpellId() { return spellId; }
        public void setSpellId(String spellId) { this.spellId = spellId; }
    }

    public enum CastingStyle {
        SKILLBAR,
        COMBO
    }

    public SpellProfile() {
    }

    // ====== SKILL BAR ======

    public void setSkillBarSlot(int slot, String spellId) {
        if (spellId == null) {
            skillBar.remove(slot);
        } else {
            // Remove a spell de qualquer outro slot antes de atribuir
            String normalized = spellId.toLowerCase();
            skillBar.entrySet().removeIf(e -> e.getKey() != slot && normalized.equalsIgnoreCase(e.getValue()));
            skillBar.put(slot, spellId);
        }
    }

    public String getSkillInSlot(int slot) {
        return skillBar.get(slot);
    }

    // ====== COMBOS ======

    public void setComboSlot(int slot, String sequence, String spellId) {
        if (slot < 1 || slot > 4) { return; }
        // Remove a spell de qualquer outro combo slot antes de atribuir
        if (spellId != null) {
            String normalized = spellId.toLowerCase();
            for (Map.Entry<Integer, ComboBinding> entry : comboSlots.entrySet()) {
                if (entry.getKey() != slot && normalized.equalsIgnoreCase(entry.getValue().getSpellId())) {
                    entry.getValue().setSpellId(null);
                }
            }
        }
        comboSlots.put(slot, new ComboBinding(sequence, spellId));
    }

    public ComboBinding getComboSlot(int slot) {
        return comboSlots.get(slot);
    }

    public String getSpellByCombo(String comboSequence) {
        for (ComboBinding binding : comboSlots.values()) {
            if (binding.getSequence().equalsIgnoreCase(comboSequence)) {
                return binding.getSpellId();
            }
        }
        return null;
    }

    public Map<Integer, ComboBinding> getComboSlots() {
        return Collections.unmodifiableMap(comboSlots);
    }

    public void setComboLegacy(String combo, String spellId) {
        // Remove a spell de qualquer outro combo antes de atribuir
        if (spellId != null) {
            String normalized = spellId.toLowerCase();
            for (ComboBinding binding : comboSlots.values()) {
                if (normalized.equalsIgnoreCase(binding.getSpellId())
                        && !binding.getSequence().equalsIgnoreCase(combo)) {
                    binding.setSpellId(null);
                }
            }
        }
        for (Map.Entry<Integer, ComboBinding> entry : comboSlots.entrySet()) {
            if (entry.getValue().getSequence().equalsIgnoreCase(combo)) {
                entry.getValue().setSpellId(spellId);
                return;
            }
        }
        for (int i = 1; i <= 4; i++) {
            if (!comboSlots.containsKey(i)) {
                comboSlots.put(i, new ComboBinding(combo, spellId));
                return;
            }
        }
    }

    // ====== SPELL LEVELS ======

    public int getSpellLevel(String spellId) {
        return spellLevels.getOrDefault(spellId.toLowerCase(), 1);
    }

    public void setSpellLevel(String spellId, int level) {
        spellLevels.put(spellId.toLowerCase(), Math.max(1, level));
    }

    // ====== COOLDOWNS ======

    public boolean isOnCooldown(String spellId) {
        String normalized = spellId.toLowerCase();
        return cooldowns.containsKey(normalized) && cooldowns.get(normalized) > System.currentTimeMillis();
    }

    public long getCooldownRemainingKey(String spellId) {
        if (!isOnCooldown(spellId)) { return 0; }
        return cooldowns.get(spellId.toLowerCase()) - System.currentTimeMillis();
    }

    public void setCooldown(String spellId, double seconds) {
        cooldowns.put(spellId.toLowerCase(), System.currentTimeMillis() + (long)(seconds * 1000L));
    }

    // ====== UNLOCK / LEARN ======

    public Set<String> getUnlockedSpells() {
        return Collections.unmodifiableSet(unlockedSpells);
    }

    public void unlockSpell(String spellId) {
        unlockedSpells.add(spellId.toLowerCase());
    }

    // ====== ULTIMATE ======

    public String getEquippedUltimate() {
        return equippedUltimate;
    }

    public void setEquippedUltimate(String spellId) {
        this.equippedUltimate = spellId != null ? spellId.toLowerCase() : null;
    }

    public void unlearnSpell(String spellId) {
        String normalized = spellId.toLowerCase();
        unlockedSpells.remove(normalized);
        lockedSpells.remove(normalized);
        skillBar.values().removeIf(id -> normalized.equals(id));
        comboSlots.values().removeIf(binding -> normalized.equals(binding.getSpellId()));
        // Clear equipped ultimate if it matches
        if (normalized.equals(equippedUltimate)) {
            equippedUltimate = null;
        }
        // Clear all spell-specific progression data
        spellLevels.remove(normalized);
        spellXP.remove(normalized);
        achievedMilestones.removeIf(key -> key.startsWith(normalized + ":"));
        masteredSpells.remove(normalized);
        spellStatistics.remove(normalized);
        cooldowns.remove(normalized);
    }

    public boolean hasSpell(String spellId) {
        return unlockedSpells.contains(spellId.toLowerCase());
    }

    // ====== CASTING STYLE ======

    public CastingStyle getCastingStyle() {
        return castingStyle;
    }

    public void setCastingStyle(CastingStyle castingStyle) {
        this.castingStyle = castingStyle;
    }

    // ====== SPELL XP (Feature 1) ======

    public double getSpellXP(String spellId) {
        return spellXP.getOrDefault(spellId.toLowerCase(), 0.0);
    }

    public void addSpellXP(String spellId, double xp) {
        spellXP.merge(spellId.toLowerCase(), xp, Double::sum);
    }

    public void setSpellXP(String spellId, double xp) {
        spellXP.put(spellId.toLowerCase(), xp);
    }

    // ====== MILESTONES (Feature 2) ======

    public boolean hasMilestone(String spellId, int level) {
        return achievedMilestones.contains(spellId.toLowerCase() + ":" + level);
    }

    public void achieveMilestone(String spellId, int level) {
        achievedMilestones.add(spellId.toLowerCase() + ":" + level);
    }

    // ====== MASTERY (Feature 3) ======

    public boolean isMastered(String spellId) {
        return masteredSpells.contains(spellId.toLowerCase());
    }

    public void setMastered(String spellId) {
        masteredSpells.add(spellId.toLowerCase());
    }

    public Set<String> getMasteredSpells() {
        return Collections.unmodifiableSet(masteredSpells);
    }

    // ====== LOCKING (Feature 4) ======

    public boolean isLocked(String spellId) {
        return lockedSpells.contains(spellId.toLowerCase());
    }

    public void lockSpell(String spellId) {
        lockedSpells.add(spellId.toLowerCase());
    }

    public void unlockSpellForRemoval(String spellId) {
        lockedSpells.remove(spellId.toLowerCase());
    }

    // ====== SPELL MEMORY (Feature 6) ======

    public int getRememberedLevel(String spellId) {
        return spellMemory.getOrDefault(spellId.toLowerCase(), 0);
    }

    public void rememberSpell(String spellId, int level) {
        spellMemory.put(spellId.toLowerCase(), level);
    }

    public void forgetSpell(String spellId) {
        spellMemory.remove(spellId.toLowerCase());
    }

    // ====== STATISTICS (Feature 13) ======

    public SpellStats getSpellStats(String spellId) {
        return spellStatistics.computeIfAbsent(spellId.toLowerCase(), k -> new SpellStats());
    }

    public Map<String, SpellStats> getAllSpellStats() {
        return Collections.unmodifiableMap(spellStatistics);
    }
}
