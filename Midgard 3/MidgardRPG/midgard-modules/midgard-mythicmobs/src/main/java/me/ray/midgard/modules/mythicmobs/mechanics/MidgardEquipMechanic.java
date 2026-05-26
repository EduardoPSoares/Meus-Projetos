package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class MidgardEquipMechanic implements ITargetedEntitySkill {

    private final String itemId;
    private final String slot;
    private final float dropChance;

    public MidgardEquipMechanic(MythicLineConfig config) {
        this.itemId = config.getString(new String[]{"item", "i"}, "SWORD");
        this.slot = config.getString(new String[]{"slot", "s"}, "HAND").toUpperCase();
        this.dropChance = config.getFloat(new String[]{"dropchance", "dc"}, 0.0f);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!target.isLiving()) {
            return SkillResult.INVALID_TARGET;
        }
        
        LivingEntity entity = (LivingEntity) BukkitAdapter.adapt(target);
        EntityEquipment equipment = entity.getEquipment();
        
        if (equipment == null) {
            return SkillResult.CONDITION_FAILED;
        }
        
        if (ItemModule.getInstance() == null || ItemModule.getInstance().getItemManager() == null) {
            return SkillResult.CONDITION_FAILED;
        }
        MidgardItem midgardItem = ItemModule.getInstance().getItemManager().getMidgardItem(itemId);
        if (midgardItem == null) {
            return SkillResult.CONDITION_FAILED;
        }
        
        ItemStack itemStack = midgardItem.build();
        
        switch (slot) {
            case "HAND":
            case "MAINHAND":
                equipment.setItemInMainHand(itemStack);
                equipment.setItemInMainHandDropChance(dropChance);
                break;
            case "OFFHAND":
                equipment.setItemInOffHand(itemStack);
                equipment.setItemInOffHandDropChance(dropChance);
                break;
            case "HEAD":
            case "HELMET":
                equipment.setHelmet(itemStack);
                equipment.setHelmetDropChance(dropChance);
                break;
            case "CHEST":
            case "CHESTPLATE":
                equipment.setChestplate(itemStack);
                equipment.setChestplateDropChance(dropChance);
                break;
            case "LEGS":
            case "LEGGINGS":
                equipment.setLeggings(itemStack);
                equipment.setLeggingsDropChance(dropChance);
                break;
            case "FEET":
            case "BOOTS":
                equipment.setBoots(itemStack);
                equipment.setBootsDropChance(dropChance);
                break;
        }
        
        return SkillResult.SUCCESS;
    }
}
