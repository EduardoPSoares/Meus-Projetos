package me.ray.midgard.modules.commands;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.commands.admin.CommandsAdminCommand;
import me.ray.midgard.modules.commands.registry.CentralCommandRegistry;
import me.ray.midgard.modules.commands.validator.CommandValidator;

/**
 * Módulo central de gerenciamento de comandos do MidgardRPG.
 * 
 * Este módulo é responsável por:
 * - Centralizar o registro de todos os comandos do projeto
 * - Validar se comandos registrados estão funcionando
 * - Sincronizar o sistema de help com comandos reais
 * - Fornecer ferramentas administrativas para gerenciar comandos
 * - Detectar comandos duplicados ou conflitantes
 * - Gerenciar permissões de comandos
 */
public class CommandsModule extends RPGModule {

    private static volatile CommandsModule instance;
    private CentralCommandRegistry registry;
    private CommandValidator validator;

    public CommandsModule() {
        super("MidgardCommands", ModulePriority.HIGH);
    }

    @Override
    public void onEnable() {
        try {
            // Inicializar o registro central de comandos
            this.registry = new CentralCommandRegistry(plugin);
            
            // Inicializar o validador de comandos
            this.validator = new CommandValidator(registry);
            
            // Registrar comandos administrativos do módulo
            registerAdminCommands();
            
            // Escanear e registrar comandos existentes
            scanExistingCommands();
            
            MidgardLogger.info("MidgardCommands habilitado! " + registry.getRegisteredCommandCount() + " comandos registrados.");
            instance = this;
        } catch (Exception e) {
            MidgardLogger.error("Erro fatal ao habilitar MidgardCommands", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (registry != null) {
                registry.unregisterAll();
            }
            instance = null;
        } catch (Exception e) {
            MidgardLogger.error("Erro ao desabilitar MidgardCommands", e);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        try {
            if (validator != null) {
                // Re-validar comandos após reload
                validator.validateAll();
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar configurações do Commands", e);
        }
    }

    private void registerAdminCommands() {
        // Registrar o comando /rpg admin commands
        if (MidgardCore.getAdminCommand() != null) {
            MidgardCore.getAdminCommand().registerSubcommand(new CommandsAdminCommand(this));
        }
    }

    private void scanExistingCommands() {
        // Escanear comandos já registrados no sistema
        registry.scanAndRegisterExisting();
    }

    /**
     * Obtém a instância do módulo.
     */
    public static CommandsModule getInstance() {
        return instance;
    }

    /**
     * Obtém o registro central de comandos.
     */
    public CentralCommandRegistry getRegistry() {
        return registry;
    }

    /**
     * Obtém o validador de comandos.
     */
    public CommandValidator getValidator() {
        return validator;
    }
}
