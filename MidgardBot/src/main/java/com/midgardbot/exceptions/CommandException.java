package com.midgardbot.exceptions;

/**
 * Lançada quando ocorre um erro durante a execução de um comando.
 */
public class CommandException extends BotException {

    private final String commandName;

    public CommandException(String commandName, String message) {
        super("Erro no comando '" + commandName + "': " + message);
        this.commandName = commandName;
    }

    public CommandException(String commandName, String message, Throwable cause) {
        super("Erro no comando '" + commandName + "': " + message, cause);
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}
