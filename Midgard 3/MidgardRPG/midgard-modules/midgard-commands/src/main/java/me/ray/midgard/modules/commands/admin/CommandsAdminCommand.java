package me.ray.midgard.modules.commands.admin;

import me.ray.midgard.core.command.CommandCategory;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.commands.CommandsModule;
import me.ray.midgard.modules.commands.registry.CentralCommandRegistry;
import me.ray.midgard.modules.commands.registry.CommandDescriptor;
import me.ray.midgard.modules.commands.validator.CommandValidator;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comando administrativo para gerenciar comandos do MidgardRPG.
 * 
 * Uso:
 * - /rpg admin commands list [módulo] - Lista todos os comandos
 * - /rpg admin commands info <comando> - Informações detalhadas de um comando
 * - /rpg admin commands validate - Valida a consistência do sistema de comandos
 * - /rpg admin commands modules - Lista módulos com comandos registrados
 * - /rpg admin commands search <termo> - Busca comandos por nome/descrição
 */
public class CommandsAdminCommand extends MidgardCommand {

    private final CommandsModule module;

    public CommandsAdminCommand(CommandsModule module) {
        super("commands", "midgard.admin.commands", false);
        this.module = module;
    }

    private String msg(String key) {
        return module.getMessage(key);
    }

    @Override
    public String getDescription() {
        return msg("descriptions.commands");
    }

    @Override
    public String getUsage() {
        return msg("descriptions.commands_usage");
    }

