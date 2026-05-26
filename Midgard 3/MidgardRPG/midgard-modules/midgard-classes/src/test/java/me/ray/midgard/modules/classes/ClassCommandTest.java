package me.ray.midgard.modules.classes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassCommandTest {

    private ClassesModule module;
    private ClassCommand command;

    @BeforeEach
    void setUp() {
        module = new ClassesModule();
        command = new ClassCommand(module);
    }

    // ============================================
    // CONSTRUCTOR & METADATA
    // ============================================

    @Nested
    class MetadataTests {

        @Test
        void shouldHaveCorrectName() {
            assertEquals("class", command.getName());
        }

        @Test
        void shouldHaveCorrectPermission() {
            assertEquals("midgard.admin.class", command.getPermission());
        }

        @Test
        void shouldBePlayerOnly() {
            assertTrue(command.isPlayerOnly());
        }

        @Test
        void shouldHaveClassesAlias() {
            List<String> aliases = command.getAliases();
            assertNotNull(aliases);
            assertEquals(1, aliases.size());
            assertEquals("classes", aliases.get(0));
        }
    }

    // ============================================
    // DESCRIPTION & USAGE (delegates to module.getMessage)
    // ============================================

    @Nested
    class DescriptionTests {

        @Test
        void shouldReturnDescriptionFromModule() {
            // Without messagesConfig, getMessage returns the path
            String desc = command.getDescription();
            assertEquals("command.class_description", desc);
        }

        @Test
        void shouldReturnUsageFromModule() {
            String usage = command.getUsage();
            assertEquals("command.class_usage", usage);
        }
    }

    // ============================================
    // MODULE REFERENCE
    // ============================================

    @Nested
    class ModuleReferenceTests {

        @Test
        void shouldStoreModuleReference() throws Exception {
            Field moduleField = ClassCommand.class.getDeclaredField("module");
            moduleField.setAccessible(true);
            assertSame(module, moduleField.get(command));
        }

        @Test
        void shouldAcceptDifferentModuleInstances() throws Exception {
            ClassesModule module2 = new ClassesModule();
            ClassCommand command2 = new ClassCommand(module2);

            Field moduleField = ClassCommand.class.getDeclaredField("module");
            moduleField.setAccessible(true);
            assertSame(module2, moduleField.get(command2));
            assertNotSame(module, moduleField.get(command2));
        }
    }
}
