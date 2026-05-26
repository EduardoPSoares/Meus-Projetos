package com.midgard.fooddecay.gui;

import static com.midgard.core.utils.MessageUtils.sc;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecipeEditorText {

    private RecipeEditorText() {
    }

    public static List<String> buildFieldLore(String description, String currentValueLine, String... actions) {
        List<String> lore = new ArrayList<>();
        lore.add(sc("&8Informacoes"));
        for (String line : description.split("\\|")) {
            lore.add(sc("&7" + line.trim()));
        }

        lore.add("");
        lore.add(sc("&8Estado Atual"));
        lore.add(sc(currentValueLine));

        lore.add("");
        lore.add(sc("&8Acoes"));
        for (String action : actions) {
            lore.add(sc(action));
        }
        return lore;
    }

    public static List<String> buildTextLore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(sc(line));
        }
        return lore;
    }

    public static List<String> buildTabLore(boolean active, String line1, String line2) {
        return buildTextLore(
                line1,
                line2,
                "",
                active ? "&aPagina aberta agora." : "&eClique para abrir esta pagina."
        );
    }

    public static String getDisplayValue(String value, String fallback) {
        return value != null ? value : fallback;
    }

    public static String formatDouble(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        String formatted = String.format(Locale.US, "%.2f", value);
        while (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    public static String formatMaterial(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    public static String currentValueLine(String label, String value, boolean configured) {
        return "&f" + label + ": " + (configured ? "&a" : "&c") + value;
    }

    public static String leftAction(String text) {
        return "&aClique esquerdo: &7" + text;
    }

    public static String rightAction(String text) {
        return "&bClique direito: &7" + text;
    }

    public static String shiftAction(String text) {
        return "&cShift + clique: &7" + text;
    }

    public static String noteAction(String text) {
        return "&eObservacao: &7" + text;
    }

    public static String requiredAction() {
        return "&6Obrigatorio para salvar";
    }

    public static String requiredAction(String fieldName) {
        return "&6Obrigatorio: &f" + fieldName;
    }

    public static String optionalAction() {
        return "&9Opcional";
    }

    public static String requirementStatusLine(String fieldName, boolean configured) {
        return "&f" + fieldName + ": " + (configured ? "&aOK" : "&cPendente");
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String blankToNull(String value) {
        return trimToNull(value);
    }
}
