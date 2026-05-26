package me.ray.midgardLoremakers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.ray.midgardLoremakers.command.LoreMakerCommand;
import me.ray.midgardLoremakers.config.PluginConfiguration;
import me.ray.midgardLoremakers.data.DatabaseManager;
import me.ray.midgardLoremakers.service.LoreBookService;
import me.ray.midgardLoremakers.service.TokenService;
import me.ray.midgardLoremakers.web.LoreMakerWebServer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MidgardLoremakers extends JavaPlugin {

    private PluginConfiguration pluginConfiguration;
    private DatabaseManager databaseManager;
    private TokenService tokenService;
    private LoreBookService loreBookService;
    private LoreMakerWebServer webServer;
    private ExecutorService webExecutor;
    private Gson gson;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();

        try {
            reloadRuntimeConfiguration();
            registerCommands();
        } catch (Exception exception) {
            getLogger().severe("Falha ao iniciar o MidgardLoremakers: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        stopRuntime();
    }

    private void registerCommands() {
        PluginCommand command = getCommand("loremaker");
        if (command == null) {
            throw new IllegalStateException("Comando loremaker nao encontrado no plugin.yml");
        }

        LoreMakerCommand loreMakerCommand = new LoreMakerCommand(this);
        command.setExecutor(loreMakerCommand);
        command.setTabCompleter(loreMakerCommand);
    }

    public synchronized PluginConfiguration pluginConfiguration() {
        return pluginConfiguration;
    }

    public synchronized TokenService tokenService() {
        return tokenService;
    }

    public synchronized LoreBookService loreBookService() {
        return loreBookService;
    }

    public synchronized PluginConfiguration reloadRuntimeConfiguration() throws Exception {
        reloadConfig();
        PluginConfiguration newConfiguration = PluginConfiguration.from(getConfig());

        PluginConfiguration previousConfiguration = this.pluginConfiguration;
        DatabaseManager previousDatabaseManager = this.databaseManager;
        TokenService previousTokenService = this.tokenService;
        LoreBookService previousLoreBookService = this.loreBookService;

        stopRuntime();

        try {
            applyRuntime(newConfiguration, previousConfiguration, previousDatabaseManager);
            logConfigurationState();
            return this.pluginConfiguration;
        } catch (Exception exception) {
            if (previousConfiguration != null && previousDatabaseManager != null && previousTokenService != null && previousLoreBookService != null) {
                try {
                    this.databaseManager = previousDatabaseManager;
                    this.tokenService = previousTokenService;
                    this.loreBookService = previousLoreBookService;
                    this.pluginConfiguration = previousConfiguration;
                    this.webExecutor = Executors.newVirtualThreadPerTaskExecutor();
                    this.webServer = new LoreMakerWebServer(this, previousConfiguration, previousTokenService, previousLoreBookService, gson, webExecutor);
                    this.webServer.start();
                    getLogger().warning("Falha ao aplicar nova configuracao. A configuracao anterior foi restaurada.");
                } catch (Exception rollbackException) {
                    exception.addSuppressed(rollbackException);
                    this.webServer = null;
                    this.webExecutor = null;
                }
            }

            throw exception;
        }
    }

    private void applyRuntime(PluginConfiguration newConfiguration,
                              PluginConfiguration previousConfiguration,
                              DatabaseManager previousDatabaseManager) throws Exception {
        DatabaseManager nextDatabaseManager;
        if (shouldReuseDatabaseManager(previousConfiguration, newConfiguration, previousDatabaseManager)) {
            nextDatabaseManager = previousDatabaseManager;
        } else {
            nextDatabaseManager = new DatabaseManager(getDataFolder().toPath().resolve(newConfiguration.databaseFileName()), gson);
            nextDatabaseManager.initialize();
        }

        TokenService nextTokenService = new TokenService(nextDatabaseManager, newConfiguration.security());
        LoreBookService nextLoreBookService = new LoreBookService(nextDatabaseManager, newConfiguration.bookLimits());
        ExecutorService nextWebExecutor = Executors.newVirtualThreadPerTaskExecutor();
        LoreMakerWebServer nextWebServer = new LoreMakerWebServer(this, newConfiguration, nextTokenService, nextLoreBookService, gson, nextWebExecutor);
        nextWebServer.start();

        this.pluginConfiguration = newConfiguration;
        this.databaseManager = nextDatabaseManager;
        this.tokenService = nextTokenService;
        this.loreBookService = nextLoreBookService;
        this.webExecutor = nextWebExecutor;
        this.webServer = nextWebServer;
    }

    private boolean shouldReuseDatabaseManager(PluginConfiguration previousConfiguration,
                                               PluginConfiguration newConfiguration,
                                               DatabaseManager previousDatabaseManager) {
        return previousConfiguration != null
                && previousDatabaseManager != null
                && previousConfiguration.databaseFileName().equals(newConfiguration.databaseFileName());
    }

    private void stopRuntime() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (webExecutor != null) {
            webExecutor.close();
            webExecutor = null;
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void logConfigurationState() {
        if (!pluginConfiguration.hasValidPublicRouting()) {
            getLogger().warning("Configuracao publica invalida: " + pluginConfiguration.publicRoutingValidationError());
        }

        getLogger().info("MidgardLoremakers ativo em " + pluginConfiguration.web().bindHost() + ":" + pluginConfiguration.web().port());
        getLogger().info("Roteamento publico do painel: " + pluginConfiguration.publicRoutingDescription());
    }
}
