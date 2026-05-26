package me.ray.midgard.core.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.permission.PermissionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Gerenciador unificado de comandos do MidgardRPG.
 * Responsável por organizar comandos em categorias e fornecer
 * sistema de permissões e tab completion contextual.
 * 
 * Estrutura de comandos:
 * - /rpg <comando> - Comandos de jogador
 * - /rpg admin <comando> - Comandos administrativos
 * - /rpg help - Ajuda geral
 * - /rpg help <comando> - Ajuda específica
 */
public class UnifiedCommandManager extends MidgardCommand {

    private final Map<String, MidgardCommand> playerCommands = new LinkedHashMap<>();
    private final Map<String, MidgardCommand> adminCommands = new LinkedHashMap<>();
    private final Map<String, MidgardCommand> moderatorCommands = new LinkedHashMap<>();
    
    private final Map<String, CommandCategory> commandCategories = new LinkedHashMap<>();

    public UnifiedCommandManager() {
        super("midgardrpg", null, false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MidgardCore.getHelpSystem().sendMainHelp(sender);
            return;
        }

        String commandName = args[0].toLowerCase();
        
        // Help command
        if ("help".equals(commandName) || "?".equals(commandName)) {
            if (args.length > 1) {
                MidgardCore.getHelpSystem().sendCommandHelp(sender, args[1]);
            } else {
                MidgardCore.getHelpSystem().sendMainHelp(sender);
            }
            return;
        }
        
        // Admin command
        if ("admin".equals(commandName) || "adm".equals(commandName) || "a".equals(commandName)) {
            if (!sender.hasPermission("midgard.admin")) {
                MessageUtils.send(sender, msg("core.commands.no_permission_admin"));
                return;
            }
            
            String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);
            handleAdminCommand(sender, adminArgs);
            return;
        }
        
        // Regular command lookup
        MidgardCommand targetCommand = findCommand(commandName);
        
        if (targetCommand == null) {
            String safe = MiniMessage.miniMessage().stripTags(commandName);
            MessageUtils.send(sender, msg("core.commands.unknown_command").replace("%command%", safe));
            MessageUtils.send(sender, msg("core.commands.use_help"));
            return;
        }

        // Check category permission
        CommandCategory category = commandCategories.get(commandName);
        if (!hasPermission(sender, category)) {
            MessageUtils.send(sender, msg("core.commands.no_permission"));
            return;
        }

