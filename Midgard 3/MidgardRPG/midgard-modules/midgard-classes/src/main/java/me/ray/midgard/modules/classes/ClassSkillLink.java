package me.ray.midgard.modules.classes;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.skill.SkillProvider;
import me.ray.midgard.core.text.MessageUtils;

/**
 * Representa o vínculo entre uma classe e uma habilidade.
 * Define qual habilidade é desbloqueada em qual nível.
 */
public class ClassSkillLink {

    private final String skillId;
    private final int unlockLevel;

    public ClassSkillLink(String skillId, int unlockLevel) {
        this.skillId = skillId;
        this.unlockLevel = unlockLevel;
    }

    public String getSkillId() {
        return skillId;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    /**
     * Tenta aplicar (desbloquear) a habilidade para o perfil, se o nível for atingido.
     *
     * @param profile Perfil do jogador.
     * @param currentLevel Nível atual da classe.
     * @return true se desbloqueou uma NOVA habilidade, false caso contrário.
     */
    public boolean tryUnlock(MidgardProfile profile, int currentLevel) {
        if (currentLevel < unlockLevel) {
            return false;
        }

        SkillProvider provider = MidgardCore.getSkillProvider();
        if (provider == null) {
            MidgardLogger.debug(DebugCategory.CORE, "SkillProvider não encontrado ao tentar desbloquear skill %s", skillId);
            return false;
        }

        if (!provider.hasSkill(profile, skillId)) {
            provider.unlockSkill(profile, skillId);
            
            String skillName = provider.getSkillName(skillId);
            ClassesModule classesModule = ClassesModule.getInstance();
            String msg = classesModule != null ? classesModule.getMessage("skills.unlocked") : null;
            if (msg != null && profile.getPlayer() != null) {
                MessageUtils.send(profile.getPlayer(), msg.replace("%skill_name%", skillName));
            }
            return true;
        }
        
        return false;
    }
}
