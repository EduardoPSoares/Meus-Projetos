package me.ray.midgard.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import me.ray.midgard.proxy.command.GlobalChatCommand;
import me.ray.midgard.proxy.command.LobbyCommand;
import me.ray.midgard.proxy.command.ReloadCommand;
import me.ray.midgard.proxy.config.ConfigManager;
import me.ray.midgard.proxy.listener.SecurityListener;
import me.ray.midgard.proxy.manager.GlobalChatManager;
import me.ray.midgard.proxy.manager.SessionManager;
import me.ray.midgard.proxy.redis.ProxyRedisManager;
import me.ray.midgard.proxy.redis.RedisCredentials;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "midgard-proxy",
    name = "MidgardProxy",
    version = "1.0.0",
    description = "Proxy Controller for MidgardRPG",
    authors = {"Ray"}
)
public class MidgardProxy {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private ProxyRedisManager redisManager;

    @Inject
    public MidgardProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.configManager = new ConfigManager(dataDirectory, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Inicializando MidgardProxy...");

        // Load credentials from config
        RedisCredentials credentials = configManager.getRedisCredentials();
        
        if (configManager.isRedisEnabled()) {
            this.redisManager = new ProxyRedisManager(server, logger);
            this.redisManager.initialize(credentials);
            if (this.redisManager.isEnabled()) {
                logger.info("Sistema de Redis ativo!");
            } else {
                logger.warn("Falha ao inicializar Redis. Algumas funcionalidades serão desativadas.");
            }
        } else {
            logger.info("Redis desativado na configuração.");
        }
        
        // Managers
        // Initialize managers even without Redis, but they must handle null/inactive redisManager gracefully
        GlobalChatManager chatManager = new GlobalChatManager(server, redisManager != null ? redisManager : new ProxyRedisManager(server, logger), configManager, logger);
        if (redisManager != null && redisManager.isEnabled()) {
             chatManager.init();
             // Register Chat Command only if Redis is OK
             CommandManager commandManager = server.getCommandManager();
             CommandMeta globalChatMeta = commandManager.metaBuilder("g")
                .aliases("global")
                .plugin(this)
                .build();
             commandManager.register(globalChatMeta, new GlobalChatCommand(redisManager, configManager));
        }
        
        SessionManager sessionManager = new SessionManager(this, server, redisManager != null ? redisManager : new ProxyRedisManager(server, logger), logger);
        if (redisManager != null && redisManager.isEnabled()) {
            sessionManager.init();
        }

        // Listeners
        // Security listener depends on session manager logic which depends on Redis. 
        // If no Redis, maybe skip security check or allow all?
        if (redisManager != null && redisManager.isEnabled()) {
            server.getEventManager().register(this, new SecurityListener(sessionManager));
        }

        // Register Commands
        CommandManager commandManager = server.getCommandManager();
        
        CommandMeta lobbyMeta = commandManager.metaBuilder("lobby")
                .aliases("hub", "l")
                .plugin(this)
                .build();
        commandManager.register(lobbyMeta, new LobbyCommand(server, configManager));
        
        CommandMeta mainMeta = commandManager.metaBuilder("midgardproxy")
                .plugin(this)
                .build();
        commandManager.register(mainMeta, new ReloadCommand(configManager));
        
        logger.info("MidgardProxy inicializado com sucesso!");
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redisManager != null) {
            redisManager.shutdown();
        }
    }
    
    public ProxyRedisManager getRedisManager() {
        return redisManager;
    }
}
