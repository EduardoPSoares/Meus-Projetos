package com.midgardbot.features.link;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.features.sync.RoleSyncManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Vínculo (Lado do Bot).
 * Verifica os códigos gerados pelo servidor Minecraft e realiza a associação com o ID do Discord.
 */
public class LinkManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkManager.class);
    private static final File DATA_FOLDER = new File("data");
    private static final File PENDING_FILE = new File(DATA_FOLDER, "pending_codes.json");
    private static final File LINKED_FILE = new File(DATA_FOLDER, "linked_accounts.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Código -> UUID (Lido do arquivo gerado pelo Minecraft)
    private static Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
    
    // UUID -> DiscordID (Arquivo compartilhado)
    private static Map<UUID, String> linkedAccounts = new ConcurrentHashMap<>();

    static {
        if (!DATA_FOLDER.exists()) DATA_FOLDER.mkdirs();
        loadPending();
        loadLinked();
    }

    public static UUID verifyCode(String code) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM midgard_links WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("uuid"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao verificar código no banco", e);
            }
            return null;
        } else {
            loadPending(); // Recarrega para pegar códigos novos do Minecraft
            return pendingCodes.get(code);
        }
    }

    public static boolean linkAccount(String code, String discordId) {
        UUID uuid = verifyCode(code);
        if (uuid == null) return false;

        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection()) {
                // Atualiza o registro para remover o código e adicionar o discord_id
                try (PreparedStatement ps = conn.prepareStatement("UPDATE midgard_links SET discord_id = ?, code = NULL WHERE uuid = ?")) {
                    ps.setString(1, discordId);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
                
                // Processa cargos pendentes
                try {
                    RoleSyncManager.processPending(discordId, uuid.toString());
                } catch (Throwable t) {
                    LOGGER.error("Erro ao processar sync de cargos para " + discordId, t);
                }
                return true;
            } catch (SQLException e) {
                LOGGER.error("Erro ao vincular conta no banco", e);
                return false;
            }
        } else {
            linkedAccounts.put(uuid, discordId);
            pendingCodes.remove(code);
            
            saveLinked();
            savePending();
            
            // Processa cargos pendentes
            com.midgardbot.features.sync.RoleSyncManager.processPending(discordId, uuid.toString());
            
            return true;
        }
    }

    public static boolean forceLink(String discordId, UUID uuid) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection()) {
                // Tenta atualizar primeiro (caso já exista um registro com esse UUID, ex: gerou código mas não vinculou)
                try (PreparedStatement ps = conn.prepareStatement("UPDATE midgard_links SET discord_id = ?, code = NULL WHERE uuid = ?")) {
                    ps.setString(1, discordId);
                    ps.setString(2, uuid.toString());
                    int rows = ps.executeUpdate();
                    
                    if (rows == 0) {
                        // Se não atualizou nada, insere novo
                        try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO midgard_links (uuid, discord_id, code) VALUES (?, ?, NULL)")) {
                            psInsert.setString(1, uuid.toString());
                            psInsert.setString(2, discordId);
                            psInsert.executeUpdate();
                        }
                    }
                }
                
                // Processa cargos pendentes
                try {
                    RoleSyncManager.processPending(discordId, uuid.toString());
                } catch (Throwable t) {
                    LOGGER.error("Erro ao processar sync de cargos para " + discordId, t);
                }
                return true;
            } catch (SQLException e) {
                LOGGER.error("Erro ao forçar vínculo de conta no banco", e);
                return false;
            }
        } else {
            linkedAccounts.put(uuid, discordId);
            // Remove de pendentes se estiver lá (pelo UUID)
            pendingCodes.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
            
            saveLinked();
            savePending();
            
            // Processa cargos pendentes
            com.midgardbot.features.sync.RoleSyncManager.processPending(discordId, uuid.toString());
            
            return true;
        }
    }
    
    public static boolean isLinked(String discordId) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM midgard_links WHERE discord_id = ?")) {
                ps.setString(1, discordId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao verificar link no banco", e);
                return false;
            }
        } else {
            return linkedAccounts.containsValue(discordId);
        }
    }

    public static UUID getUUID(String discordId) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM midgard_links WHERE discord_id = ?")) {
                ps.setString(1, discordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("uuid"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao obter UUID no banco", e);
            }
            return null;
        } else {
            for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
                if (entry.getValue().equals(discordId)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    public static String getDiscordId(UUID uuid) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT discord_id FROM midgard_links WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao obter Discord ID no banco", e);
            }
            return null;
        } else {
            return linkedAccounts.get(uuid);
        }
    }

    public static boolean unlinkAccount(String discordId) {
        if (DatabaseManager.isConnected()) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_links WHERE discord_id = ?")) {
                ps.setString(1, discordId);
                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                LOGGER.error("Erro ao desvincular conta no banco", e);
                return false;
            }
        } else {
            UUID uuidToRemove = null;
            for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
                if (entry.getValue().equals(discordId)) {
                    uuidToRemove = entry.getKey();
                    break;
                }
            }

            if (uuidToRemove != null) {
                linkedAccounts.remove(uuidToRemove);
                saveLinked();
                return true;
            }
            return false;
        }
    }

    private static synchronized void loadPending() {
        if (!PENDING_FILE.exists()) return;
        try (Reader reader = new FileReader(PENDING_FILE)) {
            Type type = new TypeToken<Map<String, UUID>>(){}.getType();
            Map<String, UUID> data = gson.fromJson(reader, type);
            if (data != null) {
                pendingCodes = new ConcurrentHashMap<>(data);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Erro ao carregar códigos pendentes", e);
        }
    }

    private static synchronized void savePending() {
        File tmpFile = new File(DATA_FOLDER, "pending_codes.json.tmp");
        try (Writer writer = new FileWriter(tmpFile)) {
            gson.toJson(pendingCodes, writer);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar códigos pendentes", e);
            return;
        }
        try {
            java.nio.file.Files.move(tmpFile.toPath(), PENDING_FILE.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Falha ao renomear arquivo temporário de códigos pendentes", e);
        }
    }

    private static synchronized void loadLinked() {
        if (!LINKED_FILE.exists()) return;
        try (Reader reader = new FileReader(LINKED_FILE)) {
            Type type = new TypeToken<Map<UUID, String>>(){}.getType();
            Map<UUID, String> data = gson.fromJson(reader, type);
            if (data != null) {
                linkedAccounts = new ConcurrentHashMap<>(data);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Erro ao carregar contas vinculadas", e);
        }
    }

    private static synchronized void saveLinked() {
        File tmpFile = new File(DATA_FOLDER, "linked_accounts.json.tmp");
        try (Writer writer = new FileWriter(tmpFile)) {
            gson.toJson(linkedAccounts, writer);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar contas vinculadas", e);
            return;
        }
        try {
            java.nio.file.Files.move(tmpFile.toPath(), LINKED_FILE.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Falha ao renomear arquivo temporário de contas vinculadas", e);
        }
    }

    public static Map<UUID, String> getAllLinkedAccounts() {
        if (DatabaseManager.isConnected()) {
             Map<UUID, String> result = new ConcurrentHashMap<>();
             try (Connection conn = DatabaseManager.getConnection();
                  PreparedStatement ps = conn.prepareStatement("SELECT uuid, discord_id FROM midgard_links")) {
                 try (ResultSet rs = ps.executeQuery()) {
                     while (rs.next()) {
                         String uuidStr = rs.getString("uuid");
                         String discordId = rs.getString("discord_id");
                         if (uuidStr != null && discordId != null) {
                             result.put(UUID.fromString(uuidStr), discordId);
                         }
                     }
                 }
             } catch (SQLException e) {
                 LOGGER.error("Erro ao obter todas contas vinculadas do banco", e);
             }
             return result;
        } else {
             return new ConcurrentHashMap<>(linkedAccounts);
        }
    }
}
