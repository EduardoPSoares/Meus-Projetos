package me.ray.midgard.modules.commands.validator;

import me.ray.midgard.modules.commands.validator.CommandValidator.ValidationIssue;
import me.ray.midgard.modules.commands.validator.CommandValidator.ValidationIssue.Severity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationIssueTest {

    @Test
    void shouldStoreAllFields() {
        ValidationIssue issue = new ValidationIssue(
                Severity.ERROR,
                "Command not found",
                "combat",
                "attack"
        );

        assertEquals(Severity.ERROR, issue.getSeverity());
        assertEquals("Command not found", issue.getMessage());
        assertEquals("combat", issue.getModule());
        assertEquals("attack", issue.getCommand());
    }

    @Test
    void shouldReturnCorrectSeverity_error() {
        ValidationIssue issue = new ValidationIssue(Severity.ERROR, "msg", "mod", "cmd");
        assertEquals(Severity.ERROR, issue.getSeverity());
    }

    @Test
    void shouldReturnCorrectSeverity_warning() {
        ValidationIssue issue = new ValidationIssue(Severity.WARNING, "msg", "mod", "cmd");
        assertEquals(Severity.WARNING, issue.getSeverity());
    }

    @Test
    void shouldReturnCorrectSeverity_info() {
        ValidationIssue issue = new ValidationIssue(Severity.INFO, "msg", "mod", "cmd");
        assertEquals(Severity.INFO, issue.getSeverity());
    }

    @Test
    void shouldIncludeAllFieldsInToString() {
        ValidationIssue issue = new ValidationIssue(
                Severity.WARNING,
                "Missing permission",
                "spells",
                "cast"
        );

        String str = issue.toString();
        assertTrue(str.contains("WARNING"));
        assertTrue(str.contains("Missing permission"));
        assertTrue(str.contains("spells"));
        assertTrue(str.contains("cast"));
    }

    @Nested
    class SeverityEnumTest {

        @Test
        void shouldHaveThreeValues() {
            assertEquals(3, Severity.values().length);
        }

        @Test
        void shouldContainInfo() {
            assertNotNull(Severity.valueOf("INFO"));
        }

        @Test
        void shouldContainWarning() {
            assertNotNull(Severity.valueOf("WARNING"));
        }

        @Test
        void shouldContainError() {
            assertNotNull(Severity.valueOf("ERROR"));
        }
    }
}
