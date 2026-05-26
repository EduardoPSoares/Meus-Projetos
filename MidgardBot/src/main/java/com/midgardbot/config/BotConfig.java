package com.midgardbot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Gerencia a configuração do bot carregando variáveis de ambiente.
 * Utiliza a biblioteca Dotenv para ler o arquivo .env.
 */
public class BotConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfig.class);

    private static Dotenv dotenv;
    private static java.util.Map<String, String> manualConfig = new java.util.HashMap<>();
    private static String activeProfile = null;
    private static boolean initialized = false;

    /**
     * Inicializa a configuração com o perfil especificado.
     * @param profile Nome do perfil (ex: "test") ou null para produção
     */
    public static void init(String profile) {
        initialized = true;
        activeProfile = profile;
        manualConfig.clear();
        dotenv = null;

        LOGGER.debug("Iniciando configuração...");

        // Determina o nome do arquivo de configuração baseado no perfil
        String configFileName;
        if (profile != null && !profile.isEmpty()) {
            configFileName = "config." + profile + ".env";
            LOGGER.info("Perfil ativo: {} (arquivo: {})", profile, configFileName);
        } else {
            configFileName = "config.env";
        }

        File envFile = new File(".env");
        File configFile = new File(configFileName);

        // Verifica se o arquivo do perfil existe
        if (profile != null && !profile.isEmpty() && !configFile.exists()) {
            LOGGER.error("Arquivo de configuração do perfil '{}' não encontrado: {}", profile, configFileName);
            LOGGER.info("Crie o arquivo {} com as configurações do servidor de teste.", configFileName);
            LOGGER.info("Você pode copiar o config.env.example como base.");
        }

        // 2. Gerar config.env se não existir (e .env também não) - Apenas para perfil padrão
        if (profile == null && !configFile.exists() && !envFile.exists()) {
            LOGGER.info("Nenhum arquivo de configuração encontrado. Gerando config.env padrão...");
            createDefaultConfig(configFile);
        }

        // 3. Carregar configurações
        try {
            // Carrega manualmente primeiro para garantir prioridade do arquivo local sobre variáveis de ambiente
            if (configFile.exists()) {
                manualLoad(configFile);
            }

            if (profile == null && envFile.exists()) {
                dotenv = Dotenv.configure().filename(".env").ignoreIfMissing().load();
                LOGGER.info("Configurações carregadas de .env.");
            } else if (configFile.exists()) {
                dotenv = Dotenv.configure().filename(configFileName).ignoreIfMissing().load();
                LOGGER.info("Configurações carregadas de {}.", configFileName);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar {}: {}", configFileName, e.getMessage());
            LOGGER.warn("O arquivo {} parece conter erros de formatação ou caracteres inválidos.", configFileName);
            LOGGER.warn("Verifique se não há chaves duplicadas ou valores mal formatados.");
            
            // Se falhar e não tiver carregado manualmente ainda, tenta agora
            if (manualConfig.isEmpty()) {
                LOGGER.info("Tentando carregar configurações manualmente (Modo de Recuperação)...");
                manualLoad(configFile);
            }
        }
    }

    /**
     * Retorna o perfil ativo atual.
     * @return Nome do perfil ou null se for produção
     */
    public static String getActiveProfile() {
        return activeProfile;
    }

    /**
     * Verifica se está rodando em modo de teste.
     */
    public static boolean isTestMode() {
        return activeProfile != null && !activeProfile.isEmpty();
    }

    private static void manualLoad(File file) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    // put substitui valores anteriores, resolvendo duplicatas automaticamente
                    manualConfig.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Falha no carregamento manual: {}", e.getMessage());
        }
    }

    private static void createDefaultConfig(File file) {
        if (file.exists() && file.length() > 0) {
            LOGGER.warn("Tentativa de sobrescrever config.env existente ignorada.");
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# Bot Configuration\n");
            writer.write("TOKEN=\n");
            writer.write("PREFIX=!\n");
            writer.write("\n");
            writer.write("# Channels\n");
            writer.write("STAFF_CHANNEL_ID=\n");
            writer.write("RESULTS_CHANNEL_ID=\n");
            writer.write("LOG_CHANNEL_ID=\n");
            writer.write("PUNISHMENT_CHANNEL_ID=\n");
            writer.write("BACKUP_CHANNEL_ID=\n");
            writer.write("WELCOME_CHANNEL_ID=\n");
            writer.write("STAFF_FEEDBACK_CHANNEL_ID=\n");
            writer.write("STREAM_ANNOUNCE_CHANNEL_ID=\n");
            writer.write("SECURITY_CHANNEL_ID=\n");
            writer.write("\n");
            writer.write("# Streamer API Keys\n");
            writer.write("TWITCH_CLIENT_ID=\n");
            writer.write("TWITCH_CLIENT_SECRET=\n");
            writer.write("YOUTUBE_API_KEY=\n");
            writer.write("\n");
            writer.write("# Roles\n");
            writer.write("STREAM_NOTIFICATION_ROLE_ID=\n");
            writer.write("CITIZEN_ROLE_ID=\n");
            writer.write("NOTIFICATION_ROLE_ID=\n");
            writer.write("UNDER_AGE_ROLE_ID=\n");
            writer.write("ANTINUKE_BYPASS_ROLE_ID=\n");
            writer.write("CHAT_BYPASS_ROLE_IDS=\n");
            writer.write("\n");
            writer.write("# Permissões (Controle de Acesso)\n");
            writer.write("# IDs de cargos separados por vírgula\n");
            writer.write("PERM_CMD_BAN=\n");
            writer.write("PERM_CMD_KICK=\n");
            writer.write("PERM_CMD_WARN=\n");
            writer.write("PERM_CMD_UNWARN=\n");
            writer.write("PERM_CMD_UNBAN=\n");
            writer.write("PERM_CMD_TEMPBAN=\n");
            writer.write("PERM_CMD_CLEAR=\n");
            writer.write("PERM_CMD_MAINTENANCE=\n");
            writer.write("PERM_CMD_LOCKDOWN=\n");
            writer.write("PERM_CMD_BACKUP=\n");
            writer.write("PERM_CMD_BLACKLIST=\n");
            writer.write("PERM_CMD_BYPASS=\n");
            writer.write("PERM_CMD_WHITELIST_SETUP=\n");
            writer.write("PERM_CMD_WHITELIST_REMOVE=\n");
            writer.write("PERM_CMD_WHITELIST_RESET=\n");
            writer.write("PERM_CMD_WHITELIST_FORCE=\n");
            writer.write("PERM_CMD_WHITELIST_REFRESH=\n");
            writer.write("PERM_CMD_WHITELIST_TOGGLE=\n");
            writer.write("PERM_CMD_WHITELIST_INFO=\n");
            writer.write("PERM_CMD_REVIEW=\n");
            writer.write("PERM_CMD_REVIEW_SETUP=\n");
            writer.write("PERM_CMD_STAFFSTATS=\n");
            writer.write("PERM_CMD_DEBUG=\n");
            writer.write("PERM_CMD_TICKET_SETUP=\n");
            writer.write("PERM_CMD_TICKET_CLEAR=\n");
            writer.write("PERM_CMD_LINK_SETUP=\n");
            writer.write("PERM_CMD_LINK_FORCE=\n");
            writer.write("PERM_CMD_SETNICK=\n");
            writer.write("PERM_CMD_LEAVEGUILDS=\n");
            writer.write("PERM_CMD_POLL=\n");
            writer.write("PERM_CMD_REPORT=\n");
            writer.write("PERM_CMD_RELOAD=\n");
            writer.write("PERM_CMD_STRESS=\n");
            writer.write("PERM_CMD_FINDUSER=\n");
            writer.write("PERM_CMD_FEEDBACK_UPDATE=\n");
            writer.write("PERM_CMD_LIMIT=\n");
            writer.write("PERM_CMD_LISTGUILDS=\n");
            writer.write("PERM_CMD_PENDING=\n");
            writer.write("PERM_CMD_FEEDBACK=\n");
            writer.write("PERM_CMD_STREAMER=\n");
            writer.write("PERM_CMD_BOTINFO=\n");
            writer.write("\n");
            writer.write("# Intimação (Sistema de Audiências)\n");
            writer.write("INTIMACAO_CATEGORY_ID=\n");
            writer.write("INTIMACAO_LOG_CHANNEL_ID=\n");
            writer.write("PERM_CMD_INTIMAR=\n");
            writer.write("\n");

            writer.write("# Ticket Categories\n");
            writer.write("TICKET_CATEGORY_SUPPORT=\n");
            writer.write("TICKET_CATEGORY_REPORT=\n");
            writer.write("TICKET_CATEGORY_BUG=\n");
            writer.write("TICKET_CATEGORY_LORE=\n");
            writer.write("TICKET_CATEGORY_LOG=\n");
            writer.write("\n");
            writer.write("# Ticket Roles (IDs of roles to ping/manage)\n");
            writer.write("TICKET_SUPPORT_ROLES=\n");
            writer.write("TICKET_ROLE_SUPPORT=\n");
            writer.write("TICKET_ROLE_REPORT=\n");
            writer.write("TICKET_ROLE_BUG=\n");
            writer.write("TICKET_ROLE_LORE=\n");
            writer.write("\n");
            writer.write("# Ticket Schedule (Business Hours)\n");
            writer.write("TICKET_SCHEDULE_ENABLED=true\n");
            writer.write("TICKET_SCHEDULE_TIMEZONE=America/Sao_Paulo\n");
            writer.write("TICKET_WEEKDAY_START=12\n");
            writer.write("TICKET_WEEKDAY_END=19\n");
            writer.write("TICKET_WEEKEND_START=12\n");
            writer.write("TICKET_WEEKEND_END=14\n");
            writer.write("\n");
            writer.write("# Server Status\n");
            writer.write("STATUS_CHANNEL_ID=\n");
            writer.write("\n");
            writer.write("# Server Ports\n");
            writer.write("SERVER_IP=127.0.0.1\n");
            writer.write("LOBBY_PORT=25565\n");
            writer.write("RPG_PORT=25568\n");
            writer.write("\n");
            writer.write("SOCKET_PORT=25590\n");
            writer.write("SOCKET_SECRET=midgard_secret_key_change_me\n");
            writer.write("\n");
            writer.write("# Database Configuration\n");
            writer.write("# Tipos suportados: mysql, sqlite. Se não definido ou host vazio, usa JSON (local).\n");
            writer.write("DB_TYPE=mysql\n");
            writer.write("DB_HOST=\n");
            writer.write("DB_PORT=3306\n");
            writer.write("DB_NAME=\n");
            writer.write("DB_USER=\n");
            writer.write("DB_PASS=\n");
            LOGGER.info("Arquivo config.env criado com sucesso! Carregando configurações...");
        } catch (IOException e) {
            LOGGER.error("Falha ao criar config.env: {}", e.getMessage());
        }
    }

    /**
     * Obtém o valor de uma variável de ambiente.
     * Tenta ler do arquivo .env, e se não encontrar, tenta das variáveis de sistema.
     * @param key A chave da variável (ex: "TOKEN")
     * @return O valor da variável ou null se não existir
     */
    public static String get(String key) {
        // Lazy init: se ninguém chamou init() ainda, carrega config padrão
        if (!initialized) {
            init(null);
        }
        String value = null;
        
        // 1. Prioridade: Configuração manual (arquivo local)
        // Isso garante que o arquivo config.env tenha precedência sobre variáveis de ambiente do sistema
        if (manualConfig.containsKey(key)) {
            value = manualConfig.get(key);
        }

        // 2. Prioridade: Dotenv
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        
        // 3. Prioridade: Variáveis de Sistema (fallback final)
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
    public static String getStaffFeedbackChannelId() {
        return get("STAFF_FEEDBACK_CHANNEL_ID");
    }

    public static String getToken() {
        return get("TOKEN");
    }

    public static String getPrefix() {
        return get("PREFIX");
    }

    public static String getStaffChannelId() {
        return get("STAFF_CHANNEL_ID");
    }

    public static String getResultsChannelId() {
        return get("RESULTS_CHANNEL_ID");
    }

    public static String getLogChannelId() {
        return get("LOG_CHANNEL_ID");
    }

    public static String getPunishmentChannelId() {
        return get("PUNISHMENT_CHANNEL_ID");
    }

    public static String getTwitchClientId() { return get("TWITCH_CLIENT_ID"); }
    public static String getTwitchClientSecret() { return get("TWITCH_CLIENT_SECRET"); }
    public static String getYoutubeApiKey() { return get("YOUTUBE_API_KEY"); }
    public static String getStreamAnnounceChannelId() { return get("STREAM_ANNOUNCE_CHANNEL_ID"); }
    public static String getPromotionChannelId() { return get("PROMOTION_CHANNEL_ID"); }
    public static String getStreamNotificationRoleId() { return get("STREAM_NOTIFICATION_ROLE_ID"); }
    public static String getStreamerRoleId() { return get("STREAMER_ROLE_ID"); }
    public static String getCreatorRoleId() { return get("CREATOR_ROLE_ID"); }

    public static String getCitizenRoleId() {
        return get("CITIZEN_ROLE_ID");
    }

    public static String getNotificationRoleId() {
        return get("NOTIFICATION_ROLE_ID");
    }

    public static String getUnderAgeRoleId() {
        return get("UNDER_AGE_ROLE_ID");
    }

    public static String getBackupChannelId() {
        return get("BACKUP_CHANNEL_ID");
    }

    public static String getDeathChannelId() {
        return get("DEATH_CHANNEL_ID");
    }

    public static String getRequestChannelId() {
        return get("REQUEST_CHANNEL_ID");
    }

    public static String getRequestLogChannelId() {
        return get("REQUEST_LOG_CHANNEL_ID");
    }

    public static String getFounderRoleId() {
        return get("FOUNDER_ROLE_ID");
    }

    public static String getDeveloperRoleId() {
        return get("DEVELOPER_ROLE_ID");
    }

    public static String getStaffRoleId() {
        return get("STAFF_ROLE_ID");
    }

    public static String getWelcomeChannelId() {
        return get("WELCOME_CHANNEL_ID");
    }

    public static int getSocketPort() {
        String port = get("SOCKET_PORT");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        return 25590;
    }

    // Ticket Categories
    public static String getTicketCategorySupport() {
        return get("TICKET_CATEGORY_SUPPORT");
    }

    public static String getTicketCategoryReport() {
        return get("TICKET_CATEGORY_REPORT");
    }

    public static String getTicketCategoryBug() {
        return get("TICKET_CATEGORY_BUG");
    }

    public static String getTicketCategoryLore() {
        return get("TICKET_CATEGORY_LORE");
    }

    public static String getTicketCategoryLog() {
        return get("TICKET_CATEGORY_LOG");
    }

    public static String getTicketSupportRoles() {
        return get("TICKET_SUPPORT_ROLES");
    }

    // Ticket Roles (Mentions)
    public static String getTicketRoleSupport() {
        return get("TICKET_ROLE_SUPPORT");
    }

    public static String getTicketRoleReport() {
        return get("TICKET_ROLE_REPORT");
    }

    public static String getTicketRoleBug() {
        return get("TICKET_ROLE_BUG");
    }

    public static String getTicketRoleLore() {
        return get("TICKET_ROLE_LORE");
    }

    // Ticket Schedule (Business Hours)
    public static boolean isTicketScheduleEnabled() {
        return "true".equalsIgnoreCase(get("TICKET_SCHEDULE_ENABLED", "true"));
    }

    public static String getTicketScheduleTimezone() {
        return get("TICKET_SCHEDULE_TIMEZONE", "America/Sao_Paulo");
    }

    public static int getTicketWeekdayStart() {
        try { return Integer.parseInt(get("TICKET_WEEKDAY_START", "12")); } catch (Exception e) { return 12; }
    }

    public static int getTicketWeekdayEnd() {
        try { return Integer.parseInt(get("TICKET_WEEKDAY_END", "19")); } catch (Exception e) { return 19; }
    }

    public static int getTicketWeekendStart() {
        try { return Integer.parseInt(get("TICKET_WEEKEND_START", "12")); } catch (Exception e) { return 12; }
    }

    public static int getTicketWeekendEnd() {
        try { return Integer.parseInt(get("TICKET_WEEKEND_END", "14")); } catch (Exception e) { return 14; }
    }

    // Status Channels
    public static String getStatusChannelId() {
        return get("STATUS_CHANNEL_ID");
    }

    public static String getServerIp() {
        return get("SERVER_IP", "127.0.0.1");
    }

    public static int getLobbyPort() {
        String port = get("LOBBY_PORT");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        return 25565;
    }

    public static int getRpgPort() {
        String port = get("RPG_PORT");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        return 25567;
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }
    public static String getRconHost() { return get("RCON_HOST"); }
    public static int getRconPort() { 
        String s = get("RCON_PORT");
        try {
            return s != null ? Integer.parseInt(s) : 25575;
        } catch (NumberFormatException e) {
            return 25575;
        }
    }
    public static String getRconPassword() { return get("RCON_PASSWORD"); }

    public static String getGithubToken() {
        return get("GITHUB_TOKEN");
    }

    // Permissions
    public static String getPermCmdBan() { return get("PERM_CMD_BAN"); }
    public static String getPermCmdKick() { return get("PERM_CMD_KICK"); }
    public static String getPermCmdWarn() { return get("PERM_CMD_WARN"); }
    public static String getPermCmdUnwarn() { return get("PERM_CMD_UNWARN"); }
    public static String getPermCmdUnban() { return get("PERM_CMD_UNBAN"); }
    public static String getPermCmdClear() { return get("PERM_CMD_CLEAR"); }
    public static String getPermCmdMaintenance() { return get("PERM_CMD_MAINTENANCE"); }
    public static String getPermCmdBackup() { return get("PERM_CMD_BACKUP"); }
    public static String getPermCmdBlacklist() { return get("PERM_CMD_BLACKLIST"); }
    public static String getPermCmdWhitelist() { return get("PERM_CMD_WHITELIST"); }
    public static String getPermCmdStaffStats() { return get("PERM_CMD_STAFFSTATS"); }
    public static String getPermCmdDebug() { return get("PERM_CMD_DEBUG"); }
    public static String getPermCmdClearTickets() { return get("PERM_CMD_CLEARTICKETS"); }

    public static String getSocketSecret() {
        return get("SOCKET_SECRET", "midgard_secret_key_change_me");
    }

    public static String getGithubRepo() {
        return get("GITHUB_REPO", "MidgardNetwork/MidgardProjects");
    }

    /**
     * Obtém a lista de IDs de cargos autorizados para uma determinada permissão.
     * Suporta o uso de aliases definidos no .env (ex: PERM_CMD_BAN=MODERADOR onde MODERADOR=12345).
     * @param permissionKey A chave da permissão (ex: "PERM_BAN")
     * @return Lista de IDs de cargos ou lista vazia se não configurado
     */
    public static java.util.List<String> getAuthorizedRoles(String permissionKey) {
        String roles = get(permissionKey);
        if (roles == null || roles.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        java.util.List<String> resolvedIds = new java.util.ArrayList<>();
        for (String part : roles.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // Tenta buscar se é uma variável definida no .env (ex: AJUDANTE)
            // Isso permite definir grupos de cargos no .env e reutilizá-los
            String aliasValue = get(part);
            if (aliasValue != null) {
                // Se for um alias, pode conter múltiplos IDs também
                for (String subPart : aliasValue.split(",")) {
                    resolvedIds.add(subPart.trim());
                }
            } else {
                // Se não for alias (ou não encontrado), assume que é o ID direto
                resolvedIds.add(part);
            }
        }
        return resolvedIds;
    }
}
