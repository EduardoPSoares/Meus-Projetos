package me.ray.midgard.modules.economy.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.economy.EconomyModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EconomyCommand extends MidgardCommand {

    private final EconomyModule module;

    public EconomyCommand(EconomyModule module) {
        super("mideco", "midgard.command.economy", true);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("coins");
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.economy_description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.economy_usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(module.getMessage("error.player_only"));
            return;
        }
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "pouch":
                // Give pouch for testing or manage?
                // Just let admins give it via /item give
                MessageUtils.send(player, module.getMessage("pouch.use_item_give"));
                break;
            case "give":
                if (!player.hasPermission("midgard.admin")) {
                    MessageUtils.send(player, module.getMessage("error.no_permission"));
                    return;
                }
                if (args.length < 2) {
                    MessageUtils.send(player, module.getMessage("admin.give.usage"));
                    return;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        MessageUtils.send(player, module.getMessage("admin.give.positive")); // Assuming this key exists or maps to positive_amount
                        return;
                    }
                    module.getCurrencyManager().givePhysicalCurrency(player, amount);
                    MessageUtils.send(player, module.getMessage("admin.give.success_target").replace("%amount%", String.valueOf(amount)));
                } catch (NumberFormatException e) {
                    MessageUtils.send(player, module.getMessage("error.invalid_number"));
                }
                break;
            default:
                sendHelp(player);
                break;
        }
    }
    
    private void sendHelp(Player player) {
        MessageUtils.send(player, module.getMessage("balance.header"));
        
        if (player.hasPermission("midgard.admin")) {
             MessageUtils.send(player, module.getMessage("admin.help.give").replace("/rpg admin econ", "/mideco"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("compact", "decompact"));
            if (sender.hasPermission("midgard.admin")) {
                suggestions.add("give");
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
