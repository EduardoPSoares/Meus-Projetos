package me.ray.midgard.modules.combat.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.event.PlayerLevelUpEvent;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listener responsável por recalcular o escalonamento de atributos (Stats -> Sub-stats).
 * Substitui a antiga StatScalingTask para melhorar a performance (Event-Driven).
 */
public class StatScalingListener implements Listener {

    /**
     * Recalcula atributos quando o jogador entra.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        updateStats(event.getPlayer());
    }

    /**
     * Recalcula atributos quando o jogador fecha o inventário (possível troca de armadura).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        // Skip recalculation when closing custom GUIs — no equipment changes possible
        if (event.getInventory().getHolder() instanceof me.ray.midgard.core.gui.BaseGui) {
            return;
        }
        updateStats(player);
    }

    /**
     * Recalcula atributos ao renascer.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        updateStats(event.getPlayer());
    }

    /**
     * Recalcula atributos ao subir de nível.
     */
    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        updateStats(event.getPlayer());
    }

    /**
     * Lógica central de escalonamento.
     */
    public static void updateStats(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        me.ray.midgard.modules.combat.CombatManager combatManager = me.ray.midgard.modules.combat.CombatManager.getInstance();
        if (combatManager == null) {
            return;
        }
        me.ray.midgard.modules.combat.CombatConfig config = combatManager.getConfig();
        CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);

        // --- Leitura dos Atributos Primários ---
        double strength = getValue(attributeData, CombatAttributes.STRENGTH);
        double intelligence = getValue(attributeData, CombatAttributes.INTELLIGENCE);
        double dexterity = getValue(attributeData, CombatAttributes.DEXTERITY);
        // double defense = getValue(attributeData, CombatAttributes.DEFENSE);
        double agility = getValue(attributeData, CombatAttributes.AGILITY);
        double vitality = getValue(attributeData, CombatAttributes.VITALITY);

        // --- Aplicação dos Modificadores (Limpa anteriores e aplica novos) ---
        
        // Verifica o modo de fórmula. Se for MULTIPLICATIVE, Força/Inteligência não dão dano Flat.
        boolean isAdditive = config.damageFormulaMode == me.ray.midgard.modules.combat.CombatConfig.ScalingMode.ADDITIVE;

        // FORÇA
        if (isAdditive) {
            applyScaling(attributeData, CombatAttributes.PHYSICAL_DAMAGE, "StatScaling_STR", strength * config.strengthToPhysicalDamage);
        } else {
            // Em modo multiplicativo, remove qualquer bônus flat que existia
            AttributeInstance physDmg = attributeData.getInstance(CombatAttributes.PHYSICAL_DAMAGE);
            if (physDmg != null) {
                physDmg.removeModifier("StatScaling_STR");
            }
        }
        
        AttributeInstance earthDmg = attributeData.getInstance(CombatAttributes.EARTH_DAMAGE);
        if (earthDmg != null) {
            earthDmg.removeModifier("StatScaling_STR");
        }

        // INTELIGÊNCIA
        if (isAdditive) {
            applyScaling(attributeData, CombatAttributes.MAGIC_DAMAGE, "StatScaling_INT", intelligence * config.intelligenceToMagicDamage);
        } else {
             // Em modo multiplicativo, remove bônus flat de dano mágico
            AttributeInstance magicDmg = attributeData.getInstance(CombatAttributes.MAGIC_DAMAGE);
            if (magicDmg != null) {
                magicDmg.removeModifier("StatScaling_INT");
            }
        }
        
        // Mana e Regen continuam escalando normalmente em ambos os modos (são recursos, não dano)
        applyScaling(attributeData, CombatAttributes.MAX_MANA, "StatScaling_INT", intelligence * config.intelligenceToMana);
        applyScaling(attributeData, CombatAttributes.MANA_REGEN, "StatScaling_INT", intelligence * config.intelligenceToManaRegen);
        
        AttributeInstance waterDmg = attributeData.getInstance(CombatAttributes.WATER_DAMAGE);
        if (waterDmg != null) {
            waterDmg.removeModifier("StatScaling_INT");
        }

        // DESTREZA: Chance Crítica escala igual
        applyScaling(attributeData, CombatAttributes.CRITICAL_CHANCE, "StatScaling_DEX", dexterity * config.dexterityToCritChance);
        
        AttributeInstance thunderDmg = attributeData.getInstance(CombatAttributes.THUNDER_DAMAGE);
        if (thunderDmg != null) {
            thunderDmg.removeModifier("StatScaling_DEX");
        }

        // DEFESA: Apenas defesa base (remove modifiers antigos)
        AttributeInstance fireDmg = attributeData.getInstance(CombatAttributes.FIRE_DAMAGE);
        if (fireDmg != null) {
            fireDmg.removeModifier("StatScaling_DEF");
        }

        // AGILIDADE: Escalonamento configurável (Padrão: +0.1 Speed, +0.2% Dodge)
        applyScaling(attributeData, CombatAttributes.SPEED, "StatScaling_AGI", agility * config.agilityToSpeed);
        applyScaling(attributeData, CombatAttributes.DODGE_RATING, "StatScaling_AGI", agility * config.agilityToDodge);
        
        AttributeInstance airDmg = attributeData.getInstance(CombatAttributes.AIR_DAMAGE);
        if (airDmg != null) {
            airDmg.removeModifier("StatScaling_AGI");
        }

        // VITALIDADE: Escalonamento para Vida e Defesa
        applyScaling(attributeData, CombatAttributes.MAX_HEALTH, "StatScaling_VIT", vitality * config.vitalityToHealth);
        applyScaling(attributeData, CombatAttributes.DEFENSE, "StatScaling_VIT", vitality * config.vitalityToDefense);

        // Atualização de Velocidade de Movimento (Bukkit)
        updateWalkSpeed(player, attributeData);
    }

    private static double getValue(CoreAttributeData data, String attrId) {
        AttributeInstance instance = data.getInstance(attrId);
        return instance != null ? instance.getValue() : 0.0;
    }

    private static void applyScaling(CoreAttributeData data, String targetAttr, String modifierName, double value) {
        AttributeInstance instance = data.getInstance(targetAttr);
        if (instance != null) {
            instance.removeModifier(modifierName);
            if (value > 0) {
                instance.addModifier(new AttributeModifier(modifierName, value, AttributeOperation.ADD_NUMBER));
            }
        }
    }

    private static void updateWalkSpeed(Player player, CoreAttributeData data) {
        AttributeInstance speedAttr = data.getInstance(CombatAttributes.SPEED);
        if (speedAttr != null) {
            double speedVal = speedAttr.getValue();
            float defaultSpeed = 0.2f;
            // Fórmula: 100 Speed = 0.2 (Padrão). 150 Speed = 0.3 (+50%)
            float newSpeed = defaultSpeed * (float)(speedVal / 100.0);
            newSpeed = Math.min(1.0f, Math.max(0.05f, newSpeed)); // Clamp (min 0.05 to prevent frozen players)
            
            if (player.getWalkSpeed() != newSpeed) {
                player.setWalkSpeed(newSpeed);
            }
        }
    }
}
