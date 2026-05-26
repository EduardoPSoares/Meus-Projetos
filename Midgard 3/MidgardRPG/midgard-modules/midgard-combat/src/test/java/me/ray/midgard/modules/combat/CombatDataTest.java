package me.ray.midgard.modules.combat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CombatDataTest {

    private CombatData data;

    @BeforeEach
    void setUp() {
        data = new CombatData();
    }

    // --- Default Values ---

    @Test
    void shouldHaveDefaultHealthOf100() {
        assertEquals(100.0, data.getCurrentHealth());
    }

    @Test
    void shouldHaveDefaultManaOf0() {
        assertEquals(0.0, data.getCurrentMana());
    }

    @Test
    void shouldHaveDefaultStaminaOf0() {
        assertEquals(0.0, data.getCurrentStamina());
    }

    @Test
    void shouldHaveDefaultAbsorptionOf0() {
        assertEquals(0.0, data.getCurrentAbsorption());
    }

    @Test
    void shouldHaveDefaultLevelOf1() {
        assertEquals(1, data.getLevel());
    }

    @Test
    void shouldHaveDefaultExperienceOf0() {
        assertEquals(0.0, data.getExperience());
    }

    @Test
    void shouldHaveLastRegenTickSetToCurrentTime() {
        long before = System.currentTimeMillis();
        CombatData fresh = new CombatData();
        long after = System.currentTimeMillis();
        assertTrue(fresh.getLastRegenTick() >= before && fresh.getLastRegenTick() <= after);
    }

    // --- Setters and Getters ---

    @Test
    void shouldSetAndGetHealth() {
        data.setCurrentHealth(75.5);
        assertEquals(75.5, data.getCurrentHealth());
    }

    @Test
    void shouldSetAndGetMana() {
        data.setCurrentMana(250.0);
        assertEquals(250.0, data.getCurrentMana());
    }

    @Test
    void shouldSetAndGetStamina() {
        data.setCurrentStamina(80.0);
        assertEquals(80.0, data.getCurrentStamina());
    }

    @Test
    void shouldSetAndGetAbsorption() {
        data.setCurrentAbsorption(30.0);
        assertEquals(30.0, data.getCurrentAbsorption());
    }

    @Test
    void shouldSetAndGetLevel() {
        data.setLevel(50);
        assertEquals(50, data.getLevel());
    }

    @Test
    void shouldSetAndGetExperience() {
        data.setExperience(1234.56);
        assertEquals(1234.56, data.getExperience());
    }

    @Test
    void shouldSetAndGetLastRegenTick() {
        long tick = 999999L;
        data.setLastRegenTick(tick);
        assertEquals(tick, data.getLastRegenTick());
    }

    // --- addExperience ---

    @Test
    void shouldAddPositiveExperience() {
        data.addExperience(100.0);
        assertEquals(100.0, data.getExperience());
    }

    @Test
    void shouldAccumulateExperience() {
        data.addExperience(50.0);
        data.addExperience(30.0);
        assertEquals(80.0, data.getExperience());
    }

    @Test
    void shouldHandleZeroExperienceAdd() {
        data.addExperience(0.0);
        assertEquals(0.0, data.getExperience());
    }

    @Test
    void shouldHandleNegativeExperienceAdd() {
        data.setExperience(100.0);
        data.addExperience(-30.0);
        assertEquals(70.0, data.getExperience());
    }

    // --- Edge Cases ---

    @Test
    void shouldAllowZeroHealth() {
        data.setCurrentHealth(0.0);
        assertEquals(0.0, data.getCurrentHealth());
    }

    @Test
    void shouldAllowNegativeHealth() {
        data.setCurrentHealth(-10.0);
        assertEquals(-10.0, data.getCurrentHealth());
    }

    @Test
    void shouldAllowVeryLargeValues() {
        data.setCurrentHealth(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, data.getCurrentHealth());
    }

    @Test
    void shouldAllowLevelZero() {
        data.setLevel(0);
        assertEquals(0, data.getLevel());
    }

    // --- Thread Safety (addExperience is synchronized) ---

    @Test
    void shouldHandleConcurrentAddExperience() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        double amountPerIncrement = 1.0;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                for (int i = 0; i < incrementsPerThread; i++) {
                    data.addExperience(amountPerIncrement);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount * incrementsPerThread * amountPerIncrement, data.getExperience(), 0.01);
    }
}
