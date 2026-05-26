package me.ray.midgard.modules.item.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class NbtEditorGui extends BaseGui {

    private static final int MAX_NBT_LENGTH = 2000;
    private final MidgardItem item;
    private final ItemEditionGui parent;

    public NbtEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.nbt_editor.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        String currentNbt = item.getNbtTags();
        if (currentNbt == null) currentNbt = "{}";
        
        // Display Item
        ItemBuilder displayBuilder = new ItemBuilder(Material.NAME_TAG)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.item.name"));
        
        List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.nbt_editor.item.lore");
        List<Component> lore = new ArrayList<>();
        
        for (String line : loreLines) {
            if (line.contains("%json%")) {
                // Split long JSON into multiple lines
                String json = currentNbt;
                if (json.length() > 40) {
                    for (int i = 0; i < json.length(); i += 40) {
                        String part = json.substring(i, Math.min(i + 40, json.length()));
                        lore.add(MessageUtils.parse("<white>" + part));
                    }
                } else {
                    lore.add(MessageUtils.parse(line.replace("%json%", json)));
                }
            } else {
                lore.add(MessageUtils.parse(line));
            }
        }
        
        displayBuilder.lore(lore);
        inventory.setItem(13, displayBuilder.build());
        
        // Back Button
        inventory.setItem(18, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.buttons.back"))
                .build());
                
        // Clear Button
        inventory.setItem(26, new ItemBuilder(Material.LAVA_BUCKET)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.buttons.clear"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) {
            parent.open();
        } else if (slot == 26) {
            item.setNbtTags("{}");
            item.save();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.messages.updated"));
            initializeItems();
        } else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.messages.prompt"));
            
            ChatInputListener.requestInput(player, (json) -> {
                if (isValidJson(json)) {
                    item.setNbtTags(json);
                    item.save();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.messages.updated"));
                    this.open();
                } else {
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.nbt_editor.messages.invalid"));
                    this.open();
                }
            });
        }
    }
    
    private boolean isValidJson(String json) {
        if (json == null) {
            return false;
        }
        String trimmed = json.trim();
        if (trimmed.length() > MAX_NBT_LENGTH) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(trimmed);
            return element instanceof JsonObject;
        } catch (Exception e) {
            return false;
        }
    }
}
