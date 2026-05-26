package com.midgardbot.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotExceptionTest {

    @Test
    void botException_hasMessage() {
        BotException ex = new BotException("teste");
        assertEquals("teste", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void botException_withCause() {
        RuntimeException cause = new RuntimeException("causa");
        BotException ex = new BotException("teste", cause);
        assertEquals("teste", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void configurationException_formatsMessage() {
        ConfigurationException ex = new ConfigurationException("TOKEN", "não configurado");
        assertTrue(ex.getMessage().contains("TOKEN"));
        assertTrue(ex.getMessage().contains("não configurado"));
        assertEquals("TOKEN", ex.getConfigKey());
    }

    @Test
    void configurationException_withCause() {
        IllegalArgumentException cause = new IllegalArgumentException("valor inválido");
        ConfigurationException ex = new ConfigurationException("DB_PORT", "porta inválida", cause);
        assertTrue(ex.getMessage().contains("DB_PORT"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void databaseException_hasMessage() {
        DatabaseException ex = new DatabaseException("conexão perdida");
        assertEquals("conexão perdida", ex.getMessage());
    }

    @Test
    void databaseException_withCause() {
        Exception cause = new Exception("timeout");
        DatabaseException ex = new DatabaseException("falha", cause);
        assertEquals("falha", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void commandException_formatsMessage() {
        CommandException ex = new CommandException("ban", "usuário não encontrado");
        assertTrue(ex.getMessage().contains("ban"));
        assertTrue(ex.getMessage().contains("usuário não encontrado"));
        assertEquals("ban", ex.getCommandName());
    }

    @Test
    void commandException_withCause() {
        Exception cause = new Exception("API error");
        CommandException ex = new CommandException("kick", "falha na API", cause);
        assertTrue(ex.getMessage().contains("kick"));
        assertEquals(cause, ex.getCause());
        assertEquals("kick", ex.getCommandName());
    }

    @Test
    void allExceptions_extendBotException() {
        assertTrue(BotException.class.isAssignableFrom(ConfigurationException.class));
        assertTrue(BotException.class.isAssignableFrom(DatabaseException.class));
        assertTrue(BotException.class.isAssignableFrom(CommandException.class));
    }

    @Test
    void allExceptions_extendRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(BotException.class));
    }
}
