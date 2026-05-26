package com.midgardbot.commands;

import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.midgardbot.commands.impl.HelpCommand;
import com.midgardbot.commands.impl.PingCommand;
import com.midgardbot.commands.impl.UnflagCommand;
import com.midgardbot.commands.impl.ClearCommand;
import com.midgardbot.data.DataManager;
import net.dv8tion.jda.api.Permission;

/**
 * Gerenciador de comandos de texto (prefixo).
 * Responsável por registrar comandos, ouvir mensagens e executar a lógica correspondente.
 */
public class CommandManager extends ListenerAdapter {
    private final List<ICommand> commands = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    public CommandManager() {
        // Register commands here
        addCommand(new PingCommand());
        addCommand(new HelpCommand(this));
        addCommand(new UnflagCommand());
        addCommand(new ClearCommand());
    }

    private void addCommand(ICommand cmd) {
        boolean nameFound = this.commands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

        if (nameFound) {
            throw new IllegalArgumentException("A command with this name is already present");
        }

        commands.add(cmd);
    }

    public List<ICommand> getCommands() {
        return commands;
    }

    public ICommand getCommand(String search) {
        String searchLower = search.toLowerCase();

        for (ICommand cmd : this.commands) {
            if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower)) {
                return cmd;
            }
        }

        return null;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            // Ignora mensagens de outros bots
            if (event.getAuthor().isBot()) {
                return;
            }

            String prefix = BotConfig.getPrefix();
            String raw = event.getMessage().getContentRaw();

            // Verifica se a mensagem começa com o prefixo configurado
            if (!raw.startsWith(prefix)) {
                return;
            }

            // Separa o comando dos argumentos
            String[] split = raw.replaceFirst("(?i)" + Pattern.quote(prefix), "").split("\\s+");
            String invoke = split[0].toLowerCase();
            ICommand cmd = this.getCommand(invoke);

            if (cmd != null) {
                // Ignora DMs (getMember() retorna null em DM)
                if (event.getMember() == null) return;

                // Verifica modo de manutenção
                if (DataManager.isMaintenanceMode() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                        "🚧 Manutenção",
                        "O bot está em modo de manutenção.\nApenas administradores podem usar comandos no momento.",
                        event.getJDA().getSelfUser()
                    ).build()).queue();
                    return;
                }

                event.getChannel().sendTyping().queue();
                List<String> args = Arrays.asList(split).subList(1, split.length);

                CommandContext ctx = new CommandContext(event, args);

                // --- Verificação de Permissões (centralizada via PermissionUtils) ---
                if (!com.midgardbot.utils.PermissionUtils.hasPermission(event.getMember(), cmd.getPermissionKey(), cmd.getName())) {
                    event.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                        "Sem Permissão",
                        "Você não tem permissão para usar este comando.\n_Se você acredita que isso é um erro, contate um administrador._",
                        event.getJDA().getSelfUser()
                    ).build()).queue();
                    return;
                }
                // ---------------------------------

                try {
                    // Executa o comando
                    cmd.handle(ctx);
                } catch (Exception e) {
                    // Tratamento de erro genérico para comandos
                    LOGGER.error("Command execution failed", e);
                    event.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                        "Erro de Execução",
                        "Ocorreu um erro ao executar este comando.\n" +
                        "Por favor, tente novamente mais tarde ou contate um administrador.",
                        event.getJDA().getSelfUser()
                    ).build()).queue();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro crítico no processamento de mensagem", e);
        }
    }
}