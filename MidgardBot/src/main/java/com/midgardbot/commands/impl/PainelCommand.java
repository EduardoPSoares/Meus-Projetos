package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class PainelCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "painel";
    }

    @Override
    public String getDescription() {
        return "Exibe o link de acesso ao Painel Administrativo Web";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BOTINFO";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String enabled = BotConfig.get("WEB_ENABLED");
        if (!"true".equalsIgnoreCase(enabled)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Painel Desativado",
                "O painel web administrativo não está ativo no momento.\nPeça a um administrador para ativar `WEB_ENABLED` na configuração.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String redirectUri = BotConfig.get("WEB_REDIRECT_URI");
        String panelUrl;
        if (redirectUri != null && !redirectUri.isEmpty()) {
            // Remove /callback do final para obter a URL base
            panelUrl = redirectUri.replace("/callback", "");
        } else {
            String port = BotConfig.get("WEB_PORT", "7070");
            panelUrl = "http://localhost:" + port;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("⚔️ Painel Administrativo — Midgard RPG")
            .setDescription(
                "Acesse o painel de controle do servidor para gerenciar\n" +
                "whitelists, punições, tickets, staff e muito mais.\n\n" +
                EmbedUtils.SEPARATOR
            )
            .setColor(EmbedUtils.COLOR_GOLD)
            .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl())
            .addField("🌐 Endereço", "`" + panelUrl + "`", true)
            .addField("🔐 Autenticação", "Discord OAuth2", true)
            .addField("\u200b", "\u200b", true) // spacer
            .addField("📋 Funcionalidades Disponíveis",
                "```\n" +
                "📊 Dashboard      — Status do bot e servidor\n" +
                "📜 Whitelists     — Gerenciar aprovações\n" +
                "⚖️ Moderação      — Punições e histórico\n" +
                "🎫 Tickets        — Acompanhar atendimentos\n" +
                "👮 Staff          — Estatísticas da equipe\n" +
                "👤 Jogadores      — Buscar e visualizar perfis\n" +
                "```", false)
            .addField("ℹ️ Como acessar",
                "Clique no botão abaixo e faça login com sua conta Discord.\n" +
                "Apenas membros com cargo autorizado podem entrar.", false)
            .setFooter("Midgard Admin Panel • Acesso restrito à staff", event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        event.replyEmbeds(embed.build())
            .addActionRow(
                Button.link(panelUrl, "🌐 Abrir Painel"),
                Button.link(panelUrl + "/dashboard", "📊 Ir ao Dashboard")
            )
            .setEphemeral(true)
            .queue();
    }
}
