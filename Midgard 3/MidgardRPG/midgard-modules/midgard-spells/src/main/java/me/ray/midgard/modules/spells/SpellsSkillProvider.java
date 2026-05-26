package me.ray.midgard.modules.spells;

import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.skill.SkillProvider;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.entity.Player;

public class SpellsSkillProvider implements SkillProvider {

    private final SpellsModule module;

    public SpellsSkillProvider(SpellsModule module) {
        this.module = module;
    }

    @Override
    public boolean hasSkill(MidgardProfile profile, String skillId) {
        Player player = profile.getPlayer();
        if (player == null) { return false; }

        SpellProfile spellProfile = module.getSpellManager().getProfile(player);
        return spellProfile != null && spellProfile.hasSpell(skillId);
    }

    @Override
    public void unlockSkill(MidgardProfile profile, String skillId) {
        Player player = profile.getPlayer();
        if (player == null) { return; }

        SpellProfile spellProfile = module.getSpellManager().getProfile(player);
        if (spellProfile != null) {
            spellProfile.unlockSpell(skillId);
        }
    }

    @Override
    public String getSkillName(String skillId) {
        Spell spell = module.getSpellManager().getSpell(skillId);
        return spell != null ? spell.getDisplayName() : skillId;
    }

    @Override
    public int getSkillLevel(MidgardProfile profile, String skillId) {
        Player player = profile.getPlayer();
        if (player == null) { return 0; }
        
        SpellProfile spellProfile = module.getSpellManager().getProfile(player);
        if (spellProfile == null) { return 0; }
        
        if (!spellProfile.hasSpell(skillId)) { return 0; }
        
        return spellProfile.getSpellLevel(skillId);
    }

    @Override
    public void setSkillLevel(MidgardProfile profile, String skillId, int level) {
         Player player = profile.getPlayer();
        if (player == null) { return; }
        
        SpellProfile spellProfile = module.getSpellManager().getProfile(player);
        if (spellProfile != null) {
            // Se level > 0, garante que está desbloqueado
            if (level > 0 && !spellProfile.hasSpell(skillId)) {
                spellProfile.unlockSpell(skillId);
            }
            spellProfile.setSpellLevel(skillId, level);
        }
    }

    @Override
    public void removeAllSkills(MidgardProfile profile) {
        Player player = profile.getPlayer();
        if (player == null) { return; }

        SpellProfile spellProfile = module.getSpellManager().getProfile(player);
        if (spellProfile == null) { return; }

        // Remove mastery modifiers before unlearning
        for (String spellId : new java.util.ArrayList<>(spellProfile.getUnlockedSpells())) {
            module.getSpellManager().removeSpellMasteryModifiers(player, spellId);
            spellProfile.unlearnSpell(spellId);
        }
    }
}
