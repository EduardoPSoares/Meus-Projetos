package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

import com.midgardbot.features.link.LinkManager;
import com.midgardbot.features.sync.ActionSyncManager;
import java.util.UUID;

public class KickCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Expulsa um usuário do servidor (Discord e Minecraft).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário a ser expulso", true),
            new OptionData(OptionType.STRING, "motivo", "O motivo da expulsão", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_KICK";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_KICK").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para expulsar membros.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("usuario").getAsMember();
        if (target == null) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Usuário não encontrado no servidor.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        if (!event.getMember().canInteract(target) || !event.getGuild().getSelfMember().canInteract(target)) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Não posso expulsar este usuário (cargo superior ou igual).", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        OptionMapping reasonOption = event.getOption("motivo");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Sem motivo especificado";

        event.getGuild().kick(target).reason(reason).queue(
            success -> {
                // Sync with Minecraft
                UUID uuid = LinkManager.getUUID(target.getId());
                if (uuid != null) {
                    ActionSyncManager.queueAction(uuid.toString(), "KICK", reason, event.getUser().getName());
                }

                event.replyEmbeds(
                    EmbedUtils.createSuccess("Usuário Expulso", target.getAsMention() + " foi expulso do Discord e Minecraft.\nMotivo: " + reason, event.getJDA().getSelfUser()).build()
                ).queue();

                logPunishment(event, "Kick", target.getUser(), reason);
            },
            error -> {
                event.replyEmbeds(
                    EmbedUtils.createError("Erro", "Falha ao expulsar usuário: " + error.getMessage(), event.getJDA().getSelfUser()).build()
                ).setEphemeral(true).queue();
            }
        );
    }

    private void logPunishment(SlashCommandInteractionEvent event, String type, User target, String reason) {
        String channelId = BotConfig.getPunishmentChannelId();
        if (channelId != null) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createEmbed("🔨 Punição: " + type, "", EmbedUtils.COLOR_WARNING)
                        .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                        .addField("Moderador", event.getUser().getAsMention(), false)
                        .addField("Motivo", reason, false)
                        .build()
                ).queue();
            }
        }
    }
}
