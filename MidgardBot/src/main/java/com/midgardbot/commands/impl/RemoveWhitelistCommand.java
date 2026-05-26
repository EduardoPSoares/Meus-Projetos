package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando de Remover Whitelist.
 * Remove o acesso de um jogador ao servidor e revoga o cargo de Cidadão.
 */
public class RemoveWhitelistCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "whitelist-remove";
    }

    @Override
    public String getDescription() {
        return "Executa a revogação de acesso de um usuário (Admin).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "Usuário alvo da revogação", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_REMOVE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para MANAGE_SERVER se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_REMOVE").isEmpty() && 
            !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Requer permissão de Gerenciamento de Servidor.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("usuario").getAsUser();
        String userId = target.getId();

        if (DataManager.getStatus(userId) == null && DataManager.getPendingWhitelist(userId) == null) {
            event.replyEmbeds(EmbedUtils.createError(
                "⚠️ Registro Inexistente",
                "Nenhum dado de whitelist encontrado para o usuário <@" + userId + ">.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        DataManager.removeWhitelistStatus(userId);
        DataManager.removePendingWhitelist(userId);

        event.replyEmbeds(EmbedUtils.createSuccess(
            "🗑️ Credenciais Revogadas",
            "O registro de whitelist do usuário foi removido permanentemente do sistema.\n\n" +
            "👤 **Alvo:** " + target.getAsMention() + "\n" +
            "🆔 **ID:** `" + userId + "`\n" +
            "📉 **Status:** Acesso revogado. Requer novo processo de admissão.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }
}