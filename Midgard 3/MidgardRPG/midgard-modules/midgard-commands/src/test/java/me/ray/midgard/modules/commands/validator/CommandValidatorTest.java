package me.ray.midgard.modules.commands.validator;

import me.ray.midgard.modules.commands.validator.CommandValidator.ValidationIssue;
import me.ray.midgard.modules.commands.validator.CommandValidator.ValidationIssue.Severity;
import me.ray.midgard.modules.commands.registry.CentralCommandRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandValidatorTest {

    @Mock
    private CentralCommandRegistry registry;

    private CommandValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CommandValidator(registry);
    }

    @Test
    void shouldReturnEmptyIssues_initially() {
        List<ValidationIssue> issues = validator.getIssues();
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void shouldReturnDefensiveCopyOfIssues() {
        List<ValidationIssue> issues1 = validator.getIssues();
        List<ValidationIssue> issues2 = validator.getIssues();
        assertNotSame(issues1, issues2);
    }

    @Test
    void shouldReturnZeroCountsForAllSeverities_initially() {
        Map<Severity, Integer> counts = validator.getIssueCounts();
        assertNotNull(counts);
        assertEquals(0, counts.get(Severity.INFO));
        assertEquals(0, counts.get(Severity.WARNING));
        assertEquals(0, counts.get(Severity.ERROR));
    }

    @Test
    void shouldReturnEmptyFilteredList_initially() {
        assertTrue(validator.getIssuesBySeverity(Severity.ERROR).isEmpty());
        assertTrue(validator.getIssuesBySeverity(Severity.WARNING).isEmpty());
        assertTrue(validator.getIssuesBySeverity(Severity.INFO).isEmpty());
    }

    @Test
    void shouldValidatePermissions_adminWithoutPermission() {
        // A validação de permissões é testável: comandos admin sem permissão geram warning
        // Porém depende de getAllCommands() que precisa de comandos registrados
        me.ray.midgard.modules.commands.registry.CommandDescriptor adminCmd =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("test")
                        .category(me.ray.midgard.core.command.CommandCategory.ADMIN)
                        .permission(null)
                        .module("testmod")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(adminCmd));
        when(registry.getBukkitCommandMap()).thenReturn(null);

        validator.validatePermissions();

        List<ValidationIssue> issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertEquals(Severity.WARNING, issues.get(0).getSeverity());
    }

    @Test
    void shouldValidatePermissions_nonStandardPrefix() {
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("test")
                        .category(me.ray.midgard.core.command.CommandCategory.PLAYER)
                        .permission("other.plugin.test")
                        .module("testmod")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(cmd));

        validator.validatePermissions();

        List<ValidationIssue> issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertEquals(Severity.INFO, issues.get(0).getSeverity());
    }

    @Test
    void shouldValidatePermissions_standardPermission_noIssue() {
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("test")
                        .category(me.ray.midgard.core.command.CommandCategory.PLAYER)
                        .permission("midgard.player.test")
                        .module("testmod")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(cmd));

        validator.validatePermissions();

        List<ValidationIssue> issues = validator.getIssues();
        assertTrue(issues.isEmpty());
    }

    @Test
    void shouldValidateAliasConflicts_sameModule_noConflict() {
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd1 =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("spell")
                        .module("spells")
                        .aliases("sp")
                        .build();
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd2 =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("sp")
                        .module("spells")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(cmd1, cmd2));

        validator.validateAliasConflicts();

        // Same module → no conflict
        List<ValidationIssue> warnings = validator.getIssuesBySeverity(Severity.WARNING);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void shouldValidateAliasConflicts_differentModules_conflict() {
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd1 =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("tp")
                        .module("essentials")
                        .build();
        me.ray.midgard.modules.commands.registry.CommandDescriptor cmd2 =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("teleport")
                        .module("core")
                        .aliases("tp")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(cmd1, cmd2));

        validator.validateAliasConflicts();

        List<ValidationIssue> warnings = validator.getIssuesBySeverity(Severity.WARNING);
        assertFalse(warnings.isEmpty());
    }

    @Test
    void shouldClearIssuesBetweenValidateAllCalls() {
        when(registry.getAllCommands()).thenReturn(Collections.emptyList());
        when(registry.getBukkitCommandMap()).thenReturn(null);

        validator.validateAll();
        int firstCount = validator.getIssues().size();

        validator.validateAll();
        int secondCount = validator.getIssues().size();

        // Issues should be cleared between calls, so counts must match
        assertEquals(firstCount, secondCount);
    }

    @Test
    void shouldCountIssuesBySeverity() {
        // Register diverse commands to generate mixed issues
        me.ray.midgard.modules.commands.registry.CommandDescriptor adminNoPermCmd =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("admin1")
                        .category(me.ray.midgard.core.command.CommandCategory.ADMIN)
                        .permission(null)
                        .module("mod1")
                        .build();
        me.ray.midgard.modules.commands.registry.CommandDescriptor nonStdPermCmd =
                me.ray.midgard.modules.commands.registry.CommandDescriptor.builder("player1")
                        .category(me.ray.midgard.core.command.CommandCategory.PLAYER)
                        .permission("other.perm")
                        .module("mod2")
                        .build();

        when(registry.getAllCommands()).thenReturn(List.of(adminNoPermCmd, nonStdPermCmd));

        validator.validatePermissions();

        Map<Severity, Integer> counts = validator.getIssueCounts();
        assertEquals(1, counts.get(Severity.WARNING));
        assertEquals(1, counts.get(Severity.INFO));
        assertEquals(0, counts.get(Severity.ERROR));
    }
}
