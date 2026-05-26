package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class EquippableSlotSelectionGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;

    public EquippableSlotSelectionGui(Player player, ItemModule module, MidgardItem item, BaseGui parent) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.equippable_slot_selection.title"));
        this.module = module;
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        // Background
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 10: Helmet (HEAD)
        inventory.setItem(10, new ItemBuilder(Material.IRON_HELMET)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.head.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.head.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());

        // 11: Chestplate (CHEST)
        inventory.setItem(11, new ItemBuilder(Material.IRON_CHESTPLATE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.chest.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.chest.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());

        // 12: Leggings (LEGS)
        inventory.setItem(12, new ItemBuilder(Material.IRON_LEGGINGS)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.legs.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.legs.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());

        // 13: Boots (FEET)
        inventory.setItem(13, new ItemBuilder(Material.IRON_BOOTS)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.feet.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.feet.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());

        // 14: Offhand (OFF_HAND)
        inventory.setItem(14, new ItemBuilder(Material.SHIELD)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.off_hand.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.off_hand.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());
        
        // 15: Main Hand (HAND)
        inventory.setItem(15, new ItemBuilder(Material.DIAMOND_SWORD)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.hand.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.hand.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());
        
        // 16: Clear (None)
        inventory.setItem(16, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.equippable_slot_selection.items.none.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.equippable_slot_selection.items.none.lore").stream().map(MessageUtils::parse).collect(java.util.stream.Collectors.toList()))
                .build());

        // Back button
        inventory.setItem(18, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 10) {
            item.setEquippableSlot("HEAD");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 11) {
            item.setEquippableSlot("CHEST");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 12) {
            item.setEquippableSlot("LEGS");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 13) {
            item.setEquippableSlot("FEET");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 14) {
            item.setEquippableSlot("OFF_HAND");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 15) {
            item.setEquippableSlot("HAND");
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 16) {
            item.setEquippableSlot(null);
            module.getItemManager().saveItem(item);
            parent.open();
        } else if (slot == 18) {
            parent.open();
        }
    }
}
