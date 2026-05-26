package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpdenyCommand extends EssentialsBaseCommand {

    public TpdenyCommand(EssentialsManager manager) {
        super(manager, "tpdeny", "midgard.essentials.tpdeny", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        manager.getTeleportRequestManager().denyRequest(player);
    }
}
