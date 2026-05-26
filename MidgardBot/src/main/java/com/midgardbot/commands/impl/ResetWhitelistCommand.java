package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

/**
 * Comando de Resetar Whitelist (Perigo).
 * Apaga TODOS os dados de whitelist do banco de dados.
 * Requer confirmação explícita via botão.
 */
public class ResetWhitelistCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "reset-whitelist-data";
    }

    @Override
    public String getDescription() {
        return "Executa a purga completa do banco de dados de Whitelist (CRÍTICO)";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_RESET";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_RESET").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Apenas Administradores.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(EmbedUtils.createError(
            "☣️ ALERTA DE PURGA DE DADOS",
            "Você solicitou a exclusão **TOTAL E IRREVERSÍVEL** de todos os registros de whitelist.\n\n" +
            "📉 **Impacto:**\n" +
            "• Todos os aprovados perderão o acesso.\n" +
            "• Todos os pendentes serão descartados.\n" +
            "• Histórico de reprovações será apagado.\n\n" +
            "Confirme a execução do protocolo de limpeza.",
            event.getJDA().getSelfUser()
        ).build())
        .addActionRow(
            Button.danger("reset_whitelist_confirm", "🔥 Confirmar Exclusão Total"),
            Button.secondary("reset_whitelist_cancel", "Abortar Operação")
        )
        .setEphemeral(true)
        .queue();
    }
}