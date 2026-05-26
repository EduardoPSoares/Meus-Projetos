package com.midgardbot.exceptions;

/**
 * Lançada quando ocorre um erro relacionado ao banco de dados.
 */
public class DatabaseException extends BotException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
