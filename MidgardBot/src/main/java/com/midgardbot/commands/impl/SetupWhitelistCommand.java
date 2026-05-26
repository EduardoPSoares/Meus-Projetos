package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import java.awt.Color;
import java.time.Instant;

/**
 * Comando de Setup de Whitelist.
 * Cria o painel principal de whitelist, contendo o botão para iniciar o formulário.
 */
public class SetupWhitelistCommand implements ISlashCommand {
    
    // Cores para identidade visual
    private static final Color COLOR_WHITELIST = Color.decode("#f0b132"); // Dourado/Pergaminho
    private static final Color COLOR_ERROR = Color.decode("#f04747");

    @Override
    public String getName() {
        return "setup-whitelist";
    }

    @Override
    public String getDescription() {
        return "Cria o painel de whitelist no canal atual";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_SETUP";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Verificação de Permissão
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_SETUP").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("⛔ Acesso Negado")
                .setDescription("Você não possui as credenciais necessárias (Administrador) para estabelecer o portal de whitelist.")
                .setColor(COLOR_ERROR);
            
            event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
            return;
        }

        // Construção do Embed Principal
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(COLOR_WHITELIST);
        embed.setTitle("📜 Registro de Cidadania • Midgard");
        
        // Descrição Imersiva
        embed.setDescription(
            "Seja bem-vindo ao portal de acesso. O reino de Midgard preza por uma comunidade focada em **Roleplay sério e imersivo**.\n\n" +
            "Para garantir a qualidade de nossa sociedade, todos os viajantes devem passar por um processo de aprovação antes de cruzar os portões."
        );

        // Bloco de Etapas
        embed.addField("📝 Processo de Admissão",
            "> `1` **Inicie sua Inscrição:** Clique no botão abaixo para abrir sua ficha de registro.\n" +
            "> `2` **Conte sua História:** Crie um personagem original e responda com criatividade.\n" +
            "> `3` **Análise da Equipe:** Nossos escribas avaliarão seu perfil (Prazo: 24h a 48h).\n" +
            "> `4` **Veredito:** Você será notificado automaticamente sobre sua aprovação.",
            false
        );

        // Bloco de Requisitos (Atualizado: Sem idade mínima)
        embed.addField("⚖️ Critérios de Aprovação",
            "✅ **Criatividade:** Histórias ricas e coerentes são essenciais.\n" +
            "✅ **Ortografia:** Escreva de forma clara e correta.\n" +
            "✅ **Conceitos:** Entenda o básico de RP (Meta-gaming, Power-gaming).",
            false
        );

        // Nota de Rodapé (Atualizado: 12 horas)
        embed.addField("⏳ Ciclo de Tentativas", 
            "> Você dispõe de **02 oportunidades** a cada **12 horas**.\n" +
            "> *Recomendamos que revise sua história com atenção antes de enviar.*", 
            false);

        embed.setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());
        embed.setImage(EmbedUtils.IMG_WHITELIST_PANEL); 
        embed.setFooter("Midgard RPG • Sistema de Whitelist", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());

        // Botão
        Button btnStart = Button.success("btn_whitelist_start", "🖋️ Iniciar Inscrição");

        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(btnStart)
                .queue();

        // Feedback Ephemeral para o Admin
        EmbedBuilder successEmbed = new EmbedBuilder()
            .setColor(Color.decode("#43b581"))
            .setDescription("Painel de Whitelist configurado neste canal.");

        event.replyEmbeds(successEmbed.build()).setEphemeral(true).queue();
    }
}