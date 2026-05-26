package de.maxhenkel.voicechat;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.command.MvoiceCommand;
import de.maxhenkel.voicechat.compatibility.BukkitCompatibilityManager;
import de.maxhenkel.voicechat.compatibility.Compatibility;
import de.maxhenkel.voicechat.config.Messages;
import de.maxhenkel.voicechat.config.ServerConfig;
import de.maxhenkel.voicechat.config.Translations;
import de.maxhenkel.voicechat.cooldown.VoiceCooldownManager;
import de.maxhenkel.voicechat.gui.AdminHubListener;
import de.maxhenkel.voicechat.gui.MenuViewHelper;
import de.maxhenkel.voicechat.indicator.GlobalIndicatorListener;
import de.maxhenkel.voicechat.integration.placeholderapi.VoicechatExpansion;
import de.maxhenkel.voicechat.integration.viaversion.ViaVersionCompatibility;
import de.maxhenkel.voicechat.logging.ActivityLogger;
import de.maxhenkel.voicechat.logging.JavaLoggingLogger;
import de.maxhenkel.voicechat.logging.VoicechatLogger;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.plugins.impl.BukkitVoicechatServiceImpl;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.range.PlayerRangeManager;
import de.maxhenkel.voicechat.range.gui.RangeMenuListener;
import de.maxhenkel.voicechat.recording.VoiceRecordingManager;
import de.maxhenkel.voicechat.voice.server.AudioPriorityTracker;
import de.maxhenkel.voicechat.voice.server.ServerPlayerManager;
import de.maxhenkel.voicechat.voice.server.ServerVoiceEvents;
import de.maxhenkel.voicechat.zone.RestrictedZoneManager;
import de.maxhenkel.voicechat.zone.ZoneCooldownTracker;
import de.maxhenkel.voicechat.zone.ZoneNotificationListener;
import de.maxhenkel.voicechat.zone.ZoneParticleVisualizer;
import de.maxhenkel.voicechat.zone.gui.ChatInputListener;
import de.maxhenkel.voicechat.zone.gui.ZoneMenuListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public final class Voicechat extends JavaPlugin {

    public static Voicechat INSTANCE;

    public static final String MODID = "voicechat";
    public static VoicechatLogger LOGGER;

    public static int COMPATIBILITY_VERSION = BuildConstants.COMPATIBILITY_VERSION;

    public static ServerConfig SERVER_CONFIG;
    public static Translations TRANSLATIONS;
    public static Messages MESSAGES;
    public static ServerVoiceEvents SERVER;

    public static BukkitVoicechatServiceImpl apiService;
    public static NetManager netManager;
    public static Compatibility compatibility;
    public static RestrictedZoneManager restrictedZoneManager;
    public static de.maxhenkel.voicechat.zone.GlobalZoneSettings globalZoneSettings;
    public static PlayerRangeManager playerRangeManager;
    public static ActivityLogger activityLogger;
    public static ZoneNotificationListener zoneNotificationListener;
    public static VoiceCooldownManager voiceCooldownManager;
    public static GlobalIndicatorListener globalIndicatorListener;
    public static VoiceRecordingManager voiceRecordingManager;
    public static ZoneParticleVisualizer zoneParticleVisualizer;
    public static ZoneCooldownTracker zoneCooldownTracker;
    public static AudioPriorityTracker audioPriorityTracker;
    private VoicechatExpansion voicechatExpansion;

    public static final int MAX_GROUP_NAME_LENGTH = 24;
    public static final Pattern GROUP_REGEX = Pattern.compile("^[^\\p{C}\\s][^\\p{C}]{0,23}$");

    @Override
    public void onEnable() {
        INSTANCE = this;
        LOGGER = new JavaLoggingLogger(getLogger());

        if (debugMode()) {
            LOGGER.warn("Running in debug mode - Don't leave this enabled in production!");
        }

        try {
            compatibility = BukkitCompatibilityManager.loadCompatibility();
            if (compatibility == null) {
                disablePlugin();
                return;
            }
        } catch (Throwable t) {
            LOGGER.fatal("An unexpected error occurred while loading compatibility", t);
            disablePlugin();
            return;
        }

        LOGGER.info("Compatibility version {}", COMPATIBILITY_VERSION);

        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).path(getDataFolder().toPath().resolve("voicechat-server.properties")).build();
        TRANSLATIONS = ConfigBuilder.builder(Translations::new).path(getDataFolder().toPath().resolve("translations.properties")).build();
        MESSAGES = new Messages();

        netManager = new NetManager();
        netManager.onEnable();

        restrictedZoneManager = new RestrictedZoneManager();
        globalZoneSettings = new de.maxhenkel.voicechat.zone.GlobalZoneSettings();
        playerRangeManager = new PlayerRangeManager();
        activityLogger = new ActivityLogger();
        voiceCooldownManager = new VoiceCooldownManager();
        applyGlobalCooldownSettings();
        globalIndicatorListener = new GlobalIndicatorListener();
        voiceRecordingManager = new VoiceRecordingManager();
        zoneParticleVisualizer = new ZoneParticleVisualizer();
        zoneCooldownTracker = new ZoneCooldownTracker();
        audioPriorityTracker = new AudioPriorityTracker();

        apiService = new BukkitVoicechatServiceImpl();
        getServer().getServicesManager().register(BukkitVoicechatService.class, apiService, this, ServicePriority.Normal);

        PluginCommand mvoiceCommand = getCommand("mvoice");
        if (mvoiceCommand != null) {
            MvoiceCommand mvoice = new MvoiceCommand();
            mvoiceCommand.setExecutor(mvoice);
            mvoiceCommand.setTabCompleter(mvoice);
            mvoiceCommand.setPermission(PermissionManager.ADMIN_PERMISSION.getName());
            mvoiceCommand.setPermissionMessage(MESSAGES.sem_permissao);
        } else {
            LOGGER.error("Failed to register /mvoice command");
        }

        Bukkit.getPluginManager().registerEvents(new ZoneMenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new RangeMenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new AdminHubListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(), this);

        zoneNotificationListener = new ZoneNotificationListener();
        Bukkit.getPluginManager().registerEvents(zoneNotificationListener, this);
        zoneNotificationListener.start();

        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                voicechatExpansion = new VoicechatExpansion();
                voicechatExpansion.register();
                LOGGER.info("Successfully registered PlaceholderAPI expansion");
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to register PlaceholderAPI expansion", t);
        }

        try {
            if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
                ViaVersionCompatibility.register();
                LOGGER.info("Successfully added ViaVersion mappings");
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to add ViaVersion mappings", t);
        }

        compatibility.runTask(() -> {
            SERVER = new ServerVoiceEvents();
            PluginManager.instance().init();
            SERVER.init();

            Bukkit.getPluginManager().registerEvents(SERVER, this);
            ServerPlayerManager.init(this);
        });
    }

    private void disablePlugin() {
        LOGGER.fatal("Disabling {}", BuildConstants.PLUGIN_NAME);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() == null
                    && MenuViewHelper.isPluginInventoryTitle(player.getOpenInventory().getTitle())) {
                player.closeInventory();
            }
        }

        if (netManager != null) {
            netManager.onDisable();
        }
        if (apiService != null) {
            getServer().getServicesManager().unregister(apiService);
        }
        if (voiceRecordingManager != null) {
            voiceRecordingManager.stopAll();
        }
        if (zoneParticleVisualizer != null) {
            zoneParticleVisualizer.cleanup();
        }
        if (SERVER != null) {
            SERVER.getServer().close();
        }

        if (voicechatExpansion != null) {
            try {
                voicechatExpansion.unregister();
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister PlaceholderAPI expansion: {}", e.getMessage());
            }
            voicechatExpansion = null;
        }

        org.bukkit.event.HandlerList.unregisterAll(this);

        INSTANCE = null;
        SERVER = null;
        netManager = null;
        apiService = null;
        compatibility = null;
        restrictedZoneManager = null;
        globalZoneSettings = null;
        playerRangeManager = null;
        activityLogger = null;
        zoneNotificationListener = null;
        voiceCooldownManager = null;
        globalIndicatorListener = null;
        voiceRecordingManager = null;
        zoneParticleVisualizer = null;
        zoneCooldownTracker = null;
        audioPriorityTracker = null;
        SERVER_CONFIG = null;
        TRANSLATIONS = null;
        MESSAGES = null;
        LOGGER = null;
    }

    public static boolean debugMode() {
        return System.getProperty("voicechat.debug") != null;
    }

    public static void applyGlobalCooldownSettings() {
        if (globalZoneSettings == null || voiceCooldownManager == null) {
            return;
        }
        voiceCooldownManager.setSettings(
                globalZoneSettings.getGlobalCooldownMaxTalkTimeSec(),
                globalZoneSettings.getGlobalCooldownSec()
        );
    }

    public static void persistGlobalCooldownSettings() {
        if (globalZoneSettings == null || voiceCooldownManager == null) {
            return;
        }
        globalZoneSettings.setGlobalCooldownSettings(
                voiceCooldownManager.getMaxTalkTimeMs() / 1000,
                voiceCooldownManager.getCooldownMs() / 1000
        );
        globalZoneSettings.save();
    }

    public static synchronized ReloadResult reloadRuntimeState() throws Exception {
        int oldPort = SERVER_CONFIG != null ? SERVER_CONFIG.voiceChatPort.get() : -1;

        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new)
                .path(INSTANCE.getDataFolder().toPath().resolve("voicechat-server.properties"))
                .build();
        TRANSLATIONS = ConfigBuilder.builder(Translations::new)
                .path(INSTANCE.getDataFolder().toPath().resolve("translations.properties"))
                .build();
        MESSAGES = new Messages();
        PluginCommand mvoiceCommand = INSTANCE.getCommand("mvoice");
        if (mvoiceCommand != null) {
            mvoiceCommand.setPermission(PermissionManager.ADMIN_PERMISSION.getName());
            mvoiceCommand.setPermissionMessage(MESSAGES.sem_permissao);
        }

        if (restrictedZoneManager != null) {
            restrictedZoneManager.load();
        }
        if (globalZoneSettings != null) {
            globalZoneSettings.load();
        }
        if (playerRangeManager != null) {
            playerRangeManager.load();
        }
        if (SERVER != null) {
            SERVER.restart();
        }

        return new ReloadResult(oldPort, SERVER_CONFIG.voiceChatPort.get());
    }

    public static final class ReloadResult {

        private final int oldPort;
        private final int newPort;

        private ReloadResult(int oldPort, int newPort) {
            this.oldPort = oldPort;
            this.newPort = newPort;
        }

        public int getOldPort() {
            return oldPort;
        }

        public int getNewPort() {
            return newPort;
        }

        public boolean isPortChanged() {
            return oldPort != newPort;
        }
    }
}
