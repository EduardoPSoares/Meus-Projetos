package com.midgardbot.config;

import com.midgardbot.exceptions.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    @Test
    void validate_withoutToken_throwsConfigurationException() {
        // Inicializa com perfil inexistente para garantir que TOKEN está vazio
        BotConfig.init("nonexistent_profile_for_test");
        
        // Com token vazio/null, validate deve lançar ConfigurationException
        assertThrows(ConfigurationException.class, ConfigValidator::validate);
    }

    @Test
    void validate_throwsException_containsTokenKey() {
        BotConfig.init("nonexistent_profile_for_test");
        
        ConfigurationException ex = assertThrows(ConfigurationException.class, ConfigValidator::validate);
        assertEquals("TOKEN", ex.getConfigKey());
        assertTrue(ex.getMessage().contains("TOKEN"));
    }
}
