package me.ray.midgard.modules.professions.blacksmith.forge.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.ForgeBlueprintItem;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Player-facing forge command: /rpg admin forge [subcommand]
 * Registered as a subcommand of the admin command.
 */
public class ForgeCommand extends MidgardCommand {

    private final ForgeManager forgeManager;

    public ForgeCommand(ForgeManager forgeManager) {
        super("forge", "midgard.forge.use", true);
        this.forgeManager = forgeManager;
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return msg("command.forge_description");
    }

    @Override
    public String getUsage() {
        return "/rpg admin forge [get|rotate|info|recipes|cancel|level]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "get" -> handleGetBlueprint(player, args);
            case "rotate" -> handleRotate(player);
            case "info" -> handleInfo(player);
            case "recipes" -> handleRecipes(player);
            case "cancel" -> handleCancel(player);
            case "level" -> handleLevel(player);
            default -> sendHelp(player);
        }
    }

    private void handleGetBlueprint(Player player, String[] args) {
        if (!player.hasPermission("midgard.forge.build")) {
            MessageUtils.send(player, msg("forge.no_permission_build"));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(player, msg("forge.usage_get"));
            listAvailableTemplates(player);
            return;
        }

        String templateName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        List<ForgeTemplate> templates = forgeManager.getCreationManager().getTemplates();

        ForgeTemplate found = null;
        for (ForgeTemplate t : templates) {
            if (t.isActive() && t.getName().equalsIgnoreCase(templateName)) {
                found = t;
                break;
            }
        }

        if (found == null) {
            MessageUtils.send(player, msg("forge.not_found").replace("%name%", templateName));
            listAvailableTemplates(player);
            return;
        }

        ForgeData data = forgeManager.getOrLoadData(player.getUniqueId());
        if (data.getLevel() < found.getRequiredLevel()) {
            MessageUtils.send(player, msg("forge.level_insufficient").replace("%level%", String.valueOf(found.getRequiredLevel())));
            return;
        }

        org.bukkit.inventory.ItemStack blueprint = ForgeBlueprintItem.create(found);
        player.getInventory().addItem(blueprint);
        MessageUtils.send(player, msg("forge.blueprint_received").replace("%name%", found.getName()));
        MessageUtils.send(player, msg("forge.place_instructions"));
    }

    private void listAvailableTemplates(Player player) {
        List<ForgeTemplate> templates = forgeManager.getCreationManager().getTemplates();
        List<ForgeTemplate> active = templates.stream().filter(ForgeTemplate::isActive).toList();
        if (active.isEmpty()) {
            MessageUtils.send(player, msg("forge.no_templates"));
            return;
        }
        MessageUtils.send(player, msg("forge.templates_header"));
        for (ForgeTemplate t : active) {
            MessageUtils.send(player, msg("forge.template_entry")
                    .replace("%name%", t.getName())
                    .replace("%tier%", t.getTier().getDisplayName())
                    .replace("%level%", String.valueOf(t.getRequiredLevel())));
        }
    }

    private void handleRotate(Player player) {
        if (!forgeManager.getGhostBlockManager().hasSession(player.getUniqueId())) {
            MessageUtils.send(player, msg("forge.no_build_session"));
            return;
        }
        forgeManager.getGhostBlockManager().rotateSession(player);
        MessageUtils.send(player, msg("forge.rotated"));
    }

    private void handleInfo(Player player) {
        ForgeData data = forgeManager.getOrLoadData(player.getUniqueId());
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forge.info.header"));
        MessageUtils.send(player, msg("forge.info.level").replace("%level%", String.valueOf(data.getLevel())));
        MessageUtils.send(player, msg("forge.info.xp")
                .replace("%current%", String.format("%.0f", data.getXp()))
                .replace("%max%", String.format("%.0f", data.getXpToNextLevel())));
        if (data.hasSpecialization()) {
            MessageUtils.send(player, msg("forge.info.specialization").replace("%spec%", data.getSpecialization()));
        }
        MessageUtils.send(player, msg("forge.info.items_forged").replace("%count%", String.valueOf(data.getTotalItemsForged())));
        MessageUtils.send(player, msg("forge.info.forges_count").replace("%count%", String.valueOf(forgeManager.getRegistry().countByOwner(player.getUniqueId()))));
        MessageUtils.send(player, "");
    }

    private void handleRecipes(Player player) {
        ForgeStructure nearestForge = forgeManager.getRegistry().findNearest(player.getLocation(), 3);
        if (nearestForge == null) {
            MessageUtils.send(player, msg("forge.no_forge_nearby"));
            return;
        }
        forgeManager.openRecipeBook(player, nearestForge);
    }

    private void handleCancel(Player player) {
        boolean cancelled = false;
        if (forgeManager.getSessionManager().hasActiveSession(player.getUniqueId())) {
            forgeManager.getSessionManager().cancelSession(player.getUniqueId());
            cancelled = true;
        }
        if (forgeManager.getGhostBlockManager().hasSession(player.getUniqueId())) {
            forgeManager.getGhostBlockManager().cancelSession(player.getUniqueId());
            cancelled = true;
        }
        MessageUtils.send(player, cancelled ? msg("forge.session_cancelled") : msg("forge.no_session"));
    }

    private void handleLevel(Player player) {
        ForgeData data = forgeManager.getOrLoadData(player.getUniqueId());
        int level = data.getLevel();
        double progress = data.getProgressPercent();

        int filled = (int) (progress / 5.0);
        String bar = "<green>" + "█".repeat(filled) + "<gray>" + "░".repeat(20 - filled);

        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forge.level_display.header").replace("%level%", String.valueOf(level)));
        MessageUtils.send(player, msg("forge.level_display.bar")
                .replace("%bar%", bar)
                .replace("%percent%", String.format("%.1f%%", progress)));
        MessageUtils.send(player, "");
    }

    private void sendHelp(Player player) {
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forge.help.header"));
        MessageUtils.send(player, msg("forge.help.get"));
        MessageUtils.send(player, msg("forge.help.rotate"));
        MessageUtils.send(player, msg("forge.help.info"));
        MessageUtils.send(player, msg("forge.help.level"));
        MessageUtils.send(player, msg("forge.help.recipes"));
        MessageUtils.send(player, msg("forge.help.cancel"));
        MessageUtils.send(player, "");
    }

    private String msg(String key) {
        return ProfessionsModule.getInstance().getMessage(key);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], "get", "rotate", "info", "recipes", "cancel", "level");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("get")) {
            String partial = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            List<String> names = forgeManager.getCreationManager().getTemplates().stream()
                    .filter(ForgeTemplate::isActive)
                    .map(ForgeTemplate::getName)
                    .toList();
            return match(partial, names);
        }
        return Collections.emptyList();
    }
}
