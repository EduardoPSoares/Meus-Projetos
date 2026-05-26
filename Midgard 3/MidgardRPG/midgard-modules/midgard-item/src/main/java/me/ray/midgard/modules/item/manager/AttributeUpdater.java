package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataType;

public class AttributeUpdater {

    private static final Map<ItemStat, String> STAT_MAPPING = new EnumMap<>(ItemStat.class);

    static {
        STAT_MAPPING.put(ItemStat.MAX_HEALTH, CombatAttributes.MAX_HEALTH);
        STAT_MAPPING.put(ItemStat.MAX_MANA, CombatAttributes.MAX_MANA);
        STAT_MAPPING.put(ItemStat.MAX_STAMINA, CombatAttributes.MAX_STAMINA);
        STAT_MAPPING.put(ItemStat.ATTACK_DAMAGE, CombatAttributes.PHYSICAL_DAMAGE);
        STAT_MAPPING.put(ItemStat.DEFENSE, CombatAttributes.DEFENSE);
        STAT_MAPPING.put(ItemStat.CRITICAL_STRIKE_CHANCE, CombatAttributes.CRITICAL_CHANCE);
        STAT_MAPPING.put(ItemStat.CRITICAL_STRIKE_POWER, CombatAttributes.CRITICAL_DAMAGE);
        STAT_MAPPING.put(ItemStat.DODGE_RATING, CombatAttributes.DODGE_RATING);
        STAT_MAPPING.put(ItemStat.PARRY_RATING, CombatAttributes.PARRY_RATING);
        STAT_MAPPING.put(ItemStat.BLOCK_RATING, CombatAttributes.BLOCK_RATING);
        STAT_MAPPING.put(ItemStat.BLOCK_POWER, CombatAttributes.BLOCK_POWER);
        STAT_MAPPING.put(ItemStat.ARMOR, CombatAttributes.ARMOR);
        STAT_MAPPING.put(ItemStat.ARMOR_TOUGHNESS, CombatAttributes.ARMOR_TOUGHNESS);
        STAT_MAPPING.put(ItemStat.MOVEMENT_SPEED, CombatAttributes.SPEED);
        STAT_MAPPING.put(ItemStat.COOLDOWN_REDUCTION, CombatAttributes.COOLDOWN_REDUCTION);
        STAT_MAPPING.put(ItemStat.LIFESTEAL, CombatAttributes.LIFE_STEAL);
        
        // Regeneration
        STAT_MAPPING.put(ItemStat.HEALTH_REGENERATION, CombatAttributes.HEALTH_REGEN);
        STAT_MAPPING.put(ItemStat.MAX_HEALTH_REGENERATION, CombatAttributes.MAX_HEALTH_REGEN);
        STAT_MAPPING.put(ItemStat.MANA_REGENERATION, CombatAttributes.MANA_REGEN);
        STAT_MAPPING.put(ItemStat.MAX_MANA_REGENERATION, CombatAttributes.MAX_MANA_REGEN);
        STAT_MAPPING.put(ItemStat.STAMINA_REGENERATION, CombatAttributes.STAMINA_REGEN);
        STAT_MAPPING.put(ItemStat.MAX_STAMINA_REGENERATION, CombatAttributes.MAX_STAMINA_REGEN);

        // Other Stats
        STAT_MAPPING.put(ItemStat.MAX_STELLIUM, CombatAttributes.MAX_STELLIUM);
        STAT_MAPPING.put(ItemStat.MAX_ABSORPTION, CombatAttributes.MAX_ABSORPTION);
        STAT_MAPPING.put(ItemStat.KNOCKBACK_RESISTANCE, CombatAttributes.KNOCKBACK_RESISTANCE);
        STAT_MAPPING.put(ItemStat.MYLUCK, CombatAttributes.LUCK);
        STAT_MAPPING.put(ItemStat.ATTACK_SPEED, CombatAttributes.ATTACK_SPEED);

        // Damage & Defense (ATTACK_DAMAGE already mapped above)
        STAT_MAPPING.put(ItemStat.SKILL_DAMAGE, CombatAttributes.SKILL_DAMAGE);
        STAT_MAPPING.put(ItemStat.PROJECTILE_DAMAGE, CombatAttributes.PROJECTILE_DAMAGE);
        STAT_MAPPING.put(ItemStat.MAGIC_DAMAGE, CombatAttributes.MAGIC_DAMAGE);
        STAT_MAPPING.put(ItemStat.UNDEAD_DAMAGE, CombatAttributes.UNDEAD_DAMAGE);
        
        STAT_MAPPING.put(ItemStat.DAMAGE_REDUCTION, CombatAttributes.DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.FALL_DAMAGE_REDUCTION, CombatAttributes.FALL_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.PROJECTILE_DAMAGE_REDUCTION, CombatAttributes.PROJECTILE_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.PHYSICAL_DAMAGE_REDUCTION, CombatAttributes.PHYSICAL_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.MAGIC_DAMAGE_REDUCTION, CombatAttributes.MAGIC_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.PVE_DAMAGE_REDUCTION, CombatAttributes.PVE_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.PVP_DAMAGE_REDUCTION, CombatAttributes.PVP_DAMAGE_REDUCTION);
        STAT_MAPPING.put(ItemStat.FIRE_DAMAGE_REDUCTION, CombatAttributes.FIRE_DEFENSE);
        
        STAT_MAPPING.put(ItemStat.SPELL_VAMPIRISM, CombatAttributes.SPELL_VAMPIRISM);
        STAT_MAPPING.put(ItemStat.BLOCK_COOLDOWN_REDUCTION, CombatAttributes.BLOCK_COOLDOWN_REDUCTION);
        STAT_MAPPING.put(ItemStat.DODGE_COOLDOWN_REDUCTION, CombatAttributes.DODGE_COOLDOWN_REDUCTION);
        STAT_MAPPING.put(ItemStat.PARRY_COOLDOWN_REDUCTION, CombatAttributes.PARRY_COOLDOWN_REDUCTION);
        STAT_MAPPING.put(ItemStat.SKILL_CRITICAL_STRIKE_CHANCE, CombatAttributes.SKILL_CRITICAL_CHANCE);
        STAT_MAPPING.put(ItemStat.SKILL_CRITICAL_STRIKE_POWER, CombatAttributes.SKILL_CRITICAL_DAMAGE);

        // Elemental
        STAT_MAPPING.put(ItemStat.FIRE_DAMAGE, CombatAttributes.FIRE_DAMAGE);
        STAT_MAPPING.put(ItemStat.ICE_DAMAGE, CombatAttributes.ICE_DAMAGE);
        STAT_MAPPING.put(ItemStat.LIGHT_DAMAGE, CombatAttributes.LIGHT_DAMAGE);
        STAT_MAPPING.put(ItemStat.DARKNESS_DAMAGE, CombatAttributes.DARKNESS_DAMAGE);
        STAT_MAPPING.put(ItemStat.DIVINE_DAMAGE, CombatAttributes.DIVINE_DAMAGE);

        // New RPG Stats
        STAT_MAPPING.put(ItemStat.STRENGTH, CombatAttributes.STRENGTH);
        STAT_MAPPING.put(ItemStat.INTELLIGENCE, CombatAttributes.INTELLIGENCE);
        STAT_MAPPING.put(ItemStat.DEXTERITY, CombatAttributes.DEXTERITY);
        STAT_MAPPING.put(ItemStat.ACCURACY, CombatAttributes.ACCURACY);
        STAT_MAPPING.put(ItemStat.CRITICAL_RESISTANCE, CombatAttributes.CRITICAL_RESISTANCE);
        STAT_MAPPING.put(ItemStat.THORNS, CombatAttributes.THORNS);
        STAT_MAPPING.put(ItemStat.MAGIC_RESISTANCE, CombatAttributes.MAGIC_RESISTANCE);
        
        STAT_MAPPING.put(ItemStat.ARMOR_PENETRATION, CombatAttributes.ARMOR_PENETRATION);
        STAT_MAPPING.put(ItemStat.ARMOR_PENETRATION_FLAT, CombatAttributes.ARMOR_PENETRATION_FLAT);
        STAT_MAPPING.put(ItemStat.MAGIC_PENETRATION, CombatAttributes.MAGIC_PENETRATION);
        STAT_MAPPING.put(ItemStat.MAGIC_PENETRATION_FLAT, CombatAttributes.MAGIC_PENETRATION_FLAT);
        
        STAT_MAPPING.put(ItemStat.ICE_DAMAGE_REDUCTION, CombatAttributes.ICE_DEFENSE);
        STAT_MAPPING.put(ItemStat.LIGHT_DAMAGE_REDUCTION, CombatAttributes.LIGHT_DEFENSE);
        STAT_MAPPING.put(ItemStat.DARKNESS_DAMAGE_REDUCTION, CombatAttributes.DARKNESS_DEFENSE);
        STAT_MAPPING.put(ItemStat.DIVINE_DAMAGE_REDUCTION, CombatAttributes.DIVINE_DEFENSE);
    }

