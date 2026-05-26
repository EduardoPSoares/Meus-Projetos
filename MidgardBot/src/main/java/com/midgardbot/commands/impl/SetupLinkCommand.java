package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import java.awt.Color;

/**
 * Comando de Setup de Link.
 * Cria o painel visual onde os jogadores podem iniciar o processo de vincular suas contas.
 */
public class SetupLinkCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "setup-link";
    }

    @Override
    public String getDescription() {
        return "Inicializa o painel de sincronização de contas (Admin)";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LINK_SETUP";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LINK_SETUP").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Requer permissão administrativa.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔗 Integração de Contas");
        embed.setDescription("Sincronize sua identidade Discord com o servidor In-Game para desbloquear recursos exclusivos, economia unificada e acesso à loja.");
        embed.setColor(Color.decode("#FFA500")); 

        embed.addField("🛠️ Instruções de Vínculo", 
            "1. Acesse o servidor **Minecraft**.\n" +
            "2. Clique no botão abaixo para iniciar o processo de vinculação.\n" +
            "3. Siga as instruções apresentadas.", 
            false);
        
        embed.addField("💎 Benefícios",
            "• Proteção de conta aprimorada\n• Sincronização de cargos", false);

        embed.setFooter("Midgard Security System • Sync Module", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(
                    Button.primary("btn_link_account", "🔐 Vincular Conta"),
                    Button.link("https://discord.gg/midgard", "📘 Documentação de Ajuda")
                )
                .queue();

        event.replyEmbeds(EmbedUtils.createSuccess(
            "✅ Painel Implantado",
            "O módulo de vinculação está ativo neste canal.",
            event.getJDA().getSelfUser()
        ).build()).setEphemeral(true).queue();
    }
}