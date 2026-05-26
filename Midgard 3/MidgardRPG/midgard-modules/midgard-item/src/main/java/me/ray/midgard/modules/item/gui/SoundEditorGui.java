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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SoundEditorGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public SoundEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.sound_editor.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        List<String> sounds = item.getCustomSounds();
        
        for (int i = 0; i < sounds.size(); i++) {
            if (i >= 45) {
                break;
            }
            
            String soundData = sounds.get(i);
            // Format: SOUND:VOLUME:PITCH
            String[] parts = soundData.split(":");
            String soundName = parts.length > 0 ? parts[0] : "UNKNOWN";
            String volume = parts.length > 1 ? parts[1] : "1.0";
            String pitch = parts.length > 2 ? parts[2] : "1.0";
            
            ItemBuilder builder = new ItemBuilder(Material.JUKEBOX)
                    .name(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.item.name", "%s", String.valueOf(i + 1)));
            
            List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.sound_editor.item.lore");
            List<Component> lore = new ArrayList<>();
            
            for (String line : loreLines) {
                lore.add(MessageUtils.parse(line
                        .replace("%sound%", soundName)
                        .replace("%volume%", volume)
                        .replace("%pitch%", pitch)));
            }
            
            builder.lore(lore);
            inventory.setItem(i, builder.build());
        }
        
        // Add Button
        ItemStack addBtn = new ItemBuilder(Material.LIME_DYE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.buttons.add.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.sound_editor.buttons.add.lore").stream()
                        .map(MessageUtils::parse)
                        .toList())
                .build();
        inventory.setItem(49, addBtn);
        
        // Back Button
        ItemStack backBtn = new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.buttons.back"))
                .build();
        inventory.setItem(53, backBtn);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        List<String> sounds = new ArrayList<>(item.getCustomSounds());

        if (slot < 45 && slot < sounds.size()) {
            // Remove on click
            sounds.remove(slot);
            item.setCustomSounds(sounds);
            item.save();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.messages.removed"));
            initializeItems();
        } else if (slot == 49) {
            addSound();
        } else if (slot == 53) {
            parent.open();
        }
    }

    private void addSound() {
        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.messages.prompt_sound"));
        
        ChatInputListener.requestInput(player, (soundName) -> {
            try {
                // Validate sound if possible, or just accept string for custom resource pack sounds
                // Sound.valueOf(soundName.toUpperCase()); 
                
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.messages.prompt_volume"));
                ChatInputListener.requestInput(player, (volume) -> {
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.messages.prompt_pitch"));
                    ChatInputListener.requestInput(player, (pitch) -> {
                        List<String> sounds = new ArrayList<>(item.getCustomSounds());
                        sounds.add(soundName + ":" + volume + ":" + pitch);
                        item.setCustomSounds(sounds);
                        item.save();
                        
                        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.sound_editor.messages.added"));
                        this.open();
                    });
                });
            } catch (Exception e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.error_processing"));
                this.open();
            }
        });
    }
}
