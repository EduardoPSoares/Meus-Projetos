package me.ray.midgardDiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import me.ray.midgardDiscord.utils.AutoUpdater;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.format.TextDecoration;

@Plugin(
    id = "midgardbrain",
    name = "MidgardBrain",
    version = "1.0",
    description = "Integração Discord-Minecraft para Velocity",
    authors = {"Ray"},
    dependencies = {
        @Dependency(id = "luckperms", optional = true)
    }
)
/**
 * Classe principal do plugin Velocity.
 * Responsável pela inicialização, carregamento de configurações e registro de comandos/eventos.
 */
public class MidgardVelocity {

    private final ProxyServer server;
    private final org.slf4j.Logger logger;
    private final Path dataDirectory;
    private final PluginContainer pluginContainer;
    
    private LinkManager linkManager;
    private WhitelistManager whitelistManager;
    private MaintenanceScheduler maintenanceScheduler;
    private LuckPermsHandler luckPermsHandler;
    private PunishmentManager punishmentManager;
    // Sets thread-safe para acesso concorrente (Ping Event vs Command)
    private final java.util.Set<String> maintenanceServers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> adminUsers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    // Executor dedicado para comunicação via socket (evita exaustão de threads do pool comum)
    private java.util.concurrent.ExecutorService socketExecutor;
    
    private String lobbyServerName = "lobby";
    private String botDataFolderPath = "data"; // Default relative path
    private int botSocketPort = 25590;
    
    // Database Config
    private String dbType = "";
    private String dbHost = null;
    private String dbPort = "3306";
    private String dbName = "midgard";
    private String dbUser = "root";
    private String dbPass = "";
    private boolean dbUseSSL = false;
    private String botSecretKey = "midgard_secret_key_change_me";
    
    // Update Config
    private boolean updateEnabled = true;
    private int updateCheckInterval = 60;
    private String githubToken = "";
    
    private DatabaseManager dbManager;
    private AuditLogger auditLogger;
    private MessagesManager messagesManager;
    private QueueManager queueManager;
    
    // Queue Config
    private String queueTargetServer = "rpg";
    private int queueDelay = 2;
    private int queueMaxSize = 200;

