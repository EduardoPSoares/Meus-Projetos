package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils; // Adicionado import
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando de Limpeza de Tickets.
 * Permite excluir canais de tickets em massa (abertos, fechados ou todos).
 * Útil para manutenção e organização de categorias.
 */
public class ClearTicketsCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "cleartickets";
    }

    @Override
    public String getDescription() {
        return "Executa a limpeza em massa dos registros de atendimento (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "tipo", "Categoria de tickets para expurgo", true)
            .addChoice("Apenas Abertos (Active)", "open")
            .addChoice("Apenas Arquivados (Closed)", "closed")
            .addChoice("Limpeza Total (All)", "all"));
        return options;
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_TICKET_CLEAR";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permission check
        if (BotConfig.getAuthorizedRoles("PERM_CMD_TICKET_CLEAR").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
             event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado", 
                "Você não possui credenciais para executar exclusões em massa.", 
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
             return;
        }

        String type = event.getOption("tipo").getAsString();
        String typeText = "";
        String impactLevel = "";

        switch (type) {
            case "open": 
                typeText = "EM ABERTO"; 
                impactLevel = "Médio";
                break;
            case "closed": 
                typeText = "ARQUIVADOS"; 
                impactLevel = "Baixo";
                break;
            case "all": 
                typeText = "TODOS (Global)"; 
                impactLevel = "CRÍTICO";
                break;
        }

        event.replyEmbeds(EmbedUtils.createError( // Usando estilo de erro para alerta vermelho
                "⚠️ Protocolo de Expurgo Solicitado",
                "Você iniciou um processo de **exclusão em massa**.\n" +
                "Esta operação não poderá ser desfeita e todos os dados selecionados serão perdidos permanentemente.\n\n" +
                "📂 **Alvo:** Tickets `" + typeText + "`\n" +
                "📉 **Nível de Impacto:** `" + impactLevel + "`\n\n" +
                "Confirme sua credencial para prosseguir.",
                event.getJDA().getSelfUser()
            ).build())
            .addActionRow(
                Button.danger("btn_confirm_clear_tickets:" + type, "Confirmar Exclusão"),
                Button.secondary("btn_cancel_clear_tickets", "Cancelar Operação")
            )
            .setEphemeral(true)
            .queue();
    }
}