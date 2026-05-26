package me.ray.midgard.core;

import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gerencia o ciclo de vida dos módulos do RPG.
 */
public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, RPGModule> modules = new LinkedHashMap<>();

    /**
     * Construtor do ModuleManager.
     *
     * @param plugin Instância do plugin principal.
     */
    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra um novo módulo.
     *
     * @param module Módulo a ser registrado.
     */
    public void registerModule(RPGModule module) {
        if (module == null) {
            MidgardLogger.error("Tentativa de registrar um módulo nulo!");
            return;
        }
        if (modules.containsKey(module.getName())) {
            MidgardLogger.warn("Módulo já registrado: " + module.getName() + ". Ignorando duplicata.");
            return;
        }
        modules.put(module.getName(), module);
    }

    /**
     * Obtém todos os módulos registrados.
     */
    public Map<String, RPGModule> getModules() {
        return java.util.Collections.unmodifiableMap(modules);
    }

    /**
     * Habilita todos os módulos registrados.
     */
    public void enableAll() {
        modules.values().stream()
                .filter(java.util.Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority().getValue(), m1.getPriority().getValue()))
                .forEach(module -> {
                    long start = System.currentTimeMillis();
                    ConsoleUtils.printModuleHeader(module.getName());
                    MidgardLogger.startErrorCapture();
                    try {
                        me.ray.midgard.core.debug.MidgardProfiler.monitor("module_enable:" + module.getName(), () -> {
                            MidgardLogger.debug(DebugCategory.CORE, "Iniciando módulo %s (Prioridade: %s)", module.getName(), module.getPriority());
                            module.onEnable(plugin);
                            module.setEnabled(true);
                        });
                        java.util.List<String> errors = MidgardLogger.stopErrorCapture();
                        ConsoleUtils.printModuleStatus(module.getPriority().name(), "SUCCESS", System.currentTimeMillis() - start, errors);
                    } catch (Throwable e) {
                        java.util.List<String> errors = MidgardLogger.stopErrorCapture();
                        errors.add("Critical: " + e.getMessage());
                        ConsoleUtils.printModuleStatus(module.getPriority().name(), "FAILED", System.currentTimeMillis() - start, errors);
                        MidgardLogger.error("Falha crítica ao habilitar módulo: " + module.getName(), e);
                        module.setEnabled(false);
                    }
                });
    }

    /**
     * Recarrega a configuração de todos os módulos.
     */
    public void reloadAll() {
        modules.values().stream()
                .filter(java.util.Objects::nonNull)
                .filter(RPGModule::isEnabled)
                .sorted((m1, m2) -> Integer.compare(m2.getPriority().getValue(), m1.getPriority().getValue()))
                .forEach(module -> {
                    try {
                        module.reloadConfig();
                        MidgardLogger.info("Módulo recarregado: " + module.getName());
                    } catch (Exception e) {
                        MidgardLogger.error("Falha ao recarregar módulo: " + module.getName(), e);
                    }
                });
    }

    /**
     * Obtém um módulo pelo nome.
     */
    public RPGModule getModule(String name) {
        return modules.get(name);
    }

    /**
     * Desabilita todos os módulos registrados.
     */
    public void disableAll() {
        // Disable modules in reverse priority order (lowest priority first)
        modules.values().stream()
                .filter(java.util.Objects::nonNull)
                .filter(RPGModule::isEnabled)
                .sorted((m1, m2) -> Integer.compare(m1.getPriority().getValue(), m2.getPriority().getValue()))
                .forEach(module -> {
                    try {
                        MidgardLogger.info("Desabilitando módulo: " + module.getName());
                        module.onDisable(plugin);
                        module.setEnabled(false);
                    } catch (Throwable e) {
                        MidgardLogger.error("Falha ao desabilitar módulo: " + module.getName(), e);
                    }
                });
    }
}
