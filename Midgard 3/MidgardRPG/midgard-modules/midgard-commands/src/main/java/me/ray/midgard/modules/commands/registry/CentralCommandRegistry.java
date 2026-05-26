package me.ray.midgard.modules.commands.registry;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.AdminCommandRegistry;
import me.ray.midgard.core.command.CommandCategory;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.command.UnifiedCommandManager;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro central de todos os comandos do MidgardRPG.
 * 
 * Este registro mantém controle de:
 * - Todos os comandos registrados (RPG, admin, standalone)
 * - Metadados e descrições de cada comando
 * - Estado de habilitação de cada comando
 * - Conflitos e duplicações de comandos
 */
public class CentralCommandRegistry {

    private final JavaPlugin plugin;
    private final Map<String, CommandDescriptor> commands = new ConcurrentHashMap<>();
    private final Map<String, List<String>> moduleCommands = new ConcurrentHashMap<>();
    private CommandMap bukkitCommandMap;

    public CentralCommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        initBukkitCommandMap();
    }

    private void initBukkitCommandMap() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            bukkitCommandMap = (CommandMap) f.get(Bukkit.getServer());
        } catch (Exception e) {
            MidgardLogger.error("Falha ao obter CommandMap via reflexão", e);
        }
    }

    /**
     * Registra um comando no registro central.
     */
    public void register(CommandDescriptor descriptor) {
        if (descriptor == null || descriptor.getName() == null) {
            return;
        }

        String key = descriptor.getName().toLowerCase();
        
        // Verificar duplicação
        if (commands.containsKey(key)) {
            CommandDescriptor existing = commands.get(key);
            MidgardLogger.warn("Comando duplicado detectado: '" + key + "' " +
                    "(existente: " + existing.getModule() + ", novo: " + descriptor.getModule() + ")");
        }

        commands.put(key, descriptor);

        // Registrar aliases
        for (String alias : descriptor.getAliases()) {
            commands.put(alias.toLowerCase(), descriptor);
        }

        // Mapear comando ao módulo
        moduleCommands.computeIfAbsent(descriptor.getModule().toLowerCase(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(descriptor.getName());
    }

    /**
     * Remove o registro de um comando.
     */
    public void unregister(String name) {
        if (name == null) {
            return;
        }

        CommandDescriptor descriptor = commands.remove(name.toLowerCase());
        if (descriptor != null) {
            for (String alias : descriptor.getAliases()) {
                commands.remove(alias.toLowerCase());
            }

            List<String> moduleCmds = moduleCommands.get(descriptor.getModule().toLowerCase());
            if (moduleCmds != null) {
                moduleCmds.remove(descriptor.getName());
            }
        }
    }

    /**
     * Remove todos os comandos registrados.
     */
    public void unregisterAll() {
        commands.clear();
        moduleCommands.clear();
    }

    /**
     * Obtém um descritor de comando pelo nome.
     */
    public CommandDescriptor get(String name) {
        return name != null ? commands.get(name.toLowerCase()) : null;
    }

    /**
     * Verifica se um comando está registrado.
     */
    public boolean isRegistered(String name) {
        return name != null && commands.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todos os comandos registrados (sem duplicatas de aliases).
     */
    public Collection<CommandDescriptor> getAllCommands() {
        return commands.values().stream()
                .collect(Collectors.toMap(
                        d -> d.getName().toLowerCase(),
                        d -> d,
                        (existing, replacement) -> existing
                ))
                .values();
    }

    /**
     * Obtém comandos por categoria.
     */
    public List<CommandDescriptor> getByCategory(CommandCategory category) {
        return getAllCommands().stream()
                .filter(d -> d.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Obtém comandos por módulo.
     */
    public List<CommandDescriptor> getByModule(String module) {
        return getAllCommands().stream()
                .filter(d -> d.getModule().equalsIgnoreCase(module))
                .collect(Collectors.toList());
    }

    /**
     * Obtém a contagem total de comandos registrados.
     */
    public int getRegisteredCommandCount() {
        return getAllCommands().size();
    }

    /**
     * Lista todos os módulos que têm comandos registrados.
     */
    public Set<String> getModulesWithCommands() {
        return new HashSet<>(moduleCommands.keySet());
    }

    /**
     * Escaneia e registra comandos existentes no sistema.
     */
    public void scanAndRegisterExisting() {
        // 1. Escanear comandos do UnifiedCommandManager
        scanUnifiedCommands();

        // 2. Escanear comandos admin
        scanAdminCommands();

        // 3. Escanear comandos Bukkit do plugin
        scanBukkitCommands();
    }

    private void scanUnifiedCommands() {
        try {
            UnifiedCommandManager cmdManager = MidgardCore.getCommandManager();
            if (cmdManager == null) {
                return;
            }

            // Usar reflexão para acessar os mapas de comandos internos
            Field playerField = UnifiedCommandManager.class.getDeclaredField("playerCommands");
            Field adminField = UnifiedCommandManager.class.getDeclaredField("adminCommands");
            Field modField = UnifiedCommandManager.class.getDeclaredField("moderatorCommands");

            playerField.setAccessible(true);
            adminField.setAccessible(true);
            modField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, MidgardCommand> playerCmds = (Map<String, MidgardCommand>) playerField.get(cmdManager);
            @SuppressWarnings("unchecked")
            Map<String, MidgardCommand> adminCmds = (Map<String, MidgardCommand>) adminField.get(cmdManager);
            @SuppressWarnings("unchecked")
            Map<String, MidgardCommand> modCmds = (Map<String, MidgardCommand>) modField.get(cmdManager);

            registerFromMap(playerCmds, CommandCategory.PLAYER, CommandDescriptor.CommandSource.RPG_UNIFIED);
            registerFromMap(adminCmds, CommandCategory.ADMIN, CommandDescriptor.CommandSource.RPG_UNIFIED);
            registerFromMap(modCmds, CommandCategory.MODERATOR, CommandDescriptor.CommandSource.RPG_UNIFIED);

        } catch (Exception e) {
            MidgardLogger.debug("Não foi possível escanear UnifiedCommandManager: " + e.getMessage());
        }
    }

    private void scanAdminCommands() {
        try {
            AdminCommandRegistry adminCmd = MidgardCore.getAdminCommand();
            if (adminCmd == null) {
                return;
            }

            Collection<MidgardCommand> subcommands = adminCmd.getSubcommands();
            for (MidgardCommand cmd : subcommands) {
                CommandDescriptor descriptor = CommandDescriptor.builder(cmd.getName())
                        .description(cmd.getDescription())
                        .usage("/rpg admin " + cmd.getName())
                        .permission(cmd.getPermission())
                        .aliases(cmd.getAliases())
                        .category(CommandCategory.ADMIN)
                        .module(detectModule(cmd))
                        .playerOnly(cmd.isPlayerOnly())
                        .source(CommandDescriptor.CommandSource.RPG_ADMIN)
                        .build();

                register(descriptor);
            }
        } catch (Exception e) {
            MidgardLogger.debug("Não foi possível escanear AdminCommand: " + e.getMessage());
        }
    }

    private void scanBukkitCommands() {
        if (bukkitCommandMap == null) {
            return;
        }

        try {
            Field knownCommandsField = bukkitCommandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(bukkitCommandMap);

            for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
                Command cmd = entry.getValue();

                // Apenas comandos do nosso plugin
                if (cmd instanceof PluginCommand pluginCmd) {
                    Plugin owner = pluginCmd.getPlugin();
                    if (owner != null && owner.getName().equalsIgnoreCase(plugin.getName())) {
                        // Verificar se já não foi registrado
                        if (!isRegistered(cmd.getName())) {
                            CommandDescriptor descriptor = CommandDescriptor.builder(cmd.getName())
                                    .description(cmd.getDescription())
                                    .usage(cmd.getUsage())
                                    .permission(cmd.getPermission())
                                    .aliases(cmd.getAliases())
                                    .category(CommandCategory.PLAYER)
                                    .module("standalone")
                                    .source(CommandDescriptor.CommandSource.STANDALONE)
                                    .build();

                            register(descriptor);
                        }
                    }
                }
            }
        } catch (Exception e) {
            MidgardLogger.debug("Não foi possível escanear comandos Bukkit: " + e.getMessage());
        }
    }

    private void registerFromMap(Map<String, MidgardCommand> map, CommandCategory category, CommandDescriptor.CommandSource source) {
        if (map == null) {
            return;
        }

        Set<String> processed = new HashSet<>();
        for (MidgardCommand cmd : map.values()) {
            if (processed.contains(cmd.getName().toLowerCase())) {
                continue;
            }
            processed.add(cmd.getName().toLowerCase());

            CommandDescriptor descriptor = CommandDescriptor.builder(cmd.getName())
                    .description(cmd.getDescription())
                    .usage(cmd.getUsage())
                    .permission(cmd.getPermission())
                    .aliases(cmd.getAliases())
                    .category(category)
                    .module(detectModule(cmd))
                    .playerOnly(cmd.isPlayerOnly())
                    .source(source)
                    .build();

            register(descriptor);
        }
    }

    private String detectModule(MidgardCommand cmd) {
        String className = cmd.getClass().getName();

        if (className.contains(".essentials.")) {
            return "essentials";
        }
        if (className.contains(".combat.")) {
            return "combat";
        }
        if (className.contains(".spells.")) {
            return "spells";
        }
        if (className.contains(".classes.")) {
            return "classes";
        }
        if (className.contains(".races.")) {
            return "races";
        }
        if (className.contains(".item.")) {
            return "item";
        }
        if (className.contains(".economy.")) {
            return "economy";
        }
        if (className.contains(".character.")) {
            return "character";
        }
        if (className.contains(".performance.")) {
            return "performance";
        }
        if (className.contains(".security.")) {
            return "security";
        }
        if (className.contains(".mythicmobs.")) {
            return "mythicmobs";
        }
        if (className.contains(".loader.")) {
            return "loader";
        }
        if (className.contains(".core.")) {
            return "core";
        }
        if (className.contains(".commands.")) {
            return "commands";
        }

        return "unknown";
    }

    /**
     * Obtém o CommandMap do Bukkit.
     */
    public CommandMap getBukkitCommandMap() {
        return bukkitCommandMap;
    }
}
