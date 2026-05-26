package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Chat-based numeric editor. Prompts the player to type a value in chat.
 * Supports both integer and double modes.
 */
public class NumericEditorGui {

    private final Player player;
    private final Number currentValue;
    private final Consumer<Number> onSave;
    private final ItemEditionGui parentGui;
    private final boolean isDouble;
    private final String statName;

    public NumericEditorGui(Player player, Number currentValue, Consumer<Number> onSave, ItemEditionGui parentGui, String statName) {
        this.player = player;
        this.currentValue = currentValue;
        this.onSave = onSave;
        this.parentGui = parentGui;
        this.statName = statName;
        this.isDouble = currentValue instanceof Double || currentValue instanceof Float;
    }

    /**
     * Opens the chat-based editor: closes any open inventory and prompts the player to type a value.
     */
    public void open() {
        player.closeInventory();

        String formattedValue = isDouble
                ? String.format("%.2f", currentValue.doubleValue())
                : String.valueOf(currentValue.intValue());

        player.sendMessage(MidgardCore.getLanguageManager().getMessage(
                "item.gui.editor.enter-prompt", "%s", statName));
        player.sendMessage(MidgardCore.getLanguageManager().getMessage(
                "item.gui.editor.numeric.current", "%value%", formattedValue));

        ChatInputListener.requestInput(player, (input) -> {
            try {
                Number newVal;
                if (isDouble) {
                    newVal = Double.parseDouble(input);
                } else {
                    newVal = Integer.parseInt(input);
                }
                onSave.accept(newVal);
                player.sendMessage(MidgardCore.getLanguageManager().getMessage(
                        "item.gui.editor.updated", "%s", statName));
            } catch (NumberFormatException e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage(
                        "item.gui.editor.invalid-number"));
            }

            if (parentGui != null) {
                parentGui.initializeItems();
                parentGui.open();
            }
        });
    }
}