    private static final Map<ItemStat, Attribute> VANILLA_ATTR_MAPPING = new EnumMap<>(ItemStat.class);

    static {
        // MAX_HEALTH removido intencionalmente — vida máxima é gerenciada pelo sistema RPG (CombatAttributes),
        // não pelo vanilla. Manter aqui causava "dano visual" ao equipar itens (barra de corações oscilava).
        VANILLA_ATTR_MAPPING.put(ItemStat.MOVEMENT_SPEED, Attribute.MOVEMENT_SPEED);
        VANILLA_ATTR_MAPPING.put(ItemStat.ATTACK_DAMAGE, Attribute.ATTACK_DAMAGE);
        VANILLA_ATTR_MAPPING.put(ItemStat.ATTACK_SPEED, Attribute.ATTACK_SPEED);
        VANILLA_ATTR_MAPPING.put(ItemStat.ARMOR, Attribute.ARMOR);
        VANILLA_ATTR_MAPPING.put(ItemStat.ARMOR_TOUGHNESS, Attribute.ARMOR_TOUGHNESS);
        VANILLA_ATTR_MAPPING.put(ItemStat.KNOCKBACK_RESISTANCE, Attribute.KNOCKBACK_RESISTANCE);
        VANILLA_ATTR_MAPPING.put(ItemStat.MYLUCK, Attribute.LUCK);
        VANILLA_ATTR_MAPPING.put(ItemStat.MAX_ABSORPTION, Attribute.MAX_ABSORPTION);
        VANILLA_ATTR_MAPPING.put(ItemStat.FALL_DAMAGE_MULTIPLIER, Attribute.FALL_DAMAGE_MULTIPLIER);
        VANILLA_ATTR_MAPPING.put(ItemStat.GRAVITY, Attribute.GRAVITY);
        VANILLA_ATTR_MAPPING.put(ItemStat.JUMP_STRENGTH, Attribute.JUMP_STRENGTH);
        VANILLA_ATTR_MAPPING.put(ItemStat.SAFE_FALL_DISTANCE, Attribute.SAFE_FALL_DISTANCE);
        VANILLA_ATTR_MAPPING.put(ItemStat.SCALE, Attribute.SCALE);
        VANILLA_ATTR_MAPPING.put(ItemStat.STEP_HEIGHT, Attribute.STEP_HEIGHT);
        VANILLA_ATTR_MAPPING.put(ItemStat.BURNING_TIME, Attribute.BURNING_TIME);
        VANILLA_ATTR_MAPPING.put(ItemStat.EXPLOSION_KNOCKBACK_RESISTANCE, Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
        VANILLA_ATTR_MAPPING.put(ItemStat.MINING_EFFICIENCY, Attribute.MINING_EFFICIENCY);
        VANILLA_ATTR_MAPPING.put(ItemStat.BONUS_OXYGEN, Attribute.OXYGEN_BONUS);
        VANILLA_ATTR_MAPPING.put(ItemStat.SNEAKING_SPEED, Attribute.SNEAKING_SPEED);
        VANILLA_ATTR_MAPPING.put(ItemStat.SUBMERGED_MINING_SPEED, Attribute.SUBMERGED_MINING_SPEED);
        VANILLA_ATTR_MAPPING.put(ItemStat.SWEEPING_DAMAGE_RATIO, Attribute.SWEEPING_DAMAGE_RATIO);
        VANILLA_ATTR_MAPPING.put(ItemStat.WATER_MOVEMENT_EFFICIENCY, Attribute.WATER_MOVEMENT_EFFICIENCY);
        VANILLA_ATTR_MAPPING.put(ItemStat.BLOCK_INTERACTION_RANGE, Attribute.BLOCK_INTERACTION_RANGE);
        VANILLA_ATTR_MAPPING.put(ItemStat.ENTITY_INTERACTION_RANGE, Attribute.ENTITY_INTERACTION_RANGE);
    }

    public static void updateAttributes(Player player) {
        updateAttributes(player, -1);
    }

    public static void updateAttributes(Player player, int overrideMainHandSlot) {
        // Early exit if ItemModule is not ready
        ItemModule itemModule = ItemModule.getInstance();
        if (itemModule == null || itemModule.getItemManager() == null) { return; }
        
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return; }

        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        if (attributeData == null) { return; }

        // DEBUG LOG
        // ItemModule.getInstance().getPlugin().getLogger().info("DEBUG: Updating attributes for " + player.getName());

        // 1. Clear existing equipment modifiers
        for (AttributeInstance instance : attributeData.getInstances().values()) {
            instance.removeModifier("Equipment");
        }

        // 2. Calculate new totals
        Map<String, Double> totals = new HashMap<>();

        ItemStack[] armor = player.getInventory().getArmorContents();
        // Armor contents: [Boots, Leggings, Chestplate, Helmet]
        // Indices: 0=Boots, 1=Leggings, 2=Chestplate, 3=Helmet
        
        processItem(player, armor[0], totals, EquipmentSlot.FEET);
        processItem(player, armor[1], totals, EquipmentSlot.LEGS);
        processItem(player, armor[2], totals, EquipmentSlot.CHEST);
        processItem(player, armor[3], totals, EquipmentSlot.HEAD);
        
        ItemStack mainHand;
        if (overrideMainHandSlot != -1) {
            mainHand = player.getInventory().getItem(overrideMainHandSlot);
        } else {
            mainHand = player.getInventory().getItemInMainHand();
        }

        processItem(player, mainHand, totals, EquipmentSlot.HAND);
        processItem(player, player.getInventory().getItemInOffHand(), totals, EquipmentSlot.OFF_HAND);

        // 3. Apply new modifiers
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            String attrId = entry.getKey();
            double value = entry.getValue();

            AttributeInstance instance = attributeData.getInstance(attrId);
            if (instance != null) {
                instance.addModifier(new AttributeModifier("Equipment", value, AttributeOperation.ADD_NUMBER));
                // ItemModule.getInstance().getPlugin().getLogger().info("DEBUG: Applied " + attrId + " -> " + value);
            }
        }

