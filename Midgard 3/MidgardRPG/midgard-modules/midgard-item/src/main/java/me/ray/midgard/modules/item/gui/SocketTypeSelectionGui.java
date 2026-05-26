package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class SocketTypeSelectionGui extends BaseGui {

    private final MidgardItem item;
    private final SocketConfigurationGui parent;
    
    private final String[] COLORS = {"red", "blue", "green", "yellow", "orange", "purple", "light_blue", "white", "gray"};
    // Logical display names for colors (used in socket type strings)
    private final String[] COLOR_DISPLAY = {"red", "blue", "green", "yellow", "gold", "purple", "aqua", "white", "gray"};
    private final String[] TYPES = {"WEAPON", "ARMOR", "ACCESSORY", "ANY"};
    
    private String selectedColor = "red";

    public SocketTypeSelectionGui(Player player, MidgardItem item, SocketConfigurationGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_type_selection.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        // Color Selection Row (0-1)
        int slot = 0;
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            String displayColor = COLOR_DISPLAY[i];
            boolean isSelected = selectedColor.equals(displayColor);
            Material mat = Material.valueOf(color.toUpperCase() + "_STAINED_GLASS_PANE");
            if (isSelected) {
                mat = Material.valueOf(color.toUpperCase() + "_STAINED_GLASS");
            }
            
            String nameKey = isSelected ? "item.gui.socket_type_selection.color_selected" : "item.gui.socket_type_selection.color_unselected";
            
            inventory.setItem(slot++, new ItemBuilder(mat)
                    .name(MidgardCore.getLanguageManager().getMessage(nameKey, "%color%", displayColor))
                    .build());
        }
        
        // Type Selection Row (3)
        slot = 27;
        for (String type : TYPES) {
            inventory.setItem(slot++, new ItemBuilder(Material.PAPER)
                    .name(MidgardCore.getLanguageManager().getMessage("item.gui.socket_type_selection.add_button.name", "%socket%", selectedColor + type))
                    .lore(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_type_selection.add_button.lore")))
                    .build());
        }
        
        // Back
        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_type_selection.buttons.back"))).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 18) {
            // Color selection
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                // Find color by index since we know the order
                if (slot < COLOR_DISPLAY.length) {
                    selectedColor = COLOR_DISPLAY[slot];
                    initializeItems();
                }
            }
        } else if (slot >= 27 && slot < 27 + TYPES.length) {
            // Type selection (Add)
            int typeIndex = slot - 27;
            String type = TYPES[typeIndex];
            
            List<String> sockets = new ArrayList<>(item.getGemSockets() != null ? item.getGemSockets() : new ArrayList<>());
            // Format: colorTYPE (e.g. redWEAPON) — selectedColor uses display name (gold, aqua, etc.)
            sockets.add(selectedColor + type);
            item.setGemSockets(sockets);
            item.save();
            
            parent.initializeItems();
            parent.open();
        } else if (slot == 53) {
            parent.open();
        }
    }
}
