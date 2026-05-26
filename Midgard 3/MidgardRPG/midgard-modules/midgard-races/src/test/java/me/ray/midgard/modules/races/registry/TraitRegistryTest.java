package me.ray.midgard.modules.races.registry;

import me.ray.midgard.modules.races.api.RaceTrait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TraitRegistryTest {

    private TraitRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = TraitRegistry.getInstance();
    }

    @Test
    public void testRegistration() {
        RaceTrait mockTrait = mock(RaceTrait.class);
        when(mockTrait.getId()).thenReturn("test_trait");

        registry.register(mockTrait);

        assertEquals(mockTrait, registry.getTrait("test_trait"));
    }

    @Test
    public void testDuplicateRegistration() {
        RaceTrait trait1 = mock(RaceTrait.class);
        RaceTrait trait2 = mock(RaceTrait.class);
        when(trait1.getId()).thenReturn("duplicate");
        when(trait2.getId()).thenReturn("duplicate");

        registry.register(trait1);
        registry.register(trait2);

        // Should keep the latest one or handle as needed. 
        // Based on typical registry implementation, it usually overwrites.
        assertEquals(trait2, registry.getTrait("duplicate"));
    }

    @Test
    public void testGetNonExistentTrait() {
        assertNull(registry.getTrait("non_existent"));
    }
}
