package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tema Visual para GUIs de Raças
 * Design: Hypixel/Wynncraft Premium Style
 * 
 * Paleta de Cores Padrão:
 * - Primário:   gradient:#a855f7:#ec4899 (Roxo → Rosa)
 * - Secundário: gradient:#06b6d4:#3b82f6 (Cyan → Azul)
 * - Sucesso:    gradient:#22c55e:#16a34a (Verde)
 * - Alerta:     gradient:#facc15:#f59e0b (Amarelo → Laranja)
 * - Perigo:     gradient:#ef4444:#dc2626 (Vermelho)
 */
public final class RaceGuiTheme {

    // Materiais padrão
    public static final Material GLASS_DARK = Material.BLACK_STAINED_GLASS_PANE;
    public static final Material GLASS_GRAY = Material.GRAY_STAINED_GLASS_PANE;
    public static final Material GLASS_LIGHT = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    
    // Cores de gradiente
    public static final String GRADIENT_PRIMARY = "<gradient:#a855f7:#ec4899>";
    public static final String GRADIENT_SECONDARY = "<gradient:#06b6d4:#3b82f6>";
    public static final String GRADIENT_SUCCESS = "<gradient:#22c55e:#16a34a>";
    public static final String GRADIENT_WARNING = "<gradient:#facc15:#f59e0b>";
    public static final String GRADIENT_DANGER = "<gradient:#ef4444:#dc2626>";

    private RaceGuiTheme() {
    }

    /**
     * Cria um painel de vidro decorativo
     */
    public static ItemStack createPane(Material material, String name) {
        if (material == null) {
            material = GLASS_DARK;
        }
        String displayName = (name == null || name.isBlank()) ? " " : name;
        return new ItemBuilder(material)
                .setName(displayName)
                .build();
    }
    
    /**
     * Cria painel escuro padrão
     */
    public static ItemStack createDarkPane() {
        return createPane(GLASS_DARK, " ");
    }

