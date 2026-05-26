package me.ray.midgardDiscord;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class DatabaseManager {
    private final Logger logger;
    private HikariDataSource dataSource;
    private final Path dataDirectory;
    private final String type;
    private final String host;
    private final String port;
    private final String database;
    private final String user;
    private final String password;
    private final boolean useSSL;

    // Regex para validação básica de segurança
    private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$");
    private static final Pattern DB_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public DatabaseManager(Logger logger, Path dataDirectory, String type, String host, String port, String database, String user, String password, boolean useSSL) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.useSSL = useSSL;
    }

    public String getType() {
        return type;
    }

    public synchronized void connect() {
        if (dataSource != null && !dataSource.isClosed()) return;

        HikariConfig config = new HikariConfig();
        config.setPoolName("MidgardDiscordPool");
        config.setRegisterMbeans(false); // Desabilitar JMX para reduzir superfície de ataque

        // Configurações de Timeout e Segurança do Pool
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000);      // 10 minutos
        config.setMaxLifetime(1800000);     // 30 minutos
        config.setLeakDetectionThreshold(60000); // Detectar leaks após 60s
        config.setKeepaliveTime(300000);    // 5 minutos - Previne fechamento por firewalls
        config.setMinimumIdle(2);           // Economia de recursos: mantém apenas 2 conexões ociosas
        config.setInitializationFailTimeout(-1); // Startup não bloqueante: não trava o servidor se o DB cair

        if ("sqlite".equalsIgnoreCase(type)) {
            if (dataDirectory == null) {
                logger.error("Diretório de dados não especificado para SQLite.");
                return;
            }
            Path dbPath = dataDirectory.resolve("midgard-data.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbPath.toString());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            // WAL Mode para melhor performance e menos travamentos em SQLite
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;");
            logger.info("Usando banco de dados SQLite: " + dbPath);
        } else if (isValidMysqlConfig()) {
            // Validação de porta
            try {
                int portNum = Integer.parseInt(port);
                if (portNum < 1 || portNum > 65535) {
                    logger.error("Porta inválida: " + port);
                    return;
                }
            } catch (NumberFormatException e) {
                logger.error("Porta não numérica: " + port);
                return;
            }

            // Configuração específica para MySQL
            config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
            
            if (!isLocalhost(host) && !useSSL) {
                logger.warn("SEGURANÇA: Conectando a host remoto (" + host + ") sem SSL. Senhas trafegam em texto plano!");
            }

            // Construção segura da URL JDBC
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(database);
            urlBuilder.append("?useSSL=").append(useSSL);
            
            // Proteções de Segurança
            urlBuilder.append("&allowLoadLocalInfile=false"); // Previne leitura de arquivos locais do servidor
            urlBuilder.append("&allowUrlInLocalInfile=false"); // Previne leitura de URLs remotas
            urlBuilder.append("&autoDeserialize=false"); // Previne RCE via desserialização
            
            // Timeouts para evitar DoS
            urlBuilder.append("&socketTimeout=30000");
            urlBuilder.append("&connectTimeout=15000");
            
            // Configurações de SSL/TLS
            if (useSSL) {
                urlBuilder.append("&requireSSL=true");
                urlBuilder.append("&verifyServerCertificate=true");
            } else {
                urlBuilder.append("&allowPublicKeyRetrieval=true"); // Necessário para MySQL 8+ sem SSL
            }
            
            urlBuilder.append("&characterEncoding=UTF-8");
            
            config.setJdbcUrl(urlBuilder.toString());
            
            if (user == null || password == null) {
                logger.error("Usuário ou senha não especificados para MySQL.");
                return;
            }
            config.setUsername(user);
            config.setPassword(password);
            
            // Tenta usar o driver mais recente, se não, deixa o Hikari detectar ou usar o antigo
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                config.setDriverClassName("com.mysql.jdbc.Driver");
            }

            // Otimizações de performance
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            config.setMaximumPoolSize(10);
            logger.info("Usando banco de dados MySQL.");
        } else {
            logger.warn("Configurações de banco de dados não encontradas ou incompletas. Usando modo JSON (Local).");
            return;
        }

        try {
            dataSource = new HikariDataSource(config);
            logger.info("Conectado ao banco de dados!");
            initTables();
        } catch (Exception e) {
            logger.error("Erro ao conectar ao banco de dados: " + e.getMessage());
        }
    }

    private void initTables() {
        String sql = "CREATE TABLE IF NOT EXISTS midgard_links (" +
                     "uuid VARCHAR(36) PRIMARY KEY, " +
                     "discord_id VARCHAR(32), " +
                     "code VARCHAR(16), " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        
        String sqlWhitelist = "CREATE TABLE IF NOT EXISTS midgard_whitelist (" +
                     "discord_id VARCHAR(32) PRIMARY KEY, " +
                     "nickname VARCHAR(64), " +
                     "uuid VARCHAR(36), " +
                     "status VARCHAR(32), " +
                     "answers TEXT, " +
                     "reason TEXT, " +
                     "terms_accepted BOOLEAN DEFAULT FALSE, " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";

        String sqlPunishments = "CREATE TABLE IF NOT EXISTS midgard_punishments (" +
                     "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
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
                     ");";
        
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(sqlWhitelist);
            
            // Adjust AUTO_INCREMENT for SQLite
            if ("sqlite".equalsIgnoreCase(type)) {
                sqlPunishments = sqlPunishments.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            }
            stmt.execute(sqlPunishments);
            
            // Index para performance em buscas por discord_id
            try {
                if ("sqlite".equalsIgnoreCase(type)) {
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_discord_id ON midgard_links(discord_id);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_whitelist_nickname ON midgard_whitelist(nickname);");
                } else {
                    // MySQL syntax might differ slightly if index exists, but usually safe to try or ignore error
                    // MySQL 5.7+ supports IF NOT EXISTS for indexes, but older versions don't.
                    // Vamos tentar criar e ignorar erro se já existir
                    try {
                        stmt.execute("CREATE INDEX idx_discord_id ON midgard_links(discord_id);");
                    } catch (SQLException ignored) {}

                    try {
                        stmt.execute("CREATE INDEX idx_whitelist_nickname ON midgard_whitelist(nickname);");
                    } catch (SQLException ignored) {}
                }
            } catch (Exception e) {
                logger.warn("Erro ao criar índice (pode ser ignorado se já existir): " + e.getMessage());
            }
            
        } catch (SQLException e) {
            logger.error("Erro ao criar tabelas: ", e);
        }
    }

    private boolean isValidMysqlConfig() {
        if (host == null || host.isEmpty() || database == null || database.isEmpty()) {
            return false;
        }
        if (!HOST_PATTERN.matcher(host).matches()) {
            logger.error("Hostname inválido (caracteres suspeitos): " + host);
            return false;
        }
        if (!DB_PATTERN.matcher(database).matches()) {
            logger.error("Nome do banco de dados inválido (caracteres suspeitos): " + database);
            return false;
        }
        return true;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("A conexão com o banco de dados não foi estabelecida.");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Erro detalhado ao obter conexão com o banco de dados: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao obter conexão com o banco de dados: ", e);
            throw new SQLException("Erro inesperado ao obter conexão", e);
        }
    }

    public boolean isConnected() {
        try {
            return dataSource != null && !dataSource.isClosed();
        } catch (Exception e) {
            logger.error("Erro ao verificar status da conexão: ", e);
            return false;
        }
    }

    public synchronized void close() {
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            logger.error("Erro detalhado ao fechar conexão com o banco de dados: ", e);
        }
    }

    public synchronized void reconnect() {
        logger.info("Reconectando ao banco de dados...");
        close();
        dataSource = null;
        connect();
    }

    private boolean isLocalhost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
