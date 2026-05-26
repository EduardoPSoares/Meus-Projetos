package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.multiblock.MultiblockQuality;
import com.midgard.fooddecay.multiblock.ProcessingMultiblock;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * GUI that shows real-time status of a processing multiblock when shift-clicked.
 */
public class MultiblockInspectionGui extends GuiMenu {

    private final ProcessingMultiblock mb;
    private final FoodDecayConfig config;

    public MultiblockInspectionGui(ProcessingMultiblock mb, FoodDecayConfig config) {
        super(sc(config.getGuiTitleMultiblockInspection()
                .replace("{machine}", mb.getType().getDisplayName(mb.getTier()))), 6);
        this.mb = mb;
        this.config = config;
    }

    @Override
    public void setup(Player p) {
        fillBorder(ItemBuilder.placeholder());

        // Machine icon
        setItem(4, new ItemBuilder(mb.getType().getIcon(mb.getTier()))
                .name(sc("&e&l" + mb.getType().getDisplayName(mb.getTier())))
                .lore(sc(config.msg("gui-mb-machine-subtitle")))
                .build());

        if (!mb.hasFood()) {
            setItem(22, new ItemBuilder(Material.LIGHT_GRAY_CONCRETE)
                    .name(sc(config.msg("gui-mb-empty")))
                    .lore(sc(config.msg("gui-mb-empty-1")),
                            "", sc(config.msg("gui-mb-empty-2")))
                    .build());
            showResources();
            return;
        }

        // Food item
        setItem(10, mb.getProcessingFood().clone());

        if (mb.getCompletedTime() > 0) {
            long waitedMs = System.currentTimeMillis() - mb.getCompletedTime();
            int abandonMin = config.getAbandonmentMinutes();
            ItemBuilder sb = new ItemBuilder(Material.LIME_DYE)
                    .name(sc(config.msg("gui-mb-completed")))
                    .lore(sc(config.msg("gui-mb-waiting").replace("{time}", fmtTime(waitedMs))));
            if (abandonMin > 0) {
                long remainMs = (abandonMin * 60_000L) - waitedMs;
                sb.addLore(sc(config.msg("gui-mb-spoils-in").replace("{time}", fmtTime(Math.max(0, remainMs)))));
            }
            setItem(13, sb.build());
            for (int i = 0; i < 7; i++)
                setItem(28 + i, new ItemBuilder(Material.LIME_CONCRETE)
                        .name("&a\u2588").build());
        } else {
            long elapsed = mb.getEffectiveElapsed();
            long total = mb.getProcessingMinutes(config) * 60_000L;
            int pct = (int) Math.min(99, Math.max(0, (elapsed * 100) / total));
            long remaining = Math.max(0, total - elapsed);

            ItemBuilder sb = new ItemBuilder(Material.CLOCK)
                    .name(sc(config.msg("gui-mb-processing").replace("{percent}", String.valueOf(pct))))
                    .lore(sc(config.msg("gui-mb-time-remaining").replace("{time}", fmtTime(remaining))),
                            sc(config.msg("gui-mb-time-elapsed").replace("{time}", fmtTime(elapsed))));
            if (mb.getActiveRecipe() != null) {
                sb.addLore("",
                        sc(config.msg("gui-mb-recipe-label").replace("{name}", mb.getActiveRecipe().getOutputDisplayName())),
                        sc(config.msg("gui-mb-time-label").replace("{time}", mb.getActiveRecipe().getTimeMinutes() + " min")));
            }
            setItem(13, sb.build());

            int bars = 7;
            int filled = pct * bars / 100;
            for (int i = 0; i < bars; i++) {
                Material g = i < filled ? Material.LIME_CONCRETE
                        : Material.RED_CONCRETE;
                setItem(28 + i, new ItemBuilder(g)
                        .name((i < filled ? "&a" : "&c") + "\u2588").build());
            }
        }

        // Output preview
        if (mb.getActiveRecipe() != null) {
            setItem(16, new ItemBuilder(mb.getActiveRecipe().createOutputPreview())
                    .name(sc(config.msg("gui-mb-result")))
                    .lore(sc("&7" + mb.getActiveRecipe().getOutputDisplayName()))
                    .build());
        } else {
            setItem(16, new ItemBuilder(Material.EMERALD)
                    .name(sc(config.msg("gui-mb-result")))
                    .lore(sc(config.msg("gui-mb-result-trait").replace("{trait}", config.getTraitDisplayName(mb.getType().getResultTrait()))))
                    .build());
        }

        showResources();

        // Quality indicator
        int tier = MultiblockQuality.getQualityTier(mb.getQualityBonus(), config);
        String qualLabel = MultiblockQuality.getQualityPrefix(tier, config);
        ItemBuilder qb = new ItemBuilder(tier == 2 ? Material.GOLD_INGOT
                : tier == 1 ? Material.IRON_INGOT : Material.BRICK)
                .name(sc(config.msg("gui-mb-quality-label").replace("{quality}", qualLabel)))
                .lore(sc(config.msg("gui-mb-quality-bonus").replace("{bonus}", String.format("%.0f", mb.getQualityBonus()))),
                        sc(config.msg("gui-mb-events-ok").replace("{count}", String.valueOf(mb.getEventsHandled()))),
                        sc(config.msg("gui-mb-events-missed").replace("{count}", String.valueOf(mb.getEventsMissed()))));
        if (mb.isEventActive()) {
            qb.addLore("", sc(config.msg("gui-mb-event-active")));
        }
        setItem(14, qb.build());
    }

    private void showResources() {
        switch (mb.getType()) {
            case SMOKEHOUSE -> setItem(40, new ItemBuilder(Material.COAL)
                    .name(sc(config.msg("gui-mb-fuel")))
                    .lore(sc(config.msg("gui-mb-fuel-charges").replace("{count}", String.valueOf(mb.getFuel())))).build());
            case SALT_BARREL -> setItem(40, new ItemBuilder(Material.SUGAR)
                    .name(sc(config.msg("gui-mb-salt-label")))
                    .lore(sc(config.msg("gui-mb-salt-units")
                            .replace("{count}", String.valueOf(mb.getSalt()))
                            .replace("{max}", String.valueOf(config.getSaltRequired())))).build());
            case PICKLING_CAULDRON -> setItem(40, new ItemBuilder(Material.CAULDRON)
                    .name(sc(config.msg("gui-mb-ingredients")))
                    .lore(sc(config.msg("gui-mb-water").replace("{status}", mb.hasWater() ? "&a\u2714" : "&c\u2718")),
                            sc(config.msg("gui-mb-vinegar").replace("{status}", mb.hasVinegar() ? "&a\u2714" : "&c\u2718")),
                            sc(config.msg("gui-mb-coal").replace("{status}", mb.getFuel() > 0 ? "&a\u2714" : "&c\u2718"))).build());
            case SEALING_PRESS -> setItem(40, new ItemBuilder(Material.HONEYCOMB)
                    .name(sc(config.msg("gui-mb-wax-label")))
                    .lore(sc(config.msg("gui-mb-wax-seals").replace("{count}", String.valueOf(mb.getWax())))).build());
            default -> {}
        }
    }

    private String fmtTime(long ms) {
        long s = ms / 1000;
        long h = s / 3600; long m = (s % 3600) / 60; s = s % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
