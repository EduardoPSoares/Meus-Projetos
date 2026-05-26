package me.ray.midgard.modules.commands.registry;

import me.ray.midgard.core.command.CommandCategory;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.modules.commands.CommandsModule;

/**
 * Utilitário para facilitar o registro de comandos no sistema centralizado.
 * 
 * Exemplo de uso em um módulo:
 * <pre>
 * CommandRegistrationHelper.registerPlayerCommand(myCommand, "mymodule");
 * </pre>
 */
public final class CommandRegistrationHelper {

    private CommandRegistrationHelper() {
        // Utility class
    }

    /**
     * Registra um comando de jogador no registro central.
     * 
     * @param command O comando a ser registrado
     * @param moduleName Nome do módulo que está registrando o comando
     */
    public static void registerPlayerCommand(MidgardCommand command, String moduleName) {
        registerCommand(command, moduleName, CommandCategory.PLAYER, CommandDescriptor.CommandSource.RPG_UNIFIED);
    }

    /**
     * Registra um comando administrativo no registro central.
     * 
     * @param command O comando a ser registrado
     * @param moduleName Nome do módulo que está registrando o comando
     */
    public static void registerAdminCommand(MidgardCommand command, String moduleName) {
        registerCommand(command, moduleName, CommandCategory.ADMIN, CommandDescriptor.CommandSource.RPG_ADMIN);
    }

    /**
     * Registra um comando de moderador no registro central.
     * 
     * @param command O comando a ser registrado
     * @param moduleName Nome do módulo que está registrando o comando
     */
    public static void registerModeratorCommand(MidgardCommand command, String moduleName) {
        registerCommand(command, moduleName, CommandCategory.MODERATOR, CommandDescriptor.CommandSource.RPG_UNIFIED);
    }

    /**
     * Registra um comando standalone no registro central.
     * 
     * @param command O comando a ser registrado
     * @param moduleName Nome do módulo que está registrando o comando
     */
    public static void registerStandaloneCommand(MidgardCommand command, String moduleName) {
        registerCommand(command, moduleName, CommandCategory.PLAYER, CommandDescriptor.CommandSource.STANDALONE);
    }

    /**
     * Remove o registro de um comando.
     * 
     * @param commandName Nome do comando a ser removido
     */
    public static void unregister(String commandName) {
        CommandsModule module = CommandsModule.getInstance();
        if (module != null && module.getRegistry() != null) {
            module.getRegistry().unregister(commandName);
        }
    }

    /**
     * Verifica se um comando está registrado.
     * 
     * @param commandName Nome do comando
     * @return true se o comando está registrado
     */
    public static boolean isRegistered(String commandName) {
        CommandsModule module = CommandsModule.getInstance();
        if (module != null && module.getRegistry() != null) {
            return module.getRegistry().isRegistered(commandName);
        }
        return false;
    }

    /**
     * Obtém um descritor de comando pelo nome.
     * 
     * @param commandName Nome do comando
     * @return O descritor do comando ou null se não encontrado
     */
    public static CommandDescriptor getDescriptor(String commandName) {
        CommandsModule module = CommandsModule.getInstance();
        if (module != null && module.getRegistry() != null) {
            return module.getRegistry().get(commandName);
        }
        return null;
    }

    private static void registerCommand(MidgardCommand command, String moduleName, 
                                         CommandCategory category, CommandDescriptor.CommandSource source) {
        CommandsModule module = CommandsModule.getInstance();
        if (module == null || module.getRegistry() == null) {
            return;
        }

        CommandDescriptor descriptor = CommandDescriptor.builder(command.getName())
                .description(command.getDescription())
                .usage(command.getUsage())
                .permission(command.getPermission())
                .aliases(command.getAliases())
                .category(category)
                .module(moduleName)
                .playerOnly(command.isPlayerOnly())
                .source(source)
                .build();

        module.getRegistry().register(descriptor);
    }
}
