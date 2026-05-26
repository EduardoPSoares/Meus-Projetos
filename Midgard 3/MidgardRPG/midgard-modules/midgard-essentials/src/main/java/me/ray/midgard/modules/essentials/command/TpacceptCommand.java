package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpacceptCommand extends EssentialsBaseCommand {

    public TpacceptCommand(EssentialsManager manager) {
        super(manager, "tpaccept", "midgard.essentials.tpaccept", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        manager.getTeleportRequestManager().acceptRequest(player);
    }
}
