package me.ray.midgard.modules.professions.gui;

import java.util.Collections;
import java.util.List;

/**
 * Mapeamento de slots de progressão no menu de profissão.
 * Define quais slots do inventário 54 correspondem a níveis de progressão em cada página.
 *
 * Layout do menu (54 slots = 6 linhas x 9 colunas):
 * - Linha 0 (slots 0-8): Header (ícone, ranking, xp sources, extra)
 * - Linhas 1-4 (slots 9-44): Área de progressão (serpentina contínua)
 * - Linha 5 (slots 45-53): Navegação
 *
 * Padrão serpentina vertical com conexões:
 * 5 colunas × 4 linhas + 4 conectores = 24 níveis por página.
 * Colunas ímpares sobem (bottom→top), pares descem (top→bottom).
 * Conectores entre colunas SÃO níveis (não apenas visuais).
 *
 * Visualização (números = ordem dos níveis na página):
 *   Col0↓     Col1↑        Col2↓     Col3↑        Col4↓
 *   [ 1] [ ] [ 9] [10]    [11] [ ] [19] [20]    [21]
 *   [ 2] [ ] [ 8] [ ]     [12] [ ] [18] [ ]     [22]
 *   [ 3] [ ] [ 7] [ ]     [13] [ ] [17] [ ]     [23]
 *   [ 4] [ 5] [ 6] [ ]    [14] [15] [16] [ ]    [24]
 *
 * Página 1: níveis 1-24, Página 2: 25-48, Página 3: 49-72, Página 4: 73-96, Página 5: 97-100
 */
public final class ProfessionMenuLayout {

    /** Total de páginas no menu de progressão. */
    public static final int TOTAL_PAGES = 5;

    /** Níveis por página. */
    public static final int LEVELS_PER_PAGE = 24;

    /** Max level suportado. */
    public static final int MAX_DISPLAY_LEVEL = 100;

    // --- Slots fixos (header) ---
    public static final int SLOT_PROFESSION_ICON = 0;
    public static final int SLOT_RANKING = 1;
    public static final int SLOT_XP_SOURCES = 2;
    public static final int SLOT_EXTRA_BUTTON = 3;

    // --- Slots fixos (footer) ---
    public static final int SLOT_CLOSE = 45;
    public static final int SLOT_PREV_PAGE = 52;
    public static final int SLOT_NEXT_PAGE = 53;

    /**
     * Slots de progressão — 24 por página, em padrão serpentina contínuo.
     * Colunas pares descem, ímpares sobem. Conectores são níveis na curva.
     *
     * Caminho da serpentina:
     *   Col0↓ → conn → Col1↑ → conn → Col2↓ → conn → Col3↑ → conn → Col4↓
     *
     *   [ 1] [ ] [ 9] [10] [11] [ ] [19] [20] [21]   row 1
     *   [ 2] [ ] [ 8] [ ] [12] [ ] [18] [ ] [22]     row 2
     *   [ 3] [ ] [ 7] [ ] [13] [ ] [17] [ ] [23]     row 3
     *   [ 4] [ 5] [ 6] [ ] [14] [15] [16] [ ] [24]   row 4
     */
    private static final int[] PAGE_SLOTS = {
            // Col 0 ↓ (níveis 1-4)
            9, 18, 27, 36,
            // Conector row4 col1 (nível 5)
            37,
            // Col 2 ↑ (níveis 6-9)
            38, 29, 20, 11,
            // Conector row1 col3 (nível 10)
            12,
            // Col 4 ↓ (níveis 11-14)
            13, 22, 31, 40,
            // Conector row4 col5 (nível 15)
            41,
            // Col 6 ↑ (níveis 16-19)
            42, 33, 24, 15,
            // Conector row1 col7 (nível 20)
            16,
            // Col 8 ↓ (níveis 21-24)
            17, 26, 35, 44
    };

    private static final List<int[]> PAGE_SLOT_MAP = List.of(
            PAGE_SLOTS,  // Página 1: níveis 1-24
            PAGE_SLOTS,  // Página 2: níveis 25-48
            PAGE_SLOTS,  // Página 3: níveis 49-72
            PAGE_SLOTS,  // Página 4: níveis 73-96
            PAGE_SLOTS   // Página 5: níveis 97-100 (parcial)
    );

    private ProfessionMenuLayout() {}

    /**
     * Retorna os slots de progressão para uma página (0-indexed).
     */
    public static int[] getSlotsForPage(int page) {
        if (page < 0 || page >= PAGE_SLOT_MAP.size()) {
            return new int[0];
        }
        return PAGE_SLOT_MAP.get(page).clone();
    }

    /**
     * Retorna o nível correspondente a um slot numa dada página.
     * O primeiro slot da página 0 é nível 1.
     *
     * @return nível (1-based) ou -1 se o slot não é de progressão
     */
    public static int getLevelForSlot(int page, int slot) {
        int[] slots = getSlotsForPage(page);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return (page * LEVELS_PER_PAGE) + i + 1;
            }
        }
        return -1;
    }

    /**
     * Retorna quantos níveis são exibidos em uma página.
     */
    public static int getLevelCountForPage(int page, int maxLevel) {
        int startLevel = page * LEVELS_PER_PAGE + 1;
        if (startLevel > maxLevel) {
            return 0;
        }
        int[] slots = getSlotsForPage(page);
        int remaining = maxLevel - startLevel + 1;
        return Math.min(slots.length, remaining);
    }

    /**
     * Calcula quantas páginas são necessárias para exibir N níveis.
     */
    public static int calculateTotalPages(int maxLevel) {
        if (maxLevel <= 0) { return 1; }
        // Páginas cheias (24 por página) + sobra
        int fullPages = maxLevel / LEVELS_PER_PAGE;
        int remainder = maxLevel % LEVELS_PER_PAGE;
        return remainder > 0 ? fullPages + 1 : fullPages;
    }

    /**
     * Verifica se um slot é de progressão na página dada.
     */
    public static boolean isProgressionSlot(int page, int slot) {
        int[] slots = getSlotsForPage(page);
        for (int s : slots) {
            if (s == slot) { return true; }
        }
        return false;
    }

    /**
     * Verifica se uma página tem botão de próxima página.
     */
    public static boolean hasNextPage(int page, int maxLevel) {
        return (page + 1) * LEVELS_PER_PAGE < maxLevel;
    }

    /**
     * Verifica se uma página tem botão de página anterior.
     */
    public static boolean hasPrevPage(int page) {
        return page > 0;
    }
}
