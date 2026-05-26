package com.midgardbot.exceptions;

/**
 * Exceção base do bot.
 * Todas as exceções customizadas devem estender esta classe.
 */
public class BotException extends RuntimeException {

    public BotException(String message) {
        super(message);
    }

    public BotException(String message, Throwable cause) {
        super(message, cause);
    }
}
