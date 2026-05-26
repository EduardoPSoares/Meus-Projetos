package me.ray.midgardDiscord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gerenciador de Whitelist.
 * Responsável por ler e monitorar os arquivos de whitelist (whitelist_status.json) e bypass.
 * Compartilha os dados com o bot do Discord através do sistema de arquivos.
 */
public class WhitelistManager {

    private final ProxyServer server;
    private final Logger logger;
    private final LinkManager linkManager;
    private final File file;
    private final File bypassFile;
    private final File exportFile;
    private final Gson gson;
    private final Object plugin; // Referência ao plugin principal para agendamento
    private final DatabaseManager dbManager;
    
    private long lastModified = 0;
    private long bypassLastModified = 0;
    
    private final AtomicBoolean exportScheduled = new AtomicBoolean(false);
    private final Object exportLock = new Object();
    
    // Discord ID -> Status Info
    private Map<String, WhitelistInfo> whitelistStatus = new ConcurrentHashMap<>();
    
    // UUID -> Boolean (Bypass)
    private Map<java.util.UUID, Boolean> bypassList = new ConcurrentHashMap<>();

    public WhitelistManager(Object plugin, ProxyServer server, Logger logger, Path dataDirectory, LinkManager linkManager, File botDataFolder, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.linkManager = linkManager;
        this.dbManager = dbManager;
        
        File dataFolder = botDataFolder;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.file = new File(dataFolder, "whitelist_status.json");
        
        File pluginDataFolder = dataDirectory.toFile();
        if (!pluginDataFolder.exists()) pluginDataFolder.mkdirs();
        
        this.bypassFile = new File(pluginDataFolder, "whitelist_bypass.json");
        this.exportFile = new File(pluginDataFolder, "whitelisted_players.json");
        
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
        loadBypass();
        migrateLegacyWhitelist();
    }

