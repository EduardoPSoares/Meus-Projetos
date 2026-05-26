package com.midgardbot.exceptions;

/**
 * Lançada quando uma configuração obrigatória está ausente ou inválida.
 */
public class ConfigurationException extends BotException {

    private final String configKey;

    public ConfigurationException(String configKey, String message) {
        super("Configuração '" + configKey + "': " + message);
        this.configKey = configKey;
    }

    public ConfigurationException(String configKey, String message, Throwable cause) {
        super("Configuração '" + configKey + "': " + message, cause);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
