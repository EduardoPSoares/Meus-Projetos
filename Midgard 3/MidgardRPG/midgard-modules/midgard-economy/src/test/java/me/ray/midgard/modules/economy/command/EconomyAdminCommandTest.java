package me.ray.midgard.modules.economy.command;

import me.ray.midgard.modules.economy.EconomyModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EconomyAdminCommandTest {

    @Mock
    private EconomyModule module;

    private EconomyAdminCommand command;
    private Method parseAmountMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(module.getMessage(anyString())).thenReturn("mock message");
        command = new EconomyAdminCommand(module);

        parseAmountMethod = EconomyAdminCommand.class.getDeclaredMethod("parseAmount", String.class);
        parseAmountMethod.setAccessible(true);
    }

    @Test
    void shouldHaveCorrectName() {
        assertEquals("econ", command.getName());
    }

    @Test
    void shouldHaveCorrectPermission() {
        assertEquals("midgard.admin.economy", command.getPermission());
    }

    @Test
    void shouldNotBePlayerOnly() {
        assertFalse(command.isPlayerOnly());
    }

    @Test
    void shouldHaveCorrectAliases() {
        List<String> aliases = command.getAliases();
        assertEquals(List.of("economy", "money", "eco"), aliases);
    }

    // --- parseAmount ---

    @Nested
    class ParseAmountTests {

        private int parse(String input) throws Exception {
            try {
                return (int) parseAmountMethod.invoke(command, input);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof NumberFormatException nfe) {
                    throw nfe;
                }
                throw e;
            }
        }

        @ParameterizedTest
        @CsvSource({
                "100,     100",
                "1,       1",
                "0,       0",
                "999999,  999999"
        })
        void shouldParseWholeNumbers(String input, int expected) throws Exception {
            assertEquals(expected, parse(input));
        }

        @ParameterizedTest
        @CsvSource({
                "1k,      1000",
                "5k,      5000",
                "1.5k,    1500",
                "10k,     10000",
                "100k,    100000"
        })
        void shouldParseKiloSuffix(String input, int expected) throws Exception {
            assertEquals(expected, parse(input));
        }

        @ParameterizedTest
        @CsvSource({
                "1m,      1000000",
                "2m,      2000000",
                "1.5m,    1500000",
                "0.5m,    500000"
        })
        void shouldParseMegaSuffix(String input, int expected) throws Exception {
            assertEquals(expected, parse(input));
        }

        @ParameterizedTest
        @CsvSource({
                "1b,      1000000000",
                "2b,      2000000000"
        })
        void shouldParseBillionSuffix(String input, int expected) throws Exception {
            assertEquals(expected, parse(input));
        }

        @ParameterizedTest
        @CsvSource({
                "1K,      1000",
                "1M,      1000000",
                "1B,      1000000000"
        })
        void shouldBeCaseInsensitive(String input, int expected) throws Exception {
            assertEquals(expected, parse(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowOnNullOrEmpty(String input) {
            assertThrows(NumberFormatException.class, () -> parse(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "xyz", "k", "m", "b"})
        void shouldThrowOnInvalidInput(String input) {
            assertThrows(NumberFormatException.class, () -> parse(input));
        }

        @Test
        void shouldThrowOnNaN() {
            assertThrows(NumberFormatException.class, () -> parse("NaN"));
        }

        @Test
        void shouldThrowOnInfinity() {
            assertThrows(NumberFormatException.class, () -> parse("Infinity"));
        }

        @Test
        void shouldThrowOnOverflow() {
            // 3b = 3_000_000_000 which exceeds Integer.MAX_VALUE
            assertThrows(NumberFormatException.class, () -> parse("3b"));
        }

        @Test
        void shouldHandleDecimalWithSuffix() throws Exception {
            assertEquals(2500, parse("2.5k"));
        }

        @Test
        void shouldHandleNegativeNumber() throws Exception {
            assertEquals(-100, parse("-100"));
        }

        @Test
        void shouldHandleNegativeWithSuffix() throws Exception {
            assertEquals(-1000, parse("-1k"));
        }
    }

    // --- tabComplete ---

    @Nested
    class TabCompleteTests {

        @Mock
        private CommandSender sender;
        @Mock
        private Server server;

        @BeforeEach
        void setUpSender() {
            when(sender.getServer()).thenReturn(server);
        }

        @Test
        void shouldReturnAllSubcommandsForEmptyArg() {
            List<String> result = command.tabComplete(sender, new String[]{""});
            assertTrue(result.contains("give"));
            assertTrue(result.contains("take"));
            assertTrue(result.contains("set"));
            assertTrue(result.contains("balance"));
            assertTrue(result.contains("compact"));
            assertTrue(result.contains("decompact"));
            assertTrue(result.contains("help"));
        }

        @Test
        void shouldFilterSubcommandsByPartialInput() {
            List<String> result = command.tabComplete(sender, new String[]{"g"});
            assertTrue(result.contains("give"));
            assertFalse(result.contains("take"));
        }

        @Test
        void shouldReturnPlayerNamesForGiveSecondArg() {
            Player player1 = mock(Player.class);
            when(player1.getName()).thenReturn("Steve");
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) (Collection<?>) List.of(player1);
            when(server.getOnlinePlayers()).thenReturn((Collection) players);

            List<String> result = command.tabComplete(sender, new String[]{"give", ""});
            assertTrue(result.contains("Steve"));
        }

        @Test
        void shouldReturnAmountsForGiveThirdArg() {
            List<String> result = command.tabComplete(sender, new String[]{"give", "Steve", ""});
            assertTrue(result.contains("64"));
            assertTrue(result.contains("1k"));
            assertTrue(result.contains("1m"));
        }

        @Test
        void shouldReturnEmptyForFourthArg() {
            List<String> result = command.tabComplete(sender, new String[]{"give", "Steve", "100", ""});
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnPlayerNamesForBalanceSecondArg() {
            Player p = mock(Player.class);
            when(p.getName()).thenReturn("Alex");
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) (Collection<?>) List.of(p);
            when(server.getOnlinePlayers()).thenReturn((Collection) players);

            List<String> result = command.tabComplete(sender, new String[]{"balance", ""});
            assertTrue(result.contains("Alex"));
        }

        @Test
        void shouldNotReturnPlayerNamesForCompact() {
            List<String> result = command.tabComplete(sender, new String[]{"compact", ""});
            assertTrue(result.isEmpty());
        }
    }
}
