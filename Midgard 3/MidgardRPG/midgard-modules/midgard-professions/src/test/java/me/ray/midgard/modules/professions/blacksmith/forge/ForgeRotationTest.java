package me.ray.midgard.modules.professions.blacksmith.forge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ForgeRotationTest {

    @Test
    @DisplayName("deve haver exatamente 4 rotações")
    void shouldHave4Rotations() {
        assertEquals(4, ForgeRotation.values().length);
    }

    @Test
    @DisplayName("graus devem ser 0, 90, 180, 270")
    void shouldHaveCorrectDegrees() {
        assertEquals(0, ForgeRotation.NORTH.getDegrees());
        assertEquals(90, ForgeRotation.EAST.getDegrees());
        assertEquals(180, ForgeRotation.SOUTH.getDegrees());
        assertEquals(270, ForgeRotation.WEST.getDegrees());
    }

    // ── next() ──

    @Test
    @DisplayName("next: NORTH → EAST → SOUTH → WEST → NORTH (ciclo)")
    void shouldCycleNextCorrectly() {
        assertEquals(ForgeRotation.EAST, ForgeRotation.NORTH.next());
        assertEquals(ForgeRotation.SOUTH, ForgeRotation.EAST.next());
        assertEquals(ForgeRotation.WEST, ForgeRotation.SOUTH.next());
        assertEquals(ForgeRotation.NORTH, ForgeRotation.WEST.next());
    }

    // ── previous() ──

    @Test
    @DisplayName("previous: NORTH → WEST → SOUTH → EAST → NORTH (ciclo)")
    void shouldCyclePreviousCorrectly() {
        assertEquals(ForgeRotation.WEST, ForgeRotation.NORTH.previous());
        assertEquals(ForgeRotation.NORTH, ForgeRotation.EAST.previous());
        assertEquals(ForgeRotation.EAST, ForgeRotation.SOUTH.previous());
        assertEquals(ForgeRotation.SOUTH, ForgeRotation.WEST.previous());
    }

    // ── rotate() ──

    @Test
    @DisplayName("NORTH não deve alterar coordenadas")
    void shouldNotChangeCoords_whenNorth() {
        int[] result = ForgeRotation.NORTH.rotate(2, 3, 5, 5);
        assertArrayEquals(new int[]{2, 3}, result);
    }

    @Test
    @DisplayName("EAST deve rotacionar 90 graus")
    void shouldRotate90_whenEast() {
        // relX=2, relZ=3, width=5, depth=5
        // EAST: [depth-1-relZ, relX] = [5-1-3, 2] = [1, 2]
        int[] result = ForgeRotation.EAST.rotate(2, 3, 5, 5);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    @DisplayName("SOUTH deve rotacionar 180 graus")
    void shouldRotate180_whenSouth() {
        // relX=2, relZ=3, width=5, depth=5
        // SOUTH: [width-1-relX, depth-1-relZ] = [2, 1]
        int[] result = ForgeRotation.SOUTH.rotate(2, 3, 5, 5);
        assertArrayEquals(new int[]{2, 1}, result);
    }

    @Test
    @DisplayName("WEST deve rotacionar 270 graus")
    void shouldRotate270_whenWest() {
        // relX=2, relZ=3, width=5, depth=5
        // WEST: [relZ, width-1-relX] = [3, 2]
        int[] result = ForgeRotation.WEST.rotate(2, 3, 5, 5);
        assertArrayEquals(new int[]{3, 2}, result);
    }

    @Test
    @DisplayName("rotação com dimensões assimétricas")
    void shouldHandleAsymmetricDimensions() {
        // width=7, depth=5
        // EAST rotate(3, 2, 7, 5) = [5-1-2, 3] = [2, 3]
        int[] result = ForgeRotation.EAST.rotate(3, 2, 7, 5);
        assertArrayEquals(new int[]{2, 3}, result);
    }

    @Test
    @DisplayName("rotação na origem (0,0)")
    void shouldHandleOrigin() {
        int[] resultNorth = ForgeRotation.NORTH.rotate(0, 0, 5, 5);
        assertArrayEquals(new int[]{0, 0}, resultNorth);

        int[] resultEast = ForgeRotation.EAST.rotate(0, 0, 5, 5);
        assertArrayEquals(new int[]{4, 0}, resultEast);
    }

    // ── fromYaw() ──

    @ParameterizedTest
    @CsvSource({
        "0.0, SOUTH",
        "45.0, WEST",
        "90.0, WEST",
        "135.0, NORTH",
        "180.0, NORTH",
        "225.0, EAST",
        "270.0, EAST",
        "315.0, SOUTH",
        "360.0, SOUTH"
    })
    @DisplayName("fromYaw deve mapear yaw corretamente para rotações")
    void shouldMapYawToCorrectRotation(float yaw, ForgeRotation expected) {
        assertEquals(expected, ForgeRotation.fromYaw(yaw));
    }

    @Test
    @DisplayName("fromYaw deve normalizar yaw negativa")
    void shouldNormalizeNegativeYaw() {
        // -90 → 270 → EAST
        assertEquals(ForgeRotation.EAST, ForgeRotation.fromYaw(-90));
    }

    @Test
    @DisplayName("fromYaw deve normalizar yaw > 360")
    void shouldNormalizeYawOver360() {
        assertEquals(ForgeRotation.SOUTH, ForgeRotation.fromYaw(720));
    }
}
