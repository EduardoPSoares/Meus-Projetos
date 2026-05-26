package me.ray.midgardDiscord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Monitora o arquivo de status de manutenção compartilhado com o Bot.
 * Sincroniza o estado de manutenção entre Discord e Velocity.
 */
public class MaintenanceFileMonitor {

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final File file;
    private final Gson gson;
    
    private long lastModified = 0;
    private Map<String, MaintenanceInfo> lastState = new HashMap<>();

    public MaintenanceFileMonitor(MidgardVelocity plugin, ProxyServer server, Logger logger, File botDataFolder) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.file = new File(botDataFolder, "maintenance_status.json");
        this.gson = new Gson();
    }

    public void start() {
        server.getScheduler().buildTask(plugin, this::checkFile)
                .repeat(5, TimeUnit.SECONDS)
                .schedule();
        logger.info("Monitor de arquivo de manutenção iniciado: " + file.getAbsolutePath());
    }

    private void checkFile() {
        if (!file.exists()) return;
        
        long currentModified = file.lastModified();
        if (currentModified <= lastModified) return;
        
        lastModified = currentModified;
        
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, MaintenanceInfo>>(){}.getType();
            Map<String, MaintenanceInfo> newState = gson.fromJson(reader, type);
            
            if (newState == null) return;
            
            // Check for changes
            for (Map.Entry<String, MaintenanceInfo> entry : newState.entrySet()) {
                String serverName = entry.getKey();
                MaintenanceInfo info = entry.getValue();
                
                boolean currentStatus = plugin.isMaintenance(serverName);
                
                if (info.enabled != currentStatus) {
                    logger.info("Sincronizando manutenção para " + serverName + ": " + (info.enabled ? "ATIVADO" : "DESATIVADO") + " (via Arquivo)");
                    // Use scheduler with 0 seconds to trigger immediate action with proper notifications/kicks
                    plugin.getMaintenanceScheduler().schedule(serverName, 0, info.enabled);
                }
            }
            
            lastState = newState;
            
        } catch (Exception e) {
            logger.error("Erro ao ler arquivo de manutenção", e);
        }
    }

    private static class MaintenanceInfo {
        boolean enabled;
        long timestamp;
        String user;
    }
}
