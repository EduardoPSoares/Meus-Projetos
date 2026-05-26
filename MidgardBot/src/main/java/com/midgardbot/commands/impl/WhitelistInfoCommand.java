package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.InteractionUtils;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.google.gson.reflect.TypeToken;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.awt.Color;

public class WhitelistInfoCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "whitelistinfo";
    }

    @Override
    public String getDescription() {
        return "Verifica o status da whitelist de um usuário (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário para verificar", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_INFO";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_INFO").isEmpty();
        boolean hasPermission = event.getMember().hasPermission(Permission.ADMINISTRATOR);
        
        if (!isConfigured && !hasPermission) {
            event.reply("❌ Sem permissão. Apenas Administradores.").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("usuario");
        if (userOption == null) {
            event.reply("Usuário não especificado.").setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();
        WhitelistStatusInfo info = DataManager.getStatus(target.getId());

        if (info == null) {
            event.replyEmbeds(EmbedUtils.createError(
                "Não Encontrado",
                "Nenhuma informação de whitelist encontrada para " + target.getAsMention(),
                event.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        Map<String, String> answers = null;
        if (info.answers != null && !info.answers.isEmpty()) {
            try {
                answers = new com.google.gson.Gson().fromJson(
                    info.answers, 
                    new TypeToken<Map<String, String>>(){}.getType()
                );
            } catch (Exception e) {
                // ignore
            }
        }
        
        if (answers == null) {
            answers = DataManager.getPendingWhitelist(target.getId());
        }

        if (answers == null) {
            answers = new LinkedHashMap<>();
            answers.put("Erro", "Respostas não encontradas.");
        }

        List<MessageEmbed> pages = buildPages(target, info, answers);
        
        // Register pages for pagination
        event.replyEmbeds(pages.get(0))
            .addActionRow(
                Button.secondary("btn_log_prev", "⬅️ Anterior").withDisabled(true),
                Button.secondary("btn_log_next", "Próxima ➡️").withDisabled(pages.size() <= 1)
            )
            .queue(hook -> {
                hook.retrieveOriginal().queue(msg -> {
                    InteractionUtils.registerLogView(msg.getId(), pages);
                });
            });
    }

    private List<MessageEmbed> buildPages(User target, WhitelistStatusInfo info, Map<String, String> answers) {
        List<MessageEmbed> pages = new ArrayList<>();
        
        Color color = Color.GRAY;
        String statusText = info.status.icon + " " + info.status.label;
        
        switch (info.status) {
            case APPROVED:
            case EXCELLENT:
                color = Color.GREEN;
                break;
            case REJECTED:
                color = Color.RED;
                break;
            case PENDING:
            case REVIEWING:
            case NEEDS_REVIEW:
            case FLAGGED:
            case PRIORITY:
                color = Color.ORANGE;
                break;
            case STANDBY:
                color = Color.YELLOW;
                break;
        }

        // Create 3 pages
        for (int i = 0; i < 3; i++) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Status da Whitelist: " + target.getName());
            eb.setColor(color);
            eb.setThumbnail(target.getAvatarUrl());
            
            eb.addField("Status", statusText, true);
            eb.addField("Nickname", info.nickname != null ? info.nickname : "N/A", true);
            eb.addField("Data", info.timestamp != null ? info.timestamp : "N/A", true);
            
            if (info.staffId != null && !info.staffId.isEmpty()) {
                eb.addField("Staff Responsável", "<@" + info.staffId + ">", true);
            }
            
            if (info.reason != null && !info.reason.isEmpty()) {
                eb.addField("Motivo/Obs", info.reason, false);
            }

            // Add AI Score if present
            if (answers.containsKey("_ai_score")) {
                eb.addField("🤖 Score IA", answers.get("_ai_score"), true);
            }

            eb.addField("📂 Seção", WhitelistConfig.getPageTitle(i), false);

            Map<String, String> questions = WhitelistConfig.getQuestionsByPage(i);
            for (Map.Entry<String, String> entry : questions.entrySet()) {
                String qKey = entry.getKey();
                String qText = entry.getValue();
                String answer = answers.getOrDefault(qKey, "*Sem resposta*");
                
                // Truncate if too long
                if (answer.length() > 1024) answer = answer.substring(0, 1020) + "...";
                
                eb.addField("📝 " + qText, answer, false);
            }
            
            eb.setFooter("Página " + (i + 1) + "/3 • Use os botões para navegar", null);
            pages.add(eb.build());
        }
        return pages;
    }
}
