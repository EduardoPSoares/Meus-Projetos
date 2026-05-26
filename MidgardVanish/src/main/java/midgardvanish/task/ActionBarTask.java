package midgardvanish.task;

import midgardvanish.manager.VanishManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ActionBarTask extends BukkitRunnable {

    private final VanishManager vanishManager;

    public ActionBarTask(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public void run() {
        for (UUID uuid : vanishManager.getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished == null || !vanished.isOnline()) continue;

            // ActionBar for the vanished player
            vanished.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c§lᴠᴀɴɪsʜ ᴀᴛɪᴠᴀᴅᴏ")
            );

            // Particles visible only to staff with midgardvanish.see in same world
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(vanished)) continue;
                if (!vanishManager.canSee(viewer, vanished)) continue;
                if (!viewer.getWorld().equals(vanished.getWorld())) continue;

                viewer.spawnParticle(
                        Particle.DUST,
                        vanished.getLocation().add(0, 2.5, 0),
                        5, 0.3, 0.1, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 50, 50), 0.8f)
                );
            }
        }

        // Refresh glow for vanished players
        vanishManager.refreshGlow();
    }
}
