package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class VanishCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public VanishCommand(EssentialsManager manager) {
        super("vanish", "midgard.essentials.vanish", true);
        this.manager = manager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("v", "invis");
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.vanish");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.vanish");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        manager.getVanishManager().toggleVanish(player);
    }
}
