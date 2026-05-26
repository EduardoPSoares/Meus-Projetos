package me.ray.midgard.modules.item.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatModule;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitTask;

/**
 * Aplica os efeitos dos consumíveis quando o jogador consome (come/bebe) o item.
 * Stats de restauração (MAX_HEALTH, MAX_MANA, MAX_STAMINA) restauram o recurso atual.
 * Outros stats são aplicados como buffs temporários.
 */
public class ConsumableListener implements Listener {

    private static final String MODIFIER_PREFIX = "Consumable_";
    private static final long BUFF_DURATION_TICKS = 1200L; // 60 segundos

    // Stats que restauram recurso atual ao invés de dar buff
    private static final Set<ItemStat> RESTORATION_STATS = Set.of(
        ItemStat.MAX_HEALTH, ItemStat.MAX_MANA, ItemStat.MAX_STAMINA
    );

    private final ItemModule module;
    // Tracks active buff removal tasks per player+consumable to cancel on re-consume
    private final Map<String, BukkitTask> activeBuffTasks = new ConcurrentHashMap<>();

    public ConsumableListener(ItemModule module) {
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (itemStack == null) { return; }

        String id = module.getItemManager().getItemId(itemStack);
        if (id == null) { return; }

        MidgardItem item = module.getItemManager().getMidgardItem(id);
        if (item == null) { return; }

        if (!"CONSUMABLE".equalsIgnoreCase(item.getCategoryId())) { return; }

        Map<ItemStat, Double> itemStats = ItemPDC.getStats(itemStack);
        if (itemStats.isEmpty()) {
            for (Map.Entry<ItemStat, StatRange> entry : item.getStats().entrySet()) {
                itemStats.put(entry.getKey(), entry.getValue().getMin());
            }
        }

        List<String> permanentEffects = item.getPermanentEffects();
        if (itemStats.isEmpty() && (permanentEffects == null || permanentEffects.isEmpty())) { return; }

        // Separar stats de restauração e stats de buff
        Map<ItemStat, Double> restorationStats = new EnumMap<>(ItemStat.class);
        Map<ItemStat, Double> buffStats = new EnumMap<>(ItemStat.class);

        for (Map.Entry<ItemStat, Double> entry : itemStats.entrySet()) {
            if (RESTORATION_STATS.contains(entry.getKey())) {
                restorationStats.put(entry.getKey(), entry.getValue());
            } else {
                buffStats.put(entry.getKey(), entry.getValue());
            }
        }

        // Verificar se o consumível teria algum efeito útil
        boolean hasBuffStats = !buffStats.isEmpty();
        boolean hasEffects = permanentEffects != null && !permanentEffects.isEmpty();

        if (!hasBuffStats && !hasEffects && !restorationStats.isEmpty()) {
            // Só tem stats de restauração - verificar se o jogador precisa
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                CoreAttributeData attrData = profile.getData(CoreAttributeData.class);
                CombatData combatData = profile.getData(CombatData.class);
                if (attrData != null && combatData != null) {
                    boolean needsAnything = false;

                    if (restorationStats.containsKey(ItemStat.MAX_HEALTH)) {
                        AttributeInstance maxHpAttr = attrData.getInstance(CombatAttributes.MAX_HEALTH);
                        double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 100;
                        if (combatData.getCurrentHealth() < maxHp) {
                            needsAnything = true;
                        }
                    }
                    if (restorationStats.containsKey(ItemStat.MAX_MANA)) {
                        AttributeInstance maxManaAttr = attrData.getInstance(CombatAttributes.MAX_MANA);
                        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
                        if (combatData.getCurrentMana() < maxMana) {
                            needsAnything = true;
                        }
                    }
                    if (restorationStats.containsKey(ItemStat.MAX_STAMINA)) {
                        AttributeInstance maxStamAttr = attrData.getInstance(CombatAttributes.MAX_STAMINA);
                        double maxStam = maxStamAttr != null ? maxStamAttr.getValue() : 100;
                        if (combatData.getCurrentStamina() < maxStam) {
                            needsAnything = true;
                        }
                    }

                    if (!needsAnything) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        Task.sync(player, () -> applyConsumableEffects(player, id, restorationStats, buffStats, permanentEffects));
    }

    private void applyConsumableEffects(Player player, String itemId,
            Map<ItemStat, Double> restorationStats, Map<ItemStat, Double> buffStats, List<String> permanentEffects) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return; }

        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        if (attributeData == null) { return; }

        // --- Restauração de recursos ---
        CombatData combatData = profile.getData(CombatData.class);
        if (combatData != null) {
            if (restorationStats.containsKey(ItemStat.MAX_HEALTH)) {
                double healAmount = restorationStats.get(ItemStat.MAX_HEALTH);
                AttributeInstance maxHpAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
                double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 100;
                double newHealth = Math.min(maxHp, combatData.getCurrentHealth() + healAmount);
                combatData.setCurrentHealth(newHealth);

                if (CombatModule.getInstance() != null) {
                    me.ray.midgard.modules.combat.CombatManager.getInstance().syncHealth(player, newHealth, maxHp);
                }
            }

            if (restorationStats.containsKey(ItemStat.MAX_MANA)) {
                double manaAmount = restorationStats.get(ItemStat.MAX_MANA);
                AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
                double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
                double newMana = Math.min(maxMana, combatData.getCurrentMana() + manaAmount);
                combatData.setCurrentMana(newMana);
            }

            if (restorationStats.containsKey(ItemStat.MAX_STAMINA)) {
                double stamAmount = restorationStats.get(ItemStat.MAX_STAMINA);
                AttributeInstance maxStamAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
                double maxStam = maxStamAttr != null ? maxStamAttr.getValue() : 100;
                double newStam = Math.min(maxStam, combatData.getCurrentStamina() + stamAmount);
                combatData.setCurrentStamina(newStam);
            }
        }

        // --- Buffs temporários (outros stats) ---
        if (!buffStats.isEmpty()) {
            String modifierName = MODIFIER_PREFIX + itemId;

            // Remover buff anterior do mesmo consumível
            for (AttributeInstance instance : attributeData.getInstances().values()) {
                instance.removeModifier(modifierName);
            }

            for (Map.Entry<ItemStat, Double> entry : buffStats.entrySet()) {
                String attrId = getAttributeId(entry.getKey());
                if (attrId == null) { continue; }

                AttributeInstance instance = attributeData.getInstance(attrId);
                if (instance != null) {
                    instance.addModifier(new AttributeModifier(modifierName, entry.getValue(), AttributeOperation.ADD_NUMBER));
                }
            }

            String taskKey = player.getUniqueId() + ":" + modifierName;
            BukkitTask oldTask = activeBuffTasks.remove(taskKey);
            if (oldTask != null) {
                oldTask.cancel();
            }

            BukkitTask newTask = Task.syncLater(player, () -> {
                activeBuffTasks.remove(taskKey);
                removeConsumableBuff(player, modifierName);
            }, BUFF_DURATION_TICKS);
            activeBuffTasks.put(taskKey, newTask);
        }

        // --- Efeitos de poção temporários ---
        if (permanentEffects != null) {
            for (String effectStr : permanentEffects) {
                String[] parts = effectStr.split(":");
                if (parts.length < 1) { continue; }

                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (type == null) { continue; }

                int amplifier = 0;
                if (parts.length >= 2) {
                    try { amplifier = Integer.parseInt(parts[1]); }
                    catch (NumberFormatException ignored) { /* Invalid amplifier format */ }
                }

                player.addPotionEffect(new PotionEffect(type, (int) BUFF_DURATION_TICKS, amplifier, false, true, true));
            }
        }
    }

