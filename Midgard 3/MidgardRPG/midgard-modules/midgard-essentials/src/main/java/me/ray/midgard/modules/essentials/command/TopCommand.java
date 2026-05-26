package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TopCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public TopCommand(EssentialsManager manager) {
        super("top", "midgard.essentials.top", true);
        this.manager = manager;
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.top");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.top");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        int highestY = world.getHighestBlockYAt(loc);
        Location target = new Location(world, loc.getX(), highestY + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        
        TeleportUtils.teleport(player, target);
        MessageUtils.send(player, manager.getMessage("commands.top.success"));
    }
}
