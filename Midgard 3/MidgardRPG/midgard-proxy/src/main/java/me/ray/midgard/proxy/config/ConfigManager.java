package me.ray.midgard.proxy.config;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import me.ray.midgard.proxy.redis.RedisCredentials;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final Path configPath;
    private final Logger logger;
    private CommentedConfigurationNode rootNode;

    private RedisCredentials redisCredentials = new RedisCredentials("localhost", 6379, "", false);
    private boolean redisEnabled = false;
    private List<String> lobbyServers = Collections.singletonList("lobby");
    private String globalChatFormat = "<green>[G] <white><sender>: <message>";

    @Inject
    public ConfigManager(@DataDirectory Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.configPath = dataDirectory.resolve("config.yml");
        
        ensureConfigFile();
        load();
    }

    private void ensureConfigFile() {
        if (!Files.exists(configPath.getParent())) {
            try {
                Files.createDirectories(configPath.getParent());
            } catch (IOException e) {
                logger.error("Falha ao criar diretório de configuração", e);
            }
        }

        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                    logger.info("Criado config.yml padrão.");
                } else {
                    Files.createFile(configPath);
                    logger.warn("Recurso /config.yml não encontrado, criado arquivo vazio.");
                }
            } catch (IOException e) {
                logger.error("Falha ao criar config.yml padrão", e);
            }
        }
    }

    public void load() {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .build();
        
        try {
            rootNode = loader.load();
            
            // Redis
            this.redisEnabled = rootNode.node("redis", "enabled").getBoolean(false);
            String host = rootNode.node("redis", "address").getString("localhost");
            int port = rootNode.node("redis", "port").getInt(6379);
            String password = rootNode.node("redis", "password").getString("");
            boolean ssl = rootNode.node("redis", "ssl").getBoolean(false);
            
            this.redisCredentials = new RedisCredentials(host, port, password, ssl);
            
            // Lobby
            this.lobbyServers = rootNode.node("lobby-servers").getList(String.class, Collections.singletonList("lobby"));
            
            // Messages
            this.globalChatFormat = rootNode.node("messages", "global-chat-format").getString("<green>[G] <white><sender>: <message>");
            
            logger.info("Configuração carregada com sucesso.");
        } catch (ConfigurateException e) {
            logger.error("Erro ao carregar configurações", e);
        }
    }


    public RedisCredentials getRedisCredentials() {
        return redisCredentials;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public List<String> getLobbyServers() {
        return lobbyServers;
    }

    public String getGlobalChatFormat() {
        return globalChatFormat;
    }

    public String getMessage(String key, String defaultValue) {
        if (rootNode == null) {
            return defaultValue;
        }
        return rootNode.node("messages", key).getString(defaultValue);
    }
}
