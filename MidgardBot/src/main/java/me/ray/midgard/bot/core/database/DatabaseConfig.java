package me.ray.midgard.bot.core.database;

import java.nio.file.Path;

public class DatabaseConfig {

    private final String url;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;
    private final boolean walMode;

    private DatabaseConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.maxPoolSize = builder.maxPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.maxLifetime = builder.maxLifetime;
        this.walMode = builder.walMode;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getIdleTimeout() { return idleTimeout; }
    public long getMaxLifetime() { return maxLifetime; }
    public boolean isWalMode() { return walMode; }
    public boolean isMysql() { return url.contains("mysql"); }

    public static Builder sqlite(Path databaseFile) {
        databaseFile.getParent().toFile().mkdirs();
        return new Builder("jdbc:sqlite:" + databaseFile.toAbsolutePath());
    }

    public static Builder sqlite(String fileName) {
        return sqlite(Path.of("data", fileName));
    }

    public static Builder mysql(String host, int port, String database, String username, String password) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4";
        return new Builder(url).username(username).password(password).walMode(false);
    }

    public static class Builder {
        private final String url;
        private String username;
        private String password;
        private int maxPoolSize = 10;
        private long connectionTimeout = 30_000;
        private long idleTimeout = 600_000;
        private long maxLifetime = 1_800_000;
        private boolean walMode = true;

        private Builder(String url) {
            this.url = url;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder connectionTimeout(long millis) {
            this.connectionTimeout = millis;
            return this;
        }

        public Builder idleTimeout(long millis) {
            this.idleTimeout = millis;
            return this;
        }

        public Builder maxLifetime(long millis) {
            this.maxLifetime = millis;
            return this;
        }

        public Builder walMode(boolean walMode) {
            this.walMode = walMode;
            return this;
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
