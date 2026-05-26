package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.LoreFormatter;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.ItemModule;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Aplica a qualidade da forja ao item forjado.
 * - Multiplica todos os stats numéricos pelo multiplicador da qualidade
 * - Adiciona estrelas visuais ao nome (estilo Wynncraft)
 * - Adiciona linha de qualidade no lore
 * - Grava dados de qualidade no PDC para referência futura
 */
public class ForgeQualityApplier {

    // Estrelas por tier de qualidade (estilo Wynncraft)
    private static final Map<QualityTier, String> STAR_DISPLAY = new LinkedHashMap<>();

    static {
        STAR_DISPLAY.put(QualityTier.DEFECTIVE,   "<dark_gray>[<dark_red>✦<dark_gray>✦✦✦✦✦✦]");
        STAR_DISPLAY.put(QualityTier.INFERIOR,     "<dark_gray>[<gray>✦✦<dark_gray>✦✦✦✦✦]");
        STAR_DISPLAY.put(QualityTier.COMMON,       "<dark_gray>[<white>✦✦✦<dark_gray>✦✦✦✦]");
        STAR_DISPLAY.put(QualityTier.SUPERIOR,     "<dark_gray>[<green>✦✦✦✦<dark_gray>✦✦✦]");
        STAR_DISPLAY.put(QualityTier.EXCEPTIONAL,  "<dark_gray>[<blue>✦✦✦✦✦<dark_gray>✦✦]");
        STAR_DISPLAY.put(QualityTier.MASTERPIECE,  "<dark_gray>[<gold>✦✦✦✦✦✦<dark_gray>✦]");
        STAR_DISPLAY.put(QualityTier.LEGENDARY,    "<dark_gray>[<light_purple>✦✦✦✦✦✦✦<dark_gray>]");
    }

    // Stats que NÃO devem ser multiplicados (são flags, não valores numéricos)
    private static final Set<ItemStat> EXCLUDED_STATS = Set.of(
            ItemStat.TWO_HANDED,
            ItemStat.UNBREAKABLE,
            ItemStat.REQUIRED_LEVEL
    );

    /**
     * Aplica a qualidade da forja ao ItemStack.
     *
     * @param item          O ItemStack construído pelo MidgardItemBuilder
     * @param qualityTier   O tier de qualidade resultante da forja
     * @param qualityScore  A pontuação exata de qualidade (0.0 - 1.0)
     * @return O ItemStack modificado com qualidade aplicada
     */
    public static ItemStack apply(ItemStack item, QualityTier qualityTier, double qualityScore) {
        if (item == null || !item.hasItemMeta()) { return item; }

        ItemMeta meta = item.getItemMeta();
        double multiplier = qualityTier.getStatMultiplier();

        // 1. Multiplicar stats no PDC
        applyStatMultiplier(meta, multiplier);

        // 2. Gravar dados de qualidade no PDC
        setQualityData(meta, qualityTier, qualityScore);

        // 3. Modificar o nome com estrelas
        applyStarName(meta, qualityTier);

        // Salvar o meta antes de recalcular o lore
        item.setItemMeta(meta);

        // 4. Recalcular o lore completo com os stats já multiplicados
        rebuildLore(item, qualityTier, qualityScore);

        return item;
    }

    /**
     * Multiplica todos os stats numéricos armazenados no PDC pelo multiplicador de qualidade.
     */
    private static void applyStatMultiplier(ItemMeta meta, double multiplier) {
        if (multiplier == 1.0) { return; } // COMMON não altera

        for (ItemStat stat : ItemStat.values()) {
            if (EXCLUDED_STATS.contains(stat)) { continue; }
            if (!ItemPDC.hasStat(meta, stat)) { continue; }

            double original = ItemPDC.getStat(meta, stat);
            if (original == 0) { continue; }

            double modified = Math.round(original * multiplier * 100.0) / 100.0;
            ItemPDC.setStat(meta, stat, modified);
        }
    }

    /**
     * Grava os dados de qualidade no PDC do item.
     */
    private static void setQualityData(ItemMeta meta, QualityTier tier, double score) {
        ItemPDC.setString(meta, "forge_quality_tier", tier.name());
        ItemPDC.setDouble(meta, "forge_quality_score", score);
    }

    /**
     * Adiciona as estrelas de qualidade ao nome do item.
     * Formato: "Nome do Item <estrelas>"
     */
    private static void applyStarName(ItemMeta meta, QualityTier tier) {
        Component currentName = meta.displayName();
        if (currentName == null) { return; }

        String stars = STAR_DISPLAY.getOrDefault(tier, "");
        // Recria o nome: nome original + espaço + estrelas
        Component starComponent = MessageUtils.parse(" " + stars);
        meta.displayName(currentName.append(starComponent));
    }

    /**
     * Reconstrói o lore do item usando o LoreFormatter, lendo os stats já modificados
     * do PDC, e adiciona a linha de qualidade no final.
     */
    private static void rebuildLore(ItemStack item, QualityTier qualityTier, double qualityScore) {
        // Reconstrói o lore padrão do item usando o sistema existente
        List<Component> newLore = LoreFormatter.formatLore(item);

        // Adiciona separador e informações de qualidade
        newLore.add(Component.empty());
        newLore.add(MessageUtils.parse(
                STAR_DISPLAY.getOrDefault(qualityTier, "") + " " + qualityTier.getFormattedName()
        ));
        newLore.add(MessageUtils.parse(
                "<dark_gray>Forjado — Qualidade: <white>" + String.format("%.0f%%", qualityScore * 100)
        ));

        ItemMeta meta = item.getItemMeta();
        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Lê o tier de qualidade de um item já forjado (do PDC).
     * Retorna null se o item não foi forjado.
     */
    public static QualityTier getQualityTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return null; }
        String tierName = ItemPDC.getString(item.getItemMeta(), "forge_quality_tier");
        if (tierName == null) { return null; }
        try {
            return QualityTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Lê a pontuação de qualidade de um item já forjado (do PDC).
     * Retorna -1 se o item não foi forjado.
     */
    public static double getQualityScore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return -1; }
        double score = ItemPDC.getDouble(item.getItemMeta(), "forge_quality_score");
        return score > 0 ? score : -1;
    }

    /**
     * Retorna a string visual de estrelas para um tier.
     */
    public static String getStarsDisplay(QualityTier tier) {
        return STAR_DISPLAY.getOrDefault(tier, "");
    }
}
