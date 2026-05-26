package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;

/**
 * Comando para forçar a sincronização do banco de dados.
 * Útil quando o cache local do bot está desatualizado em relação ao banco.
 */
public class SyncDatabaseCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "sync-database";
    }

    @Override
    public String getDescription() {
        return "Força a ressincronização completa do cache com o Banco de Dados (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_SYNC_DB"; // Nova permissão específica, se necessário
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Acesso Negado", 
                "Apenas administradores podem usar este comando.", 
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        try {
            // Força a sincronização
            DataManager.syncStatusFromDatabase();
            DataManager.syncPendingFromDatabase();
            
            // Obtém estatísticas
            int totalWhitelists = DataManager.getAllStatus().size();
            long totalPending = DataManager.getAllStatus().values().stream()
                    .filter(s -> s.status == com.midgardbot.data.WhitelistStatus.PENDING
                            || s.status == com.midgardbot.data.WhitelistStatus.REVIEWING
                            || s.status == com.midgardbot.data.WhitelistStatus.NEEDS_REVIEW
                            || s.status == com.midgardbot.data.WhitelistStatus.FLAGGED
                            || s.status == com.midgardbot.data.WhitelistStatus.PRIORITY
                            || s.status == com.midgardbot.data.WhitelistStatus.STANDBY)
                    .filter(s -> s.answers != null && !s.answers.isEmpty() && !"{}".equals(s.answers))
                    .count();

            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Sincronização Concluída",
                "O cache local foi atualizado com sucesso a partir do banco de dados.\n\n" +
                "📊 **Estatísticas Atuais:**\n" +
                "• Whitelists (Cache): **" + totalWhitelists + "**\n" +
                "• Pendentes (Cache): **" + totalPending + "**",
                event.getJDA().getSelfUser()
            ).build()).queue();

        } catch (Exception e) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Erro na Sincronização",
                "Falha ao sincronizar com o banco de dados: " + e.getMessage(),
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}
