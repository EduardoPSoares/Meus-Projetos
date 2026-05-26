package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends EssentialsBaseCommand {

    public SetSpawnCommand(EssentialsManager manager) {
        super(manager, "setspawn", "midgard.essentials.setspawn", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        manager.getSpawnManager().setSpawn(player.getLocation());
        MessageUtils.send(player, manager.getMessage("spawn.set"));
    }
}
