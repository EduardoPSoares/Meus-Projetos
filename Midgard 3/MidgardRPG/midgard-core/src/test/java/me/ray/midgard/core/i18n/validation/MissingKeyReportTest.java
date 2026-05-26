package me.ray.midgard.core.i18n.validation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MissingKeyReportTest {

    @Test
    void shouldStartEmpty() {
        MissingKeyReport report = new MissingKeyReport();
        assertFalse(report.hasIssues());
        assertEquals(0, report.getTotalIssues());
    }

    @Test
    void shouldStoreModuleName() {
        MissingKeyReport report = new MissingKeyReport("combat");
        assertEquals("combat", report.getModuleName());
    }

    @Test
    void shouldHaveNoModuleByDefault() {
        MissingKeyReport report = new MissingKeyReport();
        assertNull(report.getModuleName());
    }

    @Test
    void shouldAddMissingKeyFull() {
        MissingKeyReport report = new MissingKeyReport();
        report.addMissingKey("combat.hit", "CombatListener", 42, "modules/combat/lang/messages.yml");

        assertTrue(report.hasIssues());
        assertEquals(1, report.getTotalIssues());
        assertEquals(1, report.getMissingKeys().size());

        var entry = report.getMissingKeys().get(0);
        assertEquals("combat.hit", entry.key());
        assertEquals("CombatListener", entry.usedInClass());
        assertEquals(42, entry.usedAtLine());
        assertEquals("modules/combat/lang/messages.yml", entry.expectedFile());
    }

    @Test
    void shouldAddMissingKeySimplified() {
        MissingKeyReport report = new MissingKeyReport();
        report.addMissingKey("msg.test", "lang/messages.yml");

        assertEquals(1, report.getMissingKeys().size());
        var entry = report.getMissingKeys().get(0);
        assertEquals("msg.test", entry.key());
        assertNull(entry.usedInClass());
        assertEquals(0, entry.usedAtLine());
    }

    @Test
    void shouldAddUnusedKey() {
        MissingKeyReport report = new MissingKeyReport();
        report.addUnusedKey("old.key", "lang/messages.yml");

        assertTrue(report.hasIssues());
        assertEquals(1, report.getUnusedKeys().size());
        assertEquals("old.key", report.getUnusedKeys().get(0).key());
        assertEquals("lang/messages.yml", report.getUnusedKeys().get(0).definedInFile());
    }

    @Test
    void shouldAddPlaceholderIssue() {
        MissingKeyReport report = new MissingKeyReport();
        Set<String> expected = Set.of("player", "amount");
        Set<String> found = Set.of("player");
        report.addPlaceholderIssue("msg.test", expected, found);

        assertTrue(report.hasIssues());
        assertEquals(1, report.getPlaceholderIssues().size());

        var issue = report.getPlaceholderIssues().get(0);
        assertEquals("msg.test", issue.getKey());
        assertTrue(issue.getMissingPlaceholders().contains("amount"));
        assertTrue(issue.getExtraPlaceholders().isEmpty());
    }

    @Test
    void shouldDetectExtraPlaceholders() {
        MissingKeyReport report = new MissingKeyReport();
        Set<String> expected = Set.of("player");
        Set<String> found = Set.of("player", "extra");
        report.addPlaceholderIssue("msg.test", expected, found);

        var issue = report.getPlaceholderIssues().get(0);
        assertTrue(issue.getExtraPlaceholders().contains("extra"));
    }

    @Test
    void shouldAddWarning() {
        MissingKeyReport report = new MissingKeyReport();
        report.addWarning("Arquivo vazio");

        assertTrue(report.hasIssues());
        assertEquals(1, report.getWarnings().size());
        assertEquals("Arquivo vazio", report.getWarnings().get(0));
    }

    @Test
    void shouldCountTotalIssues() {
        MissingKeyReport report = new MissingKeyReport();
        report.addMissingKey("a", "file.yml");
        report.addUnusedKey("b", "file.yml");
        report.addPlaceholderIssue("c", Set.of("x"), Set.of());
        report.addWarning("warning");

        assertEquals(4, report.getTotalIssues());
    }

    @Test
    void shouldReturnImmutableLists() {
        MissingKeyReport report = new MissingKeyReport();
        report.addMissingKey("key", "file");

        assertThrows(UnsupportedOperationException.class,
                () -> report.getMissingKeys().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> report.getUnusedKeys().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> report.getPlaceholderIssues().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> report.getWarnings().clear());
    }

    @Test
    void shouldGenerateConsoleFormat() {
        MissingKeyReport report = new MissingKeyReport("combat");
        report.addMissingKey("combat.hit", "CombatListener", 42, "messages.yml");

        String output = report.toConsoleFormat();
        assertNotNull(output);
        assertTrue(output.contains("RELATÓRIO DE VALIDAÇÃO"));
        assertTrue(output.contains("combat"));
        assertTrue(output.contains("FALTANTE"));
        assertTrue(output.contains("combat.hit"));
    }

    @Test
    void shouldGenerateCleanOutputWhenNoIssues() {
        MissingKeyReport report = new MissingKeyReport();
        String output = report.toConsoleFormat();
        assertTrue(output.contains("Nenhum problema encontrado"));
    }

    @Test
    void shouldGenerateYamlStubs() {
        MissingKeyReport report = new MissingKeyReport();
        report.addMissingKey("combat.hit", "messages.yml");

        String yaml = report.generateYamlStubs();
        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());
        assertTrue(yaml.contains("MENSAGENS GERADAS AUTOMATICAMENTE"));
        assertTrue(yaml.contains("combat"));
    }

    @Test
    void shouldReturnEmptyYamlWhenNoMissingKeys() {
        MissingKeyReport report = new MissingKeyReport();
        assertEquals("", report.generateYamlStubs());
    }

    @Test
    void shouldGenerateJson() {
        MissingKeyReport report = new MissingKeyReport("test");
        report.addMissingKey("msg.test", "MyClass", 10, "messages.yml");
        report.addUnusedKey("old.key", "messages.yml");

        String json = report.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"totalIssues\": 2"));
        assertTrue(json.contains("\"module\": \"test\""));
        assertTrue(json.contains("msg.test"));
        assertTrue(json.contains("old.key"));
    }

    @Test
    void shouldHaveTimestamp() {
        MissingKeyReport report = new MissingKeyReport();
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    void shouldHandleNullPlaceholderSets() {
        MissingKeyReport.PlaceholderIssue issue = 
                new MissingKeyReport.PlaceholderIssue("key", null, null);

        assertTrue(issue.getMissingPlaceholders().isEmpty());
        assertTrue(issue.getExtraPlaceholders().isEmpty());
    }
}
