package me.ray.midgard.modules.commands.validator;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.CommandCategory;
import me.ray.midgard.core.command.HelpSystem;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.commands.CommandsModule;
import me.ray.midgard.modules.commands.registry.CentralCommandRegistry;
import me.ray.midgard.modules.commands.registry.CommandDescriptor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Validador de comandos que verifica a integridade e consistência
 * do sistema de comandos do MidgardRPG.
 * 
 * Funções:
 * - Detectar comandos no help que não existem
 * - Detectar comandos registrados mas sem documentação
 * - Detectar conflitos de nomes/aliases
 * - Verificar se permissões estão configuradas
 */
public class CommandValidator {

    private final CentralCommandRegistry registry;
    private final List<ValidationIssue> issues = new ArrayList<>();

    public CommandValidator(CentralCommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executa todas as validações e retorna os problemas encontrados.
     */
    public List<ValidationIssue> validateAll() {
        issues.clear();

        validateHelpConsistency();
        validateCommandExistence();
        validatePermissions();
        validateAliasConflicts();
        validateOrphanCommands();

        return new ArrayList<>(issues);
    }

    /**
     * Valida se o sistema de help está consistente com os comandos registrados.
     */
    public void validateHelpConsistency() {
        HelpSystem helpSystem = MidgardCore.getHelpSystem();
        if (helpSystem == null) {
            return;
        }

        // Para cada comando no registro, verificar se está no help
        for (CommandDescriptor descriptor : registry.getAllCommands()) {
            var info = helpSystem.getInfo(descriptor.getName());
            if (info == null && descriptor.getCategory() != CommandCategory.ADMIN) {
                issues.add(new ValidationIssue(
                        ValidationIssue.Severity.WARNING,
                        msg("validation.not_in_help").replace("%command%", descriptor.getName()),
                        descriptor.getModule(),
                        descriptor.getName()
                ));
            }
        }
    }

    /**
     * Valida se os comandos registrados realmente existem e funcionam.
     */
    public void validateCommandExistence() {
        CommandMap commandMap = registry.getBukkitCommandMap();
        if (commandMap == null) {
            return;
        }

        try {
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            // Verificar comandos standalone
            for (CommandDescriptor descriptor : registry.getAllCommands()) {
                if (descriptor.getSource() == CommandDescriptor.CommandSource.STANDALONE) {
                    if (!knownCommands.containsKey(descriptor.getName().toLowerCase())) {
                        issues.add(new ValidationIssue(
                                ValidationIssue.Severity.ERROR,
                                msg("validation.not_in_commandmap").replace("%command%", descriptor.getName()),
                                descriptor.getModule(),
                                descriptor.getName()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            MidgardLogger.debug("Erro ao validar existência de comandos: " + e.getMessage());
        }
    }

    /**
     * Valida se as permissões estão configuradas corretamente.
     */
    public void validatePermissions() {
        for (CommandDescriptor descriptor : registry.getAllCommands()) {
            // Comandos admin devem ter permissão
            if (descriptor.getCategory() == CommandCategory.ADMIN && descriptor.getPermission() == null) {
                issues.add(new ValidationIssue(
                        ValidationIssue.Severity.WARNING,
                        msg("validation.admin_no_permission").replace("%command%", descriptor.getName()),
                        descriptor.getModule(),
                        descriptor.getName()
                ));
            }

            // Verificar padrão de permissão
            String perm = descriptor.getPermission();
            if (perm != null && !perm.startsWith("midgard.")) {
                issues.add(new ValidationIssue(
                        ValidationIssue.Severity.INFO,
                        msg("validation.non_standard_permission").replace("%command%", descriptor.getName()).replace("%permission%", perm),
                        descriptor.getModule(),
                        descriptor.getName()
                ));
            }
        }
    }

    /**
     * Valida se há conflitos de aliases entre comandos.
     */
    public void validateAliasConflicts() {
        Map<String, List<CommandDescriptor>> aliasMap = new HashMap<>();

        for (CommandDescriptor descriptor : registry.getAllCommands()) {
            // Adicionar nome
            aliasMap.computeIfAbsent(descriptor.getName().toLowerCase(), k -> new ArrayList<>())
                    .add(descriptor);

            // Adicionar aliases
            for (String alias : descriptor.getAliases()) {
                aliasMap.computeIfAbsent(alias.toLowerCase(), k -> new ArrayList<>())
                        .add(descriptor);
            }
        }

        // Verificar conflitos
        for (Map.Entry<String, List<CommandDescriptor>> entry : aliasMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<String> modules = entry.getValue().stream()
                        .map(CommandDescriptor::getModule)
                        .distinct()
                        .toList();

                // Só é conflito se são módulos diferentes
                if (modules.size() > 1) {
                    issues.add(new ValidationIssue(
                            ValidationIssue.Severity.WARNING,
                            msg("validation.alias_conflict").replace("%alias%", entry.getKey()).replace("%modules%", String.join(", ", modules)),
                            "multiple",
                            entry.getKey()
                    ));
                }
            }
        }
    }

    /**
     * Valida se há comandos "órfãos" - registrados no Bukkit mas não no nosso sistema.
     */
    public void validateOrphanCommands() {
        CommandMap commandMap = registry.getBukkitCommandMap();
        if (commandMap == null) {
            return;
        }

        try {
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            String pluginPrefix = "midgardrpg:";

            for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
                String cmdName = entry.getKey();

                // Verificar apenas comandos do nosso plugin
                if (cmdName.startsWith(pluginPrefix)) {
                    String baseName = cmdName.substring(pluginPrefix.length());
                    if (!registry.isRegistered(baseName)) {
                        issues.add(new ValidationIssue(
                                ValidationIssue.Severity.INFO,
                                msg("validation.orphan_command").replace("%command%", baseName),
                                "orphan",
                                baseName
                        ));
                    }
                }
            }
        } catch (Exception e) {
            MidgardLogger.debug("Erro ao validar comandos órfãos: " + e.getMessage());
        }
    }

    /**
     * Obtém os problemas da última validação.
     */
    public List<ValidationIssue> getIssues() {
        return new ArrayList<>(issues);
    }

    /**
     * Obtém problemas filtrados por severidade.
     */
    public List<ValidationIssue> getIssuesBySeverity(ValidationIssue.Severity severity) {
        return issues.stream()
                .filter(i -> i.getSeverity() == severity)
                .toList();
    }

    /**
     * Obtém contagem de problemas por severidade.
     */
    public Map<ValidationIssue.Severity, Integer> getIssueCounts() {
        Map<ValidationIssue.Severity, Integer> counts = new EnumMap<>(ValidationIssue.Severity.class);
        for (ValidationIssue.Severity severity : ValidationIssue.Severity.values()) {
            counts.put(severity, 0);
        }
        for (ValidationIssue issue : issues) {
            counts.merge(issue.getSeverity(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Representa um problema encontrado durante a validação.
     */
    public static class ValidationIssue {

        public enum Severity {
            INFO,
            WARNING,
            ERROR
        }

        private final Severity severity;
        private final String message;
        private final String module;
        private final String command;

        public ValidationIssue(Severity severity, String message, String module, String command) {
            this.severity = severity;
            this.message = message;
            this.module = module;
            this.command = command;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public String getModule() {
            return module;
        }

        public String getCommand() {
            return command;
        }

        @Override
        public String toString() {
            return "[" + severity + "] " + message + " (módulo: " + module + ", comando: " + command + ")";
        }
    }

    private static String msg(String key) {
        CommandsModule mod = CommandsModule.getInstance();
        if (mod != null) {
            String m = mod.getMessage(key);
            if (m != null && !m.equals(key)) {
                return m;
            }
        }
        return key;
    }
}
