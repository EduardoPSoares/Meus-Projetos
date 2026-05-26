package com.midgardbot.config;

import com.midgardbot.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida as configurações obrigatórias na inicialização do bot.
 * Garante que erros de configuração sejam detectados cedo (fail-fast).
 */
public final class ConfigValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigValidator.class);

    private ConfigValidator() {}

    /**
     * Valida todas as configurações críticas do bot.
     * Loga warnings para configurações opcionais ausentes.
     *
     * @throws ConfigurationException se o TOKEN não estiver configurado
     */
    public static void validate() {
        LOGGER.info("Validando configurações...");
        List<String> warnings = new ArrayList<>();

        // ── Obrigatório ──
        String token = BotConfig.getToken();
        if (token == null || token.isBlank() || "seu_token_aqui".equals(token)) {
            throw new ConfigurationException("TOKEN", "Token do bot não configurado! Edite o arquivo config.env.");
        }

        String prefix = BotConfig.getPrefix();
        if (prefix == null || prefix.isBlank()) {
            warnings.add("PREFIX não definido — usando '!' como padrão.");
        }

        // ── Canais (opcionais mas recomendados) ──
        checkOptional("STAFF_CHANNEL_ID", "Canal da Staff", warnings);
        checkOptional("LOG_CHANNEL_ID", "Canal de Logs", warnings);
        checkOptional("RESULTS_CHANNEL_ID", "Canal de Resultados", warnings);
        checkOptional("WELCOME_CHANNEL_ID", "Canal de Boas-vindas", warnings);

        // ── Cargos (opcionais mas recomendados) ──
        checkOptional("CITIZEN_ROLE_ID", "Cargo de Cidadão", warnings);

        // ── Database ──
        String dbType = BotConfig.get("DB_TYPE");
        if (dbType != null && "mysql".equalsIgnoreCase(dbType)) {
            checkOptional("DB_HOST", "Host do MySQL", warnings);
            checkOptional("DB_NAME", "Nome do banco MySQL", warnings);
            checkOptional("DB_USER", "Usuário do MySQL", warnings);
        }

        // ── Socket ──
        String socketSecret = BotConfig.getSocketSecret();
        if (Constants.DEFAULT_SOCKET_SECRET.equals(socketSecret)) {
            warnings.add("SOCKET_SECRET está usando o valor padrão! Altere para uma chave segura.");
        }

        // ── Resultado ──
        if (warnings.isEmpty()) {
            LOGGER.info("Todas as configurações validadas com sucesso.");
        } else {
            LOGGER.warn("Configurações com avisos ({} encontrados):", warnings.size());
            for (String warning : warnings) {
                LOGGER.warn("  ⚠ {}", warning);
            }
        }
    }

    private static void checkOptional(String key, String description, List<String> warnings) {
        String value = BotConfig.get(key);
        if (value == null || value.isBlank()) {
            warnings.add(description + " (" + key + ") não configurado.");
        }
    }
}
