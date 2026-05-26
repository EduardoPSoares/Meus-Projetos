package com.midgardbot.commands.impl;

import com.midgardbot.commands.CommandContext;
import com.midgardbot.commands.ICommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Comando de Limpeza (Texto).
 * Remove uma quantidade específica de mensagens do canal atual.
 * Uso: !clear <quantidade>
 */
public class ClearCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) {
        // Check permissions
        if (!ctx.getEvent().getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "Permissão Negada",
                "Você precisa da permissão `Gerenciar Mensagens` para usar este comando.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        List<String> args = ctx.getArgs();
        if (args.isEmpty()) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "Uso Incorreto",
                "Por favor, especifique a quantidade de mensagens para apagar.\n" +
                "Exemplo: `!clear 10`",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "Número Inválido",
                "Por favor, forneça um número válido de mensagens.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        if (amount < 2 || amount > 100) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "Quantidade Inválida",
                "A quantidade deve ser entre 2 e 100 mensagens.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        // Ensure we are in a TextChannel
        if (!(ctx.getChannel() instanceof TextChannel)) {
             ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "Canal Inválido",
                "Este comando só pode ser usado em canais de texto.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        TextChannel channel = (TextChannel) ctx.getChannel();
        
        // Delete the command message itself to keep chat clean, then fetch history
        ctx.getEvent().getMessage().delete().queue(
            v -> fetchAndPurge(channel, amount, ctx),
            e -> fetchAndPurge(channel, amount, ctx) // Continue even if delete fails
        );
    }

    private void fetchAndPurge(TextChannel channel, int amount, CommandContext ctx) {
        channel.getHistory().retrievePast(amount).queue(messages -> {
            if (messages.isEmpty()) return;

            // Filter messages older than 2 weeks (Discord API limitation for bulk delete)
            OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(14, ChronoUnit.DAYS);
            List<Message> deletable = messages.stream()
                .filter(msg -> msg.getTimeCreated().isAfter(twoWeeksAgo))
                .collect(Collectors.toList());

            if (deletable.size() < 2) {
                // If less than 2 messages, use delete() instead of deleteMessages()
                if (!deletable.isEmpty()) {
                    deletable.get(0).delete().queue();
                }
                
                // Notify if many messages were skipped
                if (messages.size() > deletable.size()) {
                     ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                        "Mensagens Antigas",
                        "Algumas mensagens não puderam ser apagadas pois são muito antigas (mais de 2 semanas).",
                        ctx.getJDA().getSelfUser()
                    ).build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                }
                return;
            }

            try {
                channel.deleteMessages(deletable).queue(
                    success -> {
                        channel.sendMessageEmbeds(EmbedUtils.createSuccess(
                            "Chat Limpo",
                            "Foram apagadas **" + deletable.size() + "** mensagens.",
                            ctx.getJDA().getSelfUser()
                        ).build()).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                    },
                    error -> {
                         ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                            "Erro ao Apagar",
                            "Ocorreu um erro ao tentar apagar as mensagens.",
                            ctx.getJDA().getSelfUser()
                        ).build()).queue();
                    }
                );
            } catch (IllegalArgumentException e) {
                 ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                    "Erro ao Apagar",
                    "Erro interno ao processar mensagens.",
                    ctx.getJDA().getSelfUser()
                ).build()).queue();
            }
        });
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getHelp() {
        return "🗑️ Limpa uma quantidade específica de mensagens do chat.\nUso: `!clear <quantidade>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("limpar", "clean", "purge");
    }
}
