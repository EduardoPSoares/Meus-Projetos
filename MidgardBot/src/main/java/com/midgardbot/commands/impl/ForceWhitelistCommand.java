package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.WhitelistReviewHandler;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ForceWhitelistCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForceWhitelistCommand.class);

    @Override
    public String getName() {
        return "forcewhitelist";
    }

    @Override
    public String getDescription() {
        return "Importa uma whitelist antiga de um arquivo de texto (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário dono da whitelist", true),
            new OptionData(OptionType.ATTACHMENT, "arquivo", "O arquivo .txt com a whitelist antiga", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_FORCE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_FORCE").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Apenas Administradores.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("usuario").getAsUser();
        Message.Attachment attachment = event.getOption("arquivo").getAsAttachment();

        if (!"txt".equalsIgnoreCase(attachment.getFileExtension())) {
            event.reply("❌ O arquivo deve ser um .txt").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(attachment.getProxy().download().get(), StandardCharsets.UTF_8))) {
                // Ler o arquivo
                String content = reader.lines().collect(Collectors.joining("\n"));

                Map<String, String> answers = parseOldWhitelist(content);
                
                if (answers.isEmpty()) {
                    event.getHook().editOriginal("❌ Não foi possível extrair respostas do arquivo. Verifique o formato.").queue();
                    return;
                }

                // Adicionar timestamp atual
                answers.put("_timestamp", String.valueOf(System.currentTimeMillis()));
                
                // Salvar como pendente
                String answersJson = new com.google.gson.Gson().toJson(answers);
                DataManager.addPendingWhitelist(target.getId(), answers);
                DataManager.setStatus(target.getId(), com.midgardbot.data.WhitelistStatus.PENDING, null, null, answersJson, false, null);

                // Enviar para o canal da staff
                TextChannel staffChannel = event.getJDA().getTextChannelById(BotConfig.getStaffChannelId());
                if (staffChannel != null) {
                    WhitelistReviewHandler.sendWhitelistPage(staffChannel, target.getId(), target, 0, null);
                    event.getHook().editOriginal("✅ Whitelist importada e enviada para análise!").queue();
                } else {
                    event.getHook().editOriginal("⚠️ Whitelist salva, mas canal da staff não encontrado.").queue();
                }

            } catch (Exception e) {
                LOGGER.error("Erro ao processar forcewhitelist", e);
                event.getHook().editOriginal("❌ Erro ao processar arquivo: " + e.getMessage()).queue();
            }
        });
    }

    private Map<String, String> parseOldWhitelist(String content) {
        Map<String, String> answers = new LinkedHashMap<>();
        
        // Normalizar quebras de linha
        content = content.replace("\r\n", "\n");

        // Regex para capturar blocos de pergunta/resposta
        // Procura por "PERGUNTA X:" seguido de qualquer coisa até "RESPOSTA:" e depois o conteúdo até a próxima linha tracejada ou fim
        Pattern pattern = Pattern.compile("PERGUNTA\\s+(\\d+):.*?RESPOSTA:\\s*(.*?)(?=\\n-{10,}|\\n={10,}|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        Map<Integer, String> rawAnswers = new HashMap<>();

        while (matcher.find()) {
            int questionNum = Integer.parseInt(matcher.group(1));
            String answer = matcher.group(2).trim();
            rawAnswers.put(questionNum, answer);
        }

        // Mapeamento (Old ID -> New Key)
        // 1. Nick -> q1_nick
        if (rawAnswers.containsKey(1)) answers.put("q1_nick", rawAnswers.get(1));
        
        // 2. Idade -> q2_age
        if (rawAnswers.containsKey(2)) answers.put("q2_age", rawAnswers.get(2));
        
        // 3. XP -> q3_xp
        // 4. XP (Detalhes) -> append to q3_xp
        String xp = rawAnswers.getOrDefault(3, "");
        if (rawAnswers.containsKey(4)) {
            xp += "\n\n(Exp Detalhada): " + rawAnswers.get(4);
        }
        if (!xp.isEmpty()) answers.put("q3_xp", xp);

        // 5. Frustrações -> q6_frustration
        if (rawAnswers.containsKey(5)) answers.put("q6_frustration", rawAnswers.get(5));

        // 6. Protagonismo -> q7_protagonism
        if (rawAnswers.containsKey(6)) answers.put("q7_protagonism", rawAnswers.get(6));

        // 7. Bom RP -> q8_good_rp
        if (rawAnswers.containsKey(7)) answers.put("q8_good_rp", rawAnswers.get(7));

        // 8. Mau RP -> q9_bad_rp
        if (rawAnswers.containsKey(8)) answers.put("q9_bad_rp", rawAnswers.get(8));

        // 9. Quebra de Clima -> q11_break
        if (rawAnswers.containsKey(9)) answers.put("q11_break", rawAnswers.get(9));

        // 10. Meta/Power -> q10_concepts
        if (rawAnswers.containsKey(10)) answers.put("q10_concepts", rawAnswers.get(10));

        // 11. IC/OOC -> q12_ic_ooc
        if (rawAnswers.containsKey(11)) answers.put("q12_ic_ooc", rawAnswers.get(11));

        // 12. Secundários -> q5_secondary
        if (rawAnswers.containsKey(12)) answers.put("q5_secondary", rawAnswers.get(12));

        // 13. Punições -> q4_punish
        if (rawAnswers.containsKey(13)) answers.put("q4_punish", rawAnswers.get(13));

        // 14. Defeitos -> q13_flaws
        if (rawAnswers.containsKey(14)) answers.put("q13_flaws", rawAnswers.get(14));

        // 15. Lore -> q14_lore
        if (rawAnswers.containsKey(15)) answers.put("q14_lore", rawAnswers.get(15));

        return answers;
    }
}
