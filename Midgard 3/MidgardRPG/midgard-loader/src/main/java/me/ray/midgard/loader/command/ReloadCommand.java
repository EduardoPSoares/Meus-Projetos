package me.ray.midgard.loader.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeRegistry;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comando de reload completo do MidgardRPG.
 * Recarrega configurações, atributos, mensagens e todos os módulos.
 */
public class ReloadCommand extends MidgardCommand {

    private final JavaPlugin plugin;
    
    // Subcomandos válidos
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "all", "config", "messages", "attributes", "modules"
    );

    public ReloadCommand(JavaPlugin plugin) {
        super("reload", "midgard.admin.reload", false);
        this.plugin = plugin;
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("rl");
    }

    @Override
    public String getDescription() {
        return "Recarrega configurações e módulos do plugin";
    }

    @Override
    public String getUsage() {
        return "/rpg admin reload [all|config|messages|attributes|modules]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Reload total
            reloadAll(sender);
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "all" -> reloadAll(sender);
            case "config" -> reloadConfig(sender);
            case "messages" -> reloadMessages(sender);
            case "attributes" -> reloadAttributes(sender);
            case "modules" -> {
                if (args.length > 1) {
                    reloadSpecificModule(sender, args[1]);
                } else {
                    reloadAllModules(sender);
                }
            }
            default -> {
                // Talvez seja um nome de módulo
                if (MidgardCore.getModuleManager() != null && 
                    MidgardCore.getModuleManager().getModule(subcommand) != null) {
                    reloadSpecificModule(sender, subcommand);
                } else {
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.unknown_subcommand", "%sub%", subcommand));
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.usage"));
                }
            }
        }
    }

    /**
     * Reload completo de todo o plugin
     */
    private void reloadAll(CommandSender sender) {
        long startTime = System.currentTimeMillis();
        int reloadedCount = 0;
        List<String> errors = new ArrayList<>();
        
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.start_header"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.sep"));
        
        // 1. Reload Config Principal
        try {
            plugin.reloadConfig();
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.config_ok"));
            reloadedCount++;
        } catch (Exception e) {
            errors.add("Config: " + e.getMessage());
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.config_error"));
            MidgardLogger.error("Erro ao recarregar config principal", e);
        }
        
        // 2. Reload Atributos
        try {
            int attrCount = reloadAttributesInternal();
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.attributes_ok", "%count%", String.valueOf(attrCount)));
            reloadedCount++;
        } catch (Exception e) {
            errors.add("Attributes: " + e.getMessage());
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.attributes_error"));
            MidgardLogger.error("Erro ao recarregar atributos", e);
        }
        
        // 3. Reload Mensagens/Idioma
        try {
            if (MidgardCore.getLanguageManager() != null) {
                String locale = plugin.getConfig().getString("settings.locale", "pt-br");
                MidgardCore.getLanguageManager().load(locale);
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.messages_ok", "%locale%", locale));
                reloadedCount++;
            }
        } catch (Exception e) {
            errors.add("Messages: " + e.getMessage());
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.messages_error"));
            MidgardLogger.error("Erro ao recarregar mensagens", e);
        }
        
        // 4. Reload de todos os Módulos
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.modules_header"));
        
        if (MidgardCore.getModuleManager() != null) {
            Map<String, RPGModule> modules = MidgardCore.getModuleManager().getModules();
            
            for (Map.Entry<String, RPGModule> entry : modules.entrySet()) {
                RPGModule module = entry.getValue();
                if (module == null || !module.isEnabled()) continue;
                
                try {
                    module.reloadConfig();
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.module_ok", "%module%", module.getName()));
                    reloadedCount++;
                } catch (Exception e) {
                    errors.add(module.getName() + ": " + e.getMessage());
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.module_error", "%module%", module.getName(), "%error%", e.getMessage()));
                    MidgardLogger.error("Erro ao recarregar módulo: " + module.getName(), e);
                }
            }
        }
        
        // 5. Resumo Final
        long elapsed = System.currentTimeMillis() - startTime;
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.sep"));
        
        if (errors.isEmpty()) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.summary_ok", "%count%", String.valueOf(reloadedCount), "%elapsed%", String.valueOf(elapsed)));
        } else {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.summary_warn", "%errors%", String.valueOf(errors.size()), "%elapsed%", String.valueOf(elapsed)));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.check_console"));
        }
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
    }
    
    /**
     * Reload apenas da configuração principal
     */
    private void reloadConfig(CommandSender sender) {
        try {
            plugin.reloadConfig();
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.config_ok_single"));
        } catch (Exception e) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.config_error_single", "%error%", e.getMessage()));
            MidgardLogger.error("Erro ao recarregar config", e);
        }
    }
    
    /**
     * Reload apenas das mensagens
     */
    private void reloadMessages(CommandSender sender) {
        try {
            if (MidgardCore.getLanguageManager() != null) {
                String locale = plugin.getConfig().getString("settings.locale", "pt-br");
                MidgardCore.getLanguageManager().load(locale);
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.messages_ok", "%locale%", locale));
            } else {
                MessageUtils.send(sender, "&cLanguageManager não está disponível.");
            }
        } catch (Exception e) {
            if (MidgardCore.getLanguageManager() != null) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.messages_error"));
            } else {
                MessageUtils.send(sender, "&cErro ao recarregar mensagens.");
            }
            MidgardLogger.error("Erro ao recarregar mensagens", e);
        }
    }
    
    /**
     * Reload apenas dos atributos
     */
    private void reloadAttributes(CommandSender sender) {
        try {
            int count = reloadAttributesInternal();
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.attributes_ok", "%count%", String.valueOf(count)));
        } catch (Exception e) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.attributes_error"));
            MidgardLogger.error("Erro ao recarregar atributos", e);
        }
    }
    
    /**
     * Reload interno dos atributos
     */
    private int reloadAttributesInternal() {
        // Limpa atributos existentes
        AttributeRegistry.getInstance().clear();
        
        // Recarrega do arquivo
        ConfigWrapper attributesConfig = new ConfigWrapper(plugin, "settings/attributes.yml");
        ConfigurationSection section = attributesConfig.getConfig().getConfigurationSection("attributes");
        
        int count = 0;
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection attrSection = section.getConfigurationSection(key);
                if (attrSection == null) continue;
                
                String name = attrSection.getString("name", key);
                String icon = attrSection.getString("icon", "");
                String format = attrSection.getString("format", "0.0");
                double base = attrSection.getDouble("base", 0.0);
                double min = attrSection.getDouble("min", 0.0);
                double max = attrSection.getDouble("max", 100000.0);
                
                Attribute attribute = new Attribute(key, name, base, min, max, icon, format);
                AttributeRegistry.getInstance().register(key, attribute);
                count++;
            }
        }
        
        MidgardLogger.info("Recarregados " + count + " atributos do arquivo.");
        return count;
    }
    
    /**
     * Reload de todos os módulos
     */
    private void reloadAllModules(CommandSender sender) {
        if (MidgardCore.getModuleManager() == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.manager_unavailable"));
            return;
        }
        
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.reloading_all_header"));
        
        Map<String, RPGModule> modules = MidgardCore.getModuleManager().getModules();
        int success = 0;
        int failed = 0;
        
        for (Map.Entry<String, RPGModule> entry : modules.entrySet()) {
            RPGModule module = entry.getValue();
            if (module == null || !module.isEnabled()) continue;
            
            try {
                module.reloadConfig();
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.module_ok", "%module%", module.getName()));
                success++;
            } catch (Exception e) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.reload.module_error", "%module%", module.getName(), "%error%", e.getMessage()));
                MidgardLogger.error("Erro ao recarregar módulo: " + module.getName(), e);
                failed++;
            }
        }
        
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        if (failed == 0) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.all_ok", "%success%", String.valueOf(success)));
        } else {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.all_warn", "%success%", String.valueOf(success), "%failed%", String.valueOf(failed)));
        }
    }
    
    /**
     * Reload de um módulo específico
     */
    private void reloadSpecificModule(CommandSender sender, String moduleName) {
        if (MidgardCore.getModuleManager() == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.manager_unavailable"));
            return;
        }
        
        // Tenta encontrar o módulo (case-insensitive)
        RPGModule module = null;
        for (Map.Entry<String, RPGModule> entry : MidgardCore.getModuleManager().getModules().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(moduleName)) {
                module = entry.getValue();
                break;
            }
        }
        
        if (module == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.module_not_found", "%module%", moduleName));
            String available = MidgardCore.getModuleManager().getModules().keySet()
                    .stream()
                    .collect(Collectors.joining("<dark_gray>, <gray>"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.available_list", "%list%", available));
            return;
        }
        
        if (!module.isEnabled()) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.disabled", "%module%", module.getName()));
            return;
        }
        
        try {
            long start = System.currentTimeMillis();
            module.reloadConfig();
            long elapsed = System.currentTimeMillis() - start;
            
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.module_reloaded_ok", "%module%", module.getName(), "%elapsed%", String.valueOf(elapsed)));
        } catch (Exception e) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.modules.module_reloaded_error", "%module%", module.getName(), "%error%", e.getMessage()));
            MidgardLogger.error("Erro ao recarregar módulo: " + module.getName(), e);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(SUBCOMMANDS);
            
            // Adiciona nomes dos módulos
            if (MidgardCore.getModuleManager() != null) {
                suggestions.addAll(MidgardCore.getModuleManager().getModules().keySet());
            }
            
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("modules")) {
            if (MidgardCore.getModuleManager() != null) {
                return MidgardCore.getModuleManager().getModules().keySet().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
