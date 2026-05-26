package com.midgardbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionUtilsTest {

    @Test
    void hasPermission_nullPermKey_returnsTrue() {
        // Quando a chave de permissão é null, qualquer usuário tem acesso
        assertTrue(PermissionUtils.hasPermission(null, null, "test"));
    }

    @Test
    void isRateLimited_firstCall_returnsFalse() {
        assertFalse(PermissionUtils.isRateLimited("firstCallUser", "firstCallCmd"));
    }

    @Test
    void isRateLimited_immediateSecondCall_returnsTrue() {
        PermissionUtils.isRateLimited("doubleCallUser", "kick");
        assertTrue(PermissionUtils.isRateLimited("doubleCallUser", "kick"));
    }

    @Test
    void isRateLimited_differentCommands_independent() {
        PermissionUtils.isRateLimited("multiCmdUser", "ban");
        // Comando diferente NÃO deve estar em cooldown
        assertFalse(PermissionUtils.isRateLimited("multiCmdUser", "warn"));
    }

    @Test
    void isRateLimited_differentUsers_independent() {
        PermissionUtils.isRateLimited("userA", "status");
        // Usuário diferente NÃO deve estar em cooldown
        assertFalse(PermissionUtils.isRateLimited("userB", "status"));
    }

    @Test
    void cleanupCooldowns_doesNotThrow() {
        PermissionUtils.isRateLimited("cleanupUser1", "test1");
        PermissionUtils.isRateLimited("cleanupUser2", "test2");
        assertDoesNotThrow(PermissionUtils::cleanupCooldowns);
    }
}
