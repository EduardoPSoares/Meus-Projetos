package me.ray.midgard.core.utils;

import org.bukkit.Bukkit;

/**
 * Utilitário para formatar saídas no console de forma bonita e colorida.
 */
public class ConsoleUtils {

    private static final net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    private static final String PREFIX = "<gray>[" + "<gradient:#3498db:#2ecc71>MidgardRPG</gradient>" + "] <white>";
    private static final String ARROW = "<gray>▸</gray>";

    /**
     * Envia uma mensagem formatada para o console (suporta MiniMessage).
     */
    public static void log(String message) {
        // Proteção contra códigos de cor legados que quebram o MiniMessage
        if (message.contains("§")) {
            message = message.replace("§", "&");
        }
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(message));
    }

    /**
     * Envia uma mensagem formatada com prefixo padrão.
     */
    public static void info(String message) {
        log(PREFIX + message);
    }
    
    /**
     * Envia uma mensagem de sucesso.
     */
    public static void success(String message) {
        log(PREFIX + "<green>✔ <white>" + message);
    }

    /**
     * Envia uma mensagem de aviso.
     */
    public static void warn(String message) {
        log(PREFIX + "<yellow>⚠ <white>" + message);
    }

    /**
     * Envia uma mensagem de erro.
     */
    public static void error(String message) {
        log(PREFIX + "<red>✖ <white>" + message);
    }

    /**
     * Imprime o cabeçalho do plugin com estilo.
     */
    public static void printHeader() {
        log("");
        log(" <gradient:#00ffff:#0088ff>███╗   ███╗██╗██████╗  ██████╗  █████╗ ██████╗ ██████╗ </gradient>");
        log(" <gradient:#00ffff:#0088ff>████╗ ████║██║██╔══██╗██╔════╝ ██╔══██╗██╔══██╗██╔══██╗</gradient>");
        log(" <gradient:#00ffff:#0088ff>██╔████╔██║██║██║  ██║██║  ███╗███████║██████╔╝██║  ██║</gradient>");
        log(" <gradient:#00ffff:#0088ff>██║╚██╔╝██║██║██║  ██║██║   ██║██╔══██║██╔══██╗██║  ██║</gradient>");
        log(" <gradient:#00ffff:#0088ff>██║ ╚═╝ ██║██║██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝</gradient>");
        log(" <gradient:#00ffff:#0088ff>╚═╝     ╚═╝╚═╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ </gradient>");
        log("          <gray>Version: <white>1.0.0-SNAPSHOT <dark_gray>| <gray>Author: <white>Ray</white>");
        log("");
    }

    /**
     * Inicia uma nova seção de log.
     */
    public static void logSection(String title) {
        log("");
        log(" <gradient:#3498db:#2ecc71>━━━┫</gradient> <white><bold>" + title + "</bold></white> <gradient:#3498db:#2ecc71>┣" + "━".repeat(Math.max(0, 50 - title.length())) + "</gradient>");
        log("");
    }

    /**
     * Imprime um item chave-valor alinhado.
     */
    public static void logItem(String key, String value) {
        String padding = " ".repeat(Math.max(0, 20 - key.length()));
        log("  " + ARROW + " <gray>" + key + ":</gray>" + padding + "<white>" + value + "</white>");
    }

    /**
     * Imprime um status de tarefa.
     */
    public static void logStatus(String task, boolean success, String extra) {
        String symbol = success ? "<green>✔</green>" : "<red>✖</red>";
        String padding = " ".repeat(Math.max(0, 35 - task.length()));
        String extraText = extra != null && !extra.isEmpty() ? " <gray>(" + extra + ")</gray>" : "";
        log("  " + symbol + " <white>" + task + "</white>" + padding + extraText);
    }
    
    /**
     * Imprime o cabeçalho de um módulo (antes do onEnable).
     */
    public static void printModuleHeader(String moduleName) {
        log("");
        log(" <gradient:#3498db:#2ecc71>━━━┫</gradient> <white><bold>" + moduleName + "</bold></white> <gradient:#3498db:#2ecc71>┣" + "━".repeat(Math.max(0, 45 - moduleName.length())) + "</gradient>");
    }

    /**
     * Imprime o status de um módulo (depois do onEnable).
     */
    public static void printModuleStatus(String priority, String status, long timeMs, java.util.List<String> errors) {
        boolean success = status.equalsIgnoreCase("SUCCESS");
        String timeColor = timeMs > 100 ? "<red>" : (timeMs > 50 ? "<yellow>" : "<gray>");

        // Erros (Sub-bloco)
        if (errors != null && !errors.isEmpty()) {
            log("");
            log("  <red>✖ Errors Detected:</red>");
            for (String err : errors) {
                log("    <red>▪</red> <gray>" + err + "</gray>");
            }
            log("");
        }

        // Detalhes
        log("  " + ARROW + " <gray>Priority:   <white>" + priority + "</white>");
        log("  " + ARROW + " <gray>Status:     " + (success ? "<green>Enabled</green>" : "<red>Failed</red>"));
        log("  " + ARROW + " <gray>Time:       " + timeColor + timeMs + "ms</" + timeColor.substring(1));
    }

    /**
     * Imprime um bloco de carregamento de módulo com estilo próprio.
     */
    public static void printModuleBlock(String moduleName, String priority, String status, long timeMs, java.util.List<String> errors) {
        printModuleHeader(moduleName);
        printModuleStatus(priority, status, timeMs, errors);
    }

    /**
     * Imprime informações do ambiente com destaque.
     */
    public static void printEnvironmentInfo() {
        boolean folia = ServerEnvironment.isFolia();
        boolean paper = ServerEnvironment.isPaper();
        
        String serverType = folia ? "<gradient:#ff55ff:#aa00aa>Folia (Multi-Threaded)</gradient>" : 
                            paper ? "<gradient:#eeeeee:#aaaaaa>Paper (High Performance)</gradient>" : 
                            "<red>Spigot/Bukkit (Legacy)</red>";
                            
        logItem("Server Software", serverType);
        logItem("Java Version", ServerEnvironment.getJavaVersion());
        logItem("System OS", ServerEnvironment.getOS());
        
        if (folia) {
            logStatus("Multi-Threading", true, "Region & Task Optimizations");
        } else if (paper) {
            logStatus("Paper Optimization", true, "AsyncChat & Modern API");
        }
    }
}
