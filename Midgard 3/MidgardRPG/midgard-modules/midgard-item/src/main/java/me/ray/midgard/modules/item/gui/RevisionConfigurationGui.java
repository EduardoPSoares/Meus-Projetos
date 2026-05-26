package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RevisionConfigurationGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final ItemEditionGui parent;
    private final LanguageManager lang;

    public RevisionConfigurationGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.revision_configuration.title"));
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.lang = MidgardCore.getLanguageManager();
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Slot 2: Get Item
        inventory.setItem(2, new ItemBuilder(Material.EMERALD)
                .name(lang.getMessage("item.gui.item_edition.buttons.get_item.name"))
                .lore(lang.getStringList("item.gui.item_edition.buttons.get_item.lore").stream()
                        .map(MessageUtils::parse)
                        .collect(Collectors.toList()))
                .build());

        // Slot 4: Display Item
        inventory.setItem(4, item.build());

        // Slot 6: Back
        inventory.setItem(6, new ItemBuilder(Material.BARRIER)
                .name(lang.getMessage("item.gui.browser.buttons.back.name"))
                .build());

        // Slot 19: Keep Lore (Name Tag)
        inventory.setItem(19, createToggleItem(Material.NAME_TAG, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_lore.name"), 
                item.isKeepLore(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_lore.lore")));

        // Slot 20: Keep Name (Writable Book)
        inventory.setItem(20, createToggleItem(Material.WRITABLE_BOOK, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_name.name"), 
                item.isKeepName(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_name.lore")));

        // Slot 21: Keep Enchantments (Enchanting Table)
        inventory.setItem(21, createToggleItem(Material.ENCHANTING_TABLE, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_enchantments.name"), 
                item.isKeepEnchantments(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_enchantments.lore")));

        // Slot 22: Keep External SH (Spruce Sign)
        inventory.setItem(22, createToggleItem(Material.SPRUCE_SIGN, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_external_sh.name"), 
                item.isKeepExternalSH(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_external_sh.lore")));

        // Slot 28: Keep Upgrades (Nether Star)
        inventory.setItem(28, createToggleItem(Material.NETHER_STAR, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_upgrades.name"), 
                item.isKeepUpgrades(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_upgrades.lore")));

        // Slot 29: Keep Gem Stones (Emerald)
        inventory.setItem(29, createToggleItem(Material.EMERALD, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_gem_stones.name"), 
                item.isKeepGemStones(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_gem_stones.lore")));

        // Slot 30: Keep Soulbind (Ender Eye)
        inventory.setItem(30, createToggleItem(Material.ENDER_EYE, 
                lang.getRawMessage("item.gui.revision_configuration.toggles.keep_soulbind.name"), 
                item.isKeepSoulbind(),
                lang.getStringList("item.gui.revision_configuration.toggles.keep_soulbind.lore")));

        // Slot 33: Revision ID (Item Frame)
        List<String> revisionLore = lang.getStringList("item.gui.revision_configuration.revision_id.lore");
        List<String> finalRevisionLore = new ArrayList<>();
        for (String line : revisionLore) {
            finalRevisionLore.add(line.replace("%value%", String.valueOf(item.getRevisionId())));
        }
        
        inventory.setItem(33, new ItemBuilder(Material.ITEM_FRAME)
                .name(MessageUtils.parse(lang.getRawMessage("item.gui.revision_configuration.revision_id.name")))
                .lore(finalRevisionLore.stream().map(MessageUtils::parse).collect(Collectors.toList()))
                .build());
    }

    private ItemStack createToggleItem(Material material, String name, boolean value, List<String> description) {
        ItemBuilder builder = new ItemBuilder(material).name(MessageUtils.parse(name));
        
        if (description != null && !description.isEmpty()) {
            builder.lore(description.stream().map(MessageUtils::parse).collect(Collectors.toList()));
            builder.addLoreLine(MessageUtils.parse(""));
        }
        
        builder.addLoreLine(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.revision_configuration.enabled_config").replace("%s", value ? "<gold>true" : "<gold>false")));
        
        return builder.build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 6) {
            parent.open();
        } else if (slot == 2) {
             // Get Item
            player.getInventory().addItem(item.build());
            String msg = lang.getRawMessage("item.command.received")
                    .replace("%s", item.getId())
                    .replace("%amount%", "1");
            player.sendMessage(MessageUtils.parse(msg));
        } else if (slot == 19) {
            item.setKeepLore(!item.isKeepLore());
            initializeItems();
        } else if (slot == 20) {
            item.setKeepName(!item.isKeepName());
            initializeItems();
        } else if (slot == 21) {
            item.setKeepEnchantments(!item.isKeepEnchantments());
            initializeItems();
        } else if (slot == 22) {
            item.setKeepExternalSH(!item.isKeepExternalSH());
            initializeItems();
        } else if (slot == 28) {
            item.setKeepUpgrades(!item.isKeepUpgrades());
            initializeItems();
        } else if (slot == 29) {
            item.setKeepGemStones(!item.isKeepGemStones());
            initializeItems();
        } else if (slot == 30) {
            item.setKeepSoulbind(!item.isKeepSoulbind());
            initializeItems();
        } else if (slot == 33) {
            // Edit Revision ID
            if (event.getClick().isRightClick()) {
                // Increase
                item.setRevisionId(item.getRevisionId() + 1);
                String msg = lang.getRawMessage("item.gui.editor.updated").replace("%s", "Revision ID");
                player.sendMessage(MessageUtils.parse(msg));
                
                // Trigger update for all online players
                module.getItemManager().updateAllOnlinePlayers();
                
                this.open();
            } else if (event.getClick().isLeftClick()) {
                // Decrease
                int newRev = Math.max(1, item.getRevisionId() - 1);
                if (newRev != item.getRevisionId()) {
                    item.setRevisionId(newRev);
                    String msg = lang.getRawMessage("item.gui.editor.updated").replace("%s", "Revision ID");
                    player.sendMessage(MessageUtils.parse(msg));
                    
                    // Trigger update for all online players
                    module.getItemManager().updateAllOnlinePlayers();
                }
                this.open();
            }
        }
    }
}
