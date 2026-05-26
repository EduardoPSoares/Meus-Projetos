package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseMetadataSmokeTest {

    private static final Path ROOT_POM = Path.of("..", "pom.xml");
    private static final Path MODULE_SOURCE = Path.of("src", "main", "java", "com", "midgard", "fooddecay");
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    @Test
    void pluginMetadataDeclaresEveryCommandPermission() throws Exception {
        String commandSource = Files.readString(MODULE_SOURCE.resolve("FoodDecayCommand.java"));
        Matcher matcher = Pattern.compile("permission\\s*=\\s*\"([^\"]+)\"").matcher(commandSource);

        Set<String> commandPermissions = new LinkedHashSet<>();
        while (matcher.find()) {
            commandPermissions.add(matcher.group(1));
        }

        String pluginYamlText = Files.readString(RESOURCES.resolve("plugin.yml"));
        Set<String> declaredPermissions = parseDeclaredPermissions(pluginYamlText);

        assertTrue(declaredPermissions.containsAll(commandPermissions),
                "plugin.yml nao declara todas as permissoes usadas pelos comandos. Faltando: "
                        + missing(commandPermissions, declaredPermissions));
    }

    @Test
    void messagesFileHasNoDuplicateTopLevelKeys() throws Exception {
        Pattern keyPattern = Pattern.compile("^([A-Za-z0-9._-]+):");
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String rawLine : Files.readAllLines(RESOURCES.resolve("messages.yml"))) {
            if (rawLine.isBlank() || rawLine.startsWith("#") || Character.isWhitespace(rawLine.charAt(0))) {
                continue;
            }

            Matcher matcher = keyPattern.matcher(rawLine);
            if (matcher.find()) {
                counts.merge(matcher.group(1), 1, Integer::sum);
            }
        }

        Map<String, Integer> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        assertTrue(duplicates.isEmpty(), "messages.yml contem chaves duplicadas: " + duplicates);
    }

    @Test
    void runtimeVersionMatchesRootPomVersion() throws Exception {
        String pomXml = Files.readString(ROOT_POM);
        Matcher pomVersionMatcher = Pattern.compile("<version>([^<]+)</version>").matcher(pomXml);
        assertTrue(pomVersionMatcher.find(), "Nao foi possivel encontrar a versao no pom raiz");
        String rootVersion = pomVersionMatcher.group(1).trim();

        String moduleSource = Files.readString(MODULE_SOURCE.resolve("FoodDecayModule.java"));
        Matcher moduleVersionMatcher = Pattern.compile("super\\(\"[^\"]+\",\\s*\"([^\"]+)\"\\)").matcher(moduleSource);
        assertTrue(moduleVersionMatcher.find(), "Nao foi possivel encontrar a versao runtime do FoodDecayModule");

        assertEquals(rootVersion, moduleVersionMatcher.group(1),
                "A versao runtime do modulo precisa bater com a versao Maven do projeto");
    }

    private static Set<String> missing(Set<String> expected, Set<String> actual) {
        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private static Set<String> parseDeclaredPermissions(String pluginYamlText) {
        Set<String> permissions = new LinkedHashSet<>();
        boolean insidePermissions = false;

        for (String line : pluginYamlText.split("\\R")) {
            if (line.equals("permissions:")) {
                insidePermissions = true;
                continue;
            }

            if (!insidePermissions) {
                continue;
            }

            if (!line.startsWith("  ")) {
                break;
            }

            Matcher matcher = Pattern.compile("^  ([A-Za-z0-9._-]+):\\s*$").matcher(line);
            if (matcher.find()) {
                permissions.add(matcher.group(1));
            }
        }

        assertNotNull(permissions, "plugin.yml precisa declarar a secao de permissions");
        return permissions;
    }
}
