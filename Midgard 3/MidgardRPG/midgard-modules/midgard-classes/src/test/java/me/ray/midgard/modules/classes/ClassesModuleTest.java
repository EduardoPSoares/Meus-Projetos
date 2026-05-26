package me.ray.midgard.modules.classes;

import me.ray.midgard.core.ModulePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassesModuleTest {

    @BeforeEach
    void setUp() throws Exception {
        resetInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetInstance();
    }

    private void resetInstance() throws Exception {
        Field instanceField = ClassesModule.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // ============================================
    // CONSTRUCTOR
    // ============================================

    @Nested
    class ConstructorTests {

        @Test
        void shouldHaveCorrectModuleName() {
            ClassesModule module = new ClassesModule();
            assertEquals("Classes", module.getName());
        }

        @Test
        void shouldHaveNormalPriority() {
            ClassesModule module = new ClassesModule();
            assertEquals(ModulePriority.NORMAL, module.getPriority());
        }
    }

    // ============================================
    // SINGLETON
    // ============================================

    @Nested
    class SingletonTests {

        @Test
        void getInstanceShouldReturnNullByDefault() {
            assertNull(ClassesModule.getInstance());
        }

        @Test
        void getInstanceShouldReturnSetInstance() throws Exception {
            ClassesModule module = new ClassesModule();
            Field instanceField = ClassesModule.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, module);

            assertSame(module, ClassesModule.getInstance());
        }

        @Test
        void onDisableShouldClearInstance() throws Exception {
            ClassesModule module = new ClassesModule();
            Field instanceField = ClassesModule.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, module);

            assertNotNull(ClassesModule.getInstance());
            module.onDisable();
            assertNull(ClassesModule.getInstance());
        }
    }

    // ============================================
    // getMessage
    // ============================================

    @Nested
    class GetMessageTests {

        @Test
        void shouldReturnPathWhenMessagesConfigIsNull() {
            ClassesModule module = new ClassesModule();
            String result = module.getMessage("some.path");
            assertEquals("some.path", result);
        }

        @Test
        void shouldReturnPathForMissingKey() {
            ClassesModule module = new ClassesModule();
            String result = module.getMessage("nonexistent.key");
            assertEquals("nonexistent.key", result);
        }

        @Test
        void shouldReturnDifferentPathsForDifferentKeys() {
            ClassesModule module = new ClassesModule();
            assertNotEquals(
                    module.getMessage("key.one"),
                    module.getMessage("key.two")
            );
        }
    }

    // ============================================
    // getMessageList
    // ============================================

    @Nested
    class GetMessageListTests {

        @Test
        void shouldReturnEmptyListWhenConfigIsNull() {
            ClassesModule module = new ClassesModule();
            List<String> result = module.getMessageList("some.path");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyListForMissingKey() {
            ClassesModule module = new ClassesModule();
            List<String> result = module.getMessageList("nonexistent");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ============================================
    // capitalize (private — tested via reflection)
    // ============================================

    @Nested
    class CapitalizeTests {

        private String invokeCapitalize(String input) throws Exception {
            ClassesModule module = new ClassesModule();
            Method method = ClassesModule.class.getDeclaredMethod("capitalize", String.class);
            method.setAccessible(true);
            return (String) method.invoke(module, input);
        }

        @Test
        void shouldCapitalizeSimpleWord() throws Exception {
            assertEquals("Strength", invokeCapitalize("strength"));
        }

        @Test
        void shouldNotChangeAlreadyCapitalized() throws Exception {
            assertEquals("Strength", invokeCapitalize("Strength"));
        }

        @Test
        void shouldCapitalizeSingleChar() throws Exception {
            assertEquals("A", invokeCapitalize("a"));
        }

        @Test
        void shouldReturnNullForNull() throws Exception {
            assertNull(invokeCapitalize(null));
        }

        @Test
        void shouldReturnEmptyForEmpty() throws Exception {
            assertEquals("", invokeCapitalize(""));
        }

        @Test
        void shouldOnlyCapitalizeFirstLetter() throws Exception {
            assertEquals("Hello world", invokeCapitalize("hello world"));
        }

        @Test
        void shouldHandleAllUppercase() throws Exception {
            assertEquals("DEFENSE", invokeCapitalize("DEFENSE"));
        }
    }

    // ============================================
    // INITIAL STATE (before onEnable)
    // ============================================

    @Nested
    class InitialStateTests {

        @Test
        void classManagerShouldBeNullBeforeEnable() {
            ClassesModule module = new ClassesModule();
            assertNull(module.getClassManager());
        }

        @Test
        void skillTreeManagerShouldBeNullBeforeEnable() {
            ClassesModule module = new ClassesModule();
            assertNull(module.getSkillTreeManager());
        }

        @Test
        void repositoryShouldBeNullBeforeEnable() {
            ClassesModule module = new ClassesModule();
            assertNull(module.getRepository());
        }

        @Test
        void syncManagerShouldBeNullBeforeEnable() {
            ClassesModule module = new ClassesModule();
            assertNull(module.getSyncManager());
        }
    }

    // ============================================
    // applyClassAttributes — null guard
    // ============================================

    @Nested
    class ApplyClassAttributesTests {

        @Test
        void shouldNotThrowWhenProfileIsNull() {
            ClassesModule module = new ClassesModule();
            assertDoesNotThrow(() -> module.applyClassAttributes(null, null, 1));
        }

        @Test
        void shouldNotThrowWhenRpgClassIsNull() {
            ClassesModule module = new ClassesModule();
            assertDoesNotThrow(() -> module.applyClassAttributes(null, null, 1));
        }
    }
}
