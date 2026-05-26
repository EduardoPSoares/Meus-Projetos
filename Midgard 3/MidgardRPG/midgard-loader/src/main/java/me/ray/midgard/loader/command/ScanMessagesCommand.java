package me.ray.midgard.loader.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comando para escanear todo o código fonte e encontrar mensagens faltantes.
 * <p>
 * Uso: /midgard scanmessages [--generate]
 * <p>
 * Escaneia todos os módulos do projeto procurando por chamadas a:
 * - getMessage("chave")
 * - getRawMessage("chave")
 * - MessageKey.of("chave")
 * - MessageKey.builder("chave")
 * <p>
 * E verifica quais dessas chaves estão faltando nos arquivos YAML.
 *
 * @since 2.0.0
 */
public class ScanMessagesCommand extends MidgardCommand {

    private final JavaPlugin plugin;
    
    public ScanMessagesCommand(JavaPlugin plugin) {
        super("scanmessages", "midgard.admin.scan", false);
        this.plugin = plugin;
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("scanmsg", "findmissing", "checkmessages");
    }

    @Override
    public String getDescription() {
        return "Escaneia código e identifica mensagens faltantes";
    }

    @Override
    public String getUsage() {
        return "/rpg admin scanmessages [--generate]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean autoGenerate = args.length > 0 && args[0].equalsIgnoreCase("--generate");
        
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.header"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.sep"));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        
        // Encontrar o diretório raiz do projeto
        Path projectRoot = findProjectRoot();
        
        if (projectRoot == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.prod_warning"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.prod_note"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.prod_runtime"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
            
            MidgardCore.getLanguageManager().validateAndLog();
            
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.runtime_done"));
            return;
        }
        
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.project_root", "%path%", projectRoot.toString()));
        MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
        
        try {
            // Executar o escaneamento
            MidgardCore.getLanguageManager().scanAndExposeAllMissingKeys(projectRoot);
            
            if (autoGenerate) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.generating"));
                
                var report = MidgardCore.getLanguageManager().getValidator().scanAllModules(projectRoot);
                int generated = MidgardCore.getLanguageManager().getValidator().generateMissingKeys(report, true);
                
                if (generated > 0) {
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.generated", "%count%", String.valueOf(generated)));
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.reload_hint"));
                } else {
                    MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.none_generated"));
                }
            }
            
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.sep"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.done"));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.common.blank"));
            
        } catch (Exception e) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.scan.error", "%error%", e.getMessage()));
            MidgardLogger.error("Erro ao escanear mensagens", e);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("--generate");
        }
        return Collections.emptyList();
    }
    
    /**
     * Encontra o diretório raiz do projeto.
     * Procura por build.gradle ou settings.gradle subindo na hierarquia.
     */
    private Path findProjectRoot() {
        try {
            // Começar do diretório do plugin
            Path current = plugin.getDataFolder().toPath().getParent();
            
            // Subir até encontrar build.gradle ou settings.gradle
            while (current != null) {
                if (current.resolve("build.gradle").toFile().exists() ||
                    current.resolve("settings.gradle").toFile().exists()) {
                    
                    // Verificar se tem a estrutura de módulos do Midgard
                    if (current.resolve("midgard-core").toFile().exists() ||
                        current.resolve("midgard-modules").toFile().exists()) {
                        return current;
                    }
                }
                current = current.getParent();
            }
            
            // Fallback: tentar caminhos conhecidos de desenvolvimento
            String[] devPaths = {
                    System.getProperty("user.home") + "/Desktop/MidgardRPG",
                    System.getProperty("user.dir"),
                    "C:/Users/ray/Desktop/MidgardRPG"
            };
            
            for (String devPath : devPaths) {
                Path path = Path.of(devPath);
                if (path.resolve("midgard-modules").toFile().exists()) {
                    return path;
                }
            }
            
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao encontrar diretório raiz: %s", e.getMessage());
        }
        
        return null;
    }
}
