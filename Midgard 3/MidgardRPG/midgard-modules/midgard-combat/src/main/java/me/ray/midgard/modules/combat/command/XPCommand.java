package me.ray.midgard.modules.combat.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.combat.CombatModule;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class XPCommand extends MidgardCommand {

    public XPCommand() {
        super("xp", "midgard.admin.xp", false);
    }

    @Override
    public String getUsage() {
        return "/rpg admin xp";
    }

    @Override
    public String getDescription() {
        CombatModule module = CombatModule.getInstance();
        return module != null ? module.getMessage("command.xp_description") : "Gerencia XP de combate";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                if (!sender.hasPermission("midgard.admin.xp.add")) {
                    MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.no_permission"));
                    return;
                }
                handleAdd(sender, args);
                break;
            case "set":
                if (!sender.hasPermission("midgard.admin.xp.set")) {
                    MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.no_permission"));
                    return;
                }
                handleSet(sender, args);
                break;
            case "take":
            case "remove":
                if (!sender.hasPermission("midgard.admin.xp.take")) {
                    MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.no_permission"));
                    return;
                }
                handleTake(sender, args);
                break;
            default:
                sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, CombatModule.getInstance().getMessage("commands.xp.help_title"));
        for (String line : CombatModule.getInstance().getMessageList("commands.xp.help_lines")) {
            MessageUtils.send(sender, line);
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("commands.xp.usage_add"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.player_not_found").replace("%player%", args[1]));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        if (amount <= 0) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        CombatModule.getInstance().getLevelManager().addExperience(target, amount);
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile != null) {
            CombatData data = profile.getOrCreateData(CombatData.class);
            MidgardCore.getProfileManager().saveProfile(profile);
            String msg = CombatModule.getInstance().getMessage("commands.xp.success_add")
                    .replace("%player%", target.getName())
                    .replace("%amount%", String.valueOf((long) amount))
                    .replace("%level%", String.valueOf(data.getLevel()))
                    .replace("%xp%", String.format("%.0f", data.getExperience()));
            MessageUtils.send(sender, msg);
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("commands.xp.usage_take"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.player_not_found").replace("%player%", args[1]));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        if (amount <= 0) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile == null) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.profile_error"));
            return;
        }
        CombatData data = profile.getOrCreateData(CombatData.class);
        double newXp = Math.max(0, data.getExperience() - amount);
        data.setExperience(newXp);
        CombatModule.getInstance().getLevelManager().updateVanillaExperience(target, data.getLevel(), newXp);
        MidgardCore.getProfileManager().saveProfile(profile);
        String msg = CombatModule.getInstance().getMessage("commands.xp.success_take")
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf((long) amount))
                .replace("%level%", String.valueOf(data.getLevel()))
                .replace("%xp%", String.format("%.0f", data.getExperience()));
        MessageUtils.send(sender, msg);
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("commands.xp.usage_set"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.player_not_found").replace("%player%", args[1]));
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        if (value < 0) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.invalid_number"));
            return;
        }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile == null) {
            MessageUtils.send(sender, CombatModule.getInstance().getMessage("errors.profile_error"));
            return;
        }
        CombatData data = profile.getOrCreateData(CombatData.class);
        double required = CombatModule.getInstance().getLevelManager().getRequiredXp(data.getLevel());
        double clamped = Math.max(0, Math.min(value, Math.max(0, required - 1)));
        data.setExperience(clamped);
        CombatModule.getInstance().getLevelManager().updateVanillaExperience(target, data.getLevel(), clamped);
        MidgardCore.getProfileManager().saveProfile(profile);
        String msg = CombatModule.getInstance().getMessage("commands.xp.success_set")
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf((long) value))
                .replace("%level%", String.valueOf(data.getLevel()))
                .replace("%xp%", String.format("%.0f", data.getExperience()));
        MessageUtils.send(sender, msg);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("midgard.admin.xp")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return match(args[0], "add", "set", "take", "help");
        }
        if (args.length == 2) {
            return match(args[1], onlinePlayers());
        }
        if (args.length == 3) {
            return match(args[2], "50", "100", "250", "500", "1000");
        }
        return Collections.emptyList();
    }
}
