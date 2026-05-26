package me.ray.midgard.modules.professions.blacksmith.forge.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.admin.ForgeCreationManager;
import me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.ForgeBlueprintItem;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Admin forge command: /rpg admin forge [subcommand]
 * For managing forge structures, recipes, and player data.
 */
public class ForgeAdminCommand extends MidgardCommand {

    private final ForgeManager forgeManager;

    public ForgeAdminCommand(ForgeManager forgeManager) {
        super("forgeadmin", "midgard.admin.forge", true);
        this.forgeManager = forgeManager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("fa");
    }

    @Override
    public String getDescription() {
        return msg("command.forgeadmin_description");
    }

    @Override
    public String getUsage() {
        return "/rpg admin forgeadmin [list|remove|setlevel|reload|info]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            handlePanel(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "panel" -> handlePanel(player);
            case "list" -> handleList(player, args);
            case "remove" -> handleRemove(player, args);
            case "setlevel" -> handleSetLevel(player, args);
            case "reload" -> handleReload(player);
            case "info" -> handleInfo(player, args);
            case "stats" -> handleStats(player);
            case "create" -> handleCreate(player);
            case "cancelcreate" -> handleCancelCreate(player);
            case "give" -> handleGive(player, args);
            case "tp" -> handleTeleport(player, args);
            case "forges" -> handleForjas(player, args);
            case "toggle" -> handleToggle(player, args);
            case "rename" -> handleRename(player, args);
            default -> handlePanel(player);
        }
    }

    private void handlePanel(Player player) {
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager == null) {
            MessageUtils.send(player, msg("forgeadmin.system_unavailable"));
            return;
        }
        creationManager.openAdminPanel(player);
    }

    private void handleList(Player player, String[] args) {
        Collection<ForgeStructure> forges;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtils.send(player, msg("forgeadmin.player_not_found"));
                return;
            }
            forges = forgeManager.getRegistry().getByOwner(target.getUniqueId());
            MessageUtils.send(player, msg("forgeadmin.list.header_player").replace("%player%", target.getName()));
        } else {
            forges = forgeManager.getRegistry().getAll();
            MessageUtils.send(player, msg("forgeadmin.list.header_all").replace("%count%", String.valueOf(forges.size())));
        }

        for (ForgeStructure forge : forges) {
            String ownerName = Bukkit.getOfflinePlayer(forge.getOwnerUuid()).getName();
            MessageUtils.send(player, msg("forgeadmin.list.entry")
                    .replace("%id%", forge.getForgeId().toString().substring(0, 8))
                    .replace("%tier%", forge.getTier().getDisplayName())
                    .replace("%world%", forge.getWorldName())
                    .replace("%x%", String.valueOf(forge.getX()))
                    .replace("%y%", String.valueOf(forge.getY()))
                    .replace("%z%", String.valueOf(forge.getZ()))
                    .replace("%owner%", ownerName != null ? ownerName : "?"));
        }
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("forgeadmin.remove.usage"));
            return;
        }

        ForgeStructure found = findForgeByPartialId(args[1]);
        if (found == null) {
            MessageUtils.send(player, msg("forgeadmin.remove.not_found").replace("%id%", args[1]));
            return;
        }

        UUID forgeId = found.getForgeId();

        clearForgeBlocks(found);

        forgeManager.getFuelManager().clearFuel(forgeId);

        forgeManager.getRegistry().unregister(forgeId);
        forgeManager.evictSchematic(forgeId);
        if (forgeManager.getRepository() != null) {
            forgeManager.getRepository().deleteForge(forgeId);
        }
        MessageUtils.send(player, msg("forgeadmin.remove.success").replace("%id%", forgeId.toString()));
    }

    /**
     * Clears all physical blocks of a forge from the world.
     */
    private void clearForgeBlocks(ForgeStructure forge) {
        Location anchor = forge.getAnchorLocation();
        if (anchor == null || anchor.getWorld() == null) { return; }

        ForgeSchematic schematic = loadSchematicFor(forge);

        for (ForgeBlock block : schematic.getSolidBlocks()) {
            Location worldLoc = schematic.toWorldLocation(anchor, forge.getRotation(), block);
            worldLoc.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Loads the schematic for a forge (from cache or fallback to basic).
     */
    private ForgeSchematic loadSchematicFor(ForgeStructure forge) {
        return forgeManager.getSchematicFor(forge);
    }

    private void handleSetLevel(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(player, msg("forgeadmin.setlevel.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(player, msg("forgeadmin.player_not_found"));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 0 || level > 100) { throw new NumberFormatException(); }
        } catch (NumberFormatException e) {
            MessageUtils.send(player, msg("forgeadmin.setlevel.invalid_level"));
            return;
        }

        ForgeData data = forgeManager.getOrLoadData(target.getUniqueId());
        data.setLevel(level);
        if (forgeManager.getRepository() != null) {
            forgeManager.getRepository().savePlayerData(target.getUniqueId(), data);
        }

        MessageUtils.send(player, msg("forgeadmin.setlevel.success")
                .replace("%player%", target.getName())
                .replace("%level%", String.valueOf(level)));
        MessageUtils.send(target, msg("forgeadmin.setlevel.notify_player")
                .replace("%level%", String.valueOf(level)));
    }

    private void handleReload(Player player) {
        forgeManager.getRecipeManager().loadFromConfig(
                ProfessionsModule.getInstance().getConfig().getConfigurationSection("forge.recipes"));
        MessageUtils.send(player, msg("forgeadmin.reload.success")
                .replace("%count%", String.valueOf(forgeManager.getRecipeManager().size())));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("forgeadmin.info.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(player, msg("forgeadmin.player_not_found"));
            return;
        }

        ForgeData data = forgeManager.getOrLoadData(target.getUniqueId());
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forgeadmin.info.header").replace("%player%", target.getName()));
        MessageUtils.send(player, msg("forgeadmin.info.level").replace("%level%", String.valueOf(data.getLevel())));
        MessageUtils.send(player, msg("forgeadmin.info.xp")
                .replace("%current%", String.format("%.0f", data.getXp()))
                .replace("%max%", String.format("%.0f", data.getXpToNextLevel())));
        MessageUtils.send(player, msg("forgeadmin.info.specialization")
                .replace("%spec%", data.hasSpecialization() ? data.getSpecialization() : "Nenhuma"));
        MessageUtils.send(player, msg("forgeadmin.info.items_forged").replace("%count%", String.valueOf(data.getTotalItemsForged())));
        MessageUtils.send(player, msg("forgeadmin.info.legendary").replace("%count%", String.valueOf(data.getLegendaryItemsForged())));
        MessageUtils.send(player, msg("forgeadmin.info.perfect_strikes").replace("%count%", String.valueOf(data.getTotalPerfectStrikes())));
        MessageUtils.send(player, msg("forgeadmin.info.forges").replace("%count%", String.valueOf(data.getOwnedForgeIds().size())));
        MessageUtils.send(player, msg("forgeadmin.info.recipes").replace("%count%", String.valueOf(data.getUnlockedRecipes().size())));
        MessageUtils.send(player, "");
    }

    private void handleStats(Player player) {
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forgeadmin.stats.header"));
        MessageUtils.send(player, msg("forgeadmin.stats.registered").replace("%count%", String.valueOf(forgeManager.getRegistry().size())));
        MessageUtils.send(player, msg("forgeadmin.stats.active_sessions").replace("%count%", String.valueOf(forgeManager.getSessionManager().getActiveSessionCount())));
        MessageUtils.send(player, msg("forgeadmin.stats.recipes").replace("%count%", String.valueOf(forgeManager.getRecipeManager().size())));
        MessageUtils.send(player, msg("forgeadmin.stats.build_sessions").replace("%count%", String.valueOf(forgeManager.getGhostBlockManager().getActiveSessionCount())));
        MessageUtils.send(player, "");
    }

    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("forgeadmin.tp.usage"));
            return;
        }

        ForgeStructure forge = findForgeByPartialId(args[1]);
        if (forge == null) {
            MessageUtils.send(player, msg("forgeadmin.tp.not_found").replace("%id%", args[1]));
            return;
        }

        Location anchor = forge.getAnchorLocation();
        if (anchor == null || anchor.getWorld() == null) {
            MessageUtils.send(player, msg("forgeadmin.tp.world_not_loaded").replace("%world%", forge.getWorldName()));
            return;
        }

        Location tp = anchor.clone().add(0.5, 1, 0.5);
        tp.setYaw(player.getLocation().getYaw());
        tp.setPitch(player.getLocation().getPitch());
        player.teleport(tp);

        String ownerName = Bukkit.getOfflinePlayer(forge.getOwnerUuid()).getName();
        MessageUtils.send(player, msg("forgeadmin.tp.success")
                .replace("%owner%", ownerName != null ? ownerName : "?")
                .replace("%id%", forge.getForgeId().toString().substring(0, 8)));
    }

    private void handleForjas(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("forgeadmin.forges.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(player, msg("forgeadmin.player_not_found"));
            return;
        }

        List<ForgeStructure> forges = forgeManager.getRegistry().getByOwner(target.getUniqueId());
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forgeadmin.forges.header")
                .replace("%player%", target.getName())
                .replace("%count%", String.valueOf(forges.size())));

        if (forges.isEmpty()) {
            MessageUtils.send(player, msg("forgeadmin.forges.none"));
        } else {
            for (ForgeStructure forge : forges) {
                String shortId = forge.getForgeId().toString().substring(0, 8);
                String status = forge.isActive()
                        ? msg("forgeadmin.forges.status_active")
                        : msg("forgeadmin.forges.status_inactive");
                MessageUtils.send(player, "");
                MessageUtils.send(player, msg("forgeadmin.forges.entry_name")
                        .replace("%name%", forge.getName())
                        .replace("%id%", shortId));
                MessageUtils.send(player, msg("forgeadmin.forges.entry_tier")
                        .replace("%tier%", forge.getTier().getDisplayName())
                        .replace("%status%", status));
                MessageUtils.send(player, msg("forgeadmin.forges.entry_location")
                        .replace("%world%", forge.getWorldName())
                        .replace("%x%", String.valueOf(forge.getX()))
                        .replace("%y%", String.valueOf(forge.getY()))
                        .replace("%z%", String.valueOf(forge.getZ())));
                MessageUtils.send(player, msg("forgeadmin.forges.entry_stats")
                        .replace("%items%", String.valueOf(forge.getTotalItemsForged()))
                        .replace("%rotation%", forge.getRotation().name()));
                MessageUtils.send(player, msg("forgeadmin.forges.entry_commands")
                        .replace("%id%", shortId));
            }
        }
        MessageUtils.send(player, "");
    }

    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("forgeadmin.toggle.usage"));
            return;
        }

        ForgeStructure forge = findForgeByPartialId(args[1]);
        if (forge == null) {
            MessageUtils.send(player, msg("forgeadmin.toggle.not_found").replace("%id%", args[1]));
            return;
        }

        forge.setActive(!forge.isActive());
        if (forgeManager.getRepository() != null) {
            forgeManager.getRepository().saveForge(forge);
        }

        String status = forge.isActive()
                ? msg("forgeadmin.toggle.activated")
                : msg("forgeadmin.toggle.deactivated");
        String ownerName = Bukkit.getOfflinePlayer(forge.getOwnerUuid()).getName();
        MessageUtils.send(player, msg("forgeadmin.toggle.success")
                .replace("%owner%", ownerName != null ? ownerName : "?")
                .replace("%id%", forge.getForgeId().toString().substring(0, 8))
                .replace("%status%", status));
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(player, msg("forgeadmin.rename.usage"));
            return;
        }

        ForgeStructure forge = findForgeByPartialId(args[1]);
        if (forge == null) {
            MessageUtils.send(player, msg("forgeadmin.rename.not_found").replace("%id%", args[1]));
            return;
        }

        String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String oldName = forge.getName();
        forge.setName(newName);
        if (forgeManager.getRepository() != null) {
            forgeManager.getRepository().saveForge(forge);
        }

        MessageUtils.send(player, msg("forgeadmin.rename.success")
                .replace("%old%", oldName)
                .replace("%new%", newName));
    }

    /**
     * Finds a forge by partial UUID match.
     */
    private ForgeStructure findForgeByPartialId(String partialId) {
        String lower = partialId.toLowerCase();
        for (ForgeStructure forge : forgeManager.getRegistry().getAll()) {
            if (forge.getForgeId().toString().toLowerCase().startsWith(lower)) {
                return forge;
            }
        }
        return null;
    }

    private void sendHelp(Player player) {
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forgeadmin.help.header"));
        MessageUtils.send(player, msg("forgeadmin.help.list"));
        MessageUtils.send(player, msg("forgeadmin.help.forges"));
        MessageUtils.send(player, msg("forgeadmin.help.remove"));
        MessageUtils.send(player, msg("forgeadmin.help.tp"));
        MessageUtils.send(player, msg("forgeadmin.help.toggle"));
        MessageUtils.send(player, msg("forgeadmin.help.rename"));
        MessageUtils.send(player, msg("forgeadmin.help.setlevel"));
        MessageUtils.send(player, msg("forgeadmin.help.info"));
        MessageUtils.send(player, msg("forgeadmin.help.stats"));
        MessageUtils.send(player, msg("forgeadmin.help.reload"));
        MessageUtils.send(player, msg("forgeadmin.help.create"));
        MessageUtils.send(player, msg("forgeadmin.help.cancelcreate"));
        MessageUtils.send(player, msg("forgeadmin.help.give"));
        MessageUtils.send(player, "");
    }

    private void handleCreate(Player player) {
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager == null) {
            MessageUtils.send(player, msg("forgeadmin.creation_unavailable"));
            return;
        }
        creationManager.startSession(player);
    }

    private void handleCancelCreate(Player player) {
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager == null) {
            MessageUtils.send(player, msg("forgeadmin.creation_unavailable"));
            return;
        }
        creationManager.cancelSession(player);
    }

    private void handleGive(Player sender, String[] args) {
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager == null) {
            MessageUtils.send(sender, msg("forgeadmin.system_unavailable"));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, msg("forgeadmin.give.usage"));
            listTemplates(sender);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        String templateName;
        if (target != null && args.length >= 3) {
            templateName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            target = sender;
            templateName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        List<ForgeTemplate> templates = creationManager.getTemplates();
        ForgeTemplate found = null;
        for (ForgeTemplate t : templates) {
            if (t.isActive() && t.getName().equalsIgnoreCase(templateName)) {
                found = t;
                break;
            }
        }

        if (found == null) {
            MessageUtils.send(sender, msg("forgeadmin.give.not_found").replace("%name%", templateName));
            listTemplates(sender);
            return;
        }

        ItemStack blueprint = ForgeBlueprintItem.create(found);
        target.getInventory().addItem(blueprint);
        MessageUtils.send(sender, msg("forgeadmin.give.success")
                .replace("%name%", found.getName())
                .replace("%player%", target.getName()));
        if (!target.equals(sender)) {
            MessageUtils.send(target, msg("forgeadmin.give.notify_player").replace("%name%", found.getName()));
        }
    }

    private void listTemplates(Player player) {
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager == null) { return; }

        List<ForgeTemplate> templates = creationManager.getTemplates();
        List<ForgeTemplate> active = templates.stream().filter(ForgeTemplate::isActive).toList();

        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("forgeadmin.give.templates_header"));
        if (active.isEmpty()) {
            MessageUtils.send(player, msg("forgeadmin.give.no_templates"));
        } else {
            for (ForgeTemplate t : active) {
                MessageUtils.send(player, msg("forge.template_entry")
                        .replace("%name%", t.getName())
                        .replace("%tier%", t.getTier().getDisplayName())
                        .replace("%level%", String.valueOf(t.getRequiredLevel())));
            }
        }
        MessageUtils.send(player, "");
    }

    private String msg(String key) {
        return ProfessionsModule.getInstance().getMessage(key);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], "panel", "list", "remove", "setlevel", "reload", "info",
                    "stats", "create", "cancelcreate", "give", "tp", "forges", "toggle", "rename");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("list") || sub.equals("setlevel") || sub.equals("info") || sub.equals("forges")) {
                return match(args[1], onlinePlayers());
            }
            if (sub.equals("remove") || sub.equals("tp")
                    || sub.equals("toggle") || sub.equals("rename")) {
                return match(args[1], getForgeIdSuggestions());
            }
            if (sub.equals("give")) {
                List<String> suggestions = new ArrayList<>(onlinePlayers());
                suggestions.addAll(getForgeNameSuggestions());
                return match(args[1], suggestions);
            }
        } else if (args.length >= 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                String partial = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                return match(partial, getForgeNameSuggestions());
            }
        }
        return Collections.emptyList();
    }

    private List<String> getForgeIdSuggestions() {
        List<String> ids = new ArrayList<>();
        for (ForgeStructure forge : forgeManager.getRegistry().getAll()) {
            ids.add(forge.getForgeId().toString().substring(0, 8));
        }
        return ids;
    }

    private List<String> getForgeNameSuggestions() {
        List<String> names = new ArrayList<>();
        ForgeCreationManager creationManager = forgeManager.getCreationManager();
        if (creationManager != null) {
            creationManager.getTemplates().stream()
                    .filter(ForgeTemplate::isActive)
                    .map(ForgeTemplate::getName)
                    .forEach(names::add);
        }
        return names;
    }
}
