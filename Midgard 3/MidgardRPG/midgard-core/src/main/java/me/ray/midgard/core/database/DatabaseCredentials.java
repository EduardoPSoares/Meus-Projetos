package me.ray.midgard.core.database;

import java.io.File;

public record DatabaseCredentials(
    String type,
    String host,
    int port,
    String database,
    String username,
    String password,
    boolean useSsl
) {
    public String toJdbcUrl(File dataFolder) {
        if (type.equalsIgnoreCase("sqlite")) {
            String safeName = database.replaceAll("[^a-zA-Z0-9_-]", "");
            return "jdbc:sqlite:" + new File(dataFolder, safeName + ".db").getAbsolutePath();
        }
        if (!database.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database name: " + database);
        }
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=%b&characterEncoding=UTF-8",
            host, port, database, useSsl);
    }
}
