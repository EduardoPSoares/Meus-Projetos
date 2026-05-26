package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class LockdownCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "lockdown";
    }

    @Override
    public String getDescription() {
        return "Bloqueia ou desbloqueia todos os canais do servidor em caso de emergência.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "acao", "Ação a ser realizada (on/off)", true)
                .addChoice("Ativar (Lock)", "on")
                .addChoice("Desativar (Unlock)", "off")
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LOCKDOWN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LOCKDOWN").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("Permissão Negada", "Você precisa ser Administrador para usar este comando.", event.getJDA().getSelfUser()).build())
                .setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("acao").getAsString();
        Guild guild = event.getGuild();
        if (guild == null) return;

        Role everyoneRole = guild.getPublicRole();
        boolean lock = action.equals("on");

        event.deferReply().queue();

        int count = 0;
        for (TextChannel channel : guild.getTextChannels()) {
            try {
                if (lock) {
                    channel.upsertPermissionOverride(everyoneRole).deny(Permission.MESSAGE_SEND).queue();
                } else {
                    // Ao desbloquear, removemos a negação explícita (clear) ou concedemos?
                    // Geralmente clear é melhor para voltar ao padrão.
                    channel.upsertPermissionOverride(everyoneRole).clear(Permission.MESSAGE_SEND).queue();
                }
                count++;
            } catch (Exception e) {
                // Ignorar erros de permissão
            }
        }

        if (lock) {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("🔒 LOCKDOWN ATIVADO", 
                    "O servidor foi bloqueado. " + count + " canais afetados.\nApenas a Staff pode falar agora.", 
                    event.getJDA().getSelfUser()).build()
            ).queue();
        } else {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createSuccess("🔓 LOCKDOWN DESATIVADO", 
                    "O servidor foi desbloqueado. " + count + " canais liberados.", 
                    event.getJDA().getSelfUser()).build()
            ).queue();
        }
    }
}