    private void removeConsumableBuff(Player player, String modifierName) {
        if (!player.isOnline()) { return; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return; }

        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        if (attributeData == null) { return; }

        for (AttributeInstance instance : attributeData.getInstances().values()) {
            instance.removeModifier(modifierName);
        }
    }

    private static String getAttributeId(ItemStat stat) {
        return switch (stat) {
            case MAX_HEALTH -> CombatAttributes.MAX_HEALTH;
            case MAX_MANA -> CombatAttributes.MAX_MANA;
            case MAX_STAMINA -> CombatAttributes.MAX_STAMINA;
            case ATTACK_DAMAGE -> CombatAttributes.PHYSICAL_DAMAGE;
            case DEFENSE -> CombatAttributes.DEFENSE;
            case CRITICAL_STRIKE_CHANCE -> CombatAttributes.CRITICAL_CHANCE;
            case CRITICAL_STRIKE_POWER -> CombatAttributes.CRITICAL_DAMAGE;
            case DODGE_RATING -> CombatAttributes.DODGE_RATING;
            case PARRY_RATING -> CombatAttributes.PARRY_RATING;
            case BLOCK_RATING -> CombatAttributes.BLOCK_RATING;
            case BLOCK_POWER -> CombatAttributes.BLOCK_POWER;
            case ARMOR -> CombatAttributes.ARMOR;
            case ARMOR_TOUGHNESS -> CombatAttributes.ARMOR_TOUGHNESS;
            case MOVEMENT_SPEED -> CombatAttributes.SPEED;
            case COOLDOWN_REDUCTION -> CombatAttributes.COOLDOWN_REDUCTION;
            case LIFESTEAL -> CombatAttributes.LIFE_STEAL;
            case HEALTH_REGENERATION -> CombatAttributes.HEALTH_REGEN;
            case MAX_HEALTH_REGENERATION -> CombatAttributes.MAX_HEALTH_REGEN;
            case MANA_REGENERATION -> CombatAttributes.MANA_REGEN;
            case MAX_MANA_REGENERATION -> CombatAttributes.MAX_MANA_REGEN;
            case STAMINA_REGENERATION -> CombatAttributes.STAMINA_REGEN;
            case MAX_STAMINA_REGENERATION -> CombatAttributes.MAX_STAMINA_REGEN;
            case MAX_STELLIUM -> CombatAttributes.MAX_STELLIUM;
            case MAX_ABSORPTION -> CombatAttributes.MAX_ABSORPTION;
            case KNOCKBACK_RESISTANCE -> CombatAttributes.KNOCKBACK_RESISTANCE;
            case MYLUCK -> CombatAttributes.LUCK;
            case ATTACK_SPEED -> CombatAttributes.ATTACK_SPEED;
            case SKILL_DAMAGE -> CombatAttributes.SKILL_DAMAGE;
            case PROJECTILE_DAMAGE -> CombatAttributes.PROJECTILE_DAMAGE;
            case MAGIC_DAMAGE -> CombatAttributes.MAGIC_DAMAGE;
            case UNDEAD_DAMAGE -> CombatAttributes.UNDEAD_DAMAGE;
            case DAMAGE_REDUCTION -> CombatAttributes.DAMAGE_REDUCTION;
            case FALL_DAMAGE_REDUCTION -> CombatAttributes.FALL_DAMAGE_REDUCTION;
            case PROJECTILE_DAMAGE_REDUCTION -> CombatAttributes.PROJECTILE_DAMAGE_REDUCTION;
            case PHYSICAL_DAMAGE_REDUCTION -> CombatAttributes.PHYSICAL_DAMAGE_REDUCTION;
            case MAGIC_DAMAGE_REDUCTION -> CombatAttributes.MAGIC_DAMAGE_REDUCTION;
            case PVE_DAMAGE_REDUCTION -> CombatAttributes.PVE_DAMAGE_REDUCTION;
            case PVP_DAMAGE_REDUCTION -> CombatAttributes.PVP_DAMAGE_REDUCTION;
            case FIRE_DAMAGE_REDUCTION -> CombatAttributes.FIRE_DEFENSE;
            case SPELL_VAMPIRISM -> CombatAttributes.SPELL_VAMPIRISM;
            case BLOCK_COOLDOWN_REDUCTION -> CombatAttributes.BLOCK_COOLDOWN_REDUCTION;
            case DODGE_COOLDOWN_REDUCTION -> CombatAttributes.DODGE_COOLDOWN_REDUCTION;
            case PARRY_COOLDOWN_REDUCTION -> CombatAttributes.PARRY_COOLDOWN_REDUCTION;
            case SKILL_CRITICAL_STRIKE_CHANCE -> CombatAttributes.SKILL_CRITICAL_CHANCE;
            case SKILL_CRITICAL_STRIKE_POWER -> CombatAttributes.SKILL_CRITICAL_DAMAGE;
            case FIRE_DAMAGE -> CombatAttributes.FIRE_DAMAGE;
            case ICE_DAMAGE -> CombatAttributes.ICE_DAMAGE;
            case LIGHT_DAMAGE -> CombatAttributes.LIGHT_DAMAGE;
            case DARKNESS_DAMAGE -> CombatAttributes.DARKNESS_DAMAGE;
            case DIVINE_DAMAGE -> CombatAttributes.DIVINE_DAMAGE;
            case STRENGTH -> CombatAttributes.STRENGTH;
            case INTELLIGENCE -> CombatAttributes.INTELLIGENCE;
            case DEXTERITY -> CombatAttributes.DEXTERITY;
            case ACCURACY -> CombatAttributes.ACCURACY;
            case CRITICAL_RESISTANCE -> CombatAttributes.CRITICAL_RESISTANCE;
            case THORNS -> CombatAttributes.THORNS;
            case MAGIC_RESISTANCE -> CombatAttributes.MAGIC_RESISTANCE;
            case ARMOR_PENETRATION -> CombatAttributes.ARMOR_PENETRATION;
            case ARMOR_PENETRATION_FLAT -> CombatAttributes.ARMOR_PENETRATION_FLAT;
            case MAGIC_PENETRATION -> CombatAttributes.MAGIC_PENETRATION;
            case MAGIC_PENETRATION_FLAT -> CombatAttributes.MAGIC_PENETRATION_FLAT;
            case ICE_DAMAGE_REDUCTION -> CombatAttributes.ICE_DEFENSE;
            case LIGHT_DAMAGE_REDUCTION -> CombatAttributes.LIGHT_DEFENSE;
            case DARKNESS_DAMAGE_REDUCTION -> CombatAttributes.DARKNESS_DEFENSE;
            case DIVINE_DAMAGE_REDUCTION -> CombatAttributes.DIVINE_DEFENSE;
            default -> null;
        };
    }
}
