package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class StatRangeEditorGui extends BaseGui {

    private final StatRange currentValue;
    private final Consumer<String> onSave;
    private final ItemEditionGui parentGui;
    private final String statName;

    public StatRangeEditorGui(Player player, StatRange currentValue, Consumer<String> onSave, ItemEditionGui parentGui, String statName) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.editor.numeric.title").replace("%name%", statName));
        this.currentValue = currentValue != null ? currentValue : new StatRange(0, 0);
        this.onSave = onSave;
        this.parentGui = parentGui;
        this.statName = statName;
    }

    @Override
    public void initializeItems() {
        // Background filler removed per user request


        // Current Value Display (Center)
        inventory.setItem(13, new ItemBuilder(Material.PAPER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.current", "%value%", currentValue.toString()))
                .lore(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.click-type"))
                .build());

        // Decrement Buttons (Left)
        addModifyButton(12, -1, Material.RED_CONCRETE, "<red>-1");
        addModifyButton(11, -10, Material.RED_CONCRETE, "<red>-10");
        addModifyButton(10, -100, Material.RED_CONCRETE, "<red>-100");
        
        addModifyButton(3, -0.1, Material.RED_STAINED_GLASS_PANE, "<red>-0.1");
        addModifyButton(2, -0.5, Material.RED_STAINED_GLASS_PANE, "<red>-0.5");

        // Increment Buttons (Right)
        addModifyButton(14, 1, Material.LIME_CONCRETE, "<green>+1");
        addModifyButton(15, 10, Material.LIME_CONCRETE, "<green>+10");
        addModifyButton(16, 100, Material.LIME_CONCRETE, "<green>+100");
        
        addModifyButton(5, 0.1, Material.LIME_STAINED_GLASS_PANE, "<green>+0.1");
        addModifyButton(6, 0.5, Material.LIME_STAINED_GLASS_PANE, "<green>+0.5");

        // Reset Button (Bottom Center)
        inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.reset"))
                .build());

        // Back Button (Bottom Left)
        inventory.setItem(18, new ItemBuilder(Material.ARROW)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.cancel"))
                .build());
                
        // Type in Chat (Bottom Right)
        inventory.setItem(26, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.type-chat"))
                .lore(MidgardCore.getLanguageManager().getMessage("item.gui.editor.numeric.click-type"))
                .build());
    }

    private void addModifyButton(int slot, double change, Material mat, String name) {
        inventory.setItem(slot, new ItemBuilder(mat)
                .name(MessageUtils.parse(name))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18) { // Back
            parentGui.open();
            return;
        }

        if (slot == 26 || slot == 13) { // Type in Chat (or click on value)
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", statName));
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.editors.stat_range.examples")));
            
            ChatInputListener.requestInput(player, (input) -> {
                try {
                    // Validate parsing
                    StatRange.parse(input);
                    onSave.accept(input);
                    
                    parentGui.initializeItems(); // Refresh parent
                    parentGui.open();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.updated", "%s", statName));
                } catch (Exception e) {
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
                    parentGui.open();
                }
            });
            return;
        }

        if (slot == 22) { // Reset
            updateValue(0);
            return;
        }

        // Handle modifiers
        double change = 0;
        switch (slot) {
            case 10: change = -100; break;
            case 11: change = -10; break;
            case 12: change = -1; break;
            case 3: change = -0.1; break;
            case 2: change = -0.5; break;
            
            case 14: change = 1; break;
            case 15: change = 10; break;
            case 16: change = 100; break;
            case 5: change = 0.1; break;
            case 6: change = 0.5; break;
            default: return;
        }

        // Shift the entire range
        double newMin = currentValue.getMin() + change;
        double newMax = currentValue.getMax() + change;
        
        // Ensure not negative if it's supposed to be positive? 
        // Assuming stats can be anything for now, but usually damage > 0.
        // Let's just update.
        
        // Format to avoid long decimals
        newMin = Math.round(newMin * 100.0) / 100.0;
        newMax = Math.round(newMax * 100.0) / 100.0;
        
        StatRange newRange = new StatRange(newMin, newMax);
        onSave.accept(newRange.toString());
        
        // Re-open with new value
        new StatRangeEditorGui(player, newRange, onSave, parentGui, statName).open();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    private void updateValue(double newVal) {
        StatRange newRange = new StatRange(newVal, newVal);
        onSave.accept(newRange.toString());
        new StatRangeEditorGui(player, newRange, onSave, parentGui, statName).open();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }
}