    public boolean isWhitelisted(String discordId) {
        try {
            if (discordId == null) return false;
            
            // Check DB first if connected
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT status FROM midgard_whitelist WHERE discord_id = ?")) {
                    ps.setString(1, discordId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            return "APPROVED".equalsIgnoreCase(status);
                        } else {
                            // Se conectou ao DB e não encontrou, o jogador NÃO está na whitelist.
                            // Não deve fazer fallback para o arquivo, pois o DB é a fonte da verdade.
                            return false;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao verificar whitelist no DB para " + discordId, e);
                    // Fallback to file apenas em caso de erro
                }
            }

            load(); // Recarrega para garantir dados atualizados
            if (!whitelistStatus.containsKey(discordId)) {
                return false;
            }
            WhitelistInfo info = whitelistStatus.get(discordId);
            return "APPROVED".equalsIgnoreCase(info.status);
        } catch (Exception e) {
            logger.error("Erro ao verificar whitelist para Discord ID " + discordId, e);
            return false;
        }
    }
    
    public boolean isBypassed(java.util.UUID uuid) {
        try {
            loadBypass();
            return bypassList.getOrDefault(uuid, false);
        } catch (Exception e) {
            logger.error("Erro ao verificar bypass para UUID " + uuid, e);
            return false;
        }
    }

    public boolean isApprovedByNickname(String nickname) {
        try {
            if (nickname == null) return false;
            
            // Check DB first
            if (dbManager != null && dbManager.isConnected()) {
                // Usando LOWER para garantir que a busca seja case-insensitive
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT status FROM midgard_whitelist WHERE LOWER(nickname) = LOWER(?)")) {
                    ps.setString(1, nickname);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            return "APPROVED".equalsIgnoreCase(status);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao verificar nickname no DB: " + nickname, e);
                }
            }

            load();
            for (WhitelistInfo info : whitelistStatus.values()) {
                if (info.nickname != null && info.nickname.equalsIgnoreCase(nickname)) {
                    return "APPROVED".equalsIgnoreCase(info.status);
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Erro ao verificar aprovação por nickname " + nickname, e);
            return false;
        }
    }
    
    public void setWhitelisted(String discordId, boolean allowed) {
        if (discordId == null || !discordId.matches("\\d{17,20}")) {
             logger.warn("Tentativa de whitelist em ID inválido: " + discordId);
             return;
        }
        try {
            // Atualiza no banco de dados se conectado
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection()) {
                    if (allowed) {
                        // Tenta atualizar primeiro; se não existir, insere
                        try (PreparedStatement psUpdate = conn.prepareStatement(
                                "UPDATE midgard_whitelist SET status = 'APPROVED', reason = 'Adicionado via comando In-Game' WHERE discord_id = ?")) {
                            psUpdate.setString(1, discordId);
                            int rows = psUpdate.executeUpdate();
                            if (rows == 0) {
                                // Registro não existe no DB, insere novo
                                String insertSql;
                                if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                                    insertSql = "INSERT INTO midgard_whitelist (discord_id, status, reason, created_at) VALUES (?, 'APPROVED', 'Adicionado via comando In-Game', CURRENT_TIMESTAMP) " +
                                                "ON CONFLICT(discord_id) DO UPDATE SET status = 'APPROVED', reason = 'Adicionado via comando In-Game'";
                                } else {
                                    insertSql = "INSERT INTO midgard_whitelist (discord_id, status, reason, created_at) VALUES (?, 'APPROVED', 'Adicionado via comando In-Game', CURRENT_TIMESTAMP) " +
                                                "ON DUPLICATE KEY UPDATE status = 'APPROVED', reason = 'Adicionado via comando In-Game'";
                                }
                                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                                    psInsert.setString(1, discordId);
                                    psInsert.executeUpdate();
                                }
                            }
                        }
                    } else {
                        // Remove da whitelist no DB
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE midgard_whitelist SET status = 'REMOVED', reason = 'Removido via comando In-Game' WHERE discord_id = ?")) {
                            ps.setString(1, discordId);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao atualizar whitelist no DB para " + discordId, e);
                }
            }

            // Sempre atualiza o JSON como fallback/cache
            load();
            if (allowed) {
                WhitelistInfo info = whitelistStatus.getOrDefault(discordId, new WhitelistInfo());
                info.status = "APPROVED";
                info.timestamp = java.time.LocalDate.now().toString();
                info.reason = "Adicionado via comando In-Game";
                whitelistStatus.put(discordId, info);
            } else {
                whitelistStatus.remove(discordId);
            }
            save();
        } catch (Exception e) {
            logger.error("Erro ao definir whitelist para Discord ID " + discordId, e);
        }
    }
    
    public void addBypass(java.util.UUID uuid) {
        try {
            bypassList.put(uuid, true);
            saveBypass();
        } catch (Exception e) {
            logger.error("Erro ao adicionar bypass para UUID " + uuid, e);
        }
    }
    
    public void removeBypass(java.util.UUID uuid) {
        try {
            bypassList.remove(uuid);
            saveBypass();
        } catch (Exception e) {
            logger.error("Erro ao remover bypass para UUID " + uuid, e);
        }
    }
    
    public String getStatus(String discordId) {
        try {
            if (discordId == null) return "NONE";
            
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT status FROM midgard_whitelist WHERE discord_id = ?")) {
                    ps.setString(1, discordId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("status");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao obter status do DB para " + discordId, e);
                }
            }

            load();
            if (!whitelistStatus.containsKey(discordId)) {
                return "NONE";
            }
            return whitelistStatus.get(discordId).status;
        } catch (Exception e) {
            logger.error("Erro ao obter status para Discord ID " + discordId, e);
            return "ERROR";
        }
    }

    public String getReason(String discordId) {
        try {
            if (discordId == null) return null;

            // Consulta o DB primeiro se conectado
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT reason FROM midgard_whitelist WHERE discord_id = ?")) {
                    ps.setString(1, discordId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("reason");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao obter razão do DB para " + discordId, e);
                    // Fallback para arquivo
                }
            }

            load();
            if (!whitelistStatus.containsKey(discordId)) {
                return null;
            }
            return whitelistStatus.get(discordId).reason;
        } catch (Exception e) {
            logger.error("Erro ao obter razão para Discord ID " + discordId, e);
            return null;
        }
    }

    private void load() {
        try {
            if (!file.exists()) {
                return;
            }
            
            // Proteção contra arquivos gigantes
            if (file.length() > 5 * 1024 * 1024) { // 5MB
                logger.error("Arquivo de whitelist muito grande (>5MB). Ignorando para evitar crash.");
                return;
            }
            
            // Otimização: Só recarrega se o arquivo foi modificado
            long currentModified = file.lastModified();
            if (currentModified <= lastModified) {
                return;
            }
            lastModified = currentModified;

            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, WhitelistInfo>>(){}.getType();
                Map<String, WhitelistInfo> data = gson.fromJson(reader, type);
                if (data != null) {
                    whitelistStatus = new ConcurrentHashMap<>(data);
                }
                requestExport();
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar whitelist: " + e.getMessage(), e);
        }
    }
    
    private synchronized void save() {
        try {
            saveAtomic(file, whitelistStatus);
            lastModified = file.lastModified();
            requestExport();
        } catch (Exception e) {
            logger.error("Erro ao salvar whitelist", e);
        }
    }
    
    private void loadBypass() {
        try {
            if (!bypassFile.exists()) return;
            
            long currentModified = bypassFile.lastModified();
            if (currentModified <= bypassLastModified) {
                return;
            }
            bypassLastModified = currentModified;

            try (Reader reader = new FileReader(bypassFile)) {
                Type type = new TypeToken<Map<java.util.UUID, Boolean>>(){}.getType();
                Map<java.util.UUID, Boolean> data = gson.fromJson(reader, type);
                if (data != null) bypassList = new ConcurrentHashMap<>(data);
                requestExport();
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar bypass: " + e.getMessage(), e);
        }
    }

    private synchronized void saveBypass() {
        try {
            saveAtomic(bypassFile, bypassList);
            bypassLastModified = bypassFile.lastModified();
            requestExport();
        } catch (Exception e) {
            logger.error("Erro ao salvar bypass", e);
        }
    }
    
    private void saveAtomic(File file, Object data) {
        try {
            int attempts = 0;
            while (attempts < 3) {
                try {
                    File tempFile = new File(file.getPath() + ".tmp");
                    try (Writer writer = new FileWriter(tempFile)) {
                        gson.toJson(data, writer);
                    }
                    Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    return; // Sucesso
                } catch (IOException e) {
                    attempts++;
                    if (attempts >= 3) {
                        logger.error("Erro ao salvar arquivo " + file.getName() + " após 3 tentativas: " + e.getMessage());
                    } else {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro crítico em saveAtomic para arquivo " + file.getName(), e);
        }
    }
    
    public void requestExport() {
        try {
            if (exportScheduled.getAndSet(true)) return;
            
            // Debounce de 5 segundos
            server.getScheduler().buildTask(plugin, () -> {
                try {
                    exportScheduled.set(false);
                    exportConsolidatedWhitelist();
                } catch (Exception e) {
                    logger.error("Erro na task de exportação", e);
                }
            }).delay(5, TimeUnit.SECONDS).schedule();
        } catch (Exception e) {
            logger.error("Erro ao solicitar exportação", e);
        }
    }
    
    private void migrateLegacyWhitelist() {
        // Tenta encontrar whitelist.txt na pasta do plugin ou na raiz
        // 1. Tenta na pasta do plugin (onde fica config.yml, bypass, etc)
        File legacyFile = new File(bypassFile.getParentFile(), "whitelist.txt");
        
        // 2. Tenta na pasta de dados do bot (botDataFolder)
        if (!legacyFile.exists()) {
            legacyFile = new File(file.getParentFile(), "whitelist.txt");
        }
        
        // 3. Tenta na raiz do servidor (um nível acima da pasta de dados do bot, assumindo que seja 'data')
        if (!legacyFile.exists()) {
            legacyFile = new File(file.getParentFile().getParentFile(), "whitelist.txt");
        }
        
        if (!legacyFile.exists()) return;
        
        logger.info("Encontrado arquivo whitelist.txt. Iniciando migração para o banco de dados...");
        
        try {
            List<String> lines = Files.readAllLines(legacyFile.toPath());
            int count = 0;
            
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection()) {
                    String sql;
                    // SQLite syntax vs MySQL syntax
                    if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                        sql = "INSERT INTO midgard_whitelist (discord_id, nickname, status, reason, created_at) VALUES (?, ?, ?, ?, ?) " +
                              "ON CONFLICT(discord_id) DO NOTHING";
                    } else {
                        sql = "INSERT IGNORE INTO midgard_whitelist (discord_id, nickname, status, reason, created_at) VALUES (?, ?, ?, ?, ?)";
                    }
                    
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (String line : lines) {
                            String nickname = line.trim();
                            if (nickname.isEmpty()) continue;
                            
                            // Gera um ID temporário para migração
                            String discordId = "legacy_" + nickname.toLowerCase();
                            if (discordId.length() > 32) discordId = discordId.substring(0, 32);
                            
                            ps.setString(1, discordId);
                            ps.setString(2, nickname);
                            ps.setString(3, "APPROVED");
                            ps.setString(4, "Legacy Migration");
                            ps.setString(5, java.time.LocalDate.now().toString());
                            ps.addBatch();
                            count++;
                        }
                        ps.executeBatch();
                    }
                }
                logger.info("Migração concluída: " + count + " jogadores importados.");
                
                // Renomeia o arquivo para não processar novamente
                File renamed = new File(legacyFile.getParentFile(), "whitelist.txt.migrated");
                if (legacyFile.renameTo(renamed)) {
                    logger.info("SUCESSO: Arquivo whitelist.txt processado e renomeado para whitelist.txt.migrated");
                } else {
                    logger.warn("ATENÇÃO: Falha ao renomear whitelist.txt. Remova-o manualmente para evitar reprocessamento.");
                }
            } else {
                logger.warn("Banco de dados não conectado. Migração adiada.");
            }
        } catch (Exception e) {
            logger.error("Erro ao migrar whitelist.txt", e);
        }
    }

    public boolean updateDiscordId(String nickname, String newDiscordId) {
        try {
            if (dbManager != null && dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection()) {
                    // 1. Verifica se já existe registro com esse Discord ID (Conflito)
                    try (PreparedStatement psCheck = conn.prepareStatement("SELECT nickname FROM midgard_whitelist WHERE discord_id = ?")) {
                        psCheck.setString(1, newDiscordId);
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) {
                                String existingNick = rs.getString("nickname");
                                logger.warn("Conflito detectado: Discord ID " + newDiscordId + " já está associado a " + existingNick + ". Resolvendo...");
                                
                                // Remove o registro conflitante (assumindo que o registro Legacy que estamos tentando atualizar é o correto/aprovado)
                                try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM midgard_whitelist WHERE discord_id = ?")) {
                                    psDel.setString(1, newDiscordId);
                                    psDel.executeUpdate();
                                    logger.info("Registro conflitante removido para permitir atualização.");
                                }
                            }
                        }
                    }

                    // 2. Realiza a atualização
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE midgard_whitelist SET discord_id = ? WHERE LOWER(nickname) = LOWER(?)")) {
                        ps.setString(1, newDiscordId);
                        ps.setString(2, nickname);
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            logger.info("SUCESSO: Atualizado Discord ID para nickname " + nickname + ": " + newDiscordId);
                            load(); // Recarrega cache
                            return true;
                        } else {
                            logger.warn("FALHA: Não foi possível atualizar Discord ID. Nenhum registro encontrado para nickname " + nickname);
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("ERRO ao atualizar Discord ID para nickname " + nickname, e);
            return false;
        }
    }

    public void exportConsolidatedWhitelist() {
        try {
            synchronized (exportLock) {
                List<WhitelistedPlayer> exportList = new ArrayList<>();
                
                // 1. Add Bypassed Players
                for (java.util.UUID uuid : bypassList.keySet()) {
                    if (Boolean.TRUE.equals(bypassList.get(uuid))) {
                        // Velocity não tem getOfflinePlayer facilmente acessível sem estar online
                        // Vamos tentar pegar o nome se o jogador estiver online, senão "Unknown"
                        String name = server.getPlayer(uuid).map(p -> p.getUsername()).orElse("Unknown");
                        exportList.add(new WhitelistedPlayer(name, uuid.toString(), "BYPASS", null, java.time.LocalDate.now().toString()));
                    }
                }
                
                // 2. Add Discord Approved Players
                for (Map.Entry<String, WhitelistInfo> entry : whitelistStatus.entrySet()) {
                    String discordId = entry.getKey();
                    WhitelistInfo info = entry.getValue();
                    
                    if ("APPROVED".equalsIgnoreCase(info.status)) {
                        java.util.UUID uuid = linkManager.getUUIDFromDiscordId(discordId);
                        
                        String name = "Unknown";
                        if (uuid != null) {
                            name = server.getPlayer(uuid).map(p -> p.getUsername()).orElse(info.nickname != null ? info.nickname : "Unknown");
                        } else if (info.nickname != null) {
                            name = info.nickname;
                        }
                        
                        String uuidStr = (uuid != null) ? uuid.toString() : "NOT_LINKED";
                        
                        exportList.add(new WhitelistedPlayer(name, uuidStr, "DISCORD", discordId, info.timestamp));
                    }
                }
                
                // Uso de saveAtomic para garantir integridade do arquivo de exportação
                saveAtomic(exportFile, exportList);
            }
        } catch (Exception e) {
            logger.error("Erro ao exportar whitelist consolidada", e);
        }
    }

    // Classe interna para mapear o JSON
    private static class WhitelistInfo {
        String status;
        String timestamp;
        String reason;
        String nickname;
    }
    
    private static class WhitelistedPlayer {
        String name;
        String uuid;
        String type;
        String discordId;
        String date;
        
        public WhitelistedPlayer(String name, String uuid, String type, String discordId, String date) {
            this.name = name;
            this.uuid = uuid;
            this.type = type;
            this.discordId = discordId;
            this.date = date;
        }
    }
}
