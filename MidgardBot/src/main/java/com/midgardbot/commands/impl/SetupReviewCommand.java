package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.features.whitelist.ReviewPanelManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Collections;
import java.util.List;

public class SetupReviewCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "setup-analisar";
    }

    @Override
    public String getDescription() {
        return "Cria o painel fixo de análise de whitelists (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_REVIEW_SETUP";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_REVIEW_SETUP").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }

        var allStatus = DataManager.getAllStatus();
        int pendingCount = (int) allStatus.values().stream()
                .filter(s -> s.status == com.midgardbot.data.WhitelistStatus.PENDING
                        || s.status == com.midgardbot.data.WhitelistStatus.REVIEWING
                        || s.status == com.midgardbot.data.WhitelistStatus.NEEDS_REVIEW
                        || s.status == com.midgardbot.data.WhitelistStatus.FLAGGED
                        || s.status == com.midgardbot.data.WhitelistStatus.PRIORITY
                        || s.status == com.midgardbot.data.WhitelistStatus.STANDBY)
                .filter(s -> s.answers != null && !s.answers.isEmpty() && !"{}".equals(s.answers))
                .count();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🛡️ ✦ CENTRAL DE ANÁLISE ✦")
            .setDescription("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                    "👋 **Bem-vindo ao painel de gerenciamento de Whitelists.**\n" +
                    "📝 Utilize este painel para revisar as aplicações pendentes.\n\n" +
                    "❓ **» COMO FUNCIONA**\n" +
                    "1️⃣ Clique em **Iniciar Análise** para puxar uma aplicação.\n" +
                    "2️⃣ O sistema entrará em **Modo Foco**.\n" +
                    "3️⃣ Revise as respostas e decida o veredito.\n" +
                    "4️⃣ Continue para a próxima aplicação.\n" +
                    "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            .setColor(java.awt.Color.decode("#FFAA00"))
            .addField("📊 » STATUS ATUAL", 
                (pendingCount > 0 ? "• Pendentes: **" + pendingCount + "**" : "• Pendentes: **0** (Tudo limpo!)"), 
                true)
            .addField("👀 » EM ANÁLISE", "• Staffs: Ninguém", true)
            .setFooter("MidgardBot • Sistema de Whitelist", null)
            .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(Button.primary("btn_start_review", "🧐 Iniciar Análise (" + pendingCount + ")")
                    .withDisabled(pendingCount == 0))
                .queue(msg -> {
                    ReviewPanelManager.setPanel(msg.getChannel().getId(), msg.getId());
                    ReviewPanelManager.updatePanel(event.getJDA());
                    event.reply("✅ Painel criado e vinculado com sucesso!").setEphemeral(true).queue();
                });
    }
}
