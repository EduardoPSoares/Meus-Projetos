package de.maxhenkel.voicechat.indicator;

import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalIndicatorListener {

    private final Map<UUID, Long> lastIndicatorTime = new ConcurrentHashMap<>();
    private static final long INDICATOR_INTERVAL_MS = 2000;

    /**
     * Called when a global player is speaking. Shows an action bar indicator
     * to all players in the same world (throttled to avoid spam).
     */
    public void onGlobalSpeak(Player speaker) {
        long now = System.currentTimeMillis();
        Long lastTime = lastIndicatorTime.get(speaker.getUniqueId());
        if (lastTime != null && (now - lastTime) < INDICATOR_INTERVAL_MS) {
            return;
        }
        lastIndicatorTime.put(speaker.getUniqueId(), now);

        String message = Voicechat.MESSAGES.format(
                "global.indicator.action_bar",
                "&d[Global] &6&l%s&r&d esta falando (voz global)",
                speaker.getName()
        );

        for (Player player : speaker.getWorld().getPlayers()) {
            if (!player.getUniqueId().equals(speaker.getUniqueId())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            }
        }
    }
}
