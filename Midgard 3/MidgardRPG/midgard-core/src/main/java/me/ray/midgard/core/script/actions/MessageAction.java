package me.ray.midgard.core.script.actions;

import me.ray.midgard.core.script.Action;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.entity.Player;

public class MessageAction implements Action {

    private final String message;

    public MessageAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(Player player) {
        MessageUtils.send(player, message);
    }
}
