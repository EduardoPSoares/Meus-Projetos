package com.midgardbot.config;

/**
 * Constantes globais do bot.
 * Centraliza magic strings e números usados em múltiplos locais.
 */
public final class Constants {

    private Constants() {}

    // ─── Versão ───
    public static final String BOT_VERSION = "1.0.2";
    public static final String BOT_NAME = "MidgardBot";

    // ─── Limites e Timeouts ───
    public static final int MAX_WHITELIST_ATTEMPTS = 2;
    public static final long WHITELIST_REFILL_MS = 12 * 60 * 60 * 1000L; // 12h
    public static final long RATE_LIMIT_COOLDOWN_MS = 3_000L; // 3s entre comandos
    public static final long DEBOUNCE_INTERVAL_MS = 2_000L; // anti-double click
    public static final long CACHE_CLEANUP_INTERVAL_MS = 3_600_000L; // 1h
    public static final int BACKUP_RETENTION_DAYS = 7;
    public static final int BACKUP_INTERVAL_HOURS = 6;
    public static final int SOCKET_TIMEOUT_MS = 5_000; // timeout de leitura do socket
    public static final int STATUS_CHECK_INTERVAL_S = 30; // intervalo de verificação de status
    public static final int LOG_CACHE_MAX_SIZE = 100; // max entradas no cache de logs

    // ─── Database ───
    public static final String DEFAULT_DB_TYPE = "mysql";
    public static final int DEFAULT_DB_PORT = 3306;
    public static final int HIKARI_MAX_POOL_SIZE_MYSQL = 10;
    public static final int HIKARI_MAX_POOL_SIZE_SQLITE = 1;
    public static final String SQLITE_DB_PATH = "data/database.db";

    // ─── Rede ───
    public static final int DEFAULT_SOCKET_PORT = 25590;
    public static final int DEFAULT_LOBBY_PORT = 25565;
    public static final int DEFAULT_RPG_PORT = 25567;
    public static final int DEFAULT_RCON_PORT = 25575;
    public static final String DEFAULT_SERVER_IP = "127.0.0.1";
    public static final String DEFAULT_SOCKET_SECRET = "midgard_secret_key_change_me";

    // ─── Ticket ───
    public static final String DEFAULT_TICKET_TIMEZONE = "America/Sao_Paulo";
    public static final int DEFAULT_WEEKDAY_START = 12;
    public static final int DEFAULT_WEEKDAY_END = 19;
    public static final int DEFAULT_WEEKEND_START = 12;
    public static final int DEFAULT_WEEKEND_END = 14;

    // ─── Diretórios ───
    public static final String DATA_DIR = "data";
    public static final String BACKUPS_DIR = "backups";

    // ─── Cores de Embed (RGB int) ───
    public static final int COLOR_SUCCESS = 0x2ECC71;
    public static final int COLOR_ERROR = 0xE74C3C;
    public static final int COLOR_WARNING = 0xF39C12;
    public static final int COLOR_INFO = 0x3498DB;
}
