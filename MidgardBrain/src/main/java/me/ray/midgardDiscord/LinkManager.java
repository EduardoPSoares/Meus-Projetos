package me.ray.midgardDiscord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Vínculo de Contas (Link).
 * Mapeia UUIDs de jogadores Minecraft para IDs de usuários do Discord.
 * Utiliza arquivos JSON compartilhados com o bot para persistência.
 */
public class LinkManager {

    private final MidgardVelocity plugin;
    private final Logger logger;
    private final DatabaseManager dbManager;
    private final File linkedFile;
    private final File pendingFile;
    private final Gson gson;
    
    private long linkedLastModified = 0;
    private long pendingLastModified = 0;
    
    // UUID do Minecraft -> ID do Discord
    private Map<UUID, String> linkedAccounts = new ConcurrentHashMap<>();
    
    public static class PendingLink {
        public UUID uuid;
        public long timestamp;

        public PendingLink(UUID uuid) {
            this.uuid = uuid;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Código -> PendingLink
    private Map<String, PendingLink> pendingCodes = new ConcurrentHashMap<>();

    // Cooldown para evitar spam de geração de códigos (UUID -> Timestamp)
    private Map<UUID, Long> lastLinkRequest = new ConcurrentHashMap<>();

    // SecureRandom é criptograficamente seguro, impedindo previsão de códigos
    private final SecureRandom random = new SecureRandom();

    public LinkManager(MidgardVelocity plugin, Logger logger, DatabaseManager dbManager, File dataFolder) {
        this.plugin = plugin;
        this.logger = logger;
        this.dbManager = dbManager;
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.linkedFile = new File(dataFolder, "linked_accounts.json");
        this.pendingFile = new File(dataFolder, "pending_codes.json");
        
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Otimização de Memória: Se estiver usando banco de dados, não carrega o JSON em memória
        if (!dbManager.isConnected()) {
            loadLinked();
        } else {
            logger.info("Usando banco de dados para links. Arquivo JSON local será ignorado para economizar memória.");
        }
        
        loadPending();
        startCleanupTask();
    }

    private void startCleanupTask() {
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                try {
                    long now = System.currentTimeMillis();
                    long expirationTime = 10 * 60 * 1000; // 10 minutos
                    boolean changed = false;
                    
                    Iterator<Map.Entry<String, PendingLink>> it = pendingCodes.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, PendingLink> entry = it.next();
                        if (now - entry.getValue().timestamp > expirationTime) {
                            it.remove();
                            changed = true;
                        }
                    }
                    
                    if (changed) {
                        savePending();
                    }
                    
                    // Limpa cooldowns antigos também
                    lastLinkRequest.entrySet().removeIf(entry -> now - entry.getValue() > 60000); // 1 minuto
                    
                } catch (Exception e) {
                    logger.error("Erro na task de limpeza do LinkManager: ", e);
                }
            })
            .repeat(1, java.util.concurrent.TimeUnit.MINUTES)
            .schedule();
    }

    public boolean isLinked(UUID uuid) {
        try {
            if (dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM midgard_links WHERE uuid = ? AND discord_id IS NOT NULL")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                } catch (SQLException e) {
                    logger.error("Erro ao verificar link no banco: ", e);
                    return false;
                }
            } else {
                loadLinked(); // Recarrega para ver se o Bot atualizou
                return linkedAccounts.containsKey(uuid);
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em isLinked: ", e);
            return false;
        }
    }
    
    public String getDiscordId(UUID uuid) {
        try {
            if (dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT discord_id FROM midgard_links WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("discord_id");
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Erro ao obter Discord ID no banco: ", e);
                }
                return null;
            } else {
                loadLinked();
                return linkedAccounts.get(uuid);
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em getDiscordId: ", e);
            return null;
        }
    }

    public UUID getUUIDFromDiscordId(String discordId) {
        try {
            if (dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM midgard_links WHERE discord_id = ?")) {
                    ps.setString(1, discordId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return UUID.fromString(rs.getString("uuid"));
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Erro ao obter UUID no banco: ", e);
                }
                return null;
            } else {
                loadLinked();
                for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
                    if (entry.getValue().equals(discordId)) {
                        return entry.getKey();
                    }
                }
                return null;
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em getUUIDFromDiscordId: ", e);
            return null;
        }
    }

    public String generateCode(UUID uuid) {
        // Cooldown de 5 segundos
        Long lastRequest = lastLinkRequest.get(uuid);
        if (lastRequest != null && System.currentTimeMillis() - lastRequest < 5000) {
            return null; // Retorna null para indicar que deve esperar (o comando deve tratar isso)
        }
        lastLinkRequest.put(uuid, System.currentTimeMillis());

        try {
            if (dbManager.isConnected()) {
                // Limpeza de códigos expirados no banco (opcional, mas boa prática se tiver permissão de DELETE)
                // ...
                
                String code = generateRandomCode();
                String sql;
                
                if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                    sql = "INSERT INTO midgard_links (uuid, code, created_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                          "ON CONFLICT(uuid) DO UPDATE SET code = excluded.code, created_at = CURRENT_TIMESTAMP";
                } else {
                    sql = "INSERT INTO midgard_links (uuid, code, created_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                          "ON DUPLICATE KEY UPDATE code = ?, created_at = CURRENT_TIMESTAMP";
                }
                
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, code);
                    } else {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, code);
                        ps.setString(3, code);
                    }
                    
                    ps.executeUpdate();
                    return code;
                } catch (SQLException e) {
                    // Fallback para tabelas antigas (sem created_at)
                    logger.warn("Tentando fallback de SQL (tabela antiga?): " + e.getMessage());
                    try {
                        if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                            sql = "INSERT INTO midgard_links (uuid, code) VALUES (?, ?) " +
                                  "ON CONFLICT(uuid) DO UPDATE SET code = excluded.code";
                        } else {
                            sql = "INSERT INTO midgard_links (uuid, code) VALUES (?, ?) " +
                                  "ON DUPLICATE KEY UPDATE code = ?";
                        }
                        
                        try (Connection conn = dbManager.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                             
                            if ("sqlite".equalsIgnoreCase(dbManager.getType())) {
                                ps.setString(1, uuid.toString());
                                ps.setString(2, code);
                            } else {
                                ps.setString(1, uuid.toString());
                                ps.setString(2, code);
                                ps.setString(3, code);
                            }
                            ps.executeUpdate();
                            return code;
                        }
                    } catch (SQLException ex) {
                        logger.error("Erro ao gerar código no banco (Fallback falhou): ", ex);
                        return null;
                    }
                }
            } else {
                loadPending(); // Recarrega códigos existentes
                cleanupExpiredCodes(); // Remove códigos velhos da memória para evitar vazamento
                
                // Verifica se já existe um código válido para este UUID para evitar spam de disco
                for (Map.Entry<String, PendingLink> entry : pendingCodes.entrySet()) {
                    if (entry.getValue().uuid.equals(uuid)) {
                        // Se o código ainda é válido (menos de 10 min), retorna ele mesmo
                        if (System.currentTimeMillis() - entry.getValue().timestamp < 600000) {
                            return entry.getKey();
                        }
                    }
                }
                
                // Remove qualquer código anterior (expirado ou inválido) para este UUID
                pendingCodes.entrySet().removeIf(entry -> entry.getValue().uuid.equals(uuid));
                
                // Proteção contra DoS: Limite máximo de códigos pendentes
                if (pendingCodes.size() >= 3000) {
                    // Remove o mais antigo para liberar espaço
                    String oldestCode = null;
                    long oldestTime = Long.MAX_VALUE;
                    for (Map.Entry<String, PendingLink> entry : pendingCodes.entrySet()) {
                        if (entry.getValue().timestamp < oldestTime) {
                            oldestTime = entry.getValue().timestamp;
                            oldestCode = entry.getKey();
                        }
                    }
                    if (oldestCode != null) {
                        pendingCodes.remove(oldestCode);
                    }
                }
                
                String code = null;
                for (int attempts = 0; attempts < 10; attempts++) {
                    String candidate = generateRandomCode();
                    if (pendingCodes.putIfAbsent(candidate, new PendingLink(uuid)) == null) {
                        code = candidate;
                        break;
                    }
                }
                
                if (code == null) {
                    logger.warn("Falha ao gerar código único após 10 tentativas.");
                    return null;
                }
                
                savePending(); // Salva para o Bot ler
                return code;
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em generateCode: ", e);
            return null;
        }
    }

    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        long expirationTime = 600000; // 10 minutos
        
        Iterator<Map.Entry<String, PendingLink>> it = pendingCodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PendingLink> entry = it.next();
            if (now - entry.getValue().timestamp > expirationTime) {
                it.remove();
            }
        }

        // Limpeza do mapa de cooldown (remove entradas com mais de 1 minuto)
        lastLinkRequest.entrySet().removeIf(entry -> now - entry.getValue() > 60000);
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Método removido por segurança (não utilizado pelo plugin, a verificação é feita pelo Bot)
    /*
    public UUID verifyCode(String code) {
        ...
    }
    */

    public void linkAccount(UUID uuid, String discordId) {
        // Validação defensiva do ID do Discord
        if (discordId == null || !discordId.matches("\\d{17,20}")) {
            logger.warn("Tentativa de vincular ID do Discord inválido: " + discordId);
            return;
        }

        try {
            if (dbManager.isConnected()) {
                 try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE midgard_links SET discord_id = ?, code = NULL WHERE uuid = ?")) {
                    ps.setString(1, discordId);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Erro ao vincular conta no banco: ", e);
                }
            } else {
                linkedAccounts.put(uuid, discordId);
                // Remove o código usado
                pendingCodes.entrySet().removeIf(entry -> entry.getValue().uuid.equals(uuid));
                saveLinked();
                savePending();
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em linkAccount: ", e);
        }
    }

    public void unlinkAccount(UUID uuid) {
        try {
            if (dbManager.isConnected()) {
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("UPDATE midgard_links SET discord_id = NULL WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Erro ao desvincular conta no banco: ", e);
                }
            } else {
                if (linkedAccounts.containsKey(uuid)) {
                    linkedAccounts.remove(uuid);
                    saveLinked();
                }
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em unlinkAccount: ", e);
        }
    }

    private void loadLinked() {
        try {
            if (!linkedFile.exists()) return;
            
            if (linkedFile.length() > 10 * 1024 * 1024) { // 10MB (pode ter muitos links)
                logger.error("Arquivo de links muito grande (>10MB). Ignorando.");
                return;
            }
            
            long currentModified = linkedFile.lastModified();
            if (currentModified <= linkedLastModified) return;
            linkedLastModified = currentModified;

            try (Reader reader = new FileReader(linkedFile)) {
                Type type = new TypeToken<Map<UUID, String>>(){}.getType();
                Map<UUID, String> data = gson.fromJson(reader, type);
                if (data != null) {
                    linkedAccounts = new ConcurrentHashMap<>(data);
                }
            } catch (IOException e) {
                logger.error("Erro ao carregar contas vinculadas: ", e);
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em loadLinked: ", e);
        }
    }

    private void saveLinked() {
        try {
            saveAtomic(linkedFile, linkedAccounts);
            linkedLastModified = linkedFile.lastModified();
        } catch (Exception e) {
            logger.error("Erro inesperado em saveLinked: ", e);
        }
    }
    
    private void loadPending() {
        try {
            if (!pendingFile.exists()) return;
            
            if (pendingFile.length() > 2 * 1024 * 1024) { // 2MB
                logger.error("Arquivo de códigos pendentes muito grande (>2MB). Resetando.");
                pendingFile.delete();
                return;
            }
            
            long currentModified = pendingFile.lastModified();
            if (currentModified <= pendingLastModified) return;
            pendingLastModified = currentModified;

            try (Reader reader = new FileReader(pendingFile)) {
                Type type = new TypeToken<Map<String, PendingLink>>(){}.getType();
                Map<String, PendingLink> data = gson.fromJson(reader, type);
                if (data != null) {
                    pendingCodes = new ConcurrentHashMap<>(data);
                }
            } catch (Exception e) {
                logger.warn("Erro ao carregar códigos pendentes (possível formato antigo), resetando arquivo: " + e.getMessage());
                pendingCodes.clear();
                savePending();
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em loadPending: ", e);
        }
    }

    private void savePending() {
        try {
            saveAtomic(pendingFile, pendingCodes);
            pendingLastModified = pendingFile.lastModified();
        } catch (Exception e) {
            logger.error("Erro inesperado em savePending: ", e);
        }
    }
    
    private synchronized void saveAtomic(File file, Object data) {
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
                        logger.error("Erro ao salvar arquivo " + file.getName() + " após 3 tentativas: ", e);
                    } else {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro inesperado em saveAtomic: ", e);
        }
    }
}
