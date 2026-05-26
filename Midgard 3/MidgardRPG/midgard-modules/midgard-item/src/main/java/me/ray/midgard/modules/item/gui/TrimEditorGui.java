package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class TrimEditorGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public TrimEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.trim_editor.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        String materialKey = item.getTrimMaterial();
        String patternKey = item.getTrimPattern();
        
        if (materialKey == null) {
            materialKey = "NONE";
        }
        if (patternKey == null) {
            patternKey = "NONE";
        }

        // Display Item
        ItemBuilder displayBuilder = new ItemBuilder(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.item.name"));
        
        List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.trim_editor.item.lore");
        List<Component> lore = new ArrayList<>();
        
        for (String line : loreLines) {
            lore.add(MessageUtils.parse(line
                    .replace("%material%", materialKey)
                    .replace("%pattern%", patternKey)));
        }
        
        displayBuilder.lore(lore);
        inventory.setItem(13, displayBuilder.build());
        
        // Back Button
        inventory.setItem(18, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.buttons.back"))
                .build());
                
        // Clear Button
        inventory.setItem(26, new ItemBuilder(Material.LAVA_BUCKET)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.buttons.clear"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            parent.open();
        } else if (slot == 26) {
            item.setTrimMaterial(null);
            item.setTrimPattern(null);
            item.save();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.messages.updated"));
            initializeItems();
        } else if (slot == 13) {
            if (event.isLeftClick()) {
                // Edit Material
                player.closeInventory();
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.messages.prompt_material"));
                ChatInputListener.requestInput(player, (input) -> {
                    // Basic validation could be added here checking against Registry.TRIM_MATERIAL
                    item.setTrimMaterial(input.toUpperCase());
                    item.save();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.messages.updated"));
                    this.open();
                });
            } else if (event.isRightClick()) {
                // Edit Pattern
                player.closeInventory();
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.messages.prompt_pattern"));
                ChatInputListener.requestInput(player, (input) -> {
                     // Basic validation could be added here checking against Registry.TRIM_PATTERN
                    item.setTrimPattern(input.toUpperCase());
                    item.save();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.trim_editor.messages.updated"));
                    this.open();
                });
            }
        }
    }
}
