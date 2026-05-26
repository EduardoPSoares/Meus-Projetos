package me.ray.midgard.modules.mythicmobs;

import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import me.ray.midgard.modules.mythicmobs.conditions.MidgardAttributeCondition;
import me.ray.midgard.modules.mythicmobs.conditions.MidgardClassCondition;
import me.ray.midgard.modules.mythicmobs.conditions.MidgardLevelCondition;
import me.ray.midgard.modules.mythicmobs.conditions.MidgardManaCondition;
import me.ray.midgard.modules.mythicmobs.conditions.MidgardStaminaCondition;
import me.ray.midgard.modules.mythicmobs.drops.MidgardItemDrop;
import me.ray.midgard.modules.mythicmobs.drops.MidgardLootDrop;
import me.ray.midgard.modules.mythicmobs.drops.MidgardXpDrop;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardAttributeMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardDamageMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardDropLootMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardEquipMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardGiveClassExpMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardGiveExpMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardLevelScaleMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardManaMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardPlayerAttributeMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardShieldMechanic;
import me.ray.midgard.modules.mythicmobs.mechanics.MidgardStaminaMechanic;
import me.ray.midgard.modules.mythicmobs.targeters.MidgardClassTargeter;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
 
public class MythicMobsListener implements Listener {

    @EventHandler
    public void onMechanicLoad(MythicMechanicLoadEvent event) {
        try {
            // System.out.println("MidgardRPG: Loading mechanic " + event.getMechanicName()); // Debug
            switch (event.getMechanicName().toLowerCase()) {
                case "midgard-damage":
                case "midgarddamage":
                    event.register(new MidgardDamageMechanic(event.getConfig()));
                    break;
                case "midgard-give-exp":
                case "midgardgiveexp":
                    event.register(new MidgardGiveExpMechanic(event.getConfig()));
                    break;
                case "midgard-mana":
                case "midgardmana":
                    event.register(new MidgardManaMechanic(event.getConfig()));
                    break;
                case "midgard-stamina":
                case "midgardstamina":
                    event.register(new MidgardStaminaMechanic(event.getConfig()));
                    break;
                case "midgard-equip":
                case "midgardequip":
                    event.register(new MidgardEquipMechanic(event.getConfig()));
                    break;
                case "midgard-set-attribute":
                case "midgardsetattribute":
                    // System.out.println("MidgardRPG: Registered midgard-set-attribute mechanic!"); // Debug
                    event.register(new MidgardAttributeMechanic(event.getConfig()));
                    break;
                case "midgard-set-attribute-player":
                case "midgardsetattributeplayer":
                    event.register(new MidgardPlayerAttributeMechanic(event.getConfig()));
                    break;
                case "midgard-give-class-exp":
                case "midgardgiveclassexp":
                    event.register(new MidgardGiveClassExpMechanic(event.getConfig()));
                    break;
                case "midgard-scale":
                case "midgardscale":
                    event.register(new MidgardLevelScaleMechanic(event.getConfig()));
                    break;
                case "midgard-shield":
                case "midgardshield":
                    event.register(new MidgardShieldMechanic(event.getConfig()));
                    break;
                case "midgard-drop-loot":
                case "midgarddroploot":
                    event.register(new MidgardDropLootMechanic(event.getConfig()));
                    break;
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar mecânica MythicMobs: " + event.getMechanicName(), e);
        }
    }

    @EventHandler
    public void onConditionLoad(MythicConditionLoadEvent event) {
        try {
            switch (event.getConditionName().toLowerCase()) {
                case "midgard-class":
                case "midgardclass":
                    event.register(new MidgardClassCondition(event.getConfig()));
                    break;
                case "midgard-level":
                case "midgardlevel":
                    event.register(new MidgardLevelCondition(event.getConfig()));
                    break;
                case "midgard-attribute":
                case "midgardattribute":
                    event.register(new MidgardAttributeCondition(event.getConfig()));
                    break;
                case "midgard-mana":
                case "midgardmana":
                    event.register(new MidgardManaCondition(event.getConfig()));
                    break;
                case "midgard-stamina":
                case "midgardstamina":
                    event.register(new MidgardStaminaCondition(event.getConfig()));
                    break;
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar condição MythicMobs: " + event.getConditionName(), e);
        }
    }

    @EventHandler
    public void onDropLoad(MythicDropLoadEvent event) {
        try {
            if (event.getDropName().equalsIgnoreCase("midgard-item") || event.getDropName().equalsIgnoreCase("midgarditem")) {
                event.register(new MidgardItemDrop(event.getConfig()));
            }
            if (event.getDropName().equalsIgnoreCase("midgard-loot") || event.getDropName().equalsIgnoreCase("midgardloot")) {
                event.register(new MidgardLootDrop(event.getConfig()));
            }
            if (event.getDropName().equalsIgnoreCase("midgard-xp") || event.getDropName().equalsIgnoreCase("midgardxp")) {
                event.register(new MidgardXpDrop(event.getConfig()));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar drop MythicMobs: " + event.getDropName(), e);
        }
    }

    @EventHandler
    public void onTargeterLoad(MythicTargeterLoadEvent event) {
        try {
            if (event.getTargeterName().equalsIgnoreCase("MidgardClass")) {
                event.register(new MidgardClassTargeter(event.getConfig()));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar targeter MythicMobs: " + event.getTargeterName(), e);
        }
    }
}
