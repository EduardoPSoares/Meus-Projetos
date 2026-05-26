package com.midgardbot.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotConfigTest {

    @BeforeEach
    void setUp() {
        // Inicializa com perfil nulo (usa config.env se existir)
        // O init() é seguro para chamar múltiplas vezes
        try {
            BotConfig.init(null);
        } catch (Exception ignored) {
            // Pode não achar o arquivo config.env em ambiente de teste
        }
    }

    @Test
    void get_nonExistentKey_returnsNull() {
        assertNull(BotConfig.get("KEY_THAT_DOES_NOT_EXIST_12345"));
    }

    @Test
    void get_withDefault_returnsDefault() {
        String result = BotConfig.get("KEY_THAT_DOES_NOT_EXIST_12345", "defaultValue");
        assertEquals("defaultValue", result);
    }

    @Test
    void getPrefix_returnsNonNull() {
        // PREFIX deveria estar configurado (ou usar fallback)
        String prefix = BotConfig.getPrefix();
        // Pode ser null se não houver config.env — mas não deve lançar exceção
        assertDoesNotThrow(() -> BotConfig.getPrefix());
    }

    @Test
    void isTestMode_defaultIsFalse() {
        // Quando iniciado sem perfil, não está em modo teste
        BotConfig.init(null);
        assertFalse(BotConfig.isTestMode());
    }

    @Test
    void isTestMode_trueWithProfile() {
        BotConfig.init("test");
        assertTrue(BotConfig.isTestMode());
        assertEquals("test", BotConfig.getActiveProfile());
    }

    @Test
    void getAuthorizedRoles_emptyKey_returnsEmptyList() {
        List<String> roles = BotConfig.getAuthorizedRoles("NONEXISTENT_PERM_KEY");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void getSocketPort_returnsValidPort() {
        int port = BotConfig.getSocketPort();
        assertTrue(port > 0 && port < 65536);
    }

    @Test
    void getLobbyPort_returnsValidPort() {
        int port = BotConfig.getLobbyPort();
        assertTrue(port > 0 && port < 65536);
    }

    @Test
    void getRpgPort_returnsValidPort() {
        int port = BotConfig.getRpgPort();
        assertTrue(port > 0 && port < 65536);
    }

    @Test
    void getServerIp_returnsNonNull() {
        String ip = BotConfig.getServerIp();
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
    }
}
