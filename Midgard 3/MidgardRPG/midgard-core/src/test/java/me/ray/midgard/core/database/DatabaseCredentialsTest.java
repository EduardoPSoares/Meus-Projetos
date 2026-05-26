package me.ray.midgard.core.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseCredentialsTest {

    @TempDir
    File tempDir;

    @Test
    void shouldStoreFields() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "mysql", "localhost", 3306, "midgard", "root", "pass", false);

        assertEquals("mysql", creds.type());
        assertEquals("localhost", creds.host());
        assertEquals(3306, creds.port());
        assertEquals("midgard", creds.database());
        assertEquals("root", creds.username());
        assertEquals("pass", creds.password());
        assertFalse(creds.useSsl());
    }

    @Test
    void shouldGenerateMysqlJdbcUrl() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "mysql", "localhost", 3306, "midgard", "root", "pass", true);

        String url = creds.toJdbcUrl(tempDir);
        assertTrue(url.startsWith("jdbc:mysql://"));
        assertTrue(url.contains("localhost:3306/midgard"));
        assertTrue(url.contains("useSSL=true"));
    }

    @Test
    void shouldGenerateSqliteJdbcUrl() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "sqlite", "", 0, "midgard_data", "", "", false);

        String url = creds.toJdbcUrl(tempDir);
        assertTrue(url.startsWith("jdbc:sqlite:"));
        assertTrue(url.contains("midgard_data.db"));
    }

    @Test
    void shouldSanitizeSqliteDatabaseName() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "sqlite", "", 0, "../../hack", "", "", false);

        String url = creds.toJdbcUrl(tempDir);
        // Characters . and / should be stripped
        assertFalse(url.contains(".."));
    }

    @Test
    void shouldRejectInvalidMysqlDatabaseName() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "mysql", "localhost", 3306, "invalid;drop", "root", "pass", false);

        assertThrows(IllegalArgumentException.class, () -> creds.toJdbcUrl(tempDir));
    }

    @Test
    void shouldAcceptValidMysqlDatabaseName() {
        DatabaseCredentials creds = new DatabaseCredentials(
                "mysql", "localhost", 3306, "midgard_rpg", "root", "pass", false);

        // Should not throw
        assertDoesNotThrow(() -> creds.toJdbcUrl(tempDir));
    }
}
