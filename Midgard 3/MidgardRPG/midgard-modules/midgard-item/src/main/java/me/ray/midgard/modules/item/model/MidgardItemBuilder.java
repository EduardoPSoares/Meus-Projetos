package me.ray.midgard.modules.item.model;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.socket.SocketData;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.LoreFormatter;
import me.ray.midgard.modules.item.utils.StatRange;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;

/**
 * Construtor de ItemStack para itens do MidgardRPG.
 * <p>
 * Responsável por converter um objeto {@link MidgardItem} em um {@link ItemStack} do Bukkit,
 * aplicando todas as propriedades visuais e metadados (Lore, Nome, ModelData, etc.).
 */
public class MidgardItemBuilder {

    private final MidgardItem item;

    /**
     * Construtor do MidgardItemBuilder.
     *
     * @param item O item do MidgardRPG a ser construído.
     */
    public MidgardItemBuilder(MidgardItem item) {
        this.item = Objects.requireNonNull(item, "MidgardItem não pode ser nulo");
    }

    /**
     * Constrói o ItemStack final.
     *
     * @return O ItemStack configurado.
     */
    public ItemStack build() {
        ItemStack itemStack = createBaseItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) { return itemStack; }

        Map<ItemStat, Double> rolledStats = rollAndApplyStats(meta);
        applyDisplayAndLore(meta, rolledStats);
        applyItemModel(meta);
        applyEquippableSlot(meta);
        applyEquippableModel(meta);
        applyDurabilityAndFlags(meta, rolledStats);
        applyEnchantments(meta);

        if (!"CONSUMABLE".equalsIgnoreCase(item.getCategoryId())) {
            applyAttributes(meta, rolledStats);
        }

        applyArmorTrim(meta);
        applyPDCMetadata(meta);
        applyStackAndTooltip(meta);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createBaseItemStack() {
        ItemStack itemStack = null;
        if (item.getNexoId() != null && me.ray.midgard.core.integration.NexoUtils.isNexoEnabled()) {
             itemStack = me.ray.midgard.core.integration.NexoUtils.getCustomItem(item.getNexoId());
        }

        if (itemStack == null) {
            Material mat = item.getMaterial();
            if (mat == null) {
                 ItemModule.getInstance().getPlugin().getLogger().warning("Material do item é nulo para o item: " + item.getId());
                 mat = Material.STONE;
            }
            itemStack = new ItemStack(mat);
        }
        return itemStack;
    }

    private Map<ItemStat, Double> rollAndApplyStats(ItemMeta meta) {
        ItemPDC.setString(meta, "midgard_rng_rolled", "true");
        Map<ItemStat, Double> rolledStats = new HashMap<>();
        for (Map.Entry<ItemStat, StatRange> entry : item.getStats().entrySet()) {
            double val = entry.getValue().getRandom();
            if (val != 0) {
                rolledStats.put(entry.getKey(), val);
                ItemPDC.setStat(meta, entry.getKey(), val);
            }
        }
        return rolledStats;
    }

    private void applyDisplayAndLore(ItemMeta meta, Map<ItemStat, Double> rolledStats) {
        if (item.getDisplayName() != null) {
            meta.displayName(MessageUtils.parse(item.getDisplayName()));
        }
        List<Component> lore = LoreFormatter.formatLore(item, null, rolledStats);
        meta.lore(lore);
    }

    private void applyItemModel(ItemMeta meta) {
        if (item.getCustomModelData() != 0) {
            setCustomModelData(meta, item.getCustomModelData());
        }

        if (item.getItemModel() != null && !item.getItemModel().isEmpty()) {
            try {
                NamespacedKey key = NamespacedKey.fromString(item.getItemModel());
                if (key != null) {
                    java.lang.reflect.Method setItemModel = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
                    setItemModel.setAccessible(true);
                    setItemModel.invoke(meta, key);
                }
            } catch (NoSuchMethodException e) {
                // Feature not supported on this server version
            } catch (Exception e) {
                ItemModule.getInstance().getPlugin().getLogger().log(Level.WARNING, "Falha ao definir modelo do item para " + item.getId(), e);
            }
        }
    }

    private void applyEquippableSlot(ItemMeta meta) {
        if (item.getEquippableSlot() == null || item.getEquippableSlot().isEmpty()) {
            return;
        }

        NamespacedKey slotKey = ItemPDC.key("midgard_equippable_slot");
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, item.getEquippableSlot());