    @Override
    public List<String> getAliases() {
        return List.of("cmds", "cmd");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (module.getRegistry() == null) {
            MessageUtils.send(sender, msg("system_not_initialized"));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subcommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subcommand) {
            case "list" -> executeList(sender, subArgs);
            case "info" -> executeInfo(sender, subArgs);
            case "validate" -> executeValidate(sender);
            case "modules" -> executeModules(sender);
            case "search" -> executeSearch(sender, subArgs);
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));
        MessageUtils.send(sender, msg("admin.help.subcommands_title"));
        MessageUtils.send(sender, msg("admin.help.list"));
        MessageUtils.send(sender, msg("admin.help.info"));
        MessageUtils.send(sender, msg("admin.help.validate"));
        MessageUtils.send(sender, msg("admin.help.modules"));
        MessageUtils.send(sender, msg("admin.help.search"));
        MessageUtils.send(sender, "");
    }

    private void executeList(CommandSender sender, String[] args) {
        CentralCommandRegistry registry = module.getRegistry();
        Collection<CommandDescriptor> commands;

        String moduleFilter = args.length > 0 ? args[0] : null;

        if (moduleFilter != null) {
            commands = registry.getByModule(moduleFilter);
            if (commands.isEmpty()) {
                MessageUtils.send(sender, msg("admin.list.empty_module").replace("%module%", moduleFilter));
                return;
            }
        } else {
            commands = registry.getAllCommands();
        }

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));

        // Agrupar por categoria
        Map<CommandCategory, List<CommandDescriptor>> byCategory = commands.stream()
                .collect(Collectors.groupingBy(CommandDescriptor::getCategory));

        for (CommandCategory category : CommandCategory.values()) {
            List<CommandDescriptor> catCommands = byCategory.get(category);
            if (catCommands == null || catCommands.isEmpty()) {
                continue;
            }

            String categoryMsg = switch (category) {
                case PLAYER -> msg("admin.list.category_player");
                case MODERATOR -> msg("admin.list.category_moderator");
                case ADMIN -> msg("admin.list.category_admin");
            };

            MessageUtils.send(sender, "");
            MessageUtils.send(sender, categoryMsg);

            for (CommandDescriptor desc : catCommands) {
                String status = desc.isEnabled() ? msg("admin.list.command_enabled") : msg("admin.list.command_disabled");
                MessageUtils.send(sender, msg("admin.list.format")
                        .replace("%status%", status)
                        .replace("%name%", desc.getName())
                        .replace("%module%", desc.getModule()));
            }
        }

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.list.total").replace("%count%", String.valueOf(commands.size())));
        MessageUtils.send(sender, "");
    }

    private void executeInfo(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtils.send(sender, msg("admin.info.usage"));
            return;
        }

        String cmdName = args[0];
        CommandDescriptor desc = module.getRegistry().get(cmdName);

        if (desc == null) {
            MessageUtils.send(sender, msg("admin.info.not_found").replace("%command%", cmdName));
            return;
        }

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));
        MessageUtils.send(sender, msg("admin.info.name").replace("%name%", desc.getName()));
        MessageUtils.send(sender, msg("admin.info.description").replace("%description%", desc.getDescription()));
        MessageUtils.send(sender, msg("admin.info.usage_info").replace("%usage%", desc.getUsage()));

        if (!desc.getAliases().isEmpty()) {
            MessageUtils.send(sender, msg("admin.info.aliases").replace("%aliases%", String.join(", ", desc.getAliases())));
        }

        MessageUtils.send(sender, msg("admin.info.permission").replace("%permission%", desc.getPermission() != null ? desc.getPermission() : msg("admin.info.permission_none")));
        MessageUtils.send(sender, msg("admin.info.category").replace("%category%", desc.getCategory().name()));
        MessageUtils.send(sender, msg("admin.info.module").replace("%module%", desc.getModule()));
        MessageUtils.send(sender, msg("admin.info.source").replace("%source%", desc.getSource().name()));
        MessageUtils.send(sender, msg("admin.info.player_only").replace("%value%", desc.isPlayerOnly() ? msg("admin.info.yes") : msg("admin.info.no")));
        MessageUtils.send(sender, msg("admin.info.enabled").replace("%value%", desc.isEnabled() ? msg("admin.info.yes") : msg("admin.info.no")));
        MessageUtils.send(sender, "");
    }

    private void executeValidate(CommandSender sender) {
        CommandValidator validator = module.getValidator();
        if (validator == null) {
            MessageUtils.send(sender, msg("system_not_initialized"));
            return;
        }

        MessageUtils.send(sender, msg("admin.validate.start"));
        List<CommandValidator.ValidationIssue> issues = validator.validateAll();

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));

        if (issues.isEmpty()) {
            MessageUtils.send(sender, msg("admin.validate.no_issues"));
        } else {
            Map<CommandValidator.ValidationIssue.Severity, Integer> counts = validator.getIssueCounts();

            MessageUtils.send(sender, msg("admin.validate.issues_title"));
            MessageUtils.send(sender, msg("admin.validate.errors").replace("%count%", String.valueOf(counts.get(CommandValidator.ValidationIssue.Severity.ERROR))));
            MessageUtils.send(sender, msg("admin.validate.warnings").replace("%count%", String.valueOf(counts.get(CommandValidator.ValidationIssue.Severity.WARNING))));
            MessageUtils.send(sender, msg("admin.validate.info").replace("%count%", String.valueOf(counts.get(CommandValidator.ValidationIssue.Severity.INFO))));
            MessageUtils.send(sender, "");

            for (CommandValidator.ValidationIssue issue : issues) {
                String icon = switch (issue.getSeverity()) {
                    case ERROR -> msg("admin.validate.icon_error");
                    case WARNING -> msg("admin.validate.icon_warning");
                    case INFO -> msg("admin.validate.icon_info");
                };
                MessageUtils.send(sender, msg("admin.validate.issue_format")
                        .replace("%icon%", icon)
                        .replace("%message%", issue.getMessage()));
            }
        }

        MessageUtils.send(sender, "");
    }

    private void executeModules(CommandSender sender) {
        CentralCommandRegistry registry = module.getRegistry();
        Set<String> modules = registry.getModulesWithCommands();

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));
        MessageUtils.send(sender, msg("admin.modules.title"));
        MessageUtils.send(sender, "");

        for (String mod : modules.stream().sorted().toList()) {
            int count = registry.getByModule(mod).size();
            MessageUtils.send(sender, msg("admin.modules.module_format")
                    .replace("%module%", mod)
                    .replace("%count%", String.valueOf(count)));
        }

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.modules.total").replace("%count%", String.valueOf(modules.size())));
        MessageUtils.send(sender, "");
    }

    private void executeSearch(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtils.send(sender, msg("admin.search.usage"));
            return;
        }

        String term = String.join(" ", args).toLowerCase();
        CentralCommandRegistry registry = module.getRegistry();

        List<CommandDescriptor> matches = registry.getAllCommands().stream()
                .filter(d -> d.getName().toLowerCase().contains(term) ||
                        (d.getDescription() != null && d.getDescription().toLowerCase().contains(term)) ||
                        d.getAliases().stream().anyMatch(a -> a.toLowerCase().contains(term)))
                .toList();

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("admin.help.header"));
        MessageUtils.send(sender, msg("admin.help.sep"));

        if (matches.isEmpty()) {
            MessageUtils.send(sender, msg("admin.search.no_results").replace("%term%", term));
        } else {
            MessageUtils.send(sender, msg("admin.search.results_title").replace("%term%", term));
            MessageUtils.send(sender, "");

            for (CommandDescriptor desc : matches) {
                MessageUtils.send(sender, msg("admin.search.result_format")
                        .replace("%name%", desc.getName())
                        .replace("%module%", desc.getModule())
                        .replace("%description%", desc.getDescription()));
            }
        }

        MessageUtils.send(sender, "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], "list", "info", "validate", "modules", "search");
        }

        if (args.length == 2 && module.getRegistry() != null) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("list")) {
                return match(args[1], module.getRegistry().getModulesWithCommands());
            }

            if (subcommand.equals("info")) {
                return match(args[1], module.getRegistry().getAllCommands().stream()
                        .map(CommandDescriptor::getName)
                        .toList());
            }
        }

        return Collections.emptyList();
    }
}
