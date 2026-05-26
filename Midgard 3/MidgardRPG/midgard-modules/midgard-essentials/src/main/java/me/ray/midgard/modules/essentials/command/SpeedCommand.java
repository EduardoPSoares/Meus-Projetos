package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpeedCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public SpeedCommand(EssentialsManager manager) {
        super("speed", "midgard.essentials.speed", true);
        this.manager = manager;
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.speed");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.speed");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length != 1) {
            MessageUtils.send(player, manager.getMessage("commands.speed.usage"));
            return;
        }

        try {
            float speed = Float.parseFloat(args[0]);
            if (Float.isNaN(speed) || Float.isInfinite(speed) || speed < 1 || speed > 10) {
                MessageUtils.send(player, manager.getMessage("commands.speed.invalid_range"));
                return;
            }

            // Converter 1-10 para 0.1-1.0 (aproximadamente)
            // Default walk speed is 0.2, fly speed is 0.1
            // Bukkit setSpeed accepts -1 to 1.
            
            float finalSpeed = speed / 10f; 

            if (player.isFlying()) {
                player.setFlySpeed(finalSpeed);
                String msg = manager.getMessage("commands.speed.fly_set")
                    .replace("%speed%", String.valueOf((int)speed));
                MessageUtils.send(player, msg);
            } else {
                player.setWalkSpeed(finalSpeed);
                String msg = manager.getMessage("commands.speed.walk_set")
                    .replace("%speed%", String.valueOf((int)speed));
                MessageUtils.send(player, msg);
            }

        } catch (NumberFormatException e) {
            MessageUtils.send(player, manager.getMessage("errors.invalid_number"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
