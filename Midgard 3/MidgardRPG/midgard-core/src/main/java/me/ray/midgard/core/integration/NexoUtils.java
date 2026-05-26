package me.ray.midgard.core.integration;

import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.fonts.FontManager;
import com.nexomc.nexo.glyphs.Glyph;
import com.nexomc.nexo.items.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Utilitários para integração com o plugin Nexo
 * 
 * Suporta:
 * - Itens customizados (NexoItems)
 * - Glifos/Fontes customizadas
 * - Ícones de menu
 * - Parsing de texto com glifos
 * 
 * @author MidgardRPG
 */
public class NexoUtils {

    private static Boolean nexoEnabled = null;

    /**
     * Verifica se o Nexo está habilitado
     */
    public static boolean isNexoEnabled() {
        if (nexoEnabled == null) {
            nexoEnabled = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Nexo");
        }
        return nexoEnabled;
    }

    /**
     * Alias para isNexoEnabled() para melhor legibilidade
     */
    public static boolean isAvailable() {
        return isNexoEnabled();
    }

    /**
     * Invalida o cache (chamar se o plugin for desabilitado/habilitado)
     */
    public static void invalidateCache() {
        nexoEnabled = null;
    }

    // ═══════════════════════════════════════════════════════════════════
    // ITENS CUSTOMIZADOS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Obtém um item customizado do Nexo pelo ID
     * @param id ID do item no Nexo
     * @return ItemStack ou null se não existir/Nexo desabilitado
     */
    @Nullable
    public static ItemStack getCustomItem(String id) {
        if (!isNexoEnabled() || id == null || id.isEmpty()) { return null; }
        try {
            ItemBuilder builder = NexoItems.itemFromId(id);
            return builder != null ? builder.build() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Obtém um item customizado do Nexo ou um fallback
     * @param id ID do item no Nexo
     * @param fallback Material de fallback se o item não existir
     * @return ItemStack (nunca null)
     */
    @NotNull
    public static ItemStack getCustomItemOrFallback(String id, Material fallback) {
        ItemStack item = getCustomItem(id);
        return item != null ? item : new ItemStack(fallback);
    }

    /**
     * Verifica se um ItemStack é um item do Nexo
     */
    public static boolean isCustomItem(ItemStack item) {
        if (!isNexoEnabled() || item == null) { return false; }
        try {
            return NexoItems.idFromItem(item) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Obtém o ID de um item do Nexo
     */
    @Nullable
    public static String getCustomItemId(ItemStack item) {
        if (!isNexoEnabled() || item == null) { return null; }
        try {
            return NexoItems.idFromItem(item);
        } catch (Throwable e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GLIFOS E FONTES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Obtém um glifo pelo ID
     * @param id ID do glifo (ex: "menu_items", "shop_gui_menu")
     * @return Glyph ou null se não existir
     */
    @Nullable
    public static Glyph getGlyph(String id) {
        if (!isNexoEnabled() || id == null || id.isEmpty()) { return null; }
        try {
            FontManager fontManager = NexoPlugin.instance().fontManager();
            return fontManager != null ? fontManager.glyphFromID(id) : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Obtém todos os glifos registrados
     */
    @Nullable
    public static Collection<Glyph> getAllGlyphs() {
        if (!isNexoEnabled()) { return null; }
        try {
            FontManager fontManager = NexoPlugin.instance().fontManager();
            return fontManager != null ? fontManager.glyphs() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Obtém o caractere unicode de um glifo
     * @param id ID do glifo
     * @return Caractere unicode ou string vazia se não existir
     */
    @NotNull
    public static String getGlyphChar(String id) {
        Glyph glyph = getGlyph(id);
        if (glyph == null) { return ""; }
        try {
            return glyph.getFormattedUnicodes();
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * Obtém a tag MiniMessage de um glifo
     * @param id ID do glifo
     * @return Tag no formato <glyph:id> ou string vazia
     */
    @NotNull
    public static String getGlyphTag(String id) {
        Glyph glyph = getGlyph(id);
        if (glyph == null) { return ""; }
        try {
            String tag = glyph.getGlyphTag();
            return tag != null ? tag : "";
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * Obtém o Component Adventure de um glifo
     * @param id ID do glifo
     * @return Component ou null
     */
    @Nullable
    public static Component getGlyphComponent(String id) {
        Glyph glyph = getGlyph(id);
        if (glyph == null) { return null; }
        try {
            return glyph.glyphComponent();
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Verifica se um glifo existe
     */
    public static boolean hasGlyph(String id) {
        return getGlyph(id) != null;
    }

    /**
     * Verifica se o jogador tem permissão para ver um glifo
     */
    public static boolean hasGlyphPermission(Player player, String glyphId) {
        if (!isNexoEnabled() || player == null) { return true; }
        Glyph glyph = getGlyph(glyphId);
        if (glyph == null) { return true; }
        try {
            return glyph.hasPermission(player);
        } catch (Throwable e) {
            return true;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TÍTULOS DE MENU COM GLIFOS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cria um título de menu com glifo de background
     * O glifo fica no início e o texto é centralizado com shifts negativos
     * 
     * @param glyphId ID do glifo do menu (ex: "menu_items")
     * @param title Título do menu (sem o glifo)
     * @return Título formatado com glifo ou apenas o título se não houver glifo
     */
    @NotNull
    public static String createMenuTitle(String glyphId, String title) {
        String glyphChar = getGlyphChar(glyphId);
        if (glyphChar.isEmpty()) {
            return title;
        }
        // O glifo de menu geralmente precisa de shift negativo para posicionar o texto
        // Formato: <glyfo><shift:-8>Título
        return glyphChar + "<shift:-8>" + title;
    }

    /**
     * Cria um título de menu com glifo usando o sistema de shift do Nexo
     * 
     * @param glyphId ID do glifo do menu
     * @param title Título do menu
     * @param shift Quantidade de shift (negativo para esquerda)
     * @return Título formatado
     */
    @NotNull
    public static String createMenuTitle(String glyphId, String title, int shift) {
        String glyphChar = getGlyphChar(glyphId);
        if (glyphChar.isEmpty()) {
            return title;
        }
        return glyphChar + "<shift:" + shift + ">" + title;
    }

    /**
     * Cria um título de menu apenas com o glifo (para menus com overlay completo)
     * 
     * @param glyphId ID do glifo do menu
     * @return Caractere do glifo ou string vazia
     */
    @NotNull
    public static String createMenuTitleGlyphOnly(String glyphId) {
        return getGlyphChar(glyphId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // PARSING DE TEXTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Processa um texto substituindo placeholders de glifos
     * Suporta formatos: :glyph_id:, <glyph:id>, %glyph_id%
     * 
     * @param text Texto com placeholders
     * @return Texto com glifos substituídos
     */
    @NotNull
    public static String parseGlyphs(String text) {
        if (!isNexoEnabled() || text == null || text.isEmpty()) {
            return text != null ? text : "";
        }

        String result = text;

        // Substituir formato :id: (emoji style)
        java.util.regex.Pattern emojiPattern = java.util.regex.Pattern.compile(":([a-zA-Z0-9_-]+):");
        java.util.regex.Matcher emojiMatcher = emojiPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (emojiMatcher.find()) {
            String id = emojiMatcher.group(1);
            String glyphChar = getGlyphChar(id);
            if (!glyphChar.isEmpty()) {
                emojiMatcher.appendReplacement(sb, glyphChar);
            }
        }
        emojiMatcher.appendTail(sb);
        result = sb.toString();

        // Substituir formato %id% (placeholder style)
        java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile("%glyph_([a-zA-Z0-9_-]+)%");
        java.util.regex.Matcher placeholderMatcher = placeholderPattern.matcher(result);
        sb = new StringBuffer();
        while (placeholderMatcher.find()) {
            String id = placeholderMatcher.group(1);
            String glyphChar = getGlyphChar(id);
            if (!glyphChar.isEmpty()) {
                placeholderMatcher.appendReplacement(sb, glyphChar);
            }
        }
        placeholderMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Combina parseGlyphs com o texto
     * Útil para usar em YAMLs
     * 
     * @param text Texto base
     * @param glyphId ID do glifo para prefixar
     * @return Texto com glifo no início
     */
    @NotNull
    public static String prefixWithGlyph(String text, String glyphId) {
        String glyphChar = getGlyphChar(glyphId);
        if (glyphChar.isEmpty()) {
            return text != null ? text : "";
        }
        return glyphChar + " " + (text != null ? text : "");
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMPATIBILIDADE COM ITENS DE GUI
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Obtém um item para GUI - primeiro tenta Nexo, depois fallback
     * 
     * @param nexoId ID do item no Nexo (pode ser null)
     * @param fallbackMaterial Material de fallback
     * @return ItemStack
     */
    @NotNull
    public static ItemStack getGuiItem(@Nullable String nexoId, Material fallbackMaterial) {
        if (nexoId != null && !nexoId.isEmpty()) {
            ItemStack nexoItem = getCustomItem(nexoId);
            if (nexoItem != null) {
                return nexoItem;
            }
        }
        return new ItemStack(fallbackMaterial);
    }

    /**
     * Obtém um item para GUI com quantidade
     */
    @NotNull
    public static ItemStack getGuiItem(@Nullable String nexoId, Material fallbackMaterial, int amount) {
        ItemStack item = getGuiItem(nexoId, fallbackMaterial);
        item.setAmount(amount);
        return item;
    }
}
