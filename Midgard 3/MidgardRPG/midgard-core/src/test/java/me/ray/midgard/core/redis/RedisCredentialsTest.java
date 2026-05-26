package me.ray.midgard.core.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisCredentialsTest {

    @Test
    void shouldStoreFields() {
        RedisCredentials creds = new RedisCredentials("localhost", 6379, "secret", true);

        assertEquals("localhost", creds.host());
        assertEquals(6379, creds.port());
        assertEquals("secret", creds.password());
        assertTrue(creds.useSsl());
    }

    @Test
    void shouldSupportEquals() {
        RedisCredentials c1 = new RedisCredentials("localhost", 6379, "pass", false);
        RedisCredentials c2 = new RedisCredentials("localhost", 6379, "pass", false);
        RedisCredentials c3 = new RedisCredentials("remote", 6379, "pass", false);

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }

    @Test
    void shouldSupportHashCode() {
        RedisCredentials c1 = new RedisCredentials("localhost", 6379, "pass", false);
        RedisCredentials c2 = new RedisCredentials("localhost", 6379, "pass", false);

        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
