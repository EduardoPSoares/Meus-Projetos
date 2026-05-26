package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class PollCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "poll";
    }

    @Override
    public String getDescription() {
        return "Cria uma votação simples com Sim/Não.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "pergunta", "A pergunta da votação", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_POLL";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para MESSAGE_MANAGE se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_POLL").isEmpty() && 
            !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você precisa de permissão para gerenciar mensagens.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        String question = event.getOption("pergunta").getAsString();

        event.replyEmbeds(
            EmbedUtils.createEmbed("📊 Votação da Comunidade", "", EmbedUtils.COLOR_GOLD)
                .setDescription(
                    "**Pergunta:**\n" +
                    "## 📝 " + question + "\n\n" +
                    EmbedUtils.SEPARATOR + "\n\n" +
                    "**Como votar?**\n" +
                    "> ✅ **Sim** - Concordo\n" +
                    "> ❌ **Não** - Discordo"
                )
                .setThumbnail(event.getGuild().getIconUrl())
                .setFooter("Iniciada por " + event.getUser().getName(), event.getUser().getAvatarUrl())
                .setTimestamp(java.time.Instant.now())
                .build()
        ).queue(hook -> {
            hook.retrieveOriginal().queue(message -> {
                message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue();
                message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
            });
        });
    }
}
