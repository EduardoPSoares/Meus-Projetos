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
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BiomeSelectionGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public BiomeSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.biome_selection.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        List<String> biomes = item.getRequiredBiomes();
        
        for (int i = 0; i < biomes.size(); i++) {
            if (i >= 45) {
                break;
            }
            
            String biomeName = biomes.get(i);
            
            ItemBuilder builder = new ItemBuilder(Material.OAK_SAPLING)
                    .name(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.item.name", "%biome%", biomeName));
            
            List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.biome_selection.item.lore");
            List<Component> lore = new ArrayList<>();
            
            for (String line : loreLines) {
                lore.add(MessageUtils.parse(line));
            }
            
            builder.lore(lore);
            inventory.setItem(i, builder.build());
        }
        
        // Add Button
        ItemStack addBtn = new ItemBuilder(Material.LIME_DYE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.buttons.add.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.biome_selection.buttons.add.lore").stream()
                        .map(MessageUtils::parse)
                        .toList())
                .build();
        inventory.setItem(49, addBtn);
        
        // Back Button
        ItemStack backBtn = new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.buttons.back"))
                .build();
        inventory.setItem(53, backBtn);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        List<String> biomes = new ArrayList<>(item.getRequiredBiomes());

        if (slot < 45 && slot < biomes.size()) {
            // Remove on click
            String removed = biomes.remove(slot);
            item.setRequiredBiomes(biomes);
            item.save();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.messages.removed", "%biome%", removed));
            initializeItems();
        } else if (slot == 49) {
            addBiome();
        } else if (slot == 53) {
            parent.open();
        }
    }

    private void addBiome() {
        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.messages.prompt"));
        
        ChatInputListener.requestInput(player, (biomeName) -> {
            try {
                String biomeKey = biomeName.toLowerCase();
                if (getBiomeByKey(biomeKey) == null) {
                    throw new IllegalArgumentException("Biome inválido");
                }
                
                List<String> biomes = new ArrayList<>(item.getRequiredBiomes());
                if (!biomes.contains(biomeKey)) {
                    biomes.add(biomeKey);
                    item.setRequiredBiomes(biomes);
                    item.save();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.biome_selection.messages.added", "%biome%", biomeKey));
                }
                this.open();
            } catch (IllegalArgumentException e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-biome")); // Reusing error msg
                this.open();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private org.bukkit.block.Biome getBiomeByKey(String biomeKey) {
        return Registry.BIOME.get(NamespacedKey.minecraft(biomeKey));
    }
}
