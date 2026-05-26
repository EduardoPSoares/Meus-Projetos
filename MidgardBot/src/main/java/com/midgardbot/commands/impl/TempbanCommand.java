package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.PunishmentManager;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempbanCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "tempban";
    }

    @Override
    public String getDescription() {
        return "Bane um usuário temporariamente.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário a ser banido", true),
            new OptionData(OptionType.STRING, "duracao", "Duração (ex: 1d, 12h, 30m)", true),
            new OptionData(OptionType.STRING, "motivo", "O motivo do banimento", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_TEMPBAN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_TEMPBAN").isEmpty() && !event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para banir membros.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("usuario").getAsMember();
        User targetUser = event.getOption("usuario").getAsUser();

        if (target != null) {
            if (!event.getMember().canInteract(target) || !event.getGuild().getSelfMember().canInteract(target)) {
                event.replyEmbeds(
                    EmbedUtils.createError("Erro", "Não posso banir este usuário (cargo superior ou igual).", event.getJDA().getSelfUser()).build()
                ).setEphemeral(true).queue();
                return;
            }
        }

        String durationStr = event.getOption("duracao").getAsString();
        long durationMillis = parseDuration(durationStr);

        if (durationMillis <= 0) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Formato de tempo inválido. Use d (dias), h (horas), m (minutos). Ex: 1d, 12h, 30m", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        OptionMapping reasonOption = event.getOption("motivo");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Sem motivo especificado";

        event.getGuild().ban(targetUser, 0, TimeUnit.DAYS).reason("Tempban: " + durationStr + " - " + reason).queue(
            success -> {
                PunishmentManager.addTempBan(targetUser.getId(), event.getUser().getId(), reason, durationMillis);
                
                event.replyEmbeds(
                    EmbedUtils.createSuccess("Usuário Banido Temporariamente", targetUser.getAsMention() + " foi banido por " + durationStr + ".\nMotivo: " + reason, event.getJDA().getSelfUser()).build()
                ).queue();

                logPunishment(event, "Tempban", targetUser, reason + " (" + durationStr + ")");
            },
            error -> {
                event.replyEmbeds(
                    EmbedUtils.createError("Erro", "Falha ao banir usuário: " + error.getMessage(), event.getJDA().getSelfUser()).build()
                ).setEphemeral(true).queue();
            }
        );
    }

    private long parseDuration(String duration) {
        Pattern pattern = Pattern.compile("(\\d+)([dhm])");
        Matcher matcher = pattern.matcher(duration.toLowerCase());
        
        long total = 0;
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "d": total += value * 24 * 60 * 60 * 1000; break;
                case "h": total += value * 60 * 60 * 1000; break;
                case "m": total += value * 60 * 1000; break;
            }
        }
        return total;
    }

    private void logPunishment(SlashCommandInteractionEvent event, String type, User target, String reason) {
        String channelId = BotConfig.getPunishmentChannelId();
        if (channelId != null) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createEmbed("🔨 Punição: " + type, "", EmbedUtils.COLOR_ERROR)
                        .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                        .addField("Moderador", event.getUser().getAsMention(), false)
                        .addField("Motivo", reason, false)
                        .build()
                ).queue();
            }
        }
    }
}