    /**
     * Preenche toda a borda do inventário
     */
    public static void fillBorder(Inventory inventory, ItemStack item) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        }
        if (item == null) {
            item = createDarkPane();
        }

        int size = inventory.getSize();
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("inventory size must be between 9 and 54 and a multiple of 9");
        }

        int rows = size / 9;
        int lastRowStart = (rows - 1) * 9;

        // Primeira e última linha
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, item);
            inventory.setItem(lastRowStart + slot, item);
        }

        // Laterais
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, item);
            inventory.setItem(row * 9 + 8, item);
        }
    }
    
    /**
     * Preenche apenas a linha inferior
     */
    public static void fillBottomRow(Inventory inventory, ItemStack item) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        }
        if (item == null) {
            item = createDarkPane();
        }

        int size = inventory.getSize();
        int lastRowStart = size - 9;

        for (int i = 0; i < 9; i++) {
            inventory.setItem(lastRowStart + i, item);
        }
    }
    
    /**
     * Preenche slots específicos
     */
    public static void fillSlots(Inventory inventory, ItemStack item, int... slots) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        }
        if (item == null) {
            item = createDarkPane();
        }
        if (slots == null) {
            return;
        }

        int size = inventory.getSize();
        for (int slot : slots) {
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, item);
            }
        }
    }

    /**
     * Barra de progresso estilo premium
     * Usa caracteres █ com gradiente verde
     */
    public static String progressBar(double percent, int segments) {
        if (segments < 1) { segments = 10; }
        if (segments > 30) { segments = 30; }

        double safePercent = Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round((safePercent / 100.0) * segments);
        int empty = Math.max(0, segments - filled);

        String filledBar = "▰".repeat(filled);
        String emptyBar = "▱".repeat(empty);

        if (filled <= 0) {
            return "<dark_gray>" + emptyBar;
        }
        if (empty <= 0) {
            return GRADIENT_SUCCESS + filledBar + "</gradient>";
        }
        return GRADIENT_SUCCESS + filledBar + "</gradient><dark_gray>" + emptyBar;
    }
    
    /**
     * Barra de progresso colorida personalizada
     */
    public static String progressBar(double percent, int segments, String colorStart, String colorEnd) {
        if (segments < 1) { segments = 10; }
        if (segments > 30) { segments = 30; }

        double safePercent = Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round((safePercent / 100.0) * segments);
        int empty = Math.max(0, segments - filled);

        String filledBar = "▰".repeat(filled);
        String emptyBar = "▱".repeat(empty);

        if (filled <= 0) {
            return "<dark_gray>" + emptyBar;
        }
        if (empty <= 0) {
            return "<gradient:" + colorStart + ":" + colorEnd + ">" + filledBar + "</gradient>";
        }
        return "<gradient:" + colorStart + ":" + colorEnd + ">" + filledBar + "</gradient><dark_gray>" + emptyBar;
    }

    /**
     * Grid slots para inventário de 5 linhas (45 slots)
     * Retorna slots internos (excluindo bordas)
     */
    public static List<Integer> gridSlots5Rows() {
        List<Integer> slots = new ArrayList<>();
        // Linhas 2, 3, 4 (índices 1, 2, 3) - colunas 2-8 (índices 1-7)
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }
    
    /**
     * Grid slots para inventário de 4 linhas (36 slots)
     * Retorna slots internos (excluindo bordas)
     */
    public static List<Integer> gridSlots4Rows() {
        List<Integer> slots = new ArrayList<>();
        // Linhas 2, 3 (índices 1, 2) - colunas 2-8 (índices 1-7)
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    /**
     * Grid slots padrão (retrocompatibilidade)
     */
    public static List<Integer> gridSlots() {
        return gridSlots5Rows();
    }
    
    /**
     * Formata um valor numérico para display
     */
    public static String formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.1f", value);
    }
    
    /**
     * Retorna a cor baseada no valor (positivo = verde, negativo = vermelho)
     */
    public static String getValueColor(double value) {
        return value >= 0 ? "green" : "red";
    }
    
    /**
     * Retorna o sinal do valor
     */
    public static String getValueSign(double value) {
        return value >= 0 ? "+" : "";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // NEXO INTEGRATION
    // ═══════════════════════════════════════════════════════════════════
    
    // Nexo Glyph IDs for Race GUIs
    public static final String GLYPH_RACES_MENU = "midgard_races_menu";
    public static final String GLYPH_RACE_INFO = "midgard_race_info";
    public static final String GLYPH_RACE_CONFIRM = "midgard_race_confirm";
    public static final String GLYPH_RACE_ABILITIES = "midgard_race_abilities";
    
    // Nexo Icon IDs for stats
    public static final String ICON_HEALTH = "icon_health";
    public static final String ICON_MANA = "icon_mana";
    public static final String ICON_STAMINA = "icon_stamina";
    public static final String ICON_STRENGTH = "icon_strength";
    public static final String ICON_AGILITY = "icon_agility";
    public static final String ICON_INTELLIGENCE = "icon_intelligence";
    public static final String ICON_DEFENSE = "icon_defense";
    public static final String ICON_ATTACK = "icon_attack";
    
    /**
     * Creates a menu title with Nexo glyph background for race menus.
     * 
     * @param title The text title
     * @return Formatted title with glyph background
     */
    public static String createRaceMenuTitle(String title) {
        return me.ray.midgard.core.integration.NexoUtils.createMenuTitle(GLYPH_RACES_MENU, title);
    }
    
    /**
     * Creates a menu title with Nexo glyph background for race info.
     * 
     * @param title The text title
     * @return Formatted title with glyph background
     */
    public static String createRaceInfoTitle(String title) {
        return me.ray.midgard.core.integration.NexoUtils.createMenuTitle(GLYPH_RACE_INFO, title);
    }
    
    /**
     * Creates a menu title with Nexo glyph background for confirmations.
     * 
     * @param title The text title
     * @return Formatted title with glyph background
     */
    public static String createConfirmTitle(String title) {
        return me.ray.midgard.core.integration.NexoUtils.createMenuTitle(GLYPH_RACE_CONFIRM, title);
    }
    
    /**
     * Creates a stat line with Nexo icon for lore.
     * 
     * @param iconId The Nexo icon ID
     * @param statName The stat name
     * @param value The stat value
     * @return Formatted stat line
     */
    public static String createStatLine(String iconId, String statName, double value) {
        String icon = me.ray.midgard.core.integration.NexoUtils.getGlyphChar(iconId);
        String color = getValueColor(value);
        String sign = getValueSign(value);
        
        if (icon != null && !icon.isEmpty()) {
            return icon + " <" + color + ">" + sign + formatValue(value) + " <gray>" + statName;
        }
        return "<" + color + ">" + sign + formatValue(value) + " <gray>" + statName;
    }
    
    /**
     * Creates common stat lines for race display.
     * 
     * @param health Health bonus
     * @param mana Mana bonus
     * @param strength Strength bonus
     * @return List of formatted stat lines
     */
    public static List<String> createRaceStatLines(double health, double mana, double strength) {
        List<String> lines = new ArrayList<>();
        lines.add(createStatLine(ICON_HEALTH, me.ray.midgard.modules.races.RacesModule.getInstance().getGuiMessage("admin.stats.health"), health));
        lines.add(createStatLine(ICON_MANA, me.ray.midgard.modules.races.RacesModule.getInstance().getGuiMessage("admin.stats.mana"), mana));
        lines.add(createStatLine(ICON_STRENGTH, me.ray.midgard.modules.races.RacesModule.getInstance().getGuiMessage("admin.stats.strength"), strength));
        return lines;
    }
    
    /**
     * Gets a Nexo custom item or falls back to vanilla material.
     * Useful for race icons.
     * 
     * @param nexoId The Nexo item ID
     * @param fallback Fallback material
     * @return ItemStack
     */
    public static ItemStack getNexoItem(String nexoId, Material fallback) {
        return me.ray.midgard.core.integration.NexoUtils.getGuiItem(nexoId, fallback);
    }
    
    /**
     * Parses text with Nexo glyph placeholders.
     * Supports :icon_id: and %glyph_icon_id% formats.
     * 
     * @param text Text with placeholders
     * @return Parsed text
     */
    public static String parseGlyphs(String text) {
        return me.ray.midgard.core.integration.NexoUtils.parseGlyphs(text);
    }
}
