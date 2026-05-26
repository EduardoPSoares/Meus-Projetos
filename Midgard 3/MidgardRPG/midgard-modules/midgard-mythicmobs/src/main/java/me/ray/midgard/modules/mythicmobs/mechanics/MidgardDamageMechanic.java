package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.modules.combat.DamageOverrideContext;
import org.bukkit.entity.LivingEntity;

// Midgard Damage Mechanic
public class MidgardDamageMechanic implements ITargetedEntitySkill {

    private final PlaceholderDouble amount;
    private final boolean ignoreArmor;
    private final String element;
    private final String type;

    public MidgardDamageMechanic(MythicLineConfig config) {
        this.amount = config.getPlaceholderDouble(new String[]{"amount", "a"}, 1.0);
        this.ignoreArmor = config.getBoolean(new String[]{"ignoreArmor", "ia", "i"}, false);
        this.element = config.getString(new String[]{"element", "e"}, null);
        this.type = config.getString(new String[]{"type", "t"}, "PHYSICAL");
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isLiving()) {
            return SkillResult.INVALID_TARGET;
        }
        
        LivingEntity bukkitTarget = (LivingEntity) BukkitAdapter.adapt(target);
        
        org.bukkit.entity.Entity casterEntity = BukkitAdapter.adapt(data.getCaster().getEntity());
        LivingEntity caster = (casterEntity instanceof LivingEntity le) ? le : null;
        
        double finalAmount = amount.get(data, target);

        if (element != null) {
            DamageOverrideContext.setForcedElement(bukkitTarget, element);
        }
        
        if (ignoreArmor) {
            DamageOverrideContext.setForcedType(bukkitTarget, "GLOBAL");
        } else if (type != null) {
            DamageOverrideContext.setForcedType(bukkitTarget, type);
        }
        
        DamageOverrideContext.setForcedDamage(bukkitTarget, finalAmount);

        try {
            // Garante que o dano seja aplicado mesmo se o alvo estiver em invencibilidade (ex: após outro hit imediato)
            bukkitTarget.setNoDamageTicks(0);
            bukkitTarget.setLastDamage(0); // Reseta o último dano para evitar a regra de "apenas a diferença" do Vanilla
            
            // Aplica dano usando o sistema do Bukkit, que será interceptado pelo MidgardRPG
            bukkitTarget.damage(finalAmount, caster);
        } finally {
            DamageOverrideContext.clear(bukkitTarget);
        }
        
        return SkillResult.SUCCESS;
    }
}