        // Execute
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        targetCommand.execute(sender, newArgs);
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || "help".equals(args[0].toLowerCase()) || "?".equals(args[0])) {
            AdminCommandRegistry adminRegistry = MidgardCore.getAdminCommand();
            if (adminRegistry != null && !adminRegistry.getSubcommands().isEmpty()) {
                MidgardCore.getHelpSystem().sendAdminHelp(sender, adminRegistry.getSubcommands());
            } else {
                MidgardCore.getHelpSystem().sendAdminHelp(sender, adminCommands.values());
            }
            return;
        }
        
        // Delegate to AdminCommandRegistry
        AdminCommandRegistry adminRegistry = MidgardCore.getAdminCommand();
        if (adminRegistry instanceof MidgardCommand) {
            MidgardCommand adminCommand = (MidgardCommand) adminRegistry;
            adminCommand.execute(sender, args);
        } else {
            // Fallback
            String subcommandName = args[0].toLowerCase();
            MidgardCommand subcommand = adminCommands.get(subcommandName);

            if (subcommand == null) {
                String safe = MiniMessage.miniMessage().stripTags(subcommandName);
                MessageUtils.send(sender, msg("core.commands.unknown_subcommand").replace("%command%", safe));
                MessageUtils.send(sender, msg("core.commands.use_admin_help"));
                return;
            }

            String subPerm = subcommand.getPermission();
            if (subPerm != null && !sender.hasPermission(subPerm)) {
                MessageUtils.send(sender, msg("core.commands.no_permission"));
                return;
            }

            if (subcommand.isPlayerOnly() && !(sender instanceof org.bukkit.entity.Player)) {
                MessageUtils.send(sender, msg("core.commands.player_only"));
                return;
            }

            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subcommand.execute(sender, subArgs);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add help
            completions.add("help");
            
            // Player commands
            for (Map.Entry<String, MidgardCommand> entry : playerCommands.entrySet()) {
                if (entry.getKey().equals(entry.getValue().getName().toLowerCase())) {
                    completions.add(entry.getValue().getName());
                }
            }
            
            // Moderator commands
            if (PermissionManager.isModerator(sender)) {
                for (Map.Entry<String, MidgardCommand> entry : moderatorCommands.entrySet()) {
                    if (entry.getKey().equals(entry.getValue().getName().toLowerCase())) {
                        completions.add(entry.getValue().getName());
                    }
                }
            }
            
            // Admin command
            if (PermissionManager.isAdmin(sender)) {
                completions.add("admin");
            }
            
            // Filter by permission
            List<String> filtered = TabCompletionUtils.filterByPermission(sender, completions, 
                cmdName -> {
                    MidgardCommand cmd = findCommand(cmdName);
                    return cmd != null ? cmd.getPermission() : null;
                });
            
            return TabCompletionUtils.completePartial(args[0], filtered, suggestion -> true);
            
        } else if (args.length >= 2 && isAdminAlias(args[0])) {
            if (!sender.hasPermission("midgard.admin")) {
                return Collections.emptyList();
            }
            
            String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);
            return handleAdminTabComplete(sender, adminArgs);
        }
        
        // Tab complete for other commands
        String commandName = args[0].toLowerCase();
        MidgardCommand targetCommand = findCommand(commandName);
        
        if (targetCommand != null && hasPermission(sender, commandCategories.get(commandName))) {
            String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
            List<String> suggestions = targetCommand.onTabComplete(sender, command, label, newArgs);
            if (suggestions != null) {
                return TabCompletionUtils.filterByPermission(sender, suggestions, 
                    subCmd -> targetCommand.getSubCommandPermission(subCmd));
            }
        }
        
        return Collections.emptyList();
    }
    
    private boolean isAdminAlias(String name) {
        String lower = name.toLowerCase();
        return "admin".equals(lower) || "adm".equals(lower) || "a".equals(lower);
    }

    private List<String> handleAdminTabComplete(CommandSender sender, String[] args) {
        AdminCommandRegistry adminRegistry = MidgardCore.getAdminCommand();
        if (adminRegistry instanceof MidgardCommand) {
            MidgardCommand adminCommand = (MidgardCommand) adminRegistry;
            return adminCommand.onTabComplete(sender, null, "", args);
        }
        
        // Fallback
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            
            for (Map.Entry<String, MidgardCommand> entry : adminCommands.entrySet()) {
                MidgardCommand cmd = entry.getValue();
                if (seen.contains(cmd.getName())) {
                    continue;
                }
                seen.add(cmd.getName());
                
                String perm = cmd.getPermission();
                if (perm == null || sender.hasPermission(perm)) {
                    completions.add(cmd.getName());
                }
            }
            
            return TabCompletionUtils.completePartial(args[0], completions, suggestion -> true);
        }
        
        String subcommandName = args[0].toLowerCase();
        MidgardCommand subcommand = adminCommands.get(subcommandName);
        
        if (subcommand != null) {
            String subPerm = subcommand.getPermission();
            if (subPerm == null || sender.hasPermission(subPerm)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subcommand.onTabComplete(sender, null, "", subArgs);
            }
        }
        
        return Collections.emptyList();
    }

    /**
     * Registra um comando com categoria específica.
     */
    public void registerCommand(MidgardCommand command, CommandCategory category) {
        Map<String, MidgardCommand> targetMap = getCommandMap(category);
        
        targetMap.put(command.getName().toLowerCase(), command);
        commandCategories.put(command.getName().toLowerCase(), category);
        
        for (String alias : command.getAliases()) {
            targetMap.put(alias.toLowerCase(), command);
            commandCategories.put(alias.toLowerCase(), category);
        }
        
        // Register in HelpSystem
        MidgardCore.getHelpSystem().register(CommandInfo.builder(command.getName())
            .description(command.getDescription())
            .usage(command.getUsage())
            .permission(command.getPermission())
            .aliases(command.getAliases())
            .category(category)
            .build());
    }

    /**
     * Remove o registro de um comando.
     */
    public void unregisterCommand(String name) {
        CommandCategory category = commandCategories.get(name.toLowerCase());
        if (category != null) {
            getCommandMap(category).remove(name.toLowerCase());
            commandCategories.remove(name.toLowerCase());
            MidgardCore.getHelpSystem().unregister(name);
        }
    }

    /**
     * Registra comandos de jogador de módulos externos.
     */
    public void registerPlayerCommand(MidgardCommand command) {
        registerCommand(command, CommandCategory.PLAYER);
    }

    /**
     * Registra comandos administrativos de módulos externos.
     */
    public void registerAdminCommand(MidgardCommand command) {
        registerCommand(command, CommandCategory.ADMIN);
    }

    /**
     * Registra comandos de moderador de módulos externos.
     */
    public void registerModeratorCommand(MidgardCommand command) {
        registerCommand(command, CommandCategory.MODERATOR);
    }

    private MidgardCommand findCommand(String name) {
        String lower = name.toLowerCase();
        MidgardCommand cmd = playerCommands.get(lower);
        if (cmd != null) {
            return cmd;
        }
        
        cmd = moderatorCommands.get(lower);
        if (cmd != null) {
            return cmd;
        }
        
        return adminCommands.get(lower);
    }

    private Map<String, MidgardCommand> getCommandMap(CommandCategory category) {
        return switch (category) {
            case ADMIN -> adminCommands;
            case MODERATOR -> moderatorCommands;
            default -> playerCommands;
        };
    }

    private boolean hasPermission(CommandSender sender, CommandCategory category) {
        if (category == null) {
            return true;
        }
        
        return switch (category) {
            case ADMIN -> PermissionManager.isAdmin(sender);
            case MODERATOR -> PermissionManager.isModerator(sender);
            default -> true;
        };
    }

    private String msg(String key) {
        LanguageManager lm = MidgardCore.getLanguageManager();
        if (lm != null) {
            String val = lm.getRawMessage(key);
            if (val != null && !val.equals(key)) {
                return val;
            }
        }
        return key;
    }
}