    @Inject
    public MidgardVelocity(ProxyServer server, org.slf4j.Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        
        // Inicializa o executor aqui, onde o logger já está disponível
        // Usa um ThreadPoolExecutor com fila limitada para evitar OutOfMemoryError em caso de spam ou bot offline
        this.socketExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1,
            0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(100), // Max 100 mensagens na fila
            r -> {
                Thread t = new Thread(r, "Midgard-Socket-Thread");
                t.setUncaughtExceptionHandler((thread, ex) -> {
                    this.logger.error("Erro não tratado na thread do socket: ", ex);
                });
                return t;
            },
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy() // Descarta mensagens antigas se a fila encher
        );
    }

    public ProxyServer getServer() {
        return server;
    }
    
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
    
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public QueueManager getQueueManager() {
        return queueManager;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            logger.info("Iniciando MidgardDiscord para Velocity...");
            
            // Recria o executor de sockets se já foi encerrado (PlugManX reload)
            if (socketExecutor.isShutdown()) {
                this.socketExecutor = new java.util.concurrent.ThreadPoolExecutor(
                    1, 1,
                    0L, java.util.concurrent.TimeUnit.MILLISECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(100),
                    r -> {
                        Thread t = new Thread(r, "Midgard-Socket-Thread");
                        t.setUncaughtExceptionHandler((thread, ex) -> {
                            this.logger.error("Erro não tratado na thread do socket: ", ex);
                        });
                        return t;
                    },
                    new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy()
                );
            }
            
            // Carrega configurações do arquivo config.yml
            loadConfig();
            // Garante que a config tenha todos os campos novos (atualiza se necessário)
            saveConfig();
            
            // Inicializa Banco de Dados
            this.dbManager = new DatabaseManager(logger, dataDirectory, dbType, dbHost, dbPort, dbName, dbUser, dbPass, dbUseSSL);
            this.dbManager.connect();
            
            // Inicializa Audit Logger
            this.auditLogger = new AuditLogger(dataDirectory.toFile());
            
            // Inicializa Messages Manager
            this.messagesManager = new MessagesManager(logger, dataDirectory);
            
            // Verificações de Segurança na Inicialização
            checkSecurityWarnings();
            
            File botDataFolder = new File(botDataFolderPath);
            if (!botDataFolder.exists()) {
                logger.info("Pasta de dados do bot não encontrada, criando: " + botDataFolder.getAbsolutePath());
                botDataFolder.mkdirs();
            } else {
                logger.info("Usando pasta de dados do bot: " + botDataFolder.getAbsolutePath());
            }
            
            // Inicializa gerenciadores
            this.linkManager = new LinkManager(this, logger, dbManager, botDataFolder);
            this.whitelistManager = new WhitelistManager(this, server, logger, dataDirectory, linkManager, botDataFolder, dbManager);
            this.maintenanceScheduler = new MaintenanceScheduler(this, server);
            this.punishmentManager = new PunishmentManager(logger, botDataFolder, dbManager);
            
            // Inicializa monitor de arquivo de manutenção
            MaintenanceFileMonitor maintenanceMonitor = new MaintenanceFileMonitor(this, server, logger, botDataFolder);
            maintenanceMonitor.start();
            
            // Schedule Cache Update for Punishments (Tab Complete)
            server.getScheduler().buildTask(this, () -> punishmentManager.updateCache())
                .repeat(2, java.util.concurrent.TimeUnit.MINUTES)
                .schedule();
            // Initial update
            server.getScheduler().buildTask(this, () -> punishmentManager.updateCache()).schedule();
            
            // Inicializa integração com LuckPerms (permissões)
            this.luckPermsHandler = new LuckPermsHandler(this, logger);
            this.luckPermsHandler.init();
            
            // Iniciar tarefa de sincronização de cargos
            // Verifica periodicamente se os jogadores online têm os cargos corretos
            server.getScheduler()
                .buildTask(this, new RoleSyncTask(this, luckPermsHandler, logger, botDataFolder))
                .repeat(5, java.util.concurrent.TimeUnit.SECONDS)
                .schedule();
            
            // Inicializa sistema de fila
            this.queueManager = new QueueManager(this, server, logger);
            this.queueManager.setTargetServer(queueTargetServer);
            this.queueManager.setDelayBetweenPlayers(queueDelay);
            this.queueManager.setMaxQueueSize(queueMaxSize);
            
            // Registrar comandos
            server.getCommandManager().register(
                server.getCommandManager().metaBuilder("midgard").build(),
                new VelocityCommand(this, server, whitelistManager, linkManager, botDataFolder)
            );
            
            // Registrar comando de fila (/fila)
            server.getCommandManager().register(
                server.getCommandManager().metaBuilder("fila").build(),
                new QueueCommand(this, queueManager)
            );
            
            // Registrar eventos
            server.getEventManager().register(this, new VelocityListener(this, server, logger, whitelistManager, linkManager));
            
            // Registrar canal de mensagens para comunicação com backend
            server.getChannelRegistrar().register(MinecraftChannelIdentifier.create("midgard", "maintenance"));
            server.getChannelRegistrar().register(MinecraftChannelIdentifier.create("midgard", "death"));

            // Auto Update
            if (updateEnabled) {
                try {
                    Path jarPath = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                    AutoUpdater updater = new AutoUpdater(this, dataDirectory, jarPath);
                    // Check immediately
                    server.getScheduler().buildTask(this, updater::checkForUpdate).schedule();
                    // Schedule periodic check
                    server.getScheduler().buildTask(this, updater::checkForUpdate)
                        .repeat(updateCheckInterval, java.util.concurrent.TimeUnit.MINUTES)
                        .schedule();
                } catch (Exception e) {
                    logger.error("Erro ao inicializar AutoUpdater: " + e.getMessage());
                }
            }

            logger.info("MidgardDiscord iniciado com sucesso!");
        } catch (Exception e) {
            logger.error("Erro fatal ao iniciar MidgardDiscord: ", e);
        }
    }

    private void checkSecurityWarnings() {
        // 1. Verifica credenciais padrão
        if ("mysql".equalsIgnoreCase(dbType)) {
            if ("root".equals(dbUser) && (dbPass == null || dbPass.isEmpty())) {
                logger.warn("!!! ALERTA DE SEGURANÇA !!!");
                logger.warn("Você está usando o usuário 'root' sem senha no MySQL.");
                logger.warn("Isso é extremamente perigoso. Configure um usuário dedicado e uma senha forte.");
            }
        }
        
        // 2. Verifica chave secreta do bot
        if (botSecretKey == null || botSecretKey.isEmpty() || botSecretKey.length() < 8) {
            logger.warn("!!! ALERTA DE SEGURANÇA !!!");
            logger.warn("A chave secreta do bot (bot-secret-key) não está configurada ou é muito curta.");
            logger.warn("Qualquer pessoa com acesso local à porta " + botSocketPort + " pode enviar comandos falsos.");
        }
        
        // 3. Verifica usuário do sistema (Root/Admin)
        String osUser = System.getProperty("user.name");
        if ("root".equals(osUser) || "Administrator".equals(osUser)) {
            logger.warn("!!! ALERTA DE SEGURANÇA !!!");
            logger.warn("O servidor está rodando como superusuário (" + osUser + ").");
            logger.warn("Se o servidor for comprometido, o atacante terá controle total da máquina.");
            logger.warn("Recomendamos criar um usuário limitado para rodar o servidor.");
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        disable();
    }

    /**
     * Desativa o plugin de forma limpa, desregistrando comandos, eventos, tarefas e canais.
     * Compatível com PlugManX (unload/reload sem reiniciar o proxy).
     */
    public void disable() {
        try {
            logger.info("Desligando MidgardDiscord...");
            
            // Desregistra comandos
            server.getCommandManager().unregister("midgard");
            server.getCommandManager().unregister("fila");
            
            // Desregistra todos os listeners do plugin
            server.getEventManager().unregisterListeners(this);
            
            // Cancela todas as tarefas agendadas do plugin
            server.getScheduler().tasksByPlugin(this).forEach(task -> task.cancel());
            
            // Desregistra canais de mensagens
            server.getChannelRegistrar().unregister(MinecraftChannelIdentifier.create("midgard", "maintenance"));
            server.getChannelRegistrar().unregister(MinecraftChannelIdentifier.create("midgard", "death"));
            
            // Limpa fila de jogadores
            if (queueManager != null) {
                queueManager.clear();
            }
            
            // Encerra executor de sockets
            socketExecutor.shutdown();
            try {
                if (!socketExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    socketExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                socketExecutor.shutdownNow();
            }
            
            // Fecha conexão com banco de dados
            if (dbManager != null) {
                dbManager.close();
            }
            
            if (auditLogger != null) {
                auditLogger.shutdown();
            }
            
            logger.info("MidgardDiscord desligado com sucesso.");
        } catch (Exception e) {
            logger.error("Erro ao desligar MidgardDiscord: ", e);
        }
    }
    
    private String parseConfigValue(String value) {
        if (value == null) return "";
        String val = value.trim();
        // Remove comentários inline (ex: valor # comentario)
        if (val.contains(" #")) {
            val = val.substring(0, val.indexOf(" #")).trim();
        }
        return val.replace("\"", "").replace("'", "");
    }

    private void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            Path configPath = dataDirectory.resolve("config.yml");
            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    } else {
                        // Fallback se não encontrar no jar
                        Files.createFile(configPath);
                    }
                }
            }
            
            // Leitura simples do config (YAML simples)
            // Para evitar dependências extras, vamos ler linha a linha
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;

                    if (line.startsWith("lobby-server:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) lobbyServerName = parseConfigValue(parts[1]);
                    } else if (line.startsWith("bot-data-folder:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) botDataFolderPath = parseConfigValue(parts[1]);
                    } else if (line.startsWith("bot-socket-port:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            try {
                                String val = parseConfigValue(parts[1]);
                                botSocketPort = Integer.parseInt(val);
                                
                                // Restrição de Segurança: Impede uso de portas privilegiadas (1-1024)
                                // Isso evita que o plugin interfira com serviços do sistema (SSH, HTTP, etc)
                                if (botSocketPort <= 1024 || botSocketPort > 65535) {
                                    logger.warn("Porta do socket insegura ou inválida (" + botSocketPort + "). Usando padrão 25590.");
                                    botSocketPort = 25590;
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("Porta do socket inválida no config.yml, usando padrão 25590");
                            }
                        }
                    } else if (line.startsWith("maintenance:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String val = parseConfigValue(parts[1]);
                            if (!val.equalsIgnoreCase("false") && !val.equalsIgnoreCase("true")) {
                                String[] servers = val.split(",");
                                for (String s : servers) {
                                    if (!s.trim().isEmpty()) {
                                        maintenanceServers.add(s.trim().toLowerCase());
                                    }
                                }
                            }
                        }
                    } else if (line.startsWith("admin-users:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String val = parseConfigValue(parts[1]).replace("[", "").replace("]", "");
                            String[] users = val.split(",");
                            for (String u : users) {
                                if (!u.trim().isEmpty()) {
                                    adminUsers.add(u.trim().toLowerCase());
                                }
                            }
                        }
                    } else if (line.startsWith("db-type:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbType = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-host:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbHost = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-port:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbPort = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-name:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbName = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-user:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbUser = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-pass:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbPass = parseConfigValue(parts[1]);
                    } else if (line.startsWith("db-use-ssl:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) dbUseSSL = Boolean.parseBoolean(parseConfigValue(parts[1]));
                    } else if (line.startsWith("bot-secret-key:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String val = parseConfigValue(parts[1]);
                            if (!val.isEmpty()) botSecretKey = val;
                        }
                    } else if (line.startsWith("check-interval:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            try {
                                updateCheckInterval = Integer.parseInt(parseConfigValue(parts[1]));
                            } catch (NumberFormatException e) {
                                updateCheckInterval = 60;
                            }
                        }
                    } else if (line.startsWith("github-token:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) githubToken = parseConfigValue(parts[1]);
                    } else if (line.startsWith("queue-target-server:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) queueTargetServer = parseConfigValue(parts[1]);
                    } else if (line.startsWith("queue-delay:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            try { queueDelay = Integer.parseInt(parseConfigValue(parts[1])); }
                            catch (NumberFormatException e) { queueDelay = 2; }
                        }
                    } else if (line.startsWith("queue-max-size:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            try { queueMaxSize = Integer.parseInt(parseConfigValue(parts[1])); }
                            catch (NumberFormatException e) { queueMaxSize = 200; }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Erro ao carregar configuração: ", e);
        }
    }
    
    public synchronized void saveConfig() {
        try {
            Path configPath = dataDirectory.resolve("config.yml");
            
            if (!Files.exists(configPath)) {
                return;
            }
            
            String fileContent = Files.readString(configPath);
            String servers = String.join(",", maintenanceServers);
            
            // Regex seguro: ancora no início da linha e permite espaços antes da chave
            // (?m) ativa modo multiline, ^ corresponde ao início da linha
            if (fileContent.contains("maintenance:")) {
                fileContent = fileContent.replaceAll("(?m)^\\s*maintenance:.*", "maintenance: " + java.util.regex.Matcher.quoteReplacement(servers));
            } else {
                fileContent += "\nmaintenance: " + servers;
            }

            // Atualiza admin-users
            String admins = String.join(",", adminUsers);
            String adminsYaml = "[" + admins + "]";
            
            if (fileContent.contains("admin-users:")) {
                fileContent = fileContent.replaceAll("(?m)^\\s*admin-users:.*", "admin-users: " + java.util.regex.Matcher.quoteReplacement(adminsYaml));
            } else {
                fileContent += "\nadmin-users: " + adminsYaml;
            }
            
            // Adiciona campos de DB se não existirem
            if (!fileContent.contains("db-host:")) {
                fileContent += "\n\n# Configuração de Banco de Dados\n# Tipos suportados: mysql, sqlite, json (padrão se não configurado)\ndb-type: \"\"\ndb-host: \"\"\ndb-port: \"3306\"\ndb-name: \"midgard\"\ndb-user: \"root\"\ndb-pass: \"\"\ndb-use-ssl: \"false\"";
            } else if (!fileContent.contains("db-type:")) {
                // Migração: adiciona db-type se não existir mas db-host existir
                fileContent = fileContent.replace("db-host:", "db-type: \"\"\ndb-host:");
            }
            
            if (!fileContent.contains("db-use-ssl:")) {
                fileContent += "\ndb-use-ssl: \"false\"";
            }

            if (!fileContent.contains("bot-secret-key:")) {
                fileContent += "\n\n# Chave secreta para autenticação do socket (deixe vazio para desativar)\nbot-secret-key: \"midgard_secret_key_change_me\"";
            }

            if (!fileContent.contains("github-token:")) {
                fileContent += "\n\n# Token do GitHub para atualizações automáticas (opcional)\ngithub-token: \"\"";
            }

            // Salva atomicamente usando arquivo temporário com retry
            int attempts = 0;
            while (attempts < 3) {
                try {
                    Path tempPath = dataDirectory.resolve("config.yml.tmp");
                    Files.writeString(tempPath, fileContent);
                    Files.move(tempPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    
                    // Tenta restringir permissões (apenas leitura/escrita para o dono) - Funciona melhor em Linux
                    try {
                        if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                            java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.util.EnumSet.of(
                                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
                            );
                            Files.setPosixFilePermissions(configPath, perms);
                        }
                    } catch (Exception ignored) {
                        // Ignora erro de permissão (comum em Windows)
                    }
                    
                    return;
                } catch (IOException e) {
                    attempts++;
                    if (attempts >= 3) {
                        logger.error("Erro ao salvar config.yml após 3 tentativas: ", e);
                    } else {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Erro ao salvar configuração: ", e);
        }
    }

    public synchronized void addAdmin(String username) {
        if (username == null || username.trim().isEmpty()) return;
        if (username.contains(",")) {
            logger.warn("Tentativa de adicionar admin com caractere inválido (vírgula): " + username);
            return;
        }
        try {
            if (adminUsers.add(username.toLowerCase())) {
                saveConfig();
                logger.info("Adicionado " + username + " à lista de admins (bypass de manutenção).");
                if (auditLogger != null) auditLogger.log("SYSTEM", "ADD_ADMIN", username, "Added via LuckPerms/Config");
            }
        } catch (Exception e) {
            logger.error("Erro ao adicionar admin: ", e);
        }
    }

    public synchronized void removeAdmin(String username) {
        if (username == null || username.trim().isEmpty()) return;
        try {
            if (adminUsers.remove(username.toLowerCase())) {
                saveConfig();
                logger.info("Removido " + username + " da lista de admins.");
                if (auditLogger != null) auditLogger.log("SYSTEM", "REMOVE_ADMIN", username, "Removed via LuckPerms/Config");
            }
        } catch (Exception e) {
            logger.error("Erro ao remover admin: ", e);
        }
    }

    public void reload() {
        try {
            logger.info("Recarregando configurações do MidgardBrain...");
            
            // Limpa estados atuais
            maintenanceServers.clear();
            adminUsers.clear();
            
            // Recarrega config.yml
            loadConfig();
            
            // Recarrega mensagens
            if (messagesManager != null) {
                messagesManager.load();
            }
            
            // Atualiza configurações da fila
            if (queueManager != null) {
                queueManager.setTargetServer(queueTargetServer);
                queueManager.setDelayBetweenPlayers(queueDelay);
                queueManager.setMaxQueueSize(queueMaxSize);
            }
            
            logger.info("Configurações do MidgardBrain recarregadas com sucesso!");
        } catch (Exception e) {
            logger.error("Erro ao recarregar configurações: ", e);
        }
    }

    public boolean isMaintenance(String serverName) {
        return maintenanceServers.contains(serverName.toLowerCase());
    }

    public boolean isAdmin(String username) {
        return adminUsers.contains(username.toLowerCase());
    }

    public java.util.Set<String> getAdminUsers() {
        return java.util.Collections.unmodifiableSet(adminUsers);
    }

    public void setMaintenance(String serverName, boolean state) {
        if (serverName == null || serverName.trim().isEmpty()) return;
        if (serverName.contains(",")) {
            logger.warn("Tentativa de definir manutenção para servidor com nome inválido (vírgula): " + serverName);
            return;
        }
        try {
            Component prefix = Component.text("Midgard", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY));

            if (state) {
                maintenanceServers.add(serverName.toLowerCase());
                broadcastStaff(prefix
                    .append(Component.text("Manutenção ", NamedTextColor.RED))
                    .append(Component.text("INICIADA", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" no servidor ", NamedTextColor.RED))
                    .append(Component.text(serverName, NamedTextColor.YELLOW)));
                if (auditLogger != null) auditLogger.log("SYSTEM", "MAINTENANCE_ON", serverName, "Maintenance enabled");
            } else {
                maintenanceServers.remove(serverName.toLowerCase());
                broadcastStaff(prefix
                    .append(Component.text("Manutenção ", NamedTextColor.RED))
                    .append(Component.text("FINALIZADA", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" no servidor ", NamedTextColor.RED))
                    .append(Component.text(serverName, NamedTextColor.YELLOW)));
                if (auditLogger != null) auditLogger.log("SYSTEM", "MAINTENANCE_OFF", serverName, "Maintenance disabled");
            }
            saveConfig();
        } catch (Exception e) {
            logger.error("Erro ao definir manutenção: ", e);
        }
    }

    private void broadcastStaff(Component message) {
        try {
            for (Player player : server.getAllPlayers()) {
                if (player.hasPermission("midgard.staff") || 
                    player.hasPermission("midgard.admin") || 
                    player.hasPermission("midgard.op") ||
                    isAdmin(player.getUsername())) {
                    player.sendMessage(message);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem para staff: ", e);
        }
    }
    
    public String getLobbyServerName() {
        return lobbyServerName;
    }
    
    public org.slf4j.Logger getLogger() {
        return logger;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public MaintenanceScheduler getMaintenanceScheduler() {
        return maintenanceScheduler;
    }

    public void sendSocketMessage(String message) {
        if (message == null || message.isEmpty()) return;
        // Sanitize message to prevent protocol injection
        final String safeMessage = message.replace("\n", "").replace("\r", "");
        
        socketExecutor.submit(() -> {
            logger.info("Tentando enviar mensagem socket para 127.0.0.1:" + botSocketPort + " | Msg: " + safeMessage);
            try (java.net.Socket socket = new java.net.Socket()) {
                // Set connection timeout and read timeout
                socket.connect(new java.net.InetSocketAddress("127.0.0.1", botSocketPort), 2000); // 2s connection timeout
                socket.setSoTimeout(5000); // 5s read timeout
                
                java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
                
                if (botSecretKey != null && !botSecretKey.isEmpty()) {
                    out.println("AUTH:" + botSecretKey);
                }
                
                out.println(safeMessage);
                logger.info("Mensagem socket enviada com sucesso.");
            } catch (Exception e) {
                // Ignora erros de conexão (bot pode estar offline)
                logger.warn("Erro ao enviar mensagem via socket (bot offline?): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
