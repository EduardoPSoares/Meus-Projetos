package me.ray.midgard.modules.item.command;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.gui.TypeBrowserGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemCommand extends MidgardCommand {

    private final ItemModule module;

    public ItemCommand(ItemModule module) {
        super("item", "midgard.admin.item", true);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("items");
    }

    @Override
    public String getDescription() {
        return "Gerencia itens customizados do RPG (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg admin item [give|reload|refine]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            new TypeBrowserGui(player, module).open();
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "give":
                handleGive(player, args);
                break;
            case "reload":
                handleReload(player);
                break;
            case "refine":
                if (!player.hasPermission("midgard.admin.item.refine")) {
                    MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
                    return;
                }
                new me.ray.midgard.modules.item.gui.UpgradeGui(player).open();
                break;
            default:
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.unknown"));
                break;
        }
    }

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("midgard.admin.item.give")) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.usage"));
            return;
        }

        String itemId = args[1];
        MidgardItem item = module.getItemManager().getMidgardItem(itemId);
        if (item == null) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.item-not-found", "%s", itemId));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.invalid-amount"));
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.invalid-amount"));
                return;
            }
        }

        org.bukkit.inventory.ItemStack itemStack = item.build();
        itemStack.setAmount(amount);
        player.getInventory().addItem(itemStack);
        
        String rawMessage = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.command.received");
        rawMessage = rawMessage.replace("%s", item.getId()).replace("%amount%", String.valueOf(amount));
        MessageUtils.send(player, MessageUtils.parse(rawMessage));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("midgard.admin.item.reload")) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
            return;
        }
        
        module.getCategoryManager().loadCategories();
        if (module.getTierManager() != null) {
            module.getTierManager().loadTiers();
        }
        if (module.getUpgradeManager() != null) {
            module.getUpgradeManager().loadConfig();
        }
        module.getItemManager().loadItems();
        if (module.getRecipeManager() != null) {
            module.getRecipeManager().reload();
        }
        MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.reload-success"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload", "refine"), new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[1], module.getItemManager().getItemIds(), new ArrayList<>());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[2], Arrays.asList("1", "16", "32", "64"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
