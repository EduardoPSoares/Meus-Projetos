package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends EssentialsBaseCommand {

    public SpawnCommand(EssentialsManager manager) {
        super(manager, "spawn", "midgard.essentials.spawn", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location spawn = manager.getSpawnManager().getSpawn();

        if (spawn == null) {
            MessageUtils.send(player, manager.getMessage("spawn.not_set"));
            return;
        }

        if (!TeleportUtils.isSafeLocation(spawn)) {
             MessageUtils.send(player, manager.getMessage("spawn.unsafe_location"));
             return;
        }

        TeleportUtils.teleport(player, spawn);
        MessageUtils.send(player, manager.getMessage("spawn.teleport"));
    }
}
