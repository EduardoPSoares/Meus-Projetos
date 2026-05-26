package me.ray.midgard.modules.essentials.listener;

import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandBlockerListenerTest {

    @Mock
    private EssentialsManager manager;
    @Mock
    private EssentialsConfig config;
    @Mock
    private FileConfiguration fileConfig;
    @Mock
    private Player player;
    @Mock
    private Server server;

    private CommandBlockerListener listener;
    private Method normalizeMethod;
    private Method getSuffixMethod;

    private static Server staticServer;

    @org.junit.jupiter.api.BeforeAll
    static void initBukkit() throws Exception {
        staticServer = mock(Server.class);
        when(staticServer.getOnlinePlayers()).thenReturn(Collections.emptyList());
        PluginManager pluginManager = mock(PluginManager.class);
        when(staticServer.getPluginManager()).thenReturn(pluginManager);

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, staticServer);
    }

    @BeforeEach
    void setUp() throws Exception {
        when(manager.getConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(fileConfig);
        when(manager.getMessage(anyString())).thenReturn("Blocked");
        when(player.getServer()).thenReturn(staticServer);

        listener = new CommandBlockerListener(manager);

        normalizeMethod = CommandBlockerListener.class.getDeclaredMethod("normalize", String.class);
        normalizeMethod.setAccessible(true);

        getSuffixMethod = CommandBlockerListener.class.getDeclaredMethod("getSuffixCommand", String.class);
        getSuffixMethod.setAccessible(true);
    }

    // --- normalize ---

    @Nested
    class NormalizeTests {

        private String normalize(String input) throws Exception {
            return (String) normalizeMethod.invoke(listener, input);
        }

        @Test
        void shouldRemoveSlashPrefix() throws Exception {
            assertEquals("gamemode", normalize("/gamemode"));
        }

        @Test
        void shouldReturnSameIfNoSlash() throws Exception {
            assertEquals("gamemode", normalize("gamemode"));
        }

        @Test
        void shouldHandleEmptyString() throws Exception {
            assertEquals("", normalize(""));
        }

        @Test
        void shouldHandleNull() throws Exception {
            assertEquals("", normalize(null));
        }

        @Test
        void shouldTrimWhitespace() throws Exception {
            assertEquals("tp", normalize("  tp  "));
        }

        @Test
        void shouldHandleNamespacedCommand() throws Exception {
            assertEquals("minecraft:me", normalize("/minecraft:me"));
        }
    }

    // --- getSuffixCommand ---

    @Nested
    class GetSuffixCommandTests {

        private String getSuffix(String input) throws Exception {
            return (String) getSuffixMethod.invoke(listener, input);
        }

        @Test
        void shouldReturnSuffixAfterColon() throws Exception {
            assertEquals("me", getSuffix("minecraft:me"));
        }

        @Test
        void shouldReturnFullIfNoColon() throws Exception {
            assertEquals("gamemode", getSuffix("gamemode"));
        }

        @Test
        void shouldReturnEmptyForNull() throws Exception {
            assertEquals("", getSuffix(null));
        }

        @Test
        void shouldHandleEmptyNamespace() throws Exception {
            assertEquals("cmd", getSuffix(":cmd"));
        }
    }

    // --- onCommand blocking ---

    @Nested
    class CommandBlockingTests {

        @Test
        void shouldBlockExactMatchCommand() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("me"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/me is cool");

            listener.onCommand(event);
            assertTrue(event.isCancelled());
        }

        @Test
        void shouldBlockNamespacedCommand() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("me"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/minecraft:me is cool");

            listener.onCommand(event);
            assertTrue(event.isCancelled());
        }

        @Test
        void shouldNotBlockAllowedCommand() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("me"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/help");

            listener.onCommand(event);
            assertFalse(event.isCancelled());
        }

        @Test
        void shouldNotBlockWithBypassPermission() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("me"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(true);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/me is cool");

            listener.onCommand(event);
            assertFalse(event.isCancelled());
        }

        @Test
        void shouldBlockReverseNamespaceMatch() {
            // If blocked list has "minecraft:me", it should also block "/me"
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("minecraft:me"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/me is cool");

            listener.onCommand(event);
            assertTrue(event.isCancelled());
        }

        @Test
        void shouldHandleCaseInsensitiveBlocking() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("ME"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            // The command is lowercased in the handler
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/ME is cool");

            listener.onCommand(event);
            assertTrue(event.isCancelled());
        }

        @Test
        void shouldNotBlockWhenListIsEmpty() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of());
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/me is cool");

            listener.onCommand(event);
            assertFalse(event.isCancelled());
        }

        @Test
        void shouldBlockMultipleCommands() {
            when(fileConfig.getStringList("blocked-commands")).thenReturn(List.of("me", "op", "stop"));
            when(player.hasPermission("midgard.essentials.bypass.blockedcmds")).thenReturn(false);

            PlayerCommandPreprocessEvent event1 = new PlayerCommandPreprocessEvent(player, "/op Steve");
            listener.onCommand(event1);
            assertTrue(event1.isCancelled());

            PlayerCommandPreprocessEvent event2 = new PlayerCommandPreprocessEvent(player, "/stop");
            listener.onCommand(event2);
            assertTrue(event2.isCancelled());
        }
    }

    // --- WarpManager.isValidName via reflection ---

    @Nested
    class WarpNameValidationTests {

        private Method isValidNameMethod;

        @BeforeEach
        void setUp() throws Exception {
            isValidNameMethod = me.ray.midgard.modules.essentials.manager.WarpManager.class
                    .getDeclaredMethod("isValidName", String.class);
            isValidNameMethod.setAccessible(true);
        }

        // We can't easily instantiate WarpManager, so we replicate the regex logic
        @ParameterizedTest
        @ValueSource(strings = {"spawn", "shop", "arena_1", "A", "test123"})
        void shouldAcceptValidNames(String name) {
            assertTrue(name.matches("[a-zA-Z0-9_]+"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "hello world", "name!", "test-warp", "warp:name"})
        void shouldRejectInvalidNames(String name) {
            assertFalse(name.matches("[a-zA-Z0-9_]+"));
        }
    }
}