        // 4. Clamp resources (prevent overflow on unequip) — delegated to combat module
        try {
            me.ray.midgard.modules.combat.CombatManager cm = me.ray.midgard.modules.combat.CombatManager.getInstance();
            if (cm != null) {
                cm.clampResources(player);
            }
        } catch (Exception e) {
            // Ignore if combat module not loaded (soft dependency)
        }
    }

    private static void processItem(Player player, ItemStack itemStack, Map<String, Double> totals, EquipmentSlot currentSlot) {
        if (itemStack == null) { return; }
        
        // Check if ItemModule and ItemManager are available
        ItemModule itemModule = ItemModule.getInstance();
        if (itemModule == null) { return; }
        
        ItemManager itemManager = itemModule.getItemManager();
        if (itemManager == null) { return; }
        
        String id = itemManager.getItemId(itemStack);
        MidgardItem item = (id != null) ? itemManager.getMidgardItem(id) : null;

        if (item != null) {
            // Skip consumable items - their stats are applied on consumption, not while held
            if ("CONSUMABLE".equalsIgnoreCase(item.getCategoryId())) {
                // Strip vanilla attribute modifiers from old consumable items
                stripVanillaAttributes(itemStack);
                return;
            }
            
            // Validate Slot using MidgardItem definition
            String requiredSlotName = item.getEquippableSlot();
            boolean validSlot;
            if (requiredSlotName != null && !requiredSlotName.isEmpty() && !requiredSlotName.equalsIgnoreCase("ANY")) {
                 validSlot = false;
                 if (requiredSlotName.equalsIgnoreCase("HEAD") && currentSlot == EquipmentSlot.HEAD) {
                     validSlot = true;
                 } else if (requiredSlotName.equalsIgnoreCase("CHEST") && currentSlot == EquipmentSlot.CHEST) {
                     validSlot = true;
                 } else if (requiredSlotName.equalsIgnoreCase("LEGS") && currentSlot == EquipmentSlot.LEGS) {
                     validSlot = true;
                 } else if (requiredSlotName.equalsIgnoreCase("FEET") && currentSlot == EquipmentSlot.FEET) {
                     validSlot = true;
                 } else if (requiredSlotName.equalsIgnoreCase("OFF_HAND") && currentSlot == EquipmentSlot.OFF_HAND) {
                     validSlot = true;
                 } else if (requiredSlotName.equalsIgnoreCase("HAND") && currentSlot == EquipmentSlot.HAND) {
                     validSlot = true;
                 }
            } else {
                 // No explicit slot defined - use material-based validation
                 validSlot = isValidSlot(itemStack, currentSlot);
            }

            if (!validSlot) {
                 stripVanillaAttributes(itemStack, true);
                 return;
            }

            // Level requirement check
            int requiredLevel = item.getRequiredLevel();
            if (requiredLevel > 0 && player != null) {
                MidgardProfile levelProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
                if (levelProfile != null) {
                    ClassData classData = levelProfile.getData(ClassData.class);
                    int playerLevel = (classData != null) ? classData.getLevel() : 1;
                    if (playerLevel < requiredLevel) {
                        stripVanillaAttributes(itemStack, true);
                        return;
                    }
                }
            }

            // All checks passed - rebuild vanilla attributes if they were previously stripped
            if (isVanillaStripped(itemStack)) {
                rebuildVanillaAttributes(itemStack, item);
            }
        } else {
            // Fallback validation for items not in registry or without ID
            if (!isValidSlot(itemStack, currentSlot)) {
                // ItemModule.getInstance().getPlugin().getLogger().info("DEBUG: Invalid vanilla slot for " + itemStack.getType() + ": " + currentSlot);
                return;
            }
        }

        Map<ItemStat, Double> itemStats = ItemPDC.getStats(itemStack);
        if (itemStats.isEmpty() && item != null) {
            // Fallback for legacy items
            for (Map.Entry<ItemStat, StatRange> statEntry : item.getStats().entrySet()) {
                itemStats.put(statEntry.getKey(), statEntry.getValue().getMin());
            }
        }
        
        // ItemModule.getInstance().getPlugin().getLogger().info("DEBUG: Processing " + (id != null ? id : itemStack.getType()) + " in " + currentSlot + ". Stats: " + itemStats);

        for (Map.Entry<ItemStat, Double> statEntry : itemStats.entrySet()) {
            ItemStat stat = statEntry.getKey();
            double value = statEntry.getValue();
            
            String attrId = STAT_MAPPING.get(stat);
            if (attrId != null) {
                totals.merge(attrId, value, (a, b) -> a + b);
            }
        }
    }

    private static boolean isValidSlot(ItemStack item, EquipmentSlot slot) {
        String type = item.getType().name();
        if (slot == EquipmentSlot.HEAD) { return type.endsWith("_HELMET") || type.endsWith("_SKULL") || type.endsWith("_HEAD"); }
        if (slot == EquipmentSlot.CHEST) { return type.endsWith("_CHESTPLATE") || type.equals("ELYTRA"); }
        if (slot == EquipmentSlot.LEGS) { return type.endsWith("_LEGGINGS"); }
        if (slot == EquipmentSlot.FEET) { return type.endsWith("_BOOTS"); }
        
        // For hands, allow anything EXCEPT armor
        if (slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) {
            return !type.endsWith("_HELMET") && !type.endsWith("_CHESTPLATE") && !type.endsWith("_LEGGINGS") && !type.endsWith("_BOOTS");
        }
        return false;
    }

    private static void stripVanillaAttributes(ItemStack itemStack) {
        stripVanillaAttributes(itemStack, false);
    }

    private static void stripVanillaAttributes(ItemStack itemStack, boolean setRecoverFlag) {
        if (itemStack == null || !itemStack.hasItemMeta()) { return; }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) { return; }

        boolean hadModifiers = meta.hasAttributeModifiers();
        if (hadModifiers) {
            for (Attribute attr : meta.getAttributeModifiers().keySet()) {
                meta.removeAttributeModifier(attr);
            }
        }

        if (setRecoverFlag) {
            meta.getPersistentDataContainer().set(
                me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_vanilla_stripped"),
                PersistentDataType.BYTE, (byte) 1);
        }

        if (hadModifiers || setRecoverFlag) {
            itemStack.setItemMeta(meta);
        }
    }

    private static boolean isVanillaStripped(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) { return false; }
        return itemStack.getItemMeta().getPersistentDataContainer().has(
            me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_vanilla_stripped"),
            PersistentDataType.BYTE);
    }

    private static void rebuildVanillaAttributes(ItemStack itemStack, MidgardItem item) {
        if (itemStack == null || !itemStack.hasItemMeta()) { return; }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) { return; }

        Map<ItemStat, Double> stats = ItemPDC.getStats(itemStack);
        if (stats.isEmpty()) { return; }

        // Determine correct slot group
        EquipmentSlot slot = null;
        String slotName = item.getEquippableSlot();
        if (slotName != null && !slotName.isEmpty() && !slotName.equalsIgnoreCase("ANY")) {
            try { slot = EquipmentSlot.valueOf(slotName.toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* Invalid slot name, will infer from material */ }
        }
        if (slot == null) {
            slot = inferSlotFromMaterial(itemStack.getType());
        }
        EquipmentSlotGroup group = slotToGroup(slot);

        for (Map.Entry<ItemStat, Attribute> mapping : VANILLA_ATTR_MAPPING.entrySet()) {
            Double value = stats.get(mapping.getKey());
            if (value == null || value == 0) {
                continue;
            }

            Attribute attr = mapping.getValue();
            NamespacedKey key = new NamespacedKey(ItemModule.getInstance().getPlugin(),
                "stat_" + attr.getKey().getKey().replace('.', '_').toLowerCase());
            meta.addAttributeModifier(attr, new org.bukkit.attribute.AttributeModifier(
                key, value, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, group));
        }

        meta.getPersistentDataContainer().remove(
            new NamespacedKey(ItemModule.getInstance().getPlugin(), "midgard_vanilla_stripped"));
        itemStack.setItemMeta(meta);
    }

    private static EquipmentSlotGroup slotToGroup(EquipmentSlot slot) {
        if (slot == null) {
            return EquipmentSlotGroup.ANY;
        }
        return switch (slot) {
            case HAND -> EquipmentSlotGroup.HAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            case FEET -> EquipmentSlotGroup.FEET;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case HEAD -> EquipmentSlotGroup.HEAD;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    private static EquipmentSlot inferSlotFromMaterial(Material material) {
        if (material == null) { return null; }
        String name = material.name();
        if (name.endsWith("_HELMET")) {
            return EquipmentSlot.HEAD;
        }
        if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) {
            return EquipmentSlot.CHEST;
        }
        if (name.endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        }
        if (name.endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }
}
