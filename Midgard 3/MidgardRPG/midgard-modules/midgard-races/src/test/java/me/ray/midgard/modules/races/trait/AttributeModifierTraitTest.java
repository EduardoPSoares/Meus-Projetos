package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AttributeModifierTraitTest {

    private AttributeModifierTrait trait;
    private Player mockPlayer;
    private AttributeInstance mockInstance;

    @BeforeAll
    public static void setupBukkit() {
        // if (Bukkit.getServer() == null) {
        //    Server mockServer = mock(Server.class);
        //    Bukkit.setServer(mockServer);
        // }
    }

    @BeforeEach
    public void setUp() {
        trait = new AttributeModifierTrait() {
            @Override
            protected Attribute getAttribute(String name) {
                // Use Java Proxy to create a dummy Attribute instance
                // This avoids Mockito issues and static initialization triggers
                return (Attribute) java.lang.reflect.Proxy.newProxyInstance(
                    Attribute.class.getClassLoader(),
                    new Class<?>[]{Attribute.class},
                    (proxy, method, args) -> null
                );
            }
        };
        
        mockPlayer = mock(Player.class);
        mockInstance = mock(AttributeInstance.class);
        
        when(mockPlayer.getAttribute(any(Attribute.class))).thenReturn(mockInstance);
        when(mockInstance.getModifiers()).thenReturn(new java.util.HashSet<>());
    }

    @Test
    public void testExecuteApplyAttribute() {
        Map<String, Object> config = new HashMap<>();
        config.put("attribute", "MAX_HEALTH");
        config.put("value", 10.0);
        config.put("trait_id", "test_health_boost");

        trait.execute(mockPlayer, TraitTrigger.ON_SELECT, new HashMap<>(), config);

        ArgumentCaptor<AttributeModifier> captor = ArgumentCaptor.forClass(AttributeModifier.class);
        verify(mockInstance).addModifier(captor.capture());

        AttributeModifier modifier = captor.getValue();
        assertEquals(10.0, modifier.getAmount());
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, modifier.getOperation());
        assertEquals("midgard", modifier.getKey().getNamespace());
        assertEquals("race_test_health_boost", modifier.getKey().getKey());
    }

    @Test
    public void testExecuteRemoveAttribute() {
        // Use real objects since they are safe to instantiate in isolation (verified)
        NamespacedKey modifierKey = new NamespacedKey("midgard", "race_test_health_boost");
        AttributeModifier existingModifier = new AttributeModifier(modifierKey, 10.0, AttributeModifier.Operation.ADD_NUMBER);
        
        when(mockInstance.getModifiers()).thenReturn(java.util.Set.of(existingModifier));

        Map<String, Object> config = new HashMap<>();
        config.put("attribute", "MAX_HEALTH");
        config.put("trait_id", "test_health_boost");

        trait.execute(mockPlayer, TraitTrigger.ON_REMOVE, new HashMap<>(), config);

        verify(mockInstance).removeModifier(existingModifier);
    }
}
