package com.midgardbot.data;

import com.midgardbot.config.BotConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;

    public static void connect() {
        if (dataSource != null && !dataSource.isClosed()) return;

        String type = BotConfig.get("DB_TYPE");
        String host = BotConfig.get("DB_HOST");
        String port = BotConfig.get("DB_PORT");
        String database = BotConfig.get("DB_NAME");
        String user = BotConfig.get("DB_USER");
        String password = BotConfig.get("DB_PASS");

        HikariConfig config = new HikariConfig();

        if ("sqlite".equalsIgnoreCase(type)) {
            // SQLite Configuration
            // Garante que a pasta data existe
            java.io.File dataFolder = new java.io.File("data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            config.setJdbcUrl("jdbc:sqlite:data/database.db");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1); // SQLite doesn't handle concurrent writes well
            LOGGER.info("Usando banco de dados SQLite.");
        } else if (host != null && !host.isEmpty() && database != null && !database.isEmpty()) {
            // MySQL Configuration
            // SSL habilitado por padrão para segurança. Configurável via DB_USE_SSL no config.env.
            String useSsl = BotConfig.get("DB_USE_SSL");
            boolean sslEnabled = useSsl == null || !"false".equalsIgnoreCase(useSsl);
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + sslEnabled
                    + "&allowPublicKeyRetrieval=true"
                    + "&autoReconnect=true"
                    + "&serverTimezone=UTC");
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            LOGGER.info("Usando banco de dados MySQL.");
        } else {
            LOGGER.warn("Configurações de banco de dados não encontradas ou incompletas. Usando modo JSON (Local).");
            return;
        }

        try {
            dataSource = new HikariDataSource(config);
            LOGGER.info("Conectado ao banco de dados!");
            createTables();
        } catch (Exception e) {
            LOGGER.error("Erro ao conectar ao banco de dados", e);
            LOGGER.error("═══════════════════════════════════════════════");
            LOGGER.error("  ERRO CRÍTICO DE CONEXÃO COM BANCO DE DADOS");
            LOGGER.error("═══════════════════════════════════════════════");
            LOGGER.error("Motivo: {}", e.getMessage());
            LOGGER.error("Verifique as configurações DB_HOST, DB_USER, DB_PASS no config.env");
            LOGGER.error("Funcionalidades afetadas: Whitelist, Logs, Tickets, Punições, Staff Feedback.");
            LOGGER.error("O BOT INICIARÁ EM MODO DEGRADADO.");
        }
    }

    private static void createTables() {
        try (Connection conn = getConnection()) {
            if (conn == null) return;

            boolean isSQLite = conn.getMetaData().getDriverName().toLowerCase().contains("sqlite");
            String autoIncrement = isSQLite ? "AUTOINCREMENT" : "AUTO_INCREMENT";
            String pkDef = isSQLite ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "INT AUTO_INCREMENT PRIMARY KEY";

            // Tabela de controle de versão do schema
            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_schema_version (" +
                "version INT PRIMARY KEY, " +
                "description VARCHAR(255), " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")")) {
                ps.executeUpdate();
            }

            int currentVersion = getSchemaVersion(conn);

            // --- Tabelas base (sempre com IF NOT EXISTS) ---

            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_whitelist (" +
                "discord_id VARCHAR(32) PRIMARY KEY, " +
                "nickname VARCHAR(64), " +
                "uuid VARCHAR(36), " +
                "status VARCHAR(32), " +
                "answers TEXT, " +
                "reason TEXT, " +
                "terms_accepted BOOLEAN DEFAULT FALSE, " +
                "staff_id VARCHAR(32), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")")) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_punishments (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                "target_identifier VARCHAR(45) NOT NULL, " +
                "target_name VARCHAR(32), " +
                "target_discord_id VARCHAR(32), " +
                "type VARCHAR(16) NOT NULL, " +
                "reason TEXT, " +
                "moderator_identifier VARCHAR(45), " +
                "moderator_name VARCHAR(32), " +
                "start_time BIGINT NOT NULL, " +
                "end_time BIGINT DEFAULT -1, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "removed_by VARCHAR(45), " +
                "removed_reason TEXT, " +
                "removed_at BIGINT DEFAULT -1" +
                ")")) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_streamers (" +
                "id " + pkDef + ", " +
                "user_id VARCHAR(32), " +
                "platform VARCHAR(32), " +
                "channel_name VARCHAR(128), " +
                "last_status BOOLEAN DEFAULT FALSE, " +
                "last_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")")) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_tickets (" +
                "id " + pkDef + ", " +
                "channel_name VARCHAR(100), " +
                "user_id VARCHAR(32), " +
                "category_name VARCHAR(64), " +
                "priority VARCHAR(10) DEFAULT 'NORMAL', " +
                "content LONGTEXT, " +
                "claimed_by VARCHAR(32), " +
                "collaborator_ids TEXT, " +
                "closed_at TIMESTAMP NULL" +
                ")")) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS midgard_links (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "discord_id VARCHAR(32), " +
                "code VARCHAR(16), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")")) {
                ps.executeUpdate();
            }

            // --- Migrações versionadas ---
            // Cada migração roda uma única vez. Novas migrações devem ser adicionadas ao final.

            if (currentVersion < 1) {
                // V1: Colunas adicionais na whitelist (para bancos criados antes do versionamento)
                addColumnIfNotExists(conn, "midgard_whitelist", "answers", "TEXT");
                addColumnIfNotExists(conn, "midgard_whitelist", "reason", "TEXT");
                addColumnIfNotExists(conn, "midgard_whitelist", "staff_id", "VARCHAR(32)");
                addColumnIfNotExists(conn, "midgard_whitelist", "terms_accepted", "BOOLEAN DEFAULT FALSE");
                recordMigration(conn, 1, "Colunas whitelist: answers, reason, staff_id, terms_accepted");
            }

            if (currentVersion < 2) {
                // V2: Tabela de relatórios da staff
                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS midgard_reports (" +
                    "id " + pkDef + ", " +
                    "author_id VARCHAR(32) NOT NULL, " +
                    "author_name VARCHAR(64), " +
                    "role_id VARCHAR(32), " +
                    "role_name VARCHAR(64), " +
                    "title VARCHAR(200) NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "activity_date VARCHAR(10) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")")) {
                    ps.executeUpdate();
                }
                recordMigration(conn, 2, "Tabela midgard_reports para relatórios da staff");
            }

            if (currentVersion < 3) {
                // V3: Ampliar activity_date para suportar "yyyy-MM-dd HH:mm"
                try (PreparedStatement ps = conn.prepareStatement(
                    "ALTER TABLE midgard_reports MODIFY COLUMN activity_date VARCHAR(20) NOT NULL")) {
                    ps.executeUpdate();
                } catch (SQLException e) {
                    // SQLite não suporta MODIFY COLUMN — ignora silenciosamente
                    LOGGER.debug("V3: ALTER ignorado (provavelmente SQLite): {}", e.getMessage());
                }
                recordMigration(conn, 3, "Ampliar activity_date para VARCHAR(20)");
            }

            if (currentVersion < 4) {
                // V4: Tabela de anexos de relatórios (imagens, vídeos, links)
                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS midgard_report_attachments (" +
                    "id " + pkDef + ", " +
                    "report_id INT NOT NULL, " +
                    "type VARCHAR(10) NOT NULL, " +  // 'image', 'video', 'link'
                    "filename VARCHAR(255), " +
                    "original_name VARCHAR(255), " +
                    "url TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")")) {
                    ps.executeUpdate();
                }
                recordMigration(conn, 4, "Tabela midgard_report_attachments para anexos de relatórios");
            }

            if (currentVersion < 5) {
                // V5: Tabela de reuniões gravadas
                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS midgard_meetings (" +
                    "id " + pkDef + ", " +
                    "title VARCHAR(200) NOT NULL, " +
                    "guild_id VARCHAR(32), " +
                    "channel_id VARCHAR(32), " +
                    "channel_name VARCHAR(100), " +
                    "started_by VARCHAR(32), " +
                    "started_by_name VARCHAR(64), " +
                    "duration_seconds INT DEFAULT 0, " +
                    "recording_filename VARCHAR(255), " +
                    "file_size_bytes BIGINT DEFAULT 0, " +
                    "notes TEXT, " +
                    "participant_count INT DEFAULT 0, " +
                    "participants TEXT, " +
                    "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "ended_at TIMESTAMP NULL" +
                    ")")) {
                    ps.executeUpdate();
                }
                recordMigration(conn, 5, "Tabela midgard_meetings para reuniões gravadas");
            }

            if (currentVersion < 6) {
                // V6: Faixas individuais de áudio por usuário em reuniões
                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS midgard_meeting_tracks (" +
                    "id " + pkDef + ", " +
                    "meeting_id INT NOT NULL, " +
                    "user_id VARCHAR(32) NOT NULL, " +
                    "user_name VARCHAR(64), " +
                    "filename VARCHAR(255), " +
                    "file_size_bytes BIGINT DEFAULT 0, " +
                    "speaking_segments TEXT" +
                    ")")) {
                    ps.executeUpdate();
                }
                recordMigration(conn, 6, "Tabela midgard_meeting_tracks para faixas individuais");
            }

            if (currentVersion < 7) {
                // V7: Coluna source para distinguir punições do plugin vs painel
                addColumnIfNotExists(conn, "midgard_punishments", "source", "VARCHAR(16) DEFAULT 'plugin'");
                recordMigration(conn, 7, "Coluna source em midgard_punishments");
            }

            if (currentVersion < 8) {
                // V8: Coluna claimed_by para rastrear qual staff assumiu o ticket
                addColumnIfNotExists(conn, "midgard_tickets", "claimed_by", "VARCHAR(32)");
                recordMigration(conn, 8, "Coluna claimed_by em midgard_tickets");
            }

            if (currentVersion < 9) {
                // V9: Tabela de logs do painel
                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS midgard_logs (" +
                    "id " + pkDef + ", " +
                    "category VARCHAR(32) NOT NULL, " +
                    "title VARCHAR(200) NOT NULL, " +
                    "message TEXT, " +
                    "icon VARCHAR(10), " +
                    "target_id VARCHAR(32), " +
                    "target_name VARCHAR(64), " +
                    "target_avatar VARCHAR(256), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")")) {
                    ps.executeUpdate();
                }
                recordMigration(conn, 9, "Tabela midgard_logs para logs do painel");
            }

            if (currentVersion < 10) {
                // V10: Coluna forum_thread_id para vincular punição ao post do fórum
                addColumnIfNotExists(conn, "midgard_punishments", "forum_thread_id", "VARCHAR(32)");
                recordMigration(conn, 10, "Coluna forum_thread_id em midgard_punishments");
            }

            if (currentVersion < 11) {
                // V11: Categoria do ticket para reabertura e painel web
                addColumnIfNotExists(conn, "midgard_tickets", "category_name", "VARCHAR(64)");
                recordMigration(conn, 11, "Coluna category_name em midgard_tickets");
            }

            if (currentVersion < 12) {
                // V12: Colaboradores do ticket para reabertura consistente
                addColumnIfNotExists(conn, "midgard_tickets", "collaborator_ids", "TEXT");
                recordMigration(conn, 12, "Coluna collaborator_ids em midgard_tickets");
            }

            LOGGER.info("Schema do banco atualizado. Versão atual: {}", Math.max(currentVersion, 12));

        } catch (SQLException e) {
            LOGGER.error("Erro ao criar tabelas", e);
        }
    }

    private static int getSchemaVersion(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(version) FROM midgard_schema_version");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1); // Retorna 0 se NULL (primeira execução)
            }
        } catch (SQLException e) {
            // Tabela pode não existir ainda no primeiro boot
        }
        return 0;
    }

    private static void recordMigration(Connection conn, int version, String description) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO midgard_schema_version (version, description) VALUES (?, ?)")) {
            ps.setInt(1, version);
            ps.setString(2, description);
            ps.executeUpdate();
            LOGGER.info("Migração V{} aplicada: {}", version, description);
        } catch (SQLException e) {
            LOGGER.error("Erro ao registrar migração V{}", version, e);
        }
    }

    private static void addColumnIfNotExists(Connection conn, String table, String column, String type) {
        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // Coluna já existe — esperado
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) return null;
        return dataSource.getConnection();
    }

    public static boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public static void disconnect() {
        close();
        LOGGER.info("Desconectado do banco de dados.");
    }

    // --- Ticket Methods ---

    public static int createTicket(String userId, String channelName) {
        return createTicket(userId, channelName, null);
    }

    public static int createTicket(String userId, String channelName, String categoryName) {
        if (!isConnected()) return -1;

        String sql = "INSERT INTO midgard_tickets (user_id, channel_name, category_name, priority, content, closed_at) VALUES (?, ?, ?, 'NORMAL', '[]', NULL)";
        
        try (Connection conn = getConnection()) {
            if (conn == null) return -1;
            
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, userId);
                ps.setString(2, channelName);
                ps.setString(3, categoryName);
                ps.executeUpdate();
                
                try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar ticket no banco", e);
        }
        return -1;
    }

    public static void updateTicket(int ticketId, String content, String priority) {
        updateTicket(ticketId, content, priority, null);
    }

    public static void updateTicket(int ticketId, String content, String priority, String claimedBy) {
        updateTicket(ticketId, content, priority, claimedBy, null);
    }

    public static void updateTicket(int ticketId, String content, String priority, String claimedBy, String collaboratorIds) {
        if (!isConnected()) return;

        // Atualiza o conteúdo, prioridade e define a data de fechamento para AGORA
        boolean isSQLite = false;
        try (Connection conn = getConnection()) {
            isSQLite = conn.getMetaData().getDriverName().toLowerCase().contains("sqlite");
        } catch (Exception e) { LOGGER.debug("Erro ao verificar tipo de banco de dados", e); }

        String sql;
        if (isSQLite) {
            sql = "UPDATE midgard_tickets SET content = ?, priority = ?, claimed_by = ?, collaborator_ids = ?, closed_at = datetime('now') WHERE id = ?";
        } else {
            sql = "UPDATE midgard_tickets SET content = ?, priority = ?, claimed_by = ?, collaborator_ids = ?, closed_at = NOW() WHERE id = ?";
        }

        try (Connection conn = getConnection()) {
            if (conn == null) return;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, content);
                ps.setString(2, priority);
                ps.setString(3, claimedBy);
                ps.setString(4, collaboratorIds);
                ps.setInt(5, ticketId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar ticket no banco", e);
        }
    }

    public static void updateTicketChannel(int ticketId, String channelName) {
        if (!isConnected()) return;

        String sql = "UPDATE midgard_tickets SET channel_name = ?, closed_at = NULL WHERE id = ?";

        try (Connection conn = getConnection()) {
            if (conn == null) return;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, channelName);
                ps.setInt(2, ticketId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar nome do canal do ticket no banco", e);
        }
    }

    public static void updateTicketCategory(int ticketId, String categoryName) {
        if (!isConnected()) return;

        String sql = "UPDATE midgard_tickets SET category_name = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, categoryName);
                ps.setInt(2, ticketId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar categoria do ticket no banco", e);
        }
    }

    public static void updateTicketClaimedBy(int ticketId, String claimedBy) {
        if (!isConnected()) return;

        String sql = "UPDATE midgard_tickets SET claimed_by = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            if (conn == null) return;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claimedBy);
                ps.setInt(2, ticketId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar claimed_by do ticket no banco", e);
        }
    }

    public static void deleteTicket(int ticketId) {
        if (!isConnected()) return;

        String sql = "DELETE FROM midgard_tickets WHERE id = ?";

        try (Connection conn = getConnection()) {
            if (conn == null) return;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, ticketId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao deletar ticket do banco", e);
        }
    }

    public static int cleanupGhostTickets() {
        if (!isConnected()) return 0;
        
        String sql = "DELETE FROM midgard_tickets WHERE channel_name = 'pending'";

        try (Connection conn = getConnection()) {
            if (conn == null) return 0;
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao limpar tickets fantasmas", e);
            return -1;
        }
    }

    public static java.util.Map.Entry<String, WhitelistStatusInfo> getWhitelistByNickname(String nickname) {
        if (!isConnected()) return null;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_whitelist WHERE LOWER(nickname) = LOWER(?)")) {
            
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String discordId = rs.getString("discord_id");
                    String statusStr = rs.getString("status");
                    String reason = rs.getString("reason");
                    String nick = rs.getString("nickname");
                    String answers = rs.getString("answers");
                    boolean terms = rs.getBoolean("terms_accepted");
                    
                    WhitelistStatus status;
                    try {
                        status = WhitelistStatus.valueOf(statusStr);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        status = WhitelistStatus.PENDING; // Fallback para evitar erro
                    }
                    
                    WhitelistStatusInfo info = new WhitelistStatusInfo(status, reason, nick, answers, terms);
                    
                    return new java.util.AbstractMap.SimpleEntry<>(discordId, info);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar whitelist por nickname: " + nickname, e);
        }
        return null;
    }

    // --- Report Methods ---

    public static int createReport(String authorId, String authorName, String roleId, String roleName,
                                   String title, String description, String activityDate) {
        if (!isConnected()) return -1;
        String sql = "INSERT INTO midgard_reports (author_id, author_name, role_id, role_name, title, description, activity_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, authorId);
                ps.setString(2, authorName);
                ps.setString(3, roleId);
                ps.setString(4, roleName);
                ps.setString(5, title);
                ps.setString(6, description);
                ps.setString(7, activityDate);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar relatório", e);
        }
        return -1;
    }

    public static List<Map<String, Object>> getReports(int limit, int offset) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_reports ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReport(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar relatórios", e);
        }
        return list;
    }

    public static List<Map<String, Object>> getReportsByRole(String roleId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_reports WHERE role_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReport(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar relatórios por cargo", e);
        }
        return list;
    }

    public static int countReports() {
        if (!isConnected()) return 0;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM midgard_reports");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("Erro ao contar relatórios", e);
        }
        return 0;
    }

    public static boolean deleteReport(int id, String authorId) {
        if (!isConnected()) return false;
        String sql = "DELETE FROM midgard_reports WHERE id = ? AND author_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, authorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Erro ao deletar relatório", e);
        }
        return false;
    }

    private static Map<String, Object> mapReport(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("authorId", rs.getString("author_id"));
        map.put("authorName", rs.getString("author_name"));
        map.put("roleId", rs.getString("role_id"));
        map.put("roleName", rs.getString("role_name"));
        map.put("title", rs.getString("title"));
        map.put("description", rs.getString("description"));
        map.put("activityDate", rs.getString("activity_date"));
        map.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null);
        return map;
    }

    // --- Report Attachment Methods ---

    public static int addReportAttachment(int reportId, String type, String filename, String originalName, String url) {
        if (!isConnected()) return -1;
        String sql = "INSERT INTO midgard_report_attachments (report_id, type, filename, original_name, url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reportId);
                ps.setString(2, type);
                ps.setString(3, filename);
                ps.setString(4, originalName);
                ps.setString(5, url);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao adicionar anexo ao relatório", e);
        }
        return -1;
    }

    public static List<Map<String, Object>> getReportAttachments(int reportId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_report_attachments WHERE report_id = ? ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> att = new LinkedHashMap<>();
                    att.put("id", rs.getInt("id"));
                    att.put("reportId", rs.getInt("report_id"));
                    att.put("type", rs.getString("type"));
                    att.put("filename", rs.getString("filename"));
                    att.put("originalName", rs.getString("original_name"));
                    att.put("url", rs.getString("url"));
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar anexos do relatório", e);
        }
        return list;
    }

    public static Map<Integer, List<Map<String, Object>>> getAttachmentsForReports(List<Integer> reportIds) {
        Map<Integer, List<Map<String, Object>>> result = new HashMap<>();
        if (!isConnected() || reportIds.isEmpty()) return result;
        String placeholders = String.join(",", reportIds.stream().map(id -> "?").toList());
        String sql = "SELECT * FROM midgard_report_attachments WHERE report_id IN (" + placeholders + ") ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < reportIds.size(); i++) {
                ps.setInt(i + 1, reportIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int reportId = rs.getInt("report_id");
                    Map<String, Object> att = new LinkedHashMap<>();
                    att.put("id", rs.getInt("id"));
                    att.put("reportId", reportId);
                    att.put("type", rs.getString("type"));
                    att.put("filename", rs.getString("filename"));
                    att.put("originalName", rs.getString("original_name"));
                    att.put("url", rs.getString("url"));
                    result.computeIfAbsent(reportId, k -> new ArrayList<>()).add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar anexos em lote", e);
        }
        return result;
    }

    public static void deleteAttachmentsByReport(int reportId) {
        if (!isConnected()) return;
        String sql = "DELETE FROM midgard_report_attachments WHERE report_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao deletar anexos do relatório", e);
        }
    }

    public static List<Map<String, Object>> getReportsByMonth(String yearMonth) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_reports WHERE activity_date LIKE ? ORDER BY role_name, activity_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yearMonth + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReport(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar relatórios por mês", e);
        }
        return list;
    }

    // --- Meeting Methods ---

    public static int createMeeting(String title, String guildId, String channelId, String channelName,
                                     String startedBy, String startedByName) {
        if (!isConnected()) return -1;
        String sql = "INSERT INTO midgard_meetings (title, guild_id, channel_id, channel_name, started_by, started_by_name) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);
                ps.setString(2, guildId);
                ps.setString(3, channelId);
                ps.setString(4, channelName);
                ps.setString(5, startedBy);
                ps.setString(6, startedByName);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar reunião", e);
        }
        return -1;
    }

    public static void finishMeeting(int meetingId, int durationSeconds, String recordingFilename,
                                      long fileSizeBytes, int participantCount, String participantsJson) {
        if (!isConnected()) return;
        String sql = "UPDATE midgard_meetings SET duration_seconds = ?, recording_filename = ?, " +
                     "file_size_bytes = ?, participant_count = ?, participants = ?, ended_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, durationSeconds);
            ps.setString(2, recordingFilename);
            ps.setLong(3, fileSizeBytes);
            ps.setInt(4, participantCount);
            ps.setString(5, participantsJson);
            ps.setInt(6, meetingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao finalizar reunião", e);
        }
    }

    public static void updateMeetingNotes(int meetingId, String notes) {
        if (!isConnected()) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE midgard_meetings SET notes = ? WHERE id = ?")) {
            ps.setString(1, notes);
            ps.setInt(2, meetingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar notas da reunião", e);
        }
    }

    public static List<Map<String, Object>> getMeetings(int limit, int offset) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_meetings ORDER BY started_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMeeting(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar reuniões", e);
        }
        return list;
    }

    public static Map<String, Object> getMeetingById(int id) {
        if (!isConnected()) return null;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_meetings WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMeeting(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao buscar reunião", e);
        }
        return null;
    }

    public static int countMeetings() {
        if (!isConnected()) return 0;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM midgard_meetings");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("Erro ao contar reuniões", e);
        }
        return 0;
    }

    public static boolean deleteMeeting(int id) {
        if (!isConnected()) return false;
        try (Connection conn = getConnection()) {
            // Deletar faixas individuais primeiro
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_meeting_tracks WHERE meeting_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            // Deletar reunião
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_meetings WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao deletar reunião", e);
        }
        return false;
    }

    // --- Meeting Track Methods ---

    public static void createMeetingTrack(int meetingId, String userId, String userName,
                                           String filename, long fileSizeBytes, String speakingSegments) {
        if (!isConnected()) return;
        String sql = "INSERT INTO midgard_meeting_tracks (meeting_id, user_id, user_name, filename, file_size_bytes, speaking_segments) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, meetingId);
            ps.setString(2, userId);
            ps.setString(3, userName);
            ps.setString(4, filename);
            ps.setLong(5, fileSizeBytes);
            ps.setString(6, speakingSegments);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar faixa de reunião", e);
        }
    }

    public static List<Map<String, Object>> getMeetingTracks(int meetingId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isConnected()) return list;
        String sql = "SELECT * FROM midgard_meeting_tracks WHERE meeting_id = ? ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, meetingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> track = new LinkedHashMap<>();
                    track.put("id", rs.getInt("id"));
                    track.put("meetingId", rs.getInt("meeting_id"));
                    track.put("userId", rs.getString("user_id"));
                    track.put("userName", rs.getString("user_name"));
                    track.put("filename", rs.getString("filename"));
                    track.put("fileSizeBytes", rs.getLong("file_size_bytes"));
                    track.put("speakingSegments", rs.getString("speaking_segments"));
                    list.add(track);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao listar faixas de reunião", e);
        }
        return list;
    }

    private static Map<String, Object> mapMeeting(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("title", rs.getString("title"));
        map.put("guildId", rs.getString("guild_id"));
        map.put("channelId", rs.getString("channel_id"));
        map.put("channelName", rs.getString("channel_name"));
        map.put("startedBy", rs.getString("started_by"));
        map.put("startedByName", rs.getString("started_by_name"));
        map.put("durationSeconds", rs.getInt("duration_seconds"));
        map.put("recordingFilename", rs.getString("recording_filename"));
        map.put("fileSizeBytes", rs.getLong("file_size_bytes"));
        map.put("notes", rs.getString("notes"));
        map.put("participantCount", rs.getInt("participant_count"));
        map.put("participants", rs.getString("participants"));
        map.put("startedAt", rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toString() : null);
        map.put("endedAt", rs.getTimestamp("ended_at") != null ? rs.getTimestamp("ended_at").toString() : null);
        return map;
    }
}
