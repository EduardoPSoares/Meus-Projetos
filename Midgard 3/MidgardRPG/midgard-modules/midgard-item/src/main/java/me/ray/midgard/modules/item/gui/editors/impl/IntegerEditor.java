package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class IntegerEditor implements StatEditor {

    private final BiConsumer<MidgardItem, Integer> setter;
    private final String prompt;

    // Standalone mode fields
    private Player player;
    private Consumer<Integer> simpleCallback;
    private Runnable returnAction;

    public IntegerEditor(BiConsumer<MidgardItem, Integer> setter, Function<MidgardItem, Integer> getter, String prompt) {
        this.setter = setter;
        this.prompt = prompt;
    }

    public IntegerEditor(Player player, Consumer<Integer> callback, int initialValue, String prompt) {
        this.player = player;
        this.simpleCallback = callback;
        this.prompt = prompt;
        this.setter = null;
    }

    public IntegerEditor(Player player, Consumer<Integer> callback, String prompt) {
        this(player, callback, 0, prompt);
    }

    /**
     * Define a ação de retorno após o input do chat (para editores standalone).
     */
    public IntegerEditor onReturn(Runnable returnAction) {
        this.returnAction = returnAction;
        return this;
    }

    public void open() {
        if (player == null) {
            return;
        }
        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", prompt));

        ChatInputListener.requestInput(player, (input) -> {
            try {
                int val = Integer.parseInt(input);
                if (simpleCallback != null) {
                    simpleCallback.accept(val);
                }
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.updated", "%s", prompt));
                if (returnAction != null) {
                    returnAction.run();
                }
            } catch (NumberFormatException e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
                open(); // Re-prompt
            }
        });
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        if (clickType.isRightClick()) {
            setter.accept(item, 0);
            String msg = MidgardCore.getLanguageManager().getRawMessage("item.gui.editor.reset").replace("%s", prompt);
            player.sendMessage(MessageUtils.parse(msg));
            gui.initializeItems();
            return;
        }

        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", prompt));

        ChatInputListener.requestInput(player, (input) -> {
            try {
                int val = Integer.parseInt(input);
                setter.accept(item, val);
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.updated", "%s", prompt));
            } catch (NumberFormatException e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
            }
            gui.open();
        });
    }
}