        try {
            EquipmentSlot slot = EquipmentSlot.valueOf(item.getEquippableSlot().toUpperCase());
            try {
                java.lang.reflect.Method getEquippable = meta.getClass().getMethod("getEquippable");
                getEquippable.setAccessible(true);
                Object equippableComponent = getEquippable.invoke(meta);

                java.lang.reflect.Method setSlot = equippableComponent.getClass().getMethod("setSlot", EquipmentSlot.class);
                setSlot.setAccessible(true);
                setSlot.invoke(equippableComponent, slot);
            } catch (Exception e) {
                // Component API not available or failed
            }
        } catch (IllegalArgumentException e) {
            // Invalid slot name
        }
    }

    private void applyEquippableModel(ItemMeta meta) {
        if (item.getEquippableModel() == null || item.getEquippableModel().isEmpty()) {
            return;
        }

        try {
            String modelKey = item.getEquippableModel().toLowerCase();
            NamespacedKey key = modelKey.contains(":")
                ? new NamespacedKey(modelKey.split(":")[0], modelKey.split(":")[1])
                : NamespacedKey.minecraft(modelKey);

            try {
                java.lang.reflect.Method getEquippable = meta.getClass().getMethod("getEquippable");
                getEquippable.setAccessible(true);
                Object equippableComponent = getEquippable.invoke(meta);

                java.lang.reflect.Method setModel = equippableComponent.getClass().getMethod("setModel", NamespacedKey.class);
                setModel.setAccessible(true);
                setModel.invoke(equippableComponent, key);

                if (item.getEquippableSlot() == null || item.getEquippableSlot().isEmpty()) {
                    EquipmentSlot defaultSlot = inferSlot(item.getMaterial());
                    if (defaultSlot != null) {
                        java.lang.reflect.Method setSlot = equippableComponent.getClass().getMethod("setSlot", EquipmentSlot.class);
                        setSlot.setAccessible(true);
                        setSlot.invoke(equippableComponent, defaultSlot);
                    }
                }

                try {
                    java.lang.reflect.Method setEquippable = meta.getClass().getMethod("setEquippable", equippableComponent.getClass());
                    setEquippable.setAccessible(true);
                    setEquippable.invoke(meta, equippableComponent);
                } catch (NoSuchMethodException e) {
                    for (Class<?> iface : equippableComponent.getClass().getInterfaces()) {
                        try {
                            java.lang.reflect.Method setEquippable = meta.getClass().getMethod("setEquippable", iface);
                            setEquippable.setAccessible(true);
                            setEquippable.invoke(meta, equippableComponent);
                            break;
                        } catch (NoSuchMethodException ignored) { /* Try next interface */ }
                    }
                }

            } catch (Exception e) {
                MidgardLogger.error("Falha ao definir custom model data", e);
            }
        } catch (Exception e) {
            MidgardLogger.error("Falha ao resolver custom model data", e);
        }
    }

    private void applyDurabilityAndFlags(ItemMeta meta, Map<ItemStat, Double> rolledStats) {
        if (item.getBaseItemDamage() > 0 && meta instanceof Damageable) {
            ((Damageable) meta).setDamage(item.getBaseItemDamage());
        }

        if (item.getMaxVanillaDurability() > 0 && meta instanceof Damageable) {
            ((Damageable) meta).setMaxDamage(item.getMaxVanillaDurability());
        }

        if (item.isUnbreakable() || (rolledStats.containsKey(ItemStat.UNBREAKABLE) && rolledStats.get(ItemStat.UNBREAKABLE) > 0)) {
            meta.setUnbreakable(true);
        }

        if (item.isHideEnchantments()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (item.isHideTooltip()) {
            try {
                java.lang.reflect.Method setHideTooltip = meta.getClass().getMethod("setHideTooltip", boolean.class);
                setHideTooltip.invoke(meta, true);
            } catch (NoSuchMethodException e) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            } catch (Exception e) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
        }
        if (item.isHideDurabilityBar()) {
            meta.getPersistentDataContainer().set(
                ItemPDC.key("midgard_hide_durability_bar"),
                PersistentDataType.BYTE, (byte) 1);
        }
        if (item.isHideArmorTrim()) {
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        }
    }

    private void applyEnchantments(ItemMeta meta) {
        if (item.getEnchantments() == null || item.getEnchantments().isEmpty()) {
            return;
        }

        String[] enchants = item.getEnchantments().split(",");
        for (String ench : enchants) {
            try {
                String[] parts = ench.split(":");
                NamespacedKey key = null;
                int level = 1;

                if (parts.length == 3) {
                    key = new NamespacedKey(parts[0], parts[1]);
                    try {
                        level = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        level = 1;
                    }
                } else if (parts.length == 2) {
                    key = NamespacedKey.minecraft(parts[0]);
                    try {
                        level = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        level = 1;
                    }
                }

                if (key != null) {
                     Enchantment enchantment = getRegistry(Enchantment.class).get(key);
                     if (enchantment != null) {
                         meta.addEnchant(enchantment, level, true);
                     }
                }
            } catch (Exception e) {
                 ItemModule.getInstance().getPlugin().getLogger().log(Level.WARNING, "Falha ao aplicar encantamento " + ench + " para o item " + item.getId(), e);
            }
        }
    }

    private void applyArmorTrim(ItemMeta meta) {
        if (!(meta instanceof ArmorMeta)) {
            return;
        }
        if (item.getTrimMaterial() == null || item.getTrimMaterial().isEmpty()) {
            return;
        }
        if (item.getTrimPattern() == null || item.getTrimPattern().isEmpty()) {
            return;
        }

        try {
            Registry<TrimMaterial> tmRegistry = getRegistry(TrimMaterial.class);
            Registry<TrimPattern> tpRegistry = getRegistry(TrimPattern.class);

            if (tmRegistry != null && tpRegistry != null) {
                TrimMaterial tm = tmRegistry.get(NamespacedKey.minecraft(item.getTrimMaterial().toLowerCase()));
                TrimPattern tp = tpRegistry.get(NamespacedKey.minecraft(item.getTrimPattern().toLowerCase()));
                if (tm != null && tp != null) {
                    ((ArmorMeta) meta).setTrim(new ArmorTrim(tm, tp));
                } else {
                    ItemModule.getInstance().getPlugin().getLogger().warning("Material ou padrão de acabamento inválido para o item " + item.getId());
                }
            }
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Trims not supported
        } catch (Exception e) {
            ItemModule.getInstance().getPlugin().getLogger().log(Level.WARNING, "Falha ao definir acabamento de armadura para " + item.getId(), e);
        }
    }

    private void applyPDCMetadata(ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        var plugin = ItemModule.getInstance().getPlugin();

        pdc.set(ItemPDC.key("midgard_id"), PersistentDataType.STRING, item.getId());
        pdc.set(ItemPDC.key("midgard_revision"), PersistentDataType.INTEGER, item.getRevisionId());

        if (item.getMaxCustomDurability() > 0) {
            pdc.set(ItemPDC.key("midgard_max_durability"), PersistentDataType.INTEGER, item.getMaxCustomDurability());
            pdc.set(ItemPDC.key("midgard_durability"), PersistentDataType.INTEGER, item.getMaxCustomDurability());
        }

        if (item.isLostWhenBroken()) {
            pdc.set(ItemPDC.key("midgard_lost_when_broken"), PersistentDataType.BYTE, (byte) 1);
        }

        setPdcString(pdc, plugin, "midgard_nbt_tags", item.getNbtTags());
        setPdcString(pdc, plugin, "midgard_custom_model_data_strings", item.getCustomModelDataStrings());
        setPdcString(pdc, plugin, "midgard_permission", item.getPermission());
        setPdcString(pdc, plugin, "midgard_item_particles", item.getItemParticles());

        if (item.getRequiredClasses() != null && !item.getRequiredClasses().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_required_class"), PersistentDataType.STRING, String.join(",", item.getRequiredClasses()));
        }

        setPdcFlag(pdc, plugin, "midgard_disable_interaction", item.isDisableInteraction());
        setPdcFlag(pdc, plugin, "midgard_disable_crafting", item.isDisableCrafting());
        setPdcFlag(pdc, plugin, "midgard_disable_smelting", item.isDisableSmelting());
        setPdcFlag(pdc, plugin, "midgard_disable_repairing", item.isDisableRepairing());
        setPdcFlag(pdc, plugin, "midgard_disable_enchanting", item.isDisableEnchanting());
        setPdcFlag(pdc, plugin, "midgard_disable_smithing", item.isDisableSmithing());
        setPdcFlag(pdc, plugin, "midgard_disable_item_dropping", item.isDisableItemDropping());

        setPdcString(pdc, plugin, "midgard_tier", item.getTier());
        setPdcString(pdc, plugin, "midgard_item_set", item.getItemSet());

        if (item.getRequiredBiomes() != null && !item.getRequiredBiomes().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_required_biomes"), PersistentDataType.STRING, String.join(",", item.getRequiredBiomes()));
        }

        setPdcFlag(pdc, plugin, "midgard_disable_drop_on_death", item.isDisableDropOnDeath());
        setPdcString(pdc, plugin, "midgard_camera_overlay", item.getCameraOverlay());
        setPdcFlag(pdc, plugin, "midgard_unstackable", item.isUnstackable());
        setPdcString(pdc, plugin, "midgard_cooldown_reference", item.getCooldownReference());
        setPdcString(pdc, plugin, "midgard_crafting_recipe_permission", item.getCraftingRecipePermission());

        if (item.getCustomSounds() != null && !item.getCustomSounds().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_custom_sounds"), PersistentDataType.STRING, String.join(",", item.getCustomSounds()));
        }
        if (item.getPermanentEffects() != null && !item.getPermanentEffects().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_permanent_effects"), PersistentDataType.STRING, String.join(",", item.getPermanentEffects()));
        }
        if (item.getGrantedPermissions() != null && !item.getGrantedPermissions().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_granted_permissions"), PersistentDataType.STRING, String.join(",", item.getGrantedPermissions()));
        }
        if (item.getCommands() != null && !item.getCommands().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_commands"), PersistentDataType.STRING, String.join(";;;", item.getCommands()));
        }
        if (item.getCompatibleTypes() != null && !item.getCompatibleTypes().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_compatible_types"), PersistentDataType.STRING, String.join(",", item.getCompatibleTypes()));
        }
        if (item.getCompatibleIds() != null && !item.getCompatibleIds().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_compatible_ids"), PersistentDataType.STRING, String.join(",", item.getCompatibleIds()));
        }
        if (item.getCompatibleMaterials() != null && !item.getCompatibleMaterials().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_compatible_materials"), PersistentDataType.STRING, String.join(",", item.getCompatibleMaterials()));
        }

        setPdcString(pdc, plugin, "midgard_repair_reference", item.getRepairReference());
        setPdcFlag(pdc, plugin, "midgard_amphibian", item.isAmphibian());

        if (item.getItemAbilities() != null && !item.getItemAbilities().isEmpty()) {
            pdc.set(ItemPDC.key("midgard_item_abilities"), PersistentDataType.STRING, String.join(",", item.getItemAbilities()));
        }

        if (item.getGemSockets() != null && !item.getGemSockets().isEmpty()) {
            SocketData socketData = new SocketData(item.getGemSockets());
            String data = socketData.getSockets().stream()
                    .map(e -> e.getType() + ":" + (e.getGemId() == null ? "null" : e.getGemId()))
                    .collect(java.util.stream.Collectors.joining(";"));
            ItemPDC.setString(meta, "midgard_sockets_data", data);
            pdc.set(ItemPDC.key("midgard_gem_sockets"), PersistentDataType.STRING, String.join(",", item.getGemSockets()));
        }

        if (item.getBrowserIndex() != 0) {
            pdc.set(ItemPDC.key("midgard_browser_index"), PersistentDataType.INTEGER, item.getBrowserIndex());
        }
        setPdcFlag(pdc, plugin, "midgard_disable_advanced_enchants", item.isDisableAdvancedEnchants());
    }

    private void applyStackAndTooltip(ItemMeta meta) {
        if (item.getMaxStackSize() != 64) {
            try {
                meta.setMaxStackSize(item.getMaxStackSize());
            } catch (NoSuchMethodError e) {
                // Method introduced in 1.20.6+
            } catch (Exception e) {
                ItemModule.getInstance().getPlugin().getLogger().log(Level.WARNING, "Falha ao definir tamanho máximo da pilha para " + item.getId(), e);
            }
        }

        if (item.getVanillaTooltipStyle() != null && !item.getVanillaTooltipStyle().isEmpty()) {
            try {
                NamespacedKey key = NamespacedKey.fromString(item.getVanillaTooltipStyle());
                if (key != null) {
                    java.lang.reflect.Method setTooltipStyle = meta.getClass().getMethod("setTooltipStyle", NamespacedKey.class);
                    setTooltipStyle.invoke(meta, key);
                }
            } catch (NoSuchMethodException | SecurityException e) {
                 // Not supported
            } catch (Exception e) {
                ItemModule.getInstance().getPlugin().getLogger().log(Level.WARNING, "Falha ao definir estilo de dica para " + item.getId(), e);
            }
        }

        if (item.getCustomTooltip() != null && !item.getCustomTooltip().isEmpty()) {
            NamespacedKey tooltipKey = ItemPDC.key("midgard_custom_tooltip");
            meta.getPersistentDataContainer().set(tooltipKey, PersistentDataType.STRING, item.getCustomTooltip());
        }
    }

    private void setPdcString(org.bukkit.persistence.PersistentDataContainer pdc, org.bukkit.plugin.java.JavaPlugin plugin, String key, String value) {
        if (value != null && !value.isEmpty()) {
            pdc.set(ItemPDC.key(key), PersistentDataType.STRING, value);
        }
    }

    private void setPdcFlag(org.bukkit.persistence.PersistentDataContainer pdc, org.bukkit.plugin.java.JavaPlugin plugin, String key, boolean value) {
        if (value) {
            pdc.set(ItemPDC.key(key), PersistentDataType.BYTE, (byte) 1);
        }
    }

        private void applyAttributes(ItemMeta meta, Map<ItemStat, Double> stats) {

        if (stats.containsKey(ItemStat.MAX_HEALTH)) {
            addAttribute(meta, Attribute.MAX_HEALTH, stats.get(ItemStat.MAX_HEALTH));
        }
        if (stats.containsKey(ItemStat.MOVEMENT_SPEED)) {
            addAttribute(meta, Attribute.MOVEMENT_SPEED, stats.get(ItemStat.MOVEMENT_SPEED));
        }
        if (stats.containsKey(ItemStat.ATTACK_DAMAGE)) {
            addAttribute(meta, Attribute.ATTACK_DAMAGE, stats.get(ItemStat.ATTACK_DAMAGE));
        }
        if (stats.containsKey(ItemStat.ATTACK_SPEED)) {
            addAttribute(meta, Attribute.ATTACK_SPEED, stats.get(ItemStat.ATTACK_SPEED));
        }
        if (stats.containsKey(ItemStat.ARMOR)) {
            addAttribute(meta, Attribute.ARMOR, stats.get(ItemStat.ARMOR));
        }
        if (stats.containsKey(ItemStat.ARMOR_TOUGHNESS)) {
            addAttribute(meta, Attribute.ARMOR_TOUGHNESS, stats.get(ItemStat.ARMOR_TOUGHNESS));
        }
        if (stats.containsKey(ItemStat.KNOCKBACK_RESISTANCE)) {
            addAttribute(meta, Attribute.KNOCKBACK_RESISTANCE, stats.get(ItemStat.KNOCKBACK_RESISTANCE));
        }
        if (stats.containsKey(ItemStat.MYLUCK)) {
            addAttribute(meta, Attribute.LUCK, stats.get(ItemStat.MYLUCK));
        }
        
        if (stats.containsKey(ItemStat.MAX_ABSORPTION)) {
            addAttribute(meta, Attribute.MAX_ABSORPTION, stats.get(ItemStat.MAX_ABSORPTION));
        }
        if (stats.containsKey(ItemStat.FALL_DAMAGE_MULTIPLIER)) {
            addAttribute(meta, Attribute.FALL_DAMAGE_MULTIPLIER, stats.get(ItemStat.FALL_DAMAGE_MULTIPLIER));
        }
        if (stats.containsKey(ItemStat.GRAVITY)) {
            addAttribute(meta, Attribute.GRAVITY, stats.get(ItemStat.GRAVITY));
        }
        if (stats.containsKey(ItemStat.JUMP_STRENGTH)) {
            addAttribute(meta, Attribute.JUMP_STRENGTH, stats.get(ItemStat.JUMP_STRENGTH));
        }
        if (stats.containsKey(ItemStat.SAFE_FALL_DISTANCE)) {
            addAttribute(meta, Attribute.SAFE_FALL_DISTANCE, stats.get(ItemStat.SAFE_FALL_DISTANCE));
        }
        if (stats.containsKey(ItemStat.SCALE)) {
            addAttribute(meta, Attribute.SCALE, stats.get(ItemStat.SCALE));
        }
        if (stats.containsKey(ItemStat.STEP_HEIGHT)) {
            addAttribute(meta, Attribute.STEP_HEIGHT, stats.get(ItemStat.STEP_HEIGHT));
        }
        if (stats.containsKey(ItemStat.BURNING_TIME)) {
            addAttribute(meta, Attribute.BURNING_TIME, stats.get(ItemStat.BURNING_TIME));
        }
        if (stats.containsKey(ItemStat.EXPLOSION_KNOCKBACK_RESISTANCE)) {
            addAttribute(meta, Attribute.EXPLOSION_KNOCKBACK_RESISTANCE, stats.get(ItemStat.EXPLOSION_KNOCKBACK_RESISTANCE));
        }
        if (stats.containsKey(ItemStat.MINING_EFFICIENCY)) {
            addAttribute(meta, Attribute.MINING_EFFICIENCY, stats.get(ItemStat.MINING_EFFICIENCY));
        }
        if (stats.containsKey(ItemStat.BONUS_OXYGEN)) {
            addAttribute(meta, Attribute.OXYGEN_BONUS, stats.get(ItemStat.BONUS_OXYGEN));
        }
        if (stats.containsKey(ItemStat.SNEAKING_SPEED)) {
            addAttribute(meta, Attribute.SNEAKING_SPEED, stats.get(ItemStat.SNEAKING_SPEED));
        }
        if (stats.containsKey(ItemStat.SUBMERGED_MINING_SPEED)) {
            addAttribute(meta, Attribute.SUBMERGED_MINING_SPEED, stats.get(ItemStat.SUBMERGED_MINING_SPEED));
        }
        if (stats.containsKey(ItemStat.SWEEPING_DAMAGE_RATIO)) {
            addAttribute(meta, Attribute.SWEEPING_DAMAGE_RATIO, stats.get(ItemStat.SWEEPING_DAMAGE_RATIO));
        }
        if (stats.containsKey(ItemStat.WATER_MOVEMENT_EFFICIENCY)) {
            addAttribute(meta, Attribute.WATER_MOVEMENT_EFFICIENCY, stats.get(ItemStat.WATER_MOVEMENT_EFFICIENCY));
        }
        if (stats.containsKey(ItemStat.BLOCK_INTERACTION_RANGE)) {
            addAttribute(meta, Attribute.BLOCK_INTERACTION_RANGE, stats.get(ItemStat.BLOCK_INTERACTION_RANGE));
        }
        if (stats.containsKey(ItemStat.ENTITY_INTERACTION_RANGE)) {
            addAttribute(meta, Attribute.ENTITY_INTERACTION_RANGE, stats.get(ItemStat.ENTITY_INTERACTION_RANGE));
        }
    }

    private void addAttribute(ItemMeta meta, Attribute attribute, double amount) {
        if (amount == 0) {
            return;
        }
        EquipmentSlot slot = null;
        if (item.getEquippableSlot() != null && !item.getEquippableSlot().isEmpty()) {
            try {
                slot = EquipmentSlot.valueOf(item.getEquippableSlot().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid slot name from config
            }
        }

        // Infer slot from material if not explicitly set (e.g., armor items)
        if (slot == null) {
            slot = inferSlot(item.getMaterial());
        }
        
        org.bukkit.inventory.EquipmentSlotGroup group = org.bukkit.inventory.EquipmentSlotGroup.ANY;
        if (slot != null) {
            switch (slot) {
                case HAND -> group = org.bukkit.inventory.EquipmentSlotGroup.HAND;
                case OFF_HAND -> group = org.bukkit.inventory.EquipmentSlotGroup.OFFHAND;
                case FEET -> group = org.bukkit.inventory.EquipmentSlotGroup.FEET;
                case LEGS -> group = org.bukkit.inventory.EquipmentSlotGroup.LEGS;
                case CHEST -> group = org.bukkit.inventory.EquipmentSlotGroup.CHEST;
                case HEAD -> group = org.bukkit.inventory.EquipmentSlotGroup.HEAD;
                default -> { /* Default is already ANY */ }
            }
        }
        
        NamespacedKey key = ItemPDC.key("stat_" + attribute.getKey().getKey().replace('.', '_').toLowerCase());
        meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER, group));
    }

    @SuppressWarnings("deprecation")
    private <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return Bukkit.getRegistry(type);
    }

    private EquipmentSlot inferSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) { return EquipmentSlot.HEAD; }
        if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) { return EquipmentSlot.CHEST; }
        if (name.endsWith("_LEGGINGS")) { return EquipmentSlot.LEGS; }
        if (name.endsWith("_BOOTS")) { return EquipmentSlot.FEET; }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void setCustomModelData(ItemMeta meta, int data) {
        meta.setCustomModelData(data);
    }
}
