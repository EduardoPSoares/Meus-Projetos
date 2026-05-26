package me.ray.midgard.modules.professions.gui;

import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Tema visual e utilitários de renderização para o menu de profissões.
 * Centraliza criação de ítens decorativos, cores e barras de progresso.
 */
public final class ProfessionGuiTheme {

    // Cores/gradientes
    public static final String GRADIENT_GOLD = "<gradient:#facc15:#f59e0b>";
    public static final String GRADIENT_GREEN = "<gradient:#22c55e:#16a34a>";
    public static final String GRADIENT_RED = "<gradient:#ef4444:#dc2626>";
    public static final String GRADIENT_CYAN = "<gradient:#06b6d4:#3b82f6>";
    public static final String GRADIENT_GRAY = "<gradient:#9ca3af:#6b7280>";

    // Materiais para estados de nível
    public static final Material LEVEL_LOCKED = Material.RED_STAINED_GLASS_PANE;
    public static final Material LEVEL_IN_PROGRESS = Material.YELLOW_STAINED_GLASS_PANE;
    public static final Material LEVEL_COMPLETED = Material.LIME_STAINED_GLASS_PANE;

    // Decoração
    public static final Material FILLER = Material.BLACK_STAINED_GLASS_PANE;

    private ProfessionGuiTheme() {}

    /**
     * Cria um painel de vidro preto para preencher slots vazios.
     */
    public static ItemStack createFillerPane() {
        return new ItemBuilder(FILLER)
                .setName("<dark_gray> ")
                .build();
    }

    /**
     * Preenche slots específicos com o painel de vidro.
     */
    public static void fillSlots(Inventory inv, int... slots) {
        ItemStack filler = createFillerPane();
        int size = inv.getSize();
        for (int slot : slots) {
            if (slot >= 0 && slot < size && inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }
    }

    /**
     * Preenche todos os slots vazios do inventário com vidro preto.
     */
    public static void fillEmpty(Inventory inv) {
        ItemStack filler = createFillerPane();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    /**
     * Barra de progresso estilo premium.
     */
    public static String progressBar(double percent, int segments) {
        if (segments < 1) { segments = 10; }
        double safePercent = Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round((safePercent / 100.0) * segments);
        int empty = Math.max(0, segments - filled);

        String filledBar = "▰".repeat(filled);
        String emptyBar = "▱".repeat(empty);

        if (filled <= 0) {
            return "<dark_gray>" + emptyBar;
        }
        if (empty <= 0) {
            return GRADIENT_GREEN + filledBar + "</gradient>";
        }
        return GRADIENT_GREEN + filledBar + "</gradient><dark_gray>" + emptyBar;
    }

    /**
     * Barra de progresso com cores customizadas (gradiente).
     */
    public static String progressBar(double percent, int segments, String gradient) {
        if (segments < 1) { segments = 10; }
        double safePercent = Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round((safePercent / 100.0) * segments);
        int empty = Math.max(0, segments - filled);

        String filledBar = "▰".repeat(filled);
        String emptyBar = "▱".repeat(empty);

        if (filled <= 0) {
            return "<dark_gray>" + emptyBar;
        }
        if (empty <= 0) {
            return gradient + filledBar + "</gradient>";
        }
        return gradient + filledBar + "</gradient><dark_gray>" + emptyBar;
    }

    /**
     * Formata um número grande de forma legível (1234 → "1,234").
     */
    public static String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%,d", (long) value);
        }
        return String.format("%,.1f", value);
    }

    /**
     * Retorna o material de vidro correspondente ao estado de um nível.
     */
    public static Material getLevelMaterial(int level, int playerLevel) {
        if (level <= playerLevel) {
            return LEVEL_COMPLETED;
        } else if (level == playerLevel + 1) {
            return LEVEL_IN_PROGRESS;
        } else {
            return LEVEL_LOCKED;
        }
    }

    /**
     * Retorna o texto de status formatado.
     */
    public static String getLevelStatus(int level, int playerLevel) {
        if (level <= playerLevel) {
            return ProfessionsModule.getInstance().getMessage("professions.menu.level.status_completed");
        } else if (level == playerLevel + 1) {
            return ProfessionsModule.getInstance().getMessage("professions.menu.level.status_in_progress");
        } else {
            return ProfessionsModule.getInstance().getMessage("professions.menu.level.status_locked");
        }
    }
}
