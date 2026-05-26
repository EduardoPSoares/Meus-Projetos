package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando de Alterar Apelido.
 * Facilita a alteração de apelidos de membros, útil para padronização (ex: Nick do Minecraft).
 */
public class SetNickCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "setnick";
    }

    @Override
    public String getDescription() {
        return "Atualiza a identificação (apelido) de um membro.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "Usuário alvo", true),
            new OptionData(OptionType.STRING, "nick", "Nova identificação", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_SETNICK";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para NICKNAME_MANAGE se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_SETNICK").isEmpty() && 
            !event.getMember().hasPermission(Permission.NICKNAME_MANAGE)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Requer permissão de Gerenciar Apelidos.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        User targetUser = event.getOption("usuario").getAsUser();
        String newNick = event.getOption("nick").getAsString().trim();
        
        // Atualiza no Banco de Dados
        WhitelistStatusInfo currentStatus = DataManager.getStatus(targetUser.getId());
        
        WhitelistStatus status = WhitelistStatus.APPROVED;
        String reason = "Manual setnick";
        String answers = "{}";
        boolean terms = false;

        if (currentStatus != null) {
            status = currentStatus.status;
            reason = currentStatus.reason;
            answers = currentStatus.answers;
            terms = currentStatus.termsAccepted;
        }

        DataManager.setStatus(
            targetUser.getId(),
            status,
            reason,
            newNick,
            answers,
            terms,
            event.getUser().getId()
        );

        event.replyEmbeds(EmbedUtils.createSuccess(
            "🏷️ Identidade Atualizada (Banco de Dados)",
            "O apelido foi atualizado no banco de dados com sucesso.\n\n" +
            "👤 **Usuário:** " + targetUser.getAsMention() + "\n" +
            "✏️ **Novo Valor:** `" + newNick + "`\n" +
            "ℹ️ **Nota:** O apelido no Discord não foi alterado.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }
}