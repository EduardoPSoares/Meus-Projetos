package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public BackCommand(EssentialsManager manager) {
        super("back", "midgard.essentials.back", true);
        this.manager = manager;
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.back");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.back");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        if (!manager.getTeleportHistoryManager().hasLastLocation(player)) {
            MessageUtils.send(player, manager.getMessage("back.no_location"));
            return;
        }

        Location lastLocation = manager.getTeleportHistoryManager().getLastLocation(player);
        
        if (!TeleportUtils.isSafeLocation(lastLocation)) {
             MessageUtils.send(player, manager.getMessage("back.unsafe_location"));
             return;
        }
        
        TeleportUtils.teleport(player, lastLocation);
        MessageUtils.send(player, manager.getMessage("back.success"));
    }
}
