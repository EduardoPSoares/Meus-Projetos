package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando de Desvínculo Forçado.
 * Permite que administradores removam o vínculo entre uma conta Discord e Minecraft.
 * Usado em casos de perda de conta ou punições.
 */
public class ForceUnlinkCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "forceunlink";
    }

    @Override
    public String getDescription() {
        return "Executa a desassociação forçada de uma conta (Admin Override)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "Usuário alvo da revogação", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LINK_FORCE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LINK_FORCE").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Requer nível Administrativo.", event.getJDA().getSelfUser()).build())
                .setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("usuario");
        User targetUser = userOption.getAsUser();
        String targetId = targetUser.getId();

        if (LinkManager.unlinkAccount(targetId)) {
            event.replyEmbeds(EmbedUtils.createSuccess(
                "🔗 Vínculo Revogado",
                "A associação de conta foi removida manualmente do banco de dados.\n\n" +
                "👤 **Usuário:** " + targetUser.getAsMention() + "\n" +
                "🆔 **ID:** `" + targetId + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            event.replyEmbeds(EmbedUtils.createError(
                "⚠️ Registro Não Encontrado",
                "Não há credenciais ativas vinculadas a este usuário.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
        }
    }
}