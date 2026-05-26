package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando de Blacklist.
 * Permite que administradores bloqueiem usuários de interagir com o bot (ex: fazer whitelist).
 */
public class BlacklistCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public String getDescription() {
        return "Gerencia o registro de restrições de usuários (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "acao", "Selecione a operação administrativa", true)
                .addChoice("Aplicar Restrição (Add)", "add")
                .addChoice("Revogar Restrição (Remove)", "remove"),
            new OptionData(OptionType.USER, "usuario", "O usuário alvo da operação", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BLACKLIST";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Validação de Permissão
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_BLACKLIST").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Você não possui as credenciais administrativas necessárias para executar este comando.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("acao").getAsString();
        net.dv8tion.jda.api.entities.User target = event.getOption("usuario").getAsUser();
        String targetId = target.getId();

        // Operação: ADICIONAR
        if (action.equals("add")) {
            if (DataManager.isBlacklisted(targetId)) {
                // Alterado de texto simples para Embed de Erro para manter consistência profissional
                event.replyEmbeds(EmbedUtils.createError(
                    "⚠️ Operação Cancelada",
                    "O usuário **" + target.getName() + "** já consta no banco de dados da Blacklist.",
                    event.getJDA().getSelfUser()
                ).build()).setEphemeral(true).queue();
                return;
            }
            
            DataManager.addToBlacklist(targetId);
            
            event.replyEmbeds(EmbedUtils.createError( // Mantido createError pois é uma ação punitiva (vermelho/alerta)
                "🔨 Restrição Aplicada",
                "O usuário foi adicionado à lista de bloqueio com sucesso.\n\n" +
                "👤 **Usuário:** " + target.getAsMention() + "\n" +
                "🆔 **ID:** `" + targetId + "`\n" +
                "🚫 **Efeito:** O usuário está **impedido** de enviar novas solicitações de whitelist.",
                event.getJDA().getSelfUser()
            ).build()).queue();

        // Operação: REMOVER
        } else {
            if (!DataManager.isBlacklisted(targetId)) {
                // Alterado de texto simples para Embed de Erro
                event.replyEmbeds(EmbedUtils.createError(
                    "⚠️ Dados Inexistentes",
                    "Não foi encontrado nenhum registro de bloqueio para o usuário **" + target.getName() + "**.",
                    event.getJDA().getSelfUser()
                ).build()).setEphemeral(true).queue();
                return;
            }
            
            DataManager.removeFromBlacklist(targetId);
            
            event.replyEmbeds(EmbedUtils.createSuccess(
                "♻️ Restrição Revogada",
                "O registro de bloqueio foi removido do sistema.\n\n" +
                "👤 **Usuário:** " + target.getAsMention() + "\n" +
                "🆔 **ID:** `" + targetId + "`\n" +
                "✅ **Status:** O envio de whitelists foi **liberado** novamente para este usuário.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}