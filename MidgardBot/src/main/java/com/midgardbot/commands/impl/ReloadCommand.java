package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;

/**
 * Comando de Recarregar.
 * Permite recarregar configurações (como perguntas da whitelist) sem reiniciar o bot.
 */
public class ReloadCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Atualiza as configurações do sistema em tempo real (Hot-Reload).";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_RELOAD";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_RELOAD").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Apenas Administradores.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        try {
            WhitelistConfig.loadQuestions();
            com.midgardbot.config.MessagesConfig.load();
            
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "🔄 Sistema Atualizado",
                "Os arquivos de configuração (`whitelist_questions.json` e `messages.json`) foram recarregados com sucesso.\n" +
                "As alterações foram aplicadas imediatamente.",
                event.getJDA().getSelfUser()
            ).build()).queue();
            
        } catch (Exception e) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "❌ Falha na Leitura",
                "Erro crítico ao processar arquivos de configuração:\n`" + e.getMessage() + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}