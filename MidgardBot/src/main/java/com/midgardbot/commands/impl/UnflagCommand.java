package com.midgardbot.commands.impl;

import com.midgardbot.commands.CommandContext;
import com.midgardbot.commands.ICommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;

import java.awt.Color;
import java.util.List;

/**
 * Comando de Unflag (Texto).
 * Remove a marcação de "Suspeito de IA" de um usuário, permitindo que ele tente a whitelist novamente sem restrições severas.
 */
public class UnflagCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) {
        if (!ctx.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Requer permissão de Administrador.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        List<String> args = ctx.getArgs();
        if (args.isEmpty()) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createWarning(
                "⚠️ Sintaxe Incorreta",
                "O comando requer a identificação do alvo.\n\n" +
                "**Modelo:** `!unflag <ID_Usuario>`\n" +
                "**Exemplo:** `!unflag 123456789012345678`",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        String targetId = args.get(0);
        
        if (DataManager.isFlagged(targetId)) {
            DataManager.unflagUser(targetId);
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createSuccess(
                "🛡️ Restrição Revogada",
                "O usuário foi retirado da lista de observação (Flag).\n\n" +
                "🆔 **Alvo:** `" + targetId + "`\n" +
                "✅ **Status:** As limitações de idade foram removidas.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createInfo(
                "ℹ️ Registro Limpo",
                "O ID `" + targetId + "` não consta na base de dados de restrições.\n" +
                "Nenhuma intervenção necessária.",
                ctx.getJDA().getSelfUser()
            ).setColor(Color.decode("#95a5a6")).build()).queue();
        }
    }

    @Override
    public String getName() {
        return "unflag";
    }

    @Override
    public String getHelp() {
        return "Remove manualmente a marcação de restrição etária (Flag) de um usuário.";
    }
}