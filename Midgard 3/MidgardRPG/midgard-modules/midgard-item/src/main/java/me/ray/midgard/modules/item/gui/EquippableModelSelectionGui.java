package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

public class EquippableModelSelectionGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;
    private final LanguageManager lang;

    public EquippableModelSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.equippable_model_selection.title"));
        this.item = item;
        this.parent = parent;
        this.lang = MidgardCore.getLanguageManager();
    }

    @Override
    public void initializeItems() {
        // Background
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 10: Iron
        inventory.setItem(10, new ItemBuilder(Material.IRON_INGOT)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.iron.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.iron.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // 11: Gold
        inventory.setItem(11, new ItemBuilder(Material.GOLD_INGOT)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.gold.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.gold.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // 12: Diamond
        inventory.setItem(12, new ItemBuilder(Material.DIAMOND)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.diamond.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.diamond.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // 13: Netherite
        inventory.setItem(13, new ItemBuilder(Material.NETHERITE_INGOT)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.netherite.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.netherite.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // 14: Leather (Default/None)
        inventory.setItem(14, new ItemBuilder(Material.LEATHER)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.leather.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.leather.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // 16: Custom Texture ID
        inventory.setItem(16, new ItemBuilder(Material.NAME_TAG)
                .name(lang.getMessage("item.gui.equippable_model_selection.items.custom.name"))
                .lore(lang.getStringList("item.gui.equippable_model_selection.items.custom.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // Back button
        inventory.setItem(18, new ItemBuilder(Material.ARROW).name(lang.getMessage("item.common.back")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 10) {
            setModel("iron");
        } else if (slot == 11) {
            setModel("gold");
        } else if (slot == 12) {
            setModel("diamond");
        } else if (slot == 13) {
            setModel("netherite");
        } else if (slot == 14) {
            setModel("leather");
        } else if (slot == 16) {
            player.closeInventory();
            player.sendMessage(lang.getMessage("item.gui.equippable_model_selection.prompt"));
            ChatInputListener.requestInput(player, (text) -> {
                String lower = text.toLowerCase();
                item.setEquippableModel(lower);
                item.save();
                player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.equippable_model_selection.success").replace("%s", lower)));
                parent.open();
            });
        } else if (slot == 18) {
            parent.open();
        }
    }

    private void setModel(String model) {
        item.setEquippableModel(model);
        item.save();
        player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.equippable_model_selection.success").replace("%s", model)));
        parent.open();
    }
}
