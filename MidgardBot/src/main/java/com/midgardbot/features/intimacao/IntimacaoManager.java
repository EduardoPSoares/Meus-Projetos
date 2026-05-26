package com.midgardbot.features.intimacao;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Gerencia o sistema de intimações judiciais do servidor.
 * Cria canais de audiência, notifica usuários e controla prazos.
 */
public class IntimacaoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntimacaoManager.class);
    private static final File INTIMACOES_FILE = new File("data/intimacoes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Cache de intimações ativas: chave = ID do usuário intimado
    private static final Map<String, IntimacaoData> intimacoesAtivas = new ConcurrentHashMap<>();
    
    // Scheduler para verificar prazos de 24h
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    
    static {
        load();
    }

    /**
     * Dados de uma intimação.
     */
    public static class IntimacaoData {
        public String userId;           // ID do usuário intimado
        public String staffId;          // ID do staff que intimou
        public String motivo;           // Motivo da intimação
        public String dataAudiencia;    // Data da audiência
        public String channelId;        // ID do canal de texto criado
        public String voiceChannelId;   // ID do canal de voz criado
        public String messageId;        // ID da mensagem no canal
        public String dmMessageId;      // ID da mensagem na DM
        public long criadoEm;           // Timestamp de criação
        public boolean confirmado;      // Se o usuário confirmou recebimento
        public long confirmadoEm;       // Timestamp da confirmação
        public boolean standby;         // Se a whitelist foi posta em standby
        public String guildId;          // ID do servidor

        public IntimacaoData(String userId, String staffId, String motivo, String dataAudiencia, String channelId, String guildId) {
            this.userId = userId;
            this.staffId = staffId;
            this.motivo = motivo;
            this.dataAudiencia = dataAudiencia;
            this.channelId = channelId;
            this.guildId = guildId;
            this.criadoEm = System.currentTimeMillis();
            this.confirmado = false;
            this.standby = false;
        }
    }

    /**
     * Cria uma intimação completa: canal + mensagem + DM + agendamento.
     */
    public static void criarIntimacao(Guild guild, User staff, User target, Member targetMember, 
                                       String motivo, String dataAudiencia, InteractionHook hook) {
        String categoryId = BotConfig.get("INTIMACAO_CATEGORY_ID");
        Category category = guild.getCategoryById(categoryId);
        
        if (category == null) {
            hook.editOriginalEmbeds(
                EmbedUtils.createError("Erro", "Categoria de intimações não encontrada. Verifique o ID configurado.", guild.getJDA().getSelfUser()).build()
            ).queue();
            return;
        }

        // Verifica se já existe intimação ativa para este usuário
        if (intimacoesAtivas.containsKey(target.getId())) {
            hook.editOriginalEmbeds(
                EmbedUtils.createWarning("Intimação Existente", "Já existe uma intimação ativa para este usuário.", guild.getJDA().getSelfUser()).build()
            ).queue();
            return;
        }

        // Criar nome do canal
        String rawName = target.getName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        final String sanitizedName = rawName.isEmpty() ? "user-" + target.getId() : rawName;
        String channelName = "intimacao-" + sanitizedName;

        // Criar canal na categoria
        ChannelAction<TextChannel> action = category.createTextChannel(channelName)
            .clearPermissionOverrides();

        // Canal visível apenas para o intimado e staff
        action.addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
        action.addPermissionOverride(guild.getMember(target), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);
        
        // Adicionar cargos de staff que podem ver
        String staffRoles = BotConfig.get("PERM_CMD_INTIMAR");
        if (staffRoles != null && !staffRoles.isEmpty()) {
            for (String roleId : BotConfig.getAuthorizedRoles("PERM_CMD_INTIMAR")) {
                try {
                    Role role = guild.getRoleById(roleId);
                    if (role != null) {
                        action.addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);
                    }
                } catch (Exception e) { LOGGER.debug("Erro ao adicionar permissão de cargo na intimação", e); }
            }
        }

        action.setTopic("Intimação | Usuário: " + target.getName() + " | Staff: " + staff.getName());

        action.queue(channel -> {
            // Registrar a intimação
            IntimacaoData data = new IntimacaoData(target.getId(), staff.getId(), motivo, dataAudiencia, channel.getId(), guild.getId());
            intimacoesAtivas.put(target.getId(), data);
            save();

            // Enviar embed no canal de intimação
            EmbedBuilder canalEmbed = new EmbedBuilder()
                .setTitle("⚖️ Intimação Judicial")
                .setDescription("O usuário " + target.getAsMention() + " foi intimado para uma audiência.")
                .setColor(Color.decode("#E74C3C"))
                .addField("👤 Intimado", target.getAsMention() + "\n`" + target.getId() + "`", true)
                .addField("👮 Staff Responsável", staff.getAsMention(), true)
                .addField("\u200B", "\u200B", true) // spacer
                .addField("📋 Motivo da Intimação", "> " + motivo, false)
                .addField("📅 Data da Audiência", "> " + dataAudiencia, false)
                .addField("⚠️ Importante", "O intimado tem **24 horas** para confirmar o recebimento desta intimação.\n"
                    + "Caso não confirme, sua whitelist será posta em **standby** e o acesso ao servidor será suspenso até resolução.", false)
                .setFooter("MidgardBOT • Sistema de Intimações", guild.getJDA().getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now());

            if (guild.getIconUrl() != null) {
                canalEmbed.setThumbnail(guild.getIconUrl());
            }

            channel.sendMessage(target.getAsMention())
                .setEmbeds(canalEmbed.build())
                .setActionRow(
                    Button.success("intimacao_confirmar:" + target.getId(), "✅ Confirmar Recebimento")
                )
                .queue(msg -> {
                    data.messageId = msg.getId();
                    save();
                });

            // Enviar DM ao usuário
            EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("⚖️ Você foi Intimado — Midgard RPG")
                .setDescription("Você recebeu uma **intimação judicial** no servidor **" + guild.getName() + "**.\n"
                    + "Leia atentamente as informações abaixo e confirme o recebimento.")
                .setColor(Color.decode("#E74C3C"))
                .addField("📋 Motivo", "> " + motivo, false)
                .addField("📅 Data da Audiência", "> " + dataAudiencia, false)
                .addField("📍 Canal da Audiência", "Acesse o canal " + channel.getAsMention() + " no servidor para mais informações.", false)
                .addField("⚠️ Atenção", "Você tem **24 horas** para confirmar o recebimento desta intimação.\n"
                    + "Se não confirmar, sua whitelist será posta em **standby** e você não poderá entrar no servidor até que a situação seja resolvida.", false)
                .setFooter("MidgardBOT • Sistema de Intimações", guild.getJDA().getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now());

            if (guild.getIconUrl() != null) {
                dmEmbed.setThumbnail(guild.getIconUrl());
            }

            target.openPrivateChannel().queue(dm -> {
                dm.sendMessageEmbeds(dmEmbed.build())
                    .setActionRow(
                        Button.success("intimacao_confirmar:" + target.getId(), "✅ Confirmar Recebimento")
                    )
                    .queue(
                        dmMsg -> {
                            data.dmMessageId = dmMsg.getId();
                            save();
                        },
                        error -> {
                            LOGGER.warn("Não foi possível enviar DM para {}: {}", target.getName(), error.getMessage());
                            // Notifica no canal que a DM falhou
                            channel.sendMessageEmbeds(
                                EmbedUtils.createWarning("DM não enviada", 
                                    "Não foi possível enviar a notificação por mensagem privada para " + target.getAsMention() + ".\n"
                                    + "O usuário pode ter DMs desativadas. A intimação permanece válida neste canal.",
                                    guild.getJDA().getSelfUser()).build()
                            ).queue();
                        }
                    );
            });

            // Criar canal de voz na mesma categoria
            String voiceChannelName = "\uD83D\uDD0A-intimacao-" + sanitizedName;
            category.createVoiceChannel(voiceChannelName)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getMember(target), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null)
                .queue(vc -> {
                    data.voiceChannelId = vc.getId();
                    save();

                    // Adicionar permissões dos cargos de staff ao canal de voz
                    for (String roleId : BotConfig.getAuthorizedRoles("PERM_CMD_INTIMAR")) {
                        try {
                            Role role = guild.getRoleById(roleId);
                            if (role != null) {
                                vc.upsertPermissionOverride(role).grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT).queue();
                            }
                        } catch (Exception e) { LOGGER.debug("Erro ao adicionar permissão de cargo no canal de voz da intimação", e); }
                    }

                    // Notificar no canal de texto sobre o canal de voz
                    channel.sendMessageEmbeds(
                        EmbedUtils.createInfo("Canal de Voz",
                            "\uD83D\uDD0A Um canal de voz foi criado para esta audiência: " + vc.getAsMention() + "\nUtilize-o para a audiência na data marcada.",
                            guild.getJDA().getSelfUser()).build()
                    ).queue();
                }, error -> LOGGER.warn("Erro ao criar canal de voz para intimação", error));

            // Log no canal de registros de intimações
            logIntimacao(guild, staff, target, motivo, dataAudiencia, "CRIADA");

            // Responder ao staff
            hook.editOriginalEmbeds(
                EmbedUtils.createSuccess("Intimação Criada", 
                    "Intimação para " + target.getAsMention() + " criada com sucesso.\n"
                    + "Canal de texto: " + channel.getAsMention() + "\n"
                    + "Canal de voz criado na mesma categoria.\n"
                    + "O prazo de 24h para confirmação começou a contar.",
                    guild.getJDA().getSelfUser()).build()
            ).queue();

            // Agendar verificação de 24h
            SCHEDULER.schedule(() -> verificarPrazo(target.getId()), 24, TimeUnit.HOURS);

        }, error -> {
            LOGGER.error("Erro ao criar canal de intimação", error);
            hook.editOriginalEmbeds(
                EmbedUtils.createError("Erro", "Falha ao criar o canal de intimação: " + error.getMessage(), guild.getJDA().getSelfUser()).build()
            ).queue();
        });
    }

    /**
     * Confirma o recebimento de uma intimação pelo usuário.
     */
    public static boolean confirmarRecebimento(String userId, net.dv8tion.jda.api.JDA jda) {
        IntimacaoData data = intimacoesAtivas.get(userId);
        if (data == null || data.confirmado) return false;

        data.confirmado = true;
        data.confirmadoEm = System.currentTimeMillis();
        save();

        Guild guild = jda.getGuildById(data.guildId);
        if (guild == null) return true;

        // Atualizar mensagem no canal de intimação
        TextChannel channel = guild.getTextChannelById(data.channelId);
        if (channel != null && data.messageId != null) {
            // Editar a mensagem original para remover os botões
            channel.retrieveMessageById(data.messageId).queue(originalMsg -> {
                originalMsg.editMessageComponents().queue(null, e -> {});
            }, e -> {});

            channel.sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("✅ Recebimento Confirmado")
                    .setDescription("O usuário <@" + userId + "> confirmou o recebimento da intimação.")
                    .setColor(Color.decode("#2ECC71"))
                    .addField("📅 Data da Audiência", "> " + data.dataAudiencia, false)
                    .addField("ℹ️ Status", "Aguardando comparecimento na audiência.", false)
                    .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build()
            ).queue();
        }

        // Log
        User staff = jda.getUserById(data.staffId);
        User target = jda.getUserById(userId);
        if (target != null) {
            logIntimacao(guild, staff, target, data.motivo, data.dataAudiencia, "CONFIRMADA");
        }

        // Notificar staff
        String staffChannelId = BotConfig.getStaffChannelId();
        if (staffChannelId != null) {
            TextChannel staffChannel = guild.getTextChannelById(staffChannelId);
            if (staffChannel != null) {
                staffChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("✅ Intimação Confirmada")
                        .setDescription("O usuário <@" + userId + "> confirmou o recebimento da intimação.")
                        .setColor(Color.decode("#2ECC71"))
                        .addField("📅 Audiência", data.dataAudiencia, true)
                        .addField("📋 Motivo", data.motivo, true)
                        .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build()
                ).queue();
            }
        }

        return true;
    }

    /**
     * Verifica se o prazo de 24h expirou sem confirmação.
     * Se sim, coloca a whitelist em standby.
     */
    private static void verificarPrazo(String userId) {
        IntimacaoData data = intimacoesAtivas.get(userId);
        if (data == null || data.confirmado || data.standby) return;

        // Prazo expirou sem confirmação — pôr whitelist em standby
        data.standby = true;
        save();

        // Atualizar status da whitelist para STANDBY
        WhitelistStatusInfo statusInfo = DataManager.getStatus(userId);
        if (statusInfo != null) {
            DataManager.setStatus(userId, WhitelistStatus.STANDBY, 
                "Intimação não confirmada em 24h. Motivo da intimação: " + data.motivo, 
                statusInfo.nickname, statusInfo.answers, statusInfo.termsAccepted, statusInfo.staffId);
        }

        // Buscar o JDA a partir do guild
        try {
            net.dv8tion.jda.api.JDA jda = getJDA();
            if (jda == null) {
                LOGGER.error("JDA não disponível para processar expiração de intimação: {}", userId);
                return;
            }

            Guild guild = jda.getGuildById(data.guildId);
            if (guild == null) return;

            // Atualizar canal com a mensagem de standby
            TextChannel channel = guild.getTextChannelById(data.channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("🚫 Prazo Expirado — Whitelist em Standby")
                        .setDescription("O usuário <@" + userId + "> **não confirmou** o recebimento da intimação dentro do prazo de 24 horas.")
                        .setColor(Color.decode("#E74C3C"))
                        .addField("📋 Motivo da Intimação", "> " + data.motivo, false)
                        .addField("📅 Data da Audiência", "> " + data.dataAudiencia, false)
                        .addField("⛔ Ação Tomada", "A whitelist do usuário foi posta em **STANDBY**.\n"
                            + "O usuário **não poderá entrar** no servidor até que um membro da staff resolva a situação.", false)
                        .addField("🔧 Resolução", "Um staff pode usar o sistema de whitelist para reativar o acesso do jogador após a resolução da intimação.", false)
                        .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build()
                ).queue();
            }

            // Enviar DM informando sobre o standby
            User target = jda.getUserById(userId);
            if (target != null) {
                target.openPrivateChannel().queue(dm -> {
                    dm.sendMessageEmbeds(
                        new EmbedBuilder()
                            .setTitle("🚫 Whitelist em Standby — Midgard RPG")
                            .setDescription("Você **não confirmou** o recebimento da sua intimação dentro do prazo de 24 horas.")
                            .setColor(Color.decode("#E74C3C"))
                            .addField("📋 Motivo da Intimação", "> " + data.motivo, false)
                            .addField("📅 Data da Audiência", "> " + data.dataAudiencia, false)
                            .addField("⛔ Consequência", "Sua whitelist foi posta em **STANDBY**.\n"
                                + "Você **não poderá entrar** no servidor até que a situação seja resolvida com a staff.", false)
                            .addField("ℹ️ Como resolver?", "Entre em contato com a equipe de staff no Discord para resolver sua intimação pendente.", false)
                            .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build()
                    ).queue(null, error -> LOGGER.warn("Não foi possível enviar DM de standby para {}", userId));
                });

                // Log
                User staff = jda.getUserById(data.staffId);
                logIntimacao(guild, staff, target, data.motivo, data.dataAudiencia, "STANDBY");
            }

            // Notificar staff
            String staffChannelId = BotConfig.getStaffChannelId();
            if (staffChannelId != null) {
                TextChannel staffChannel = guild.getTextChannelById(staffChannelId);
                if (staffChannel != null) {
                    staffChannel.sendMessageEmbeds(
                        new EmbedBuilder()
                            .setTitle("🚫 Intimação Expirada — Whitelist em Standby")
                            .setDescription("O usuário <@" + userId + "> não confirmou a intimação em 24h.\nA whitelist foi posta em **STANDBY**.")
                            .setColor(Color.decode("#E74C3C"))
                            .addField("📋 Motivo", data.motivo, true)
                            .addField("📅 Audiência", data.dataAudiencia, true)
                            .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build()
                    ).queue();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Erro ao processar expiração de intimação para {}", userId, e);
        }
    }

    /**
     * Registra uma intimação no canal de log de intimações.
     */
    private static void logIntimacao(Guild guild, User staff, User target, String motivo, String dataAudiencia, String acao) {
        String logChannelId = BotConfig.get("INTIMACAO_LOG_CHANNEL_ID");
        if (logChannelId == null || logChannelId.isEmpty()) return;

        TextChannel logChannel = guild.getTextChannelById(logChannelId);
        if (logChannel == null) return;

        Color color;
        String icon;
        switch (acao) {
            case "CRIADA":
                color = Color.decode("#F1C40F"); // Amarelo
                icon = "📜";
                break;
            case "CONFIRMADA":
                color = Color.decode("#2ECC71"); // Verde
                icon = "✅";
                break;
            case "STANDBY":
                color = Color.decode("#E74C3C"); // Vermelho
                icon = "🚫";
                break;
            case "RETIRADA":
                color = Color.decode("#3498DB"); // Azul
                icon = "🗑️";
                break;
            default:
                color = Color.decode("#3498DB"); // Azul
                icon = "ℹ️";
                break;
        }

        EmbedBuilder logEmbed = new EmbedBuilder()
            .setTitle(icon + " Registro de Intimação — " + acao)
            .setColor(color)
            .addField("👤 Intimado", target != null ? target.getAsMention() + " (`" + target.getId() + "`)" : "Desconhecido", true)
            .addField("👮 Staff", staff != null ? staff.getAsMention() : "Desconhecido", true)
            .addField("📋 Motivo", "> " + motivo, false)
            .addField("📅 Data da Audiência", "> " + dataAudiencia, false)
            .setFooter("MidgardBOT • Sistema de Intimações", guild.getJDA().getSelfUser().getAvatarUrl())
            .setTimestamp(Instant.now());

        if (acao.equals("CRIADA") && target != null) {
            logChannel.sendMessageEmbeds(logEmbed.build())
                .setActionRow(Button.danger("intimacao_retirar:" + target.getId(), "🗑️ Retirar Intimação"))
                .queue();
        } else {
            logChannel.sendMessageEmbeds(logEmbed.build()).queue();
        }
    }

    /**
     * Retorna a intimação ativa de um usuário (se existir).
     */
    public static IntimacaoData getIntimacao(String userId) {
        return intimacoesAtivas.get(userId);
    }

    /**
     * Remove uma intimação (quando resolvida pela staff).
     */
    public static void removerIntimacao(String userId) {
        intimacoesAtivas.remove(userId);
        save();
    }

    /**
     * Retira uma intimação por ação de um staff: remove dados, deleta canais e notifica.
     */
    public static boolean retirarIntimacao(String userId, User staffResponsavel, net.dv8tion.jda.api.JDA jda) {
        IntimacaoData data = intimacoesAtivas.get(userId);
        if (data == null) return false;

        Guild guild = jda.getGuildById(data.guildId);
        if (guild == null) {
            removerIntimacao(userId);
            return true;
        }

        // Deletar canal de texto
        TextChannel channel = guild.getTextChannelById(data.channelId);
        if (channel != null) {
            channel.delete().reason("Intimação retirada por " + staffResponsavel.getName()).queue(null,
                e -> LOGGER.warn("Erro ao deletar canal de intimação", e));
        }

        // Deletar canal de voz
        if (data.voiceChannelId != null) {
            net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel vc = guild.getVoiceChannelById(data.voiceChannelId);
            if (vc != null) {
                vc.delete().reason("Intimação retirada por " + staffResponsavel.getName()).queue(null,
                    e -> LOGGER.warn("Erro ao deletar canal de voz de intimação", e));
            }
        }

        // Se estava em standby, restaurar whitelist para APPROVED
        if (data.standby) {
            WhitelistStatusInfo statusInfo = DataManager.getStatus(userId);
            if (statusInfo != null && statusInfo.status == WhitelistStatus.STANDBY) {
                DataManager.setStatus(userId, WhitelistStatus.APPROVED,
                    "Intimação retirada por staff", statusInfo.nickname, statusInfo.answers, statusInfo.termsAccepted, statusInfo.staffId);
            }
        }

        // Notificar o usuário por DM
        User target = jda.getUserById(userId);
        if (target != null) {
            target.openPrivateChannel().queue(dm -> {
                dm.sendMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("✅ Intimação Retirada — Midgard RPG")
                        .setDescription("Sua intimação foi **retirada** pela equipe de staff.")
                        .setColor(Color.decode("#2ECC71"))
                        .addField("📋 Motivo Original", "> " + data.motivo, false)
                        .addField("👮 Retirada por", staffResponsavel.getAsMention(), false)
                        .setFooter("MidgardBOT • Sistema de Intimações", jda.getSelfUser().getAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build()
                ).queue(null, e -> LOGGER.warn("Não foi possível enviar DM de retirada para {}", userId));
            });

            logIntimacao(guild, staffResponsavel, target, data.motivo, data.dataAudiencia, "RETIRADA");
        }

        removerIntimacao(userId);
        return true;
    }

    // --- JDA Reference ---
    private static net.dv8tion.jda.api.JDA jdaInstance;

    public static void setJDA(net.dv8tion.jda.api.JDA jda) {
        jdaInstance = jda;
        // Reagendar verificações para intimações pendentes ao iniciar
        for (Map.Entry<String, IntimacaoData> entry : intimacoesAtivas.entrySet()) {
            IntimacaoData data = entry.getValue();
            if (!data.confirmado && !data.standby) {
                long elapsed = System.currentTimeMillis() - data.criadoEm;
                long remaining = TimeUnit.HOURS.toMillis(24) - elapsed;
                if (remaining <= 0) {
                    // Prazo já expirou, processar imediatamente
                    SCHEDULER.schedule(() -> verificarPrazo(entry.getKey()), 1, TimeUnit.SECONDS);
                } else {
                    // Agendar para o tempo restante
                    SCHEDULER.schedule(() -> verificarPrazo(entry.getKey()), remaining, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private static net.dv8tion.jda.api.JDA getJDA() {
        return jdaInstance;
    }

    // --- Persistência ---
    private static void load() {
        if (!INTIMACOES_FILE.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(INTIMACOES_FILE), StandardCharsets.UTF_8)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, IntimacaoData>>(){}.getType();
            Map<String, IntimacaoData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                intimacoesAtivas.putAll(loaded);
            }
            LOGGER.info("Carregadas {} intimações ativas", intimacoesAtivas.size());
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar intimações", e);
        }
    }

    private static void save() {
        try {
            if (INTIMACOES_FILE.getParentFile() != null && !INTIMACOES_FILE.getParentFile().exists()) {
                INTIMACOES_FILE.getParentFile().mkdirs();
            }
            File tempFile = new File(INTIMACOES_FILE.getPath() + ".tmp");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(intimacoesAtivas, writer);
            }
            Files.move(tempFile.toPath(), INTIMACOES_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar intimações", e);
        }
    }
}
