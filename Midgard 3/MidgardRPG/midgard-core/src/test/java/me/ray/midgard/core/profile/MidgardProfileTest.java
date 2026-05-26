package me.ray.midgard.core.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MidgardProfileTest {

    static class TestModuleData implements ModuleData {
        String value = "test";
    }

    static class OtherModuleData implements ModuleData {
        int count = 0;
    }

    @Test
    void shouldStoreUuidAndName() {
        UUID uuid = UUID.randomUUID();
        MidgardProfile profile = new MidgardProfile(uuid, "TestPlayer");

        assertEquals(uuid, profile.getUuid());
        assertEquals("TestPlayer", profile.getName());
    }

    @Test
    void shouldReturnNullForMissingData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        assertNull(profile.getData(TestModuleData.class));
    }

    @Test
    void shouldSetAndGetData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        TestModuleData data = new TestModuleData();
        data.value = "hello";

        profile.setData(data);
        TestModuleData retrieved = profile.getData(TestModuleData.class);

        assertNotNull(retrieved);
        assertEquals("hello", retrieved.value);
    }

    @Test
    void shouldGetOrCreateData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");

        TestModuleData data = profile.getOrCreateData(TestModuleData.class);
        assertNotNull(data);
        assertEquals("test", data.value);

        // Calling again should return the same instance
        TestModuleData same = profile.getOrCreateData(TestModuleData.class);
        assertSame(data, same);
    }

    @Test
    void shouldCheckHasData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        assertFalse(profile.hasData(TestModuleData.class));

        profile.setData(new TestModuleData());
        assertTrue(profile.hasData(TestModuleData.class));
    }

    @Test
    void shouldTrackMultipleModuleDataTypes() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");

        TestModuleData testData = new TestModuleData();
        OtherModuleData otherData = new OtherModuleData();
        otherData.count = 42;

        profile.setData(testData);
        profile.setData(otherData);

        assertTrue(profile.hasData(TestModuleData.class));
        assertTrue(profile.hasData(OtherModuleData.class));
        assertEquals("test", profile.getData(TestModuleData.class).value);
        assertEquals(42, profile.getData(OtherModuleData.class).count);
    }

    @Test
    void shouldReturnModuleDataSnapshot() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        profile.setData(new TestModuleData());

        Map<Class<? extends ModuleData>, ModuleData> snapshot = profile.getModuleDataSnapshot();
        assertNotNull(snapshot);
        assertEquals(1, snapshot.size());
        assertTrue(snapshot.containsKey(TestModuleData.class));
    }

    @Test
    void shouldStoreUnknownData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        assertTrue(profile.getUnknownData().isEmpty());

        JsonElement element = new JsonPrimitive("test_value");
        profile.addUnknownData("SomeModule", element);

        assertEquals(1, profile.getUnknownData().size());
        assertEquals(element, profile.getUnknownData().get("SomeModule"));
    }

    @Test
    void shouldReturnUnmodifiableUnknownData() {
        MidgardProfile profile = new MidgardProfile(UUID.randomUUID(), "Test");
        assertThrows(UnsupportedOperationException.class, () -> 
            profile.getUnknownData().put("key", new JsonPrimitive("val")));
    }
}
