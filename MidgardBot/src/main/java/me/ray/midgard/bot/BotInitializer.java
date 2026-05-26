package me.ray.midgard.bot;

import me.ray.midgard.bot.modules.whitelist.WhitelistModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class BotInitializer {

    private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);

    private static final String VERSION = "1.0.0";
    private static final String BANNER = """
            
            ╔══════════════════════════════════════════════════════════╗
            ║                                                          ║
            ║   ███╗   ███╗██╗██████╗  ██████╗  █████╗ ██████╗ ██████╗ ║
            ║   ████╗ ████║██║██╔══██╗██╔════╝ ██╔══██╗██╔══██╗██╔══██╗║
            ║   ██╔████╔██║██║██║  ██║██║  ███╗███████║██████╔╝██║  ██║║
            ║   ██║╚██╔╝██║██║██║  ██║██║   ██║██╔══██║██╔══██╗██║  ██║║
            ║   ██║ ╚═╝ ██║██║██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝║
            ║   ╚═╝     ╚═╝╚═╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝║
            ║                                                          ║
            ║                    Bot v%s                            ║
            ║               Midgard RPG Network                        ║
            ║                                                          ║
            ╚══════════════════════════════════════════════════════════╝
            """;

    private final Map<String, String> environment = new LinkedHashMap<>();
    private MidgardBot bot;

    public static void main(String[] args) {
        new BotInitializer().start(args);
    }

    private void start(String[] args) {
        printBanner();

        try {
            // Phase 1: Load environment
            phase("Carregando ambiente", this::loadEnvironment);

            // Phase 2: Validate token
            String token = getRequiredEnv("BOT_TOKEN");

            // Phase 3: Build bot
            phase("Inicializando bot", () -> {
                BotConfig config = new BotConfig(token);
                bot = new MidgardBot(config);
            });

            // Phase 4: Register modules
            phase("Registrando módulos", this::registerModules);

            // Phase 5: Enable modules
            phase("Habilitando módulos", () -> bot.enableAllModules());

            // Phase 6: Sync commands
            phase("Sincronizando comandos", () -> {
                if (bot.getConfig().getDevGuildId() > 0) {
                    bot.getCommandManager().syncCommandsToGuild(bot.getConfig().getDevGuildId());
                    logger.info("Comandos sincronizados para guild de desenvolvimento: {}", bot.getConfig().getDevGuildId());
                } else {
                    bot.getCommandManager().syncCommands();
                    logger.info("Comandos sincronizados globalmente");
                }
            });

            // Phase 7: Setup shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal recebido...");
                if (bot != null) {
                    bot.shutdown();
                }
            }, "MidgardBot-Shutdown"));

            logger.info("============================================");
            logger.info("  MidgardBot inicializado com sucesso!");
            logger.info("  Bot: {}", bot.getJda().getSelfUser().getAsTag());
            logger.info("  Guilds: {}", bot.getJda().getGuilds().size());
            logger.info("  Comandos: {}", bot.getCommandManager().getCommandCount());
            logger.info("  Módulos: {}", bot.getModuleManager().getModuleCount());
            logger.info("  Java: {}", System.getProperty("java.version"));
            logger.info("============================================");

        } catch (EnvironmentException e) {
            logger.error("╔══════════════════════════════════════════╗");
            logger.error("║         ERRO DE CONFIGURAÇÃO             ║");
            logger.error("╠══════════════════════════════════════════╣");
            logger.error("║ {}", padRight(e.getMessage(), 41) + "║");
            logger.error("║                                          ║");
            logger.error("║ Crie um arquivo .env com:                ║");
            logger.error("║   BOT_TOKEN=seu_token_aqui               ║");
            logger.error("║                                          ║");
            logger.error("║ Ou defina a variável de ambiente:        ║");
            logger.error("║   set BOT_TOKEN=seu_token_aqui           ║");
            logger.error("╚══════════════════════════════════════════╝");
            System.exit(1);
        } catch (Exception e) {
            logger.error("╔══════════════════════════════════════════╗");
            logger.error("║          ERRO AO INICIALIZAR             ║");
            logger.error("╠══════════════════════════════════════════╣");
            logger.error("║ Falha ao iniciar o MidgardBot            ║");
            logger.error("╚══════════════════════════════════════════╝");
            logger.error("Detalhes:", e);
            System.exit(1);
        }
    }

    // ==================== Module Registration ====================

    private void registerModules() {
        // ╔══════════════════════════════════════════════════════╗
        // ║  Registre todos os módulos do bot aqui              ║
        // ║  Cada módulo é automaticamente inicializado         ║
        // ║  e habilitado na ordem de registro                  ║
        // ╚══════════════════════════════════════════════════════╝

        register(new WhitelistModule());

        // Futuros módulos:
        // register(new EconomyModule());
        // register(new CombatModule());
        // register(new RacesModule());
    }

    private void register(me.ray.midgard.bot.core.module.BotModule module) {
        bot.registerModule(module);
        logger.info("  ✓ Módulo registrado: {}", module.getClass().getSimpleName().replace("Module", ""));
    }

    // ==================== Environment Loading ====================

    private void loadEnvironment() {
        Path workingDir = Path.of(System.getProperty("user.dir"));

        // Load from .env file
        for (Path dir : new Path[]{workingDir, workingDir.resolve("..").normalize()}) {
            Path envFile = dir.resolve(".env");
            if (Files.exists(envFile)) {
                loadEnvFile(envFile);
                logger.info("Arquivo .env carregado: {}", envFile);
                break;
            }
        }

        // Load from bot.properties if exists
        Path propsFile = workingDir.resolve("config").resolve("bot.properties");
        if (Files.exists(propsFile)) {
            loadPropertiesFile(propsFile);
            logger.info("Arquivo bot.properties carregado");
        }

        // Also check classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bot.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                props.forEach((k, v) -> environment.putIfAbsent(k.toString(), v.toString().trim()));
            }
        } catch (IOException e) { logger.debug("Erro ao carregar properties do classpath", e); }

        // System env vars override file values
        String[] keys = {"BOT_TOKEN", "BOT_OWNER_ID", "BOT_DEV_GUILD", "BOT_PREFIX"};
        for (String key : keys) {
            String sysVal = System.getenv(key);
            if (sysVal != null && !sysVal.isBlank()) {
                environment.put(key, sysVal);
            }
        }

        // Also check system properties (-DBOT_TOKEN=xxx)
        for (String key : keys) {
            String prop = System.getProperty(key);
            if (prop != null && !prop.isBlank()) {
                environment.put(key, prop);
            }
        }
    }

    private void loadEnvFile(Path file) {
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    // Remove surrounding quotes
                    if (value.length() >= 2 &&
                            ((value.startsWith("\"") && value.endsWith("\"")) ||
                             (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (!value.isBlank()) {
                        environment.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Erro ao ler arquivo .env: {}", file, e);
        }
    }

    private void loadPropertiesFile(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(is);
            props.forEach((k, v) -> {
                String value = v.toString().trim();
                if (!value.isBlank()) {
                    environment.putIfAbsent(k.toString(), value);
                }
            });
        } catch (IOException e) {
            logger.warn("Erro ao ler bot.properties: {}", file, e);
        }
    }

    private String getEnv(String key) {
        return environment.get(key);
    }

    private String getRequiredEnv(String key) {
        String value = getEnv(key);
        if (value == null || value.isBlank()) {
            throw new EnvironmentException(key + " não configurado!");
        }
        return value;
    }

    // ==================== Helpers ====================

    private void printBanner() {
        String formatted = String.format(BANNER, VERSION);
        for (String line : formatted.split("\n")) {
            logger.info(line);
        }
    }

    private void phase(String name, ThrowingRunnable action) throws Exception {
        logger.info("▸ {}...", name);
        long start = System.currentTimeMillis();
        action.run();
        long elapsed = System.currentTimeMillis() - start;
        logger.info("  ✓ {} ({}ms)", name, elapsed);
    }

    private static String padRight(String text, int length) {
        if (text.length() >= length) return text.substring(0, length);
        return text + " ".repeat(length - text.length());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class EnvironmentException extends RuntimeException {
        EnvironmentException(String message) {
            super(message);
        }
    }
}
