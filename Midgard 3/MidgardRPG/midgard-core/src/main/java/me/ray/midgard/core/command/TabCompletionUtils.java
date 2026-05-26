package me.ray.midgard.core.command;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.function.Predicate;

/**
 * Utilitários para tab completion dinâmico e contextual.
 * Fornece métodos para filtrar sugestões baseadas em permissões e contexto.
 */
@SuppressWarnings("unchecked")
public final class TabCompletionUtils {

    private TabCompletionUtils() {
        // Classe utilitária
    }

    /**
     * Filtra uma lista de sugestões baseado em permissões do sender.
     * 
     * @param sender O sender do comando
     * @param suggestions Lista de sugestões originais
     * @param permissionProvider Função que retorna a permissão necessária para cada sugestão
     * @return Lista filtrada de sugestões
     */
    public static List<String> filterByPermission(CommandSender sender, 
                                                 List<String> suggestions, 
                                                 java.util.function.Function<String, String> permissionProvider) {
        if (sender == null || suggestions == null || permissionProvider == null) {
            return Collections.emptyList();
        }

        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            String permission = permissionProvider.apply(suggestion);
            if (permission == null || sender.hasPermission(permission)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    /**
     * Filtra sugestões baseado em um predicado customizado.
     */
    public static List<String> filterSuggestions(List<String> suggestions, Predicate<String> filter) {
        if (suggestions == null || filter == null) {
            return Collections.emptyList();
        }

        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (filter.test(suggestion)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    /**
     * Completa parcialmente uma string baseado em uma lista de sugestões.
     * Versão melhorada que inclui filtro por permissões.
     */
    public static List<String> completePartial(CommandSender sender, 
                                              String partial, 
                                              List<String> suggestions, 
                                              java.util.function.Function<String, String> permissionProvider) {
        List<String> filtered = filterByPermission(sender, suggestions, permissionProvider);
        return StringUtil.copyPartialMatches(partial, filtered, new ArrayList<>());
    }

    /**
     * Completa parcialmente com filtro customizado.
     */
    public static List<String> completePartial(String partial, 
                                              List<String> suggestions, 
                                              Predicate<String> filter) {
        List<String> filtered = filterSuggestions(suggestions, filter);
        return StringUtil.copyPartialMatches(partial, filtered, new ArrayList<>());
    }

    /**
     * Gera sugestões hierárquicas para comandos com subcomandos.
     * 
     * @param sender O sender do comando
     * @param args Argumentos atuais do comando
     * @param commandStructure Mapa hierárquico de comandos e subcomandos
     * @param permissionProvider Função que fornece permissões para cada nível
     * @return Lista de sugestões contextuais
     */
    public static List<String> hierarchicalCompletion(CommandSender sender, 
                                                     String[] args, 
                                                     Map<String, Object> commandStructure, 
                                                     java.util.function.Function<String[], String> permissionProvider) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        // Nível 1: comandos principais
        if (args.length == 1) {
            List<String> mainCommands = new ArrayList<>(commandStructure.keySet());
            return completePartial(sender, args[0], mainCommands, 
                cmd -> permissionProvider.apply(new String[]{cmd}));
        }

        // Níveis mais profundos: navegar na estrutura hierárquica
        Object currentLevel = commandStructure;
        for (int i = 0; i < args.length - 1; i++) {
            if (currentLevel instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) currentLevel;
                currentLevel = map.get(args[i].toLowerCase());
            } else {
                break;
            }
        }

        if (currentLevel instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) currentLevel;
            List<String> subCommands = new ArrayList<>(map.keySet());
            return completePartial(sender, args[args.length - 1], subCommands, 
                subCmd -> permissionProvider.apply(args));
        }

        return Collections.emptyList();
    }

    /**
     * Remove duplicatas de uma lista mantendo a ordem.
     */
    public static List<String> removeDuplicates(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        
        for (String item : list) {
            if (seen.add(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Combina múltiplas listas de sugestões removendo duplicatas.
     */
    public static List<String> combineSuggestions(List<String>... suggestionLists) {
        List<String> combined = new ArrayList<>();
        for (List<String> list : suggestionLists) {
            if (list != null) {
                combined.addAll(list);
            }
        }
        return removeDuplicates(combined);
    }

    public static List<String> filterStartingWith(String prefix, Collection<String> options) {
        return StringUtil.copyPartialMatches(prefix, options, new ArrayList<>());
    }
}