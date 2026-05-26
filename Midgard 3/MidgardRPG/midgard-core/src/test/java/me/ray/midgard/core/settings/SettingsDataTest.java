package me.ray.midgard.core.settings;

import me.ray.midgard.core.profile.ModuleData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsDataTest {

    @Test
    void shouldBeModuleData() {
        SettingsData data = new SettingsData();
        assertInstanceOf(ModuleData.class, data);
    }

    @Test
    void shouldInstantiateWithDefaultConstructor() {
        assertDoesNotThrow(SettingsData::new);
    }
}
