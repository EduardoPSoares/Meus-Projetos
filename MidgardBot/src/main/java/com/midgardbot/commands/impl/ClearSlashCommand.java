package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando de Limpeza (Slash).
 * Versão moderna do comando de limpeza.
 * Remove mensagens em massa do canal atual.
 */
public class ClearSlashCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Executa a higienização do canal (Remoção de mensagens)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.INTEGER, "quantidade", "Volume de mensagens (2-100)", true)
                .setMinValue(2)
                .setMaxValue(100)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_CLEAR";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_CLEAR").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Requer permissão de Gerenciar Mensagens.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        if (!(event.getChannel() instanceof TextChannel)) {
            event.replyEmbeds(EmbedUtils.createError("⚠️ Canal Inválido", "Comando restrito a canais de texto.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        int amount = event.getOption("quantidade").getAsInt();
        TextChannel channel = (TextChannel) event.getChannel();

        event.deferReply(true).queue();

        channel.getHistory().retrievePast(amount).queue(messages -> {
            if (messages.isEmpty()) {
                event.getHook().sendMessageEmbeds(EmbedUtils.createWarning("⚠️ Sem Dados", "Não há mensagens recentes para processar.", event.getJDA().getSelfUser()).build()).queue();
                return;
            }

            OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(14, ChronoUnit.DAYS);
            List<Message> deletable = messages.stream()
                .filter(msg -> msg.getTimeCreated().isAfter(twoWeeksAgo))
                .collect(Collectors.toList());

            if (deletable.size() < 2) {
                if (!deletable.isEmpty()) deletable.get(0).delete().queue();
                event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess("✅ Operação Concluída", "1 mensagem removida.", event.getJDA().getSelfUser()).build()).queue();
                return;
            }

            try {
                channel.deleteMessages(deletable).queue(
                    success -> {
                        String desc = "Total removido: **" + deletable.size() + "** mensagens.";
                        if (messages.size() > deletable.size()) {
                            desc += "\n\n⚠️ **Nota:** Itens com mais de 14 dias não podem ser excluídos via API.";
                        }
                        event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess("🧹 Higienização Concluída", desc, event.getJDA().getSelfUser()).build()).queue();
                    },
                    error -> event.getHook().sendMessageEmbeds(EmbedUtils.createError("❌ Erro Crítico", "Falha na comunicação com a API.", event.getJDA().getSelfUser()).build()).queue()
                );
            } catch (IllegalArgumentException e) {
                event.getHook().sendMessageEmbeds(EmbedUtils.createError("❌ Erro Interno", "Falha no processamento.", event.getJDA().getSelfUser()).build()).queue();
            }
        });
    }
}