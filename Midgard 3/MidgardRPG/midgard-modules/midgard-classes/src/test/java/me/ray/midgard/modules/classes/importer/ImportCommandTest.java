package me.ray.midgard.modules.classes.importer;

import me.ray.midgard.modules.classes.ClassesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportCommandTest {

    private ClassesModule module;
    private ImportCommand command;

    @BeforeEach
    void setUp() {
        module = new ClassesModule();
        command = new ImportCommand(module);
    }

    // ============================================
    // CONSTRUCTOR & METADATA
    // ============================================

    @Nested
    class MetadataTests {

        @Test
        void shouldHaveCorrectName() {
            assertEquals("import", command.getName());
        }

        @Test
        void shouldHaveCorrectPermission() {
            assertEquals("midgard.admin", command.getPermission());
        }

        @Test
        void shouldNotBePlayerOnly() {
            assertFalse(command.isPlayerOnly());
        }
    }

    // ============================================
    // DESCRIPTION & USAGE
    // ============================================

    @Nested
    class DescriptionTests {

        @Test
        void shouldReturnDescriptionFromModule() {
            String desc = command.getDescription();
            assertEquals("command.import_description", desc);
        }

        @Test
        void shouldReturnUsageFromModule() {
            String usage = command.getUsage();
            assertEquals("command.import_usage", usage);
        }
    }

    // ============================================
    // MODULE REFERENCE
    // ============================================

    @Nested
    class ModuleReferenceTests {

        @Test
        void shouldStoreModuleReference() throws Exception {
            Field moduleField = ImportCommand.class.getDeclaredField("module");
            moduleField.setAccessible(true);
            assertSame(module, moduleField.get(command));
        }
    }

    // ============================================
    // filterStartsWith (private — tested via reflection)
    // ============================================

    @Nested
    class FilterTests {

        @SuppressWarnings("unchecked")
        private List<String> invokeFilter(List<String> options, String prefix) throws Exception {
            Method method = ImportCommand.class.getDeclaredMethod("filterStartsWith", List.class, String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(command, options, prefix);
        }

        @Test
        void shouldReturnMatchingOptions() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("mmocore", "mythicmobs"), "mmo");
            assertEquals(1, result.size());
            assertEquals("mmocore", result.get(0));
        }

        @Test
        void shouldReturnAllWhenEmptyPrefix() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("a", "b", "c"), "");
            assertEquals(3, result.size());
        }

        @Test
        void shouldReturnEmptyWhenNoMatch() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("mmocore"), "xyz");
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldBeCaseInsensitive() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("MMOCore"), "mmo");
            assertEquals(1, result.size());
            assertEquals("MMOCore", result.get(0));
        }

        @Test
        void shouldHandleEmptyOptionsList() throws Exception {
            List<String> result = invokeFilter(Collections.emptyList(), "test");
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnMultipleMatches() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("mmocore", "mmomobs", "other"), "mmo");
            assertEquals(2, result.size());
            assertTrue(result.contains("mmocore"));
            assertTrue(result.contains("mmomobs"));
        }

        @Test
        void shouldFilterExactMatch() throws Exception {
            List<String> result = invokeFilter(Arrays.asList("mmocore"), "mmocore");
            assertEquals(1, result.size());
            assertEquals("mmocore", result.get(0));
        }
    }

    // ============================================
    // msg helper (private — tested via reflection)
    // ============================================

    @Nested
    class MsgHelperTests {

        private String invokeMsg(String key) throws Exception {
            Method method = ImportCommand.class.getDeclaredMethod("msg", String.class);
            method.setAccessible(true);
            return (String) method.invoke(command, key);
        }

        @Test
        void shouldPrefixWithImport() throws Exception {
            String result = invokeMsg("usage");
            // Without messagesConfig, module.getMessage returns the path as-is
            assertEquals("import.usage", result);
        }

        @Test
        void shouldPrefixDifferentKeys() throws Exception {
            assertEquals("import.success", invokeMsg("success"));
            assertEquals("import.failed", invokeMsg("failed"));
            assertEquals("import.starting", invokeMsg("starting"));
        }
    }
}
