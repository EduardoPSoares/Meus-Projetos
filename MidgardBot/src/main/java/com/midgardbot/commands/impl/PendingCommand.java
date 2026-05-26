package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;

/**
 * Comando de Pendentes.
 * Exibe quantas aplicações de whitelist estão aguardando análise.
 */
public class PendingCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "pending";
    }

    @Override
    public String getDescription() {
        return "Exibe o status da fila de análise (Backlog).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_PENDING";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_PENDING").isEmpty() && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Restrito", "Sem permissão de gerenciamento.", event.getJDA().getSelfUser()).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Map<String, Map<String, String>> pending = DataManager.getAllPendingWhitelists();

        if (pending.isEmpty()) {
            event.replyEmbeds(EmbedUtils.createSuccess("✅ Fila Zerada", "Todos os tickets de whitelist foram processados.", event.getJDA().getSelfUser()).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📂 Monitoramento de Fila (Backlog)");
        embed.setColor(EmbedUtils.COLOR_WARNING);
        embed.setDescription("Visão geral das solicitações aguardando deliberação da equipe.");

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Map<String, String>> entry : pending.entrySet()) {
            String userId = entry.getKey();
            Map<String, String> answers = entry.getValue();
            String nick = answers.getOrDefault("q1_nick", "N/A");
            
            sb.append("🔹 `ID: ").append(userId).append("` | **").append(nick).append("** (<@").append(userId).append(">)\n");
            
            count++;
            if (count >= 10) {
                sb.append("\n*⚠️ Exibindo 10 de ").append(pending.size()).append(" registros totais.*");
                break;
            }
        }
        
        embed.addField("📋 Solicitações Pendentes", sb.toString(), false);
        embed.setFooter("Total em espera: " + pending.size(), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}