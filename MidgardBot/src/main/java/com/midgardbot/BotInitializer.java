package com.midgardbot;

import com.midgardbot.commands.CommandManager;
import com.midgardbot.commands.InteractionManager;
import com.midgardbot.commands.handlers.InteractionUtils;
import com.midgardbot.commands.handlers.WhitelistWizardHandler;
import com.midgardbot.commands.impl.*;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.features.ServerStatusMonitor;
import com.midgardbot.features.WelcomeListener;
import com.midgardbot.utils.BotSocketListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Responsável por inicializar e configurar todos os componentes do bot.
 * Extraído de Main.java para separar responsabilidades.
 */
public class BotInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotInitializer.class);

    /**
     * Inicializa o bot com o perfil especificado.
     *
     * @param profile Perfil de configuração (ex: "test") ou null para produção
     */
    public static void initialize(String profile) {
        BotConfig.init(profile);

        try {
            if (BotConfig.isTestMode()) {
                LOGGER.info("Iniciando MidgardBOT em modo de TESTE (perfil: {})...", BotConfig.getActiveProfile());
            } else {
                LOGGER.info("Iniciando MidgardBOT...");
            }

            initializeDatabase();
            loadConfigurations();

            String token = validateToken();

            // Instanciação antecipada de features que precisam ser passadas aos comandos
            var ticketBackupManager = new com.midgardbot.features.backup.TicketBackupManager(null);
            var ticketArchiver = new com.midgardbot.features.tickets.TicketArchiver(null);
            InteractionUtils.setTicketArchiver(ticketArchiver);

            // Registra comandos
            InteractionManager interactionManager = new InteractionManager();
            registerSlashCommands(interactionManager, ticketBackupManager, ticketArchiver);

            // Constrói JDA
            CommandManager commandManager = new CommandManager();
            JDA jda = buildJDA(token, commandManager, interactionManager, ticketArchiver);

            // Inicializações pós-JDA
            initializePostJDA(jda, ticketBackupManager, ticketArchiver);

            // Health check
            checkConfiguration(jda);

            // Inicia monitores e schedulers
            startMonitors(jda, ticketBackupManager, ticketArchiver);

            // Inicia o painel web administrativo
            var webServer = new com.midgardbot.web.WebServer(jda);
            webServer.start();

            LOGGER.info("[OK] Bot iniciado com sucesso! Logado como: {}", jda.getSelfUser().getName());
            LOGGER.info("Conectado a {} servidor(es)", jda.getGuilds().size());

            startConsoleHandler(jda);

        } catch (com.midgardbot.exceptions.ConfigurationException e) {
            LOGGER.error("Erro de configuração: {}", e.getMessage());
            return;
        } catch (Exception e) {
            LOGGER.error("Erro fatal ao iniciar o bot", e);
            try {
                DataManager.saveAllSync();
            } catch (Exception saveEx) {
                LOGGER.error("Erro ao salvar dados no shutdown de emergência", saveEx);
            }
        }
    }

    private static void initializeDatabase() {
        try {
            com.midgardbot.data.DatabaseManager.connect();
            DataManager.syncPendingFromDatabase();
            DataManager.syncStatusFromDatabase();
        } catch (Exception e) {
            LOGGER.error("Falha ao inicializar banco de dados", e);
        }
    }

    private static void loadConfigurations() {
        try {
            com.midgardbot.features.sync.RoleSyncConfig.load();
        } catch (Exception e) {
            LOGGER.error("Falha ao carregar RoleSyncConfig", e);
        }
        try {
            com.midgardbot.config.MessagesConfig.load();
        } catch (Exception e) {
            LOGGER.error("Falha ao carregar MessagesConfig", e);
        }
    }

    private static String validateToken() {
        // Validação completa de configurações (inclui token)
        com.midgardbot.config.ConfigValidator.validate();
        return BotConfig.getToken();
    }

    private static void registerSlashCommands(
            InteractionManager interactionManager,
            com.midgardbot.features.backup.TicketBackupManager ticketBackupManager,
            com.midgardbot.features.tickets.TicketArchiver ticketArchiver
    ) {
        // Feature de Requisições
        var requestFeature = new com.midgardbot.features.request.RequestFeature();
        interactionManager.addCommand(requestFeature);
        interactionManager.addCommand(new com.midgardbot.features.request.RequestTutorialCommand());

        // Whitelist
        interactionManager.addCommand(new GuideCommand());
        interactionManager.addCommand(new SetupWhitelistCommand());
        interactionManager.addCommand(new RemoveWhitelistCommand());
        interactionManager.addCommand(new ResetWhitelistCommand());
        interactionManager.addCommand(new WhitelistInfoCommand());
        interactionManager.addCommand(new ForceWhitelistCommand());
        interactionManager.addCommand(new SetWhitelistCommand());
        interactionManager.addCommand(new RefreshWhitelistsCommand());
        interactionManager.addCommand(new ReviewCommand());
        interactionManager.addCommand(new SetupReviewCommand());
        interactionManager.addCommand(new ToggleWhitelistCommand());

        // Utilitários
        interactionManager.addCommand(new LimitCommand());
        interactionManager.addCommand(new StatusCommand());
        interactionManager.addCommand(new HelpSlashCommand());
        interactionManager.addCommand(new ClearSlashCommand());
        interactionManager.addCommand(new PendingCommand());
        interactionManager.addCommand(new StaffStatsCommand());
        interactionManager.addCommand(new ReloadCommand());
        interactionManager.addCommand(new BotInfoCommand());
        interactionManager.addCommand(new FindUserCommand());

        // Moderação
        interactionManager.addCommand(new KickCommand());
        interactionManager.addCommand(new BanCommand());
        interactionManager.addCommand(new UnbanCommand());
        interactionManager.addCommand(new WarnCommand());
        interactionManager.addCommand(new UnwarnCommand());
        interactionManager.addCommand(new TempbanCommand());
        interactionManager.addCommand(new LockdownCommand());
        interactionManager.addCommand(new MaintenanceCommand());
        interactionManager.addCommand(new BlacklistCommand());
        interactionManager.addCommand(new BypassCommand());
        interactionManager.addCommand(new AntiFakeBypassCommand());

        // Tickets
        interactionManager.addCommand(new TicketSetupCommand());
        interactionManager.addCommand(new ClearTicketsCommand());
        interactionManager.addCommand(new CleanTicketsCommand());
        interactionManager.addCommand(new TicketCommand(ticketArchiver));

        // Link Discord ↔ Minecraft
        interactionManager.addCommand(new SetupLinkCommand());
        interactionManager.addCommand(new ForceUnlinkCommand());
        interactionManager.addCommand(new ForceLinkCommand());
        interactionManager.addCommand(new SetNickCommand());

        // Diversos
        interactionManager.addCommand(new StreamerCommand());
        interactionManager.addCommand(new PollCommand());
        interactionManager.addCommand(new LeaveUnknownGuildsCommand());
        interactionManager.addCommand(new ReportCommand());
        interactionManager.addCommand(new BackupCommand(ticketBackupManager));
        interactionManager.addCommand(new DatabaseCommand());
        interactionManager.addCommand(new PlayerControlCommand());
        interactionManager.addCommand(new PermaDeathCommand());
        interactionManager.addCommand(new IntimarCommand());
        interactionManager.addCommand(new RetirarIntimacaoCommand());

        // Painel Web
        interactionManager.addCommand(new PainelCommand());

        // Reuniões (gravação de áudio)
        interactionManager.addCommand(new com.midgardbot.features.meeting.MeetingCommand());
    }

    private static JDA buildJDA(
            String token,
            CommandManager commandManager,
            InteractionManager interactionManager,
            com.midgardbot.features.tickets.TicketArchiver ticketArchiver
    ) throws Exception {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        );

        // Usa MemberCachePolicy baseado em atividade para reduzir uso de memória
        // em vez de ALL que cacheia todos os membros incondicionalmente
        JDABuilder builder = JDABuilder.createDefault(token, intents)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .disableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                .setBulkDeleteSplittingEnabled(false)
                .setActivity(Activity.playing("Minecraft no Midgard RPG"))
                .addEventListeners(
                        commandManager,
                        interactionManager,
                        new com.midgardbot.features.request.RequestFeature(),
                        new WelcomeListener(),
                        new com.midgardbot.features.sync.RoleChangeListener(),
                        new com.midgardbot.features.PromotionListener(),
                        new com.midgardbot.features.whitelist.WhitelistLeaveListener(),
                        new com.midgardbot.features.security.ChatSecurityListener(),
                        new com.midgardbot.features.security.JoinSecurityListener(),
                        new com.midgardbot.features.security.AntiNukeListener(),
                        new com.midgardbot.features.tickets.TicketListener(),
                        new com.midgardbot.features.PunishmentRejoinListener(),
                        new com.midgardbot.features.ServerLogListener()
                );

        JDA jda = builder.build();
        jda.awaitReady();
        return jda;
    }

    private static void initializePostJDA(
            JDA jda,
            com.midgardbot.features.backup.TicketBackupManager ticketBackupManager,
            com.midgardbot.features.tickets.TicketArchiver ticketArchiver
    ) {
        // Whitelist
        com.midgardbot.features.whitelist.WhitelistCleaner.start(jda);
        WhitelistWizardHandler.restoreWhitelistState(jda);
        com.midgardbot.features.whitelist.ReviewPanelManager.updatePanel(jda);

        // Configurações globais
        com.midgardbot.utils.DiscordAppender.setJda(jda);
        DataManager.setJDA(jda);
        var mainGuild = com.midgardbot.web.auth.AuthController.getMainGuild(jda);
        com.midgardbot.commands.handlers.TicketHandler.syncOpenTicketPermissions(mainGuild);
        com.midgardbot.features.intimacao.IntimacaoManager.setJDA(jda);
    }

    private static void startMonitors(
            JDA jda,
            com.midgardbot.features.backup.TicketBackupManager ticketBackupManager,
            com.midgardbot.features.tickets.TicketArchiver ticketArchiver
    ) {
        ServerStatusMonitor statusMonitor = new ServerStatusMonitor(jda);
        statusMonitor.start();

        var feedbackMonitor = new com.midgardbot.features.StaffFeedbackEmbedUpdater(jda);
        feedbackMonitor.start();

        var punishmentMonitor = new com.midgardbot.features.PunishmentMonitor(jda);
        punishmentMonitor.start();

        ticketBackupManager.setJDA(jda);
        ticketBackupManager.startScheduler();

        ticketArchiver.setJDA(jda);
        ticketArchiver.start();

        var ticketAutoClose = new com.midgardbot.features.tickets.TicketAutoClose(jda);
        ticketAutoClose.start();

        // Sincronização Minecraft → Discord
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new com.midgardbot.features.sync.BotSyncTask(jda), 10, 5, TimeUnit.SECONDS);

        // Socket Listener
        BotSocketListener socketListener = new BotSocketListener(jda);
        socketListener.start();

        // Streamer monitor
        com.midgardbot.features.streamer.StreamerMonitor.start(jda);

        // Limpeza periódica de cooldowns de rate limiting
        scheduler.scheduleAtFixedRate(com.midgardbot.utils.PermissionUtils::cleanupCooldowns, 5, 5, TimeUnit.MINUTES);

        // Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Encerrando bot... Salvando dados...");
            statusMonitor.stop();
            feedbackMonitor.stop();
            punishmentMonitor.stop();
            socketListener.shutdown();
            DataManager.saveAllSync();
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(java.time.Duration.ofSeconds(10))) {
                    LOGGER.warn("JDA não encerrou a tempo, forçando shutdown...");
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Bot encerrado com segurança.");
        }));
    }

    static void checkConfiguration(JDA jda) {
        LOGGER.info("Executando verificações de configuração...");

        checkChannel(jda, "STAFF_CHANNEL_ID", "Canal da Staff");
        checkChannel(jda, "LOG_CHANNEL_ID", "Canal de Logs");
        checkChannel(jda, "RESULTS_CHANNEL_ID", "Canal de Resultados");

        String citizenRoleId = BotConfig.getCitizenRoleId();
        if (citizenRoleId != null && !citizenRoleId.isEmpty()) {
            boolean roleFound = jda.getGuilds().stream()
                    .anyMatch(guild -> {
                        try {
                            return guild.getRoleById(citizenRoleId) != null;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    });
            if (!roleFound) {
                LOGGER.warn("[CONFIG] Cargo de Cidadão (CITIZEN_ROLE_ID) não encontrado em nenhum servidor!");
            }
        } else {
            LOGGER.warn("[CONFIG] CITIZEN_ROLE_ID não configurado.");
        }

        LOGGER.info("Verificações concluídas.");
    }

    private static void checkChannel(JDA jda, String configKey, String description) {
        String channelId = BotConfig.get(configKey);
        if (channelId == null || channelId.isEmpty()) {
            LOGGER.warn("[CONFIG] {} não definido no config.env!", configKey);
        } else if (jda.getTextChannelById(channelId) == null) {
            LOGGER.warn("[CONFIG] {} não encontrado (ID: {}).", description, channelId);
        }
    }

    static void startConsoleHandler(JDA jda) {
        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                LOGGER.info("Console handler iniciado. Digite 'help' para ver os comandos.");
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split("\\s+");
                    String command = parts[0].toLowerCase();

                    switch (command) {
                        case "reload" -> {
                            LOGGER.info("[CONSOLE] Recarregando configurações...");
                            try {
                                com.midgardbot.features.whitelist.WhitelistConfig.loadQuestions();
                                LOGGER.info("[CONSOLE] Configurações recarregadas com sucesso!");
                            } catch (Exception e) {
                                LOGGER.error("[CONSOLE] Erro ao recarregar configurações: ", e);
                            }
                        }
                        case "stop", "exit" -> {
                            LOGGER.info("[CONSOLE] Encerrando bot via console...");
                            System.exit(0);
                        }
                        case "help" -> LOGGER.info("[CONSOLE] Comandos disponíveis: reload, stop, exit, help");
                        default -> LOGGER.info("[CONSOLE] Comando desconhecido: {}", command);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Erro no console handler", e);
            }
        }, "ConsoleHandler").start();
    }
}
