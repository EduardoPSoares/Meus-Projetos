package me.ray.midgard.modules.races.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.gui.RaceMainMenuGui;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RaceCommand extends MidgardCommand {

    private final RacesModule module;

    public RaceCommand(RacesModule module) {
        super("race", null, false);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("races");
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.race_description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.race_usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                openGui((Player) sender);
            } else {
                sendHelp(sender);
            }
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui":
            case "select":
                if (sender instanceof Player) { openGui((Player) sender); }
                else { MessageUtils.send(sender, module.getMessage("command.only_players")); }
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "exp":
                handleExp(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "admin":
                if (sender instanceof Player && sender.hasPermission("midgard.admin.race")) {
                    new me.ray.midgard.modules.races.gui.RaceAdminGui((Player) sender).open();
                } else {
                    MessageUtils.send(sender, module.getMessage("command.no_permission"));
                }
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
    }

    private void openGui(Player player) {
        // Abrir menu principal unificado
        new RaceMainMenuGui(player).open();
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1 && sender.hasPermission("midgard.admin.race.info")) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            MessageUtils.send(sender, module.getMessage("command.player_not_found"));
            return;
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile == null) {
            MessageUtils.send(sender, module.getMessage("command.profile_error"));
            return;
        }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) {
            MessageUtils.send(sender, module.getMessage("command.no_race"));
            return;
        }

        Race race = module.getRaceManager().getRace(data.getRaceId());
        String raceName = race != null ? race.getDisplayName() : data.getRaceId();
        
        MessageUtils.send(sender, module.getMessage("command.race_info").replace("%race%", raceName));
        MessageUtils.send(sender, module.getMessage("command.level_info").replace("%level%", String.valueOf(data.getLevel())));
        MessageUtils.send(sender, module.getMessage("command.xp_info")
            .replace("%current%", String.format("%.1f", data.getExperience()))
            .replace("%required%", String.format("%.1f", module.getLevelManager().getRequiredExperience(data.getLevel()))));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("midgard.admin.race.set")) {
            MessageUtils.send(sender, module.getMessage("command.no_permission"));
            return;
        }

        if (args.length < 3) {
            MessageUtils.send(sender, module.getMessage("command.usage_set"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("command.player_not_found"));
            return;
        }

        String raceId = args[2];
        Race race = module.getRaceManager().getRace(raceId);
        if (race == null) {
            MessageUtils.send(sender, module.getMessage("command.race_not_found").replace("%race%", raceId));
            return;
        }

        module.getRaceManager().setRace(target, race, true);
        MessageUtils.send(sender, module.getMessage("command.set_success")
            .replace("%player%", target.getName())
            .replace("%race%", race.getDisplayName()));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("midgard.admin.race.reset")) {
            MessageUtils.send(sender, module.getMessage("command.no_permission"));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, module.getMessage("command.usage_reset"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("command.player_not_found"));
            return;
        }

        module.getRaceManager().resetRace(target);
        MessageUtils.send(sender, module.getMessage("command.reset_success").replace("%player%", target.getName()));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("midgard.admin.race.reload")) {
            MessageUtils.send(sender, module.getMessage("command.no_permission"));
            return;
        }
        module.reloadConfig();
        MessageUtils.send(sender, module.getMessage("command.reload_success"));
    }

    private void handleExp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("midgard.admin.race.exp")) {
            MessageUtils.send(sender, module.getMessage("command.no_permission"));
            return;
        }
        
        if (args.length < 3) {
            MessageUtils.send(sender, module.getMessage("command.usage_exp"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("command.player_not_found"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (Double.isNaN(amount) || Double.isInfinite(amount)) {
                MessageUtils.send(sender, module.getMessage("command.invalid_amount"));
                return;
            }
            module.getLevelManager().addExperience(target, amount);
            // Persistir imediatamente
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
            if (profile != null) {
                MidgardCore.getProfileManager().saveProfile(profile);
            }
            MessageUtils.send(sender, module.getMessage("command.exp_success")
                .replace("%amount%", String.valueOf(amount))
                .replace("%player%", target.getName()));
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, module.getMessage("command.invalid_amount"));
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, module.getMessage("help.header"));
        MessageUtils.send(sender, module.getMessage("help.select"));
        MessageUtils.send(sender, module.getMessage("help.info"));
        if (sender.hasPermission("midgard.admin.race")) {
            MessageUtils.send(sender, module.getMessage("help.set"));
            MessageUtils.send(sender, module.getMessage("help.reset"));
            MessageUtils.send(sender, module.getMessage("help.exp"));
            MessageUtils.send(sender, module.getMessage("help.reload"));
            MessageUtils.send(sender, module.getMessage("help.admin"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
         if (args.length == 1) {
             List<String> options = new ArrayList<>(List.of("select", "info", "help"));
             if (sender.hasPermission("midgard.admin.race")) {
                 options.addAll(List.of("set", "reset", "exp", "reload", "admin"));
             }
             return filter(args[0], options);
         }
         if (args.length == 2) {
             String sub = args[0].toLowerCase();
             switch (sub) {
                 case "set":
                 case "reset":
                 case "exp":
                 case "info":
                     return filter(args[1], Bukkit.getOnlinePlayers().stream()
                             .map(Player::getName).collect(Collectors.toList()));
             }
         }
         if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
             List<String> raceIds = module.getRaceManager().getRaces().stream()
                     .map(race -> race.getId()).collect(Collectors.toList());
             return filter(args[2], raceIds);
         }
         return Collections.emptyList();
    }
    
    private List<String> filter(String arg, List<String> options) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }
}
