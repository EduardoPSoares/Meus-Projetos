package me.ray.midgard.modules.professions.gui;

import java.util.Collections;
import java.util.List;

/**
 * Recompensas associadas a um nível de profissão.
 * Cada nível pode ter bônus passivos, desbloqueio de habilidade e perks.
 */
public record ProfessionLevelReward(
        int level,
        String levelName,
        List<String> passiveBonuses,
        String abilityUnlock,
        String abilityDescription,
        List<String> perks
) {

    /**
     * Recompensa vazia para níveis sem conteúdo especial.
     */
    public static ProfessionLevelReward empty(int level) {
        return new ProfessionLevelReward(level, null, List.of(), null, null, List.of());
    }

    public boolean hasAbilityUnlock() {
        return abilityUnlock != null && !abilityUnlock.isBlank();
    }

    public boolean hasLevelName() {
        return levelName != null && !levelName.isBlank();
    }

    public boolean hasPassiveBonuses() {
        return passiveBonuses != null && !passiveBonuses.isEmpty();
    }

    public boolean hasPerks() {
        return perks != null && !perks.isEmpty();
    }

    @Override
    public List<String> passiveBonuses() {
        return passiveBonuses != null ? Collections.unmodifiableList(passiveBonuses) : List.of();
    }

    @Override
    public List<String> perks() {
        return perks != null ? Collections.unmodifiableList(perks) : List.of();
    }
}
