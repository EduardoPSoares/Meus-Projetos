package me.ray.midgard.modules.item.socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SocketDataTest {

    @Nested
    class ConstructorFromTypes {

        @Test
        void shouldCreateEmptySockets_fromTypeList() {
            SocketData data = new SocketData(List.of("WEAPON", "ARMOR", "ANY"));
            List<SocketEntry> sockets = data.getSockets();

            assertEquals(3, sockets.size());
            assertEquals("WEAPON", sockets.get(0).getType());
            assertEquals("ARMOR", sockets.get(1).getType());
            assertEquals("ANY", sockets.get(2).getType());

            for (SocketEntry entry : sockets) {
                assertTrue(entry.isEmpty());
            }
        }

        @Test
        void shouldHandleEmptyTypeList() {
            SocketData data = new SocketData(List.of());
            assertTrue(data.getSockets().isEmpty());
        }
    }

    @Nested
    class HasEmptySocket {

        private SocketData data;

        @BeforeEach
        void setUp() {
            data = new SocketData(List.of("WEAPON", "ARMOR", "ANY"));
        }

        @Test
        void shouldReturnTrue_whenTypeHasEmptySocket() {
            assertTrue(data.hasEmptySocket("WEAPON"));
        }

        @Test
        void shouldBeCaseInsensitive() {
            assertTrue(data.hasEmptySocket("weapon"));
            assertTrue(data.hasEmptySocket("Armor"));
        }

        @Test
        void shouldReturnFalse_whenTypeNotPresent() {
            assertFalse(data.hasEmptySocket("ACCESSORY"));
        }

        @Test
        void shouldReturnFalse_whenSocketFilled() {
            data.applyGem("WEAPON", "ruby_gem");
            assertFalse(data.hasEmptySocket("WEAPON"));
        }
    }

    @Nested
    class HasAnyEmptySocket {

        @Test
        void shouldReturnTrue_whenAllEmpty() {
            SocketData data = new SocketData(List.of("WEAPON", "ARMOR"));
            assertTrue(data.hasAnyEmptySocket());
        }

        @Test
        void shouldReturnTrue_whenSomeEmpty() {
            SocketData data = new SocketData(List.of("WEAPON", "ARMOR"));
            data.applyGem("WEAPON", "gem1");
            assertTrue(data.hasAnyEmptySocket());
        }

        @Test
        void shouldReturnFalse_whenAllFilled() {
            SocketData data = new SocketData(List.of("WEAPON", "ARMOR"));
            data.applyGem("WEAPON", "gem1");
            data.applyGem("ARMOR", "gem2");
            assertFalse(data.hasAnyEmptySocket());
        }

        @Test
        void shouldReturnFalse_whenNoSockets() {
            SocketData data = new SocketData(List.of());
            assertFalse(data.hasAnyEmptySocket());
        }
    }

    @Nested
    class ApplyGem {

        @Test
        void shouldApplyGem_toMatchingEmptySocket() {
            SocketData data = new SocketData(List.of("WEAPON", "ARMOR"));
            assertTrue(data.applyGem("WEAPON", "ruby"));

            assertFalse(data.getSockets().get(0).isEmpty());
            assertEquals("ruby", data.getSockets().get(0).getGemId());
        }

        @Test
        void shouldReturnFalse_whenNoMatchingSocket() {
            SocketData data = new SocketData(List.of("WEAPON"));
            assertFalse(data.applyGem("ARMOR", "ruby"));
        }

        @Test
        void shouldFallbackToAnySocket() {
            SocketData data = new SocketData(List.of("WEAPON", "ANY"));
            data.applyGem("WEAPON", "gem1");
            // Now WEAPON is full, but ANY should accept ARMOR type
            assertTrue(data.applyGem("ARMOR", "gem2"));
            assertEquals("gem2", data.getSockets().get(1).getGemId());
        }

        @Test
        void shouldNotApply_whenAllSocketsFull() {
            SocketData data = new SocketData(List.of("WEAPON"));
            data.applyGem("WEAPON", "gem1");
            assertFalse(data.applyGem("WEAPON", "gem2"));
        }

        @Test
        void shouldBeCaseInsensitive() {
            SocketData data = new SocketData(List.of("WEAPON"));
            assertTrue(data.applyGem("weapon", "ruby"));
        }

        @Test
        void shouldFillFirstMatchingSocket_whenMultipleExist() {
            SocketData data = new SocketData(List.of("WEAPON", "WEAPON"));
            data.applyGem("WEAPON", "gem1");

            assertEquals("gem1", data.getSockets().get(0).getGemId());
            assertTrue(data.getSockets().get(1).isEmpty());

            data.applyGem("WEAPON", "gem2");
            assertEquals("gem2", data.getSockets().get(1).getGemId());
        }
    }
}
