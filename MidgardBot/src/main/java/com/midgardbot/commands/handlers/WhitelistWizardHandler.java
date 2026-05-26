package com.midgardbot.commands.handlers;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.whitelist.WhitelistCache;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.features.whitelist.ReviewPanelManager;
import com.midgardbot.utils.EmbedUtils;
import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handler para o fluxo de whitelist do jogador (wizard).
 * Inclui: início, termos, partes 1-3, confirmação, envio e backup.
 */
public final class WhitelistWizardHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistWizardHandler.class);

    private WhitelistWizardHandler() {}

    // ========================
    //   BUTTON INTERACTIONS
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        String userId = event.getUser().getId();

        if (id.equals("btn_whitelist_start")) { handleWhitelistStart(event); return true; }
        if (id.equals("btn_terms_accept")) { handleTermsAccept(event); return true; }
        if (id.equals("btn_whitelist_part2")) { openWhitelistModal(event, "modal_whitelist_part2", "Whitelist - Parte 2/3", WhitelistConfig.getPart2Questions()); return true; }
        if (id.equals("btn_whitelist_part3")) { openWhitelistModal(event, "modal_whitelist_part3", "Whitelist - Parte 3/3", WhitelistConfig.getPart3Questions()); return true; }
        if (id.equals("btn_whitelist_edit_part3")) { openWhitelistModal(event, "modal_whitelist_part3", "Whitelist - Parte 3/3", WhitelistConfig.getPart3Questions(), WhitelistCache.getAnswers(userId)); return true; }
        if (id.equals("btn_whitelist_confirm")) { event.deferReply(true).queue(hook -> submitWhitelistFromHook(hook, event.getUser(), event.getJDA())); return true; }
        if (id.equals("btn_whitelist_cancel")) { handleWhitelistCancel(event); return true; }

        return false;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (modalId.startsWith("modal_whitelist_part")) {
            handleWhitelistPartModal(event);
            return true;
        }
        return false;
    }

    // ========================
    //   BUTTON HANDLERS
    // ========================

    private static void handleWhitelistStart(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        // Limpeza de estados zumbi
        if (DataManager.getPendingWhitelist(userId) != null) {
            WhitelistStatusInfo status = DataManager.getStatus(userId);
            if (status != null && (status.status == WhitelistStatus.REJECTED || status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.EXCELLENT)) {
                DataManager.removePendingWhitelist(userId);
            }
        }

        // Verificações
        if (DataManager.isMaintenanceMode() || !DataManager.isWhitelistEnabled()) {
            event.replyEmbeds(EmbedUtils.createWarning("Sistema Fechado",
                "🚧 O sistema de whitelist está temporariamente fechado.\nPor favor, tente novamente mais tarde.",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        if (DataManager.isBlacklisted(userId)) {
            event.replyEmbeds(EmbedUtils.createError("Acesso Negado",
                "🚫 Você está bloqueado de enviar novas whitelists.\nSe acredita que isso é um erro, contate a administração.",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        // Idade mínima
        String underAgeRoleId = BotConfig.getUnderAgeRoleId();
        if (underAgeRoleId != null && !underAgeRoleId.isEmpty()) {
            if (event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(underAgeRoleId))) {
                event.replyEmbeds(EmbedUtils.createError("Requisitos Não Atendidos",
                    "🚫 **Idade Mínima Necessária**\n\nInfelizmente, você não atende aos requisitos de idade mínima para jogar no Midgard RPG.\nNosso servidor é destinado a um público mais maduro para garantir a qualidade do Roleplay.\n\nAgradecemos seu interesse e esperamos vê-lo no futuro!",
                    event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                return;
            }
        }

        // Limite de tentativas
        if (!DataManager.canAttemptWhitelist(userId)) {
            long nextAttempt = DataManager.getNextAttemptTime(userId);
            long remaining = nextAttempt - System.currentTimeMillis();
            long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remaining);
            long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;

            event.replyEmbeds(EmbedUtils.createError("Limite de Tentativas Atingido",
                "### " + EmbedUtils.ICON_WAIT + " Suas tentativas acabaram!\n\nPara garantir a qualidade de todas as análises, limitamos as aplicações a **duas a cada 12 horas**.\n\n" +
                "**O que fazer agora?**\n> Use este tempo para refinar a história do seu personagem, reler nossas regras e voltar mais tarde com respostas ainda mais incríveis!\n\n" +
                "⏳ **Próxima tentativa em:** " + hours + "h " + minutes + "m",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        // Termos
        WhitelistStatusInfo status = DataManager.getStatus(userId);
        if (status == null || !status.termsAccepted) {
            EmbedBuilder termsEmbed = new EmbedBuilder();
            termsEmbed.setTitle("📜 Termos de Uso e Responsabilidade - Midgard");
            termsEmbed.setColor(java.awt.Color.ORANGE);
            termsEmbed.setDescription(
                "**1. Declaração de Idade e Legalidade**\n" +
                "Ao ingressar neste servidor, o usuário declara possuir idade compatível com as regras da plataforma Discord e com as normas legais vigentes em seu país de residência.\n\n" +
                "**2. Isenção de Responsabilidade**\n" +
                "O servidor **Midgard** não se responsabiliza por:\n" +
                "• Informações falsas fornecidas pelo usuário, incluindo sua idade.\n" +
                "• Acesso realizado por menores sem a devida autorização ou supervisão de responsáveis legais.\n" +
                "• Transações financeiras (compras/doações) realizadas sem consentimento do titular do meio de pagamento.\n\n" +
                "*Toda responsabilidade sobre o acesso e transações recai exclusivamente sobre o usuário e/ou seus responsáveis legais.*\n\n" +
                "**3. Morte Permanente e Perda de Itens (Roleplay)**\n" +
                "Este é um servidor de Roleplay sério. Ao jogar, você concorda que:\n" +
                "• A Morte Permanente (PK) e a perda de itens são consequências naturais da narrativa.\n" +
                "• O servidor não reverterá mortes ou itens perdidos decorrentes de ações em RP, exceto em casos comprovados de falha técnica (bug).\n" +
                "• A recusa em aceitar consequências de RP será tratada como infração às regras.\n\n" +
                "**4. Sanções**\n" +
                "Violações destas regras podem resultar em punições severas, incluindo banimento permanente, sem direito a reembolso de doações."
            );
            termsEmbed.setFooter("Ao clicar em 'Aceitar e Continuar', você confirma que leu e concorda com todos os termos acima.");

            event.replyEmbeds(termsEmbed.build())
                .addActionRow(Button.success("btn_terms_accept", "✅ Li e Aceito os Termos"))
                .setEphemeral(true)
                .queue();
            return;
        }

        WhitelistCache.clear(userId);
        openWhitelistModal(event, "modal_whitelist_part1", "Whitelist - Parte 1/3", WhitelistConfig.getPart1Questions());
    }

    private static void handleTermsAccept(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        WhitelistStatusInfo current = DataManager.getStatus(userId);
        String currentNickname = (current != null) ? current.nickname : event.getUser().getName();
        String currentAnswers = (current != null) ? current.answers : null;
        WhitelistStatus currentStatus = (current != null) ? current.status : WhitelistStatus.PENDING;
        String currentStaffId = (current != null) ? current.staffId : null;

        DataManager.setStatus(userId, currentStatus, null, currentNickname, currentAnswers, true, currentStaffId);
        WhitelistCache.clear(userId);
        openWhitelistModal(event, "modal_whitelist_part1", "Whitelist - Parte 1/3", WhitelistConfig.getPart1Questions());
    }

    private static void handleWhitelistCancel(ButtonInteractionEvent event) {
        EmbedBuilder cancelEmbed = EmbedUtils.createWarning("Envio Cancelado",
            "Você cancelou o envio da whitelist.\n\nSe desejar, clique abaixo para editar suas respostas e tentar novamente.",
            event.getJDA().getSelfUser());

        if (event.isAcknowledged()) {
            event.getHook().editOriginalEmbeds(cancelEmbed.build())
                .setActionRow(
                    Button.secondary("btn_whitelist_edit_part3", "✏️ Editar Parte 3"),
                    Button.success("btn_whitelist_confirm", "✅ Confirmar Envio")
                ).queue();
        } else {
            event.editMessageEmbeds(cancelEmbed.build())
                .setActionRow(
                    Button.secondary("btn_whitelist_edit_part3", "✏️ Editar Parte 3"),
                    Button.success("btn_whitelist_confirm", "✅ Confirmar Envio")
                ).queue();
        }
    }

    // ========================
    //    MODAL HANDLERS
    // ========================

    private static void handleWhitelistPartModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        String userId = event.getUser().getId();

        // Validações de entrada
        for (ModalMapping mapping : event.getInteraction().getValues()) {
            String answer = mapping.getAsString().trim();
            if (answer.length() < 20 && (mapping.getId().equals("q14_lore") || mapping.getId().equals("q10_concepts"))) {
                EmbedBuilder errorEmbed = EmbedUtils.createError("Resposta Muito Curta!",
                    "Sua resposta para **" + mapping.getId() + "** está muito curta.\n\n📝 **Mínimo:** 20 caracteres\n❌ **Atual:** " + answer.length() + " caracteres\n\nPor favor, desenvolva melhor sua resposta para continuar.",
                    event.getJDA().getSelfUser());
                event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();

                EmbedBuilder dmEmbed = EmbedUtils.createWarning("⚠️ Atenção: Resposta Muito Curta",
                    "Olá! Percebemos que você tentou enviar uma resposta muito curta no formulário de Whitelist.\n\n**Pergunta:** `" + mapping.getId() + "`\n**Sua Resposta:** `" + answer + "`\n\nPara garantir a qualidade do RP, pedimos que desenvolva um pouco mais suas ideias (mínimo de 20 caracteres).\nTente novamente no canal de whitelist!",
                    event.getJDA().getSelfUser());
                InteractionUtils.sendDM(event.getUser(), dmEmbed.build());
                return;
            }
        }

        // Validação de nick e idade (Parte 1)
        if (modalId.equals("modal_whitelist_part1")) {
            ModalMapping nickMapping = event.getValue("q1_nick");
            if (nickMapping != null) {
                String nick = nickMapping.getAsString().trim();
                if (!nick.matches("[a-zA-Z0-9_]+")) {
                    event.replyEmbeds(EmbedUtils.createError("Nick Inválido",
                        "O nick do Minecraft deve conter apenas letras, números e sublinhado (_).\n\n❌ **Valor inserido:** " + nick,
                        event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                    return;
                }
            }

            ModalMapping ageMapping = event.getValue("q2_age");
            if (ageMapping != null) {
                String ageStr = ageMapping.getAsString().trim().replaceAll("[^0-9]", "");
                try {
                    if (ageStr.isEmpty()) throw new NumberFormatException("Vazio");
                    int age = Integer.parseInt(ageStr);
                    if (age < 0 || age > 120) throw new NumberFormatException("Idade fora do comum");
                } catch (NumberFormatException e) {
                    event.replyEmbeds(EmbedUtils.createError("Idade Inválida",
                        "Por favor, insira uma idade válida (apenas números).\n\n❌ **Valor inserido:** " + ageMapping.getAsString(),
                        event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                    return;
                }
            }
        }

        // Salvar respostas
        event.getInteraction().getValues().forEach(mapping -> {
            WhitelistCache.addAnswer(userId, mapping.getId(), mapping.getAsString());
        });

        if (modalId.equals("modal_whitelist_part1")) {
            event.reply("✅ **Parte 1/3 concluída!**\n\n**Progresso:**\n✅ Informações Básicas\n⏳ História do Personagem\n⏳ Conhecimento do Servidor\n\nContinue para a próxima etapa!")
                .setEphemeral(true)
                .addActionRow(Button.primary("btn_whitelist_part2", "➡️ Continuar"))
                .queue();
        } else if (modalId.equals("modal_whitelist_part2")) {
            event.reply("✅ **Parte 2/3 concluída!**\n\n**Progresso:**\n✅ Informações Básicas\n✅ História do Personagem\n⏳ Conhecimento do Servidor\n\nÓtimo trabalho! Última etapa!")
                .setEphemeral(true)
                .addActionRow(Button.primary("btn_whitelist_part3", "➡️ Finalizar"))
                .queue();
        } else if (modalId.equals("modal_whitelist_part3")) {
            sendWhitelistConfirmation(event);
        }
    }

    // ========================
    //    SUBMIT & CONFIRM
    // ========================

    private static void sendWhitelistConfirmation(ModalInteractionEvent event) {
        String userId = event.getUser().getId();

        String staffChannelId = BotConfig.getStaffChannelId();
        if (staffChannelId == null || staffChannelId.equals("000000000000000000")) {
            event.replyEmbeds(EmbedUtils.createError("Erro de Configuração",
                EmbedUtils.SEPARATOR + "\n\nO canal da staff não está configurado.\n\nPor favor, entre em contato com um **Administrador**.\n\n" + EmbedUtils.SEPARATOR,
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        TextChannel staffChannel = event.getJDA().getTextChannelById(staffChannelId);
        if (staffChannel == null) {
            event.replyEmbeds(EmbedUtils.createError("Erro de Configuração",
                EmbedUtils.SEPARATOR + "\n\nO canal da staff não foi encontrado.\n\nPor favor, entre em contato com um **Administrador**.\n\n" + EmbedUtils.SEPARATOR,
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        Map<String, String> answers = new HashMap<>(WhitelistCache.getAnswers(userId));
        boolean hasPart1 = !WhitelistConfig.getPart1Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart1Questions().containsKey(k));
        boolean hasPart2 = !WhitelistConfig.getPart2Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart2Questions().containsKey(k));
        boolean hasPart3 = !WhitelistConfig.getPart3Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart3Questions().containsKey(k));

        if (answers.isEmpty() || !hasPart1 || !hasPart2 || !hasPart3) {
            event.replyEmbeds(EmbedUtils.createError("Sessão Expirada",
                "Sua sessão de whitelist expirou ou está incompleta.\n\nIsso acontece se você demorar mais de 1 hora para preencher todas as etapas.\nPor favor, inicie o processo novamente.",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            WhitelistCache.clear(userId);
            return;
        }

        int totalAnswers = answers.size();
        int totalQuestions = WhitelistConfig.getPart1Questions().size() + WhitelistConfig.getPart2Questions().size() + WhitelistConfig.getPart3Questions().size();
        int totalChars = answers.values().stream().mapToInt(String::length).sum();

        EmbedBuilder confirmEmbed = new EmbedBuilder()
            .setTitle("Confirmar Envio da Whitelist")
            .setDescription("Pré-checagem concluída. Revise e confirme seu envio.")
            .addField("Respostas", totalAnswers + " respostas salvas", true)
            .addField("Perguntas", totalQuestions + " perguntas no formulário", true)
            .addField("Caracteres", totalChars + " caracteres no total", true)
            .setColor(Color.YELLOW);

        event.replyEmbeds(confirmEmbed.build())
            .setEphemeral(true)
            .addActionRow(
                Button.success("btn_whitelist_confirm", "✅ Confirmar Envio"),
                Button.secondary("btn_whitelist_edit_part3", "✏️ Editar Parte 3"),
                Button.danger("btn_whitelist_cancel", "❌ Cancelar")
            )
            .queue();
    }

    public static void submitWhitelistFromHook(InteractionHook hook, User user, net.dv8tion.jda.api.JDA jda) {
        String userId = user.getId();

        if (DataManager.getPendingWhitelist(userId) != null) {
            WhitelistStatusInfo status = DataManager.getStatus(userId);
            if (status != null && (status.status == WhitelistStatus.REJECTED || status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.EXCELLENT)) {
                DataManager.removePendingWhitelist(userId);
                LOGGER.warn("Whitelist pendente zumbi removida para usuario: " + userId);
            } else {
                hook.sendMessageEmbeds(EmbedUtils.createError("Já Enviado", "Sua whitelist já foi enviada e está aguardando análise.", jda.getSelfUser()).build()).setEphemeral(true).queue();
                return;
            }
        }

        String staffChannelId = BotConfig.getStaffChannelId();
        if (staffChannelId == null || staffChannelId.equals("000000000000000000")) {
            hook.sendMessageEmbeds(EmbedUtils.createError("Erro de Configuração",
                EmbedUtils.SEPARATOR + "\n\nO canal da staff não está configurado.\n\nPor favor, entre em contato com um **Administrador**.\n\n" + EmbedUtils.SEPARATOR,
                jda.getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        TextChannel staffChannel = jda.getTextChannelById(staffChannelId);
        if (staffChannel == null) {
            hook.sendMessageEmbeds(EmbedUtils.createError("Erro de Configuração",
                EmbedUtils.SEPARATOR + "\n\nO canal da staff não foi encontrado.\n\nPor favor, entre em contato com um **Administrador**.\n\n" + EmbedUtils.SEPARATOR,
                jda.getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        Map<String, String> answers = new HashMap<>(WhitelistCache.getAnswers(userId));
        answers.put("timestamp", String.valueOf(System.currentTimeMillis()));

        boolean hasPart1 = !WhitelistConfig.getPart1Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart1Questions().containsKey(k));
        boolean hasPart2 = !WhitelistConfig.getPart2Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart2Questions().containsKey(k));
        boolean hasPart3 = !WhitelistConfig.getPart3Questions().isEmpty() && answers.keySet().stream().anyMatch(k -> WhitelistConfig.getPart3Questions().containsKey(k));

        if (answers.isEmpty() || !hasPart1 || !hasPart2 || !hasPart3) {
            hook.sendMessageEmbeds(EmbedUtils.createError("Sessão Expirada",
                "Sua sessão de whitelist expirou ou está incompleta.\n\nIsso acontece se você demorar mais de 1 hora para preencher todas as etapas.\nPor favor, inicie o processo novamente.",
                jda.getSelfUser()).build()).setEphemeral(true).queue();
            WhitelistCache.clear(userId);
            return;
        }

        // Flagging de idade
        String ageStr = answers.getOrDefault("q2_age", "0").replaceAll("[^0-9]", "");
        try {
            if (!ageStr.isEmpty()) {
                int age = Integer.parseInt(ageStr);
                if (age < 14) {
                    DataManager.flagUser(userId);
                    LOGGER.info("Usuario " + userId + " marcado como menor de idade (" + age + " anos).");
                }
            }
        } catch (NumberFormatException ignored) {}

        // Persist data BEFORE sending success message to prevent data loss
        if (!answers.containsKey("_timestamp")) answers.put("_timestamp", String.valueOf(System.currentTimeMillis()));
        DataManager.registerAttempt(userId);
        DataManager.addPendingWhitelist(userId, answers);
        String answersJson = new Gson().toJson(answers);
        DataManager.setStatus(userId, WhitelistStatus.PENDING, null, null, answersJson, false, null);

        EmbedBuilder successEmbed = EmbedUtils.createSuccess(
            "Whitelist Enviada com Sucesso!",
            "**Seu formulário foi recebido e está em análise!**\n\nNossa equipe irá revisar suas respostas com atenção. O resultado será divulgado no canal apropriado em breve.\n\n" +
            "> " + EmbedUtils.ICON_WAIT + " **Tempo estimado:** Até 48 horas.\n> " + EmbedUtils.ICON_INFO + " **Importante:** Não é necessário enviar novamente.",
            jda.getSelfUser()
        );
        successEmbed.setThumbnail(user.getEffectiveAvatarUrl());
        successEmbed.setImage(EmbedUtils.IMG_SUBMITTED);

        hook.sendMessageEmbeds(successEmbed.build()).setEphemeral(true).queue(msg -> {
            msg.delete().queueAfter(1, TimeUnit.MINUTES);
        });

        // Non-critical operations in async
        CompletableFuture.runAsync(() -> {
            sendWhitelistBackup(user, answers);
            WhitelistCache.clear(userId);
            ReviewPanelManager.updatePanel(jda);

            try {
                com.midgardbot.utils.PatternDetector.analyzeWhitelist(userId, answers);
                DataManager.addHistory(userId, "SYSTEM", "Sistema", "ANALYZED",
                    "Análise automática de padrões concluída (" + DataManager.getAlerts(userId).size() + " alertas gerados)");
            } catch (Exception e) {
                LOGGER.error("Erro na análise automática de padrões para " + userId, e);
            }

            InteractionUtils.logAction(jda, "📝 Nova Whitelist Enviada",
                "O usuário " + user.getAsMention() + " enviou uma nova whitelist.",
                EmbedUtils.COLOR_PRIMARY, user, null);

            staffChannel.sendMessage("📝 **Nova Whitelist Recebida:** " + user.getAsMention() + " (Use o painel de análise para verificar)")
                .queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS));
        }).exceptionally(ex -> {
            LOGGER.error("Erro ao processar tarefas pós-whitelist de " + userId, ex);
            return null;
        });
    }

    // ========================
    //    RESTORE STATE
    // ========================

    public static void restoreWhitelistState(net.dv8tion.jda.api.JDA jda) {
        LOGGER.info("Restaurando estado das whitelists pendentes...");
        Map<String, Map<String, String>> allPending = DataManager.getAllPendingWhitelists();
        int restored = 0;

        for (Map.Entry<String, Map<String, String>> entry : allPending.entrySet()) {
            String userId = entry.getKey();
            Map<String, String> data = entry.getValue();
            if (data.containsKey("_staff_message_id")) {
                String msgId = data.get("_staff_message_id");
                InteractionUtils.staffMessages.put(userId, msgId);
                InteractionUtils.staffViewPages.put(msgId, 0);
                restored++;
            }
        }
        LOGGER.info("Estado restaurado para {} whitelists.", restored);
    }

    // ========================
    //      UTILITIES
    // ========================

    private static void openWhitelistModal(ButtonInteractionEvent event, String modalId, String title, Map<String, String> questions) {
        openWhitelistModal(event, modalId, title, questions, null);
    }

    private static void openWhitelistModal(ButtonInteractionEvent event, String modalId, String title, Map<String, String> questions, Map<String, String> existingAnswers) {
        Map<String, String> answers = existingAnswers != null ? existingAnswers : WhitelistCache.getAnswers(event.getUser().getId());
        Modal.Builder modal = Modal.create(modalId, title);
        for (Map.Entry<String, String> entry : questions.entrySet()) {
            TextInput.Builder input = TextInput.create(entry.getKey(), entry.getValue(), TextInputStyle.PARAGRAPH)
                    .setRequired(true)
                    .setPlaceholder("Responda aqui...");
            String existing = answers.get(entry.getKey());
            if (existing != null && !existing.isEmpty()) {
                if (existing.length() > 4000) existing = existing.substring(0, 4000);
                input.setValue(existing);
            }
            modal.addActionRow(input.build());
        }
        event.replyModal(modal.build()).queue();
    }

    private static void sendWhitelistBackup(User user, Map<String, String> answers) {
        if (user == null) return;

        Map<String, String> allQuestions = new java.util.LinkedHashMap<>();
        allQuestions.putAll(WhitelistConfig.getPart1Questions());
        allQuestions.putAll(WhitelistConfig.getPart2Questions());
        allQuestions.putAll(WhitelistConfig.getPart3Questions());

        java.util.List<MessageEmbed> embeds = new java.util.ArrayList<>();

        EmbedBuilder firstEmbed = new EmbedBuilder()
            .setTitle("📜 Backup da sua Whitelist")
            .setColor(Color.decode("#f0b132"))
            .setDescription("Aqui está uma cópia das respostas que você enviou para a whitelist de Midgard.\n**Guarde este backup para referência futura!**\n\n**Data de Envio:** <t:" + (System.currentTimeMillis() / 1000) + ":F>");
        embeds.add(firstEmbed.build());

        EmbedBuilder currentEmbed = new EmbedBuilder().setColor(Color.decode("#f0b132"));
        int fieldsInCurrent = 0;
        int charsInCurrent = 0;

        for (Map.Entry<String, String> entry : allQuestions.entrySet()) {
            String question = entry.getValue();
            String answer = answers.getOrDefault(entry.getKey(), "*Sem resposta*");
            if (answer.length() > 1000) answer = answer.substring(0, 997) + "...";

            if (fieldsInCurrent >= 10 || charsInCurrent + answer.length() > 2000) {
                embeds.add(currentEmbed.build());
                currentEmbed = new EmbedBuilder().setColor(Color.decode("#f0b132"));
                fieldsInCurrent = 0;
                charsInCurrent = 0;
            }

            currentEmbed.addField("❓ " + question, "📝 " + answer, false);
            fieldsInCurrent++;
            charsInCurrent += answer.length() + question.length();
        }

        if (fieldsInCurrent > 0) embeds.add(currentEmbed.build());

        user.openPrivateChannel().queue(channel -> {
            for (MessageEmbed embed : embeds) {
                channel.sendMessageEmbeds(embed).queue(null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER));
            }
        }, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER));
    }
}
