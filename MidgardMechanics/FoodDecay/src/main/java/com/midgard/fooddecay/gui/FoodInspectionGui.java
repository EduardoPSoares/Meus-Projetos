package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.EnvironmentManager;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayManager;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.FoodTrait;
import com.midgard.fooddecay.SeasonHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Interactive food inspection GUI.
 * Shows detailed decay info: freshness bar, time remaining,
 * environment modifiers, and applied preservation traits.
 */
public class FoodInspectionGui extends GuiMenu {

    private final FoodDecayModule module;
    private final FoodDecayManager manager;
    private final FoodDecayConfig config;
    private final EnvironmentManager envManager;

    public FoodInspectionGui(FoodDecayModule module) {
        super(sc(module.getDecayConfig().getGuiTitleInspection()), 6);
        this.module = module;
        this.manager = module.getManager();
        this.config = module.getDecayConfig();
        this.envManager = module.getEnvironmentManager();
    }

    @Override
    public void setup(Player player) {
        ItemStack border = ItemBuilder.placeholder();
        fillBorder(border);

        setItem(4, new ItemBuilder(Material.SPYGLASS)
                .name(sc(config.msg("gui-inspect-title-item")))
                .lore(sc(config.msg("gui-inspect-subtitle-1")),
                        sc(config.msg("gui-inspect-subtitle-2")))
                .build());

        ItemStack food = player.getInventory().getItemInMainHand();

        if (food.getType().isAir() || !food.getType().isEdible()) {
            setupNoFoodView();
            return;
        }

        setItem(13, food.clone());
        boolean stamped = manager.isStamped(food);

        if (!stamped) {
            setItem(22, new ItemBuilder(Material.GRAY_DYE)
                    .name(sc(config.msg("gui-inspect-no-expiry")))
                    .lore(sc(config.msg("gui-inspect-no-expiry-1")),
                            sc(config.msg("gui-inspect-no-expiry-2")))
                    .build());
        } else {
            double freshness = manager.getFreshness(food);
            boolean expired = manager.isExpired(food);
            Set<FoodTrait> traits = manager.getTraits(food);

            setupFreshnessBar(freshness, expired);
            setupTimeInfo(player, food, freshness, expired, traits);
            setupEnvironmentInfo(player);
            setupTraitsInfo(traits);
        }

        setupBottomButtons(player);
    }

    private void setupNoFoodView() {
        setItem(22, new ItemBuilder(Material.BARRIER)
                .name(sc(config.msg("gui-inspect-no-food")))
                .lore(sc(config.msg("gui-inspect-no-food-hint")))
                .build());
        setItem(49, new ItemBuilder(Material.BARRIER)
                .name(sc(config.msg("gui-inspect-close")))
                .build(), e -> e.getWhoClicked().closeInventory());
    }

    private void setupFreshnessBar(double freshness, boolean expired) {
        int totalSlots = 7;
        int filled = expired ? 0 : (int) Math.round(freshness * totalSlots);

        for (int i = 0; i < totalSlots; i++) {
            Material mat;
            String slotName;
            if (i < filled) {
                if (freshness > 0.5) mat = Material.LIME_CONCRETE;
                else if (freshness > 0.25) mat = Material.YELLOW_CONCRETE;
                else mat = Material.ORANGE_CONCRETE;
                slotName = sc(config.msg("gui-inspect-fresh")
                        .replace("{percent}", String.valueOf((int) (freshness * 100))));
            } else {
                mat = Material.RED_CONCRETE;
                slotName = "&8░";
            }
            setItem(19 + i, new ItemBuilder(mat).name(slotName).build());
        }
    }

    private void setupTimeInfo(Player player, ItemStack food,
                               double freshness, boolean expired, Set<FoodTrait> traits) {
        if (expired) {
            setItem(29, new ItemBuilder(Material.WITHER_ROSE)
                    .name(sc(config.msg("gui-inspect-expired-title")))
                    .lore(sc(config.msg("gui-inspect-expired-1")),
                            sc(config.msg("gui-inspect-expired-2")),
                            "",
                            sc(config.msg("gui-inspect-expired-3")))
                    .build());
        } else {
            double currentMult = envManager.getTotalMultiplier(player, traits);
            long remaining = manager.getEstimatedRemainingMillis(food, currentMult);
            String timeStr = manager.formatTime(remaining);

            setItem(29, new ItemBuilder(Material.CLOCK)
                    .name(sc(config.msg("gui-inspect-time-title")))
                    .lore(
                            "",
                            sc(config.msg("gui-inspect-time-time").replace("{time}", timeStr)),
                            sc(config.msg("gui-inspect-time-fresh").replace("{percent}", String.valueOf((int) (freshness * 100)))),
                            sc(config.msg("gui-inspect-time-speed").replace("{multiplier}", String.format("%.2fx", currentMult))),
                            "",
                            sc(config.msg("gui-inspect-time-note"))
                    )
                    .build());
        }
    }

    private void setupEnvironmentInfo(Player player) {
        SeasonHook seasonHook = envManager.getSeasonHook();
        String tempStr = "—";
        String seasonStr = "—";
        if (seasonHook.isAvailable()) {
            Integer temp = seasonHook.getTemperature(player);
            if (temp != null) tempStr = temp + "°C";
            String season = seasonHook.getSeason(player.getWorld());
            if (season != null) seasonStr = season;
        }
        double envMult = envManager.getEnvironmentMultiplier(player);

        setItem(31, new ItemBuilder(Material.CAMPFIRE)
                .name(sc(config.msg("gui-inspect-env-title")))
                .lore(
                        "",
                        sc(config.msg("gui-inspect-env-temp").replace("{temp}", tempStr)),
                        sc(config.msg("gui-inspect-env-season").replace("{season}", seasonStr)),
                        sc(config.msg("gui-inspect-env-mult").replace("{multiplier}", String.format("%.2fx", envMult))),
                        "",
                        sc(config.msg("gui-inspect-env-note-1")),
                        sc(config.msg("gui-inspect-env-note-2"))
                )
                .build());
    }

    private void setupTraitsInfo(Set<FoodTrait> traits) {
        if (!traits.isEmpty()) {
            List<String> traitLore = new ArrayList<>();
            traitLore.add("");
            for (FoodTrait trait : traits) {
                double mult = config.getTraitMultiplier(trait);
                int pct = (int) ((1.0 - mult) * 100);
                traitLore.add(config.getTraitDisplayName(trait) + " &8(&a-" + pct + "%&8)");
            }
            traitLore.add("");
            traitLore.add(sc(config.msg("gui-inspect-traits-note-1")));
            traitLore.add(sc(config.msg("gui-inspect-traits-note-2")));

            setItem(33, new ItemBuilder(Material.SHIELD)
                    .name(sc(config.msg("gui-inspect-traits-title")))
                    .lore(traitLore.toArray(new String[0]))
                    .build());
        } else {
            setItem(33, new ItemBuilder(Material.FLOWER_BANNER_PATTERN)
                    .name(sc(config.msg("gui-inspect-no-traits")))
                    .lore(sc(config.msg("gui-inspect-no-traits-1")),
                            "",
                            sc(config.msg("gui-inspect-no-traits-2")),
                            sc(config.msg("gui-inspect-no-traits-3")),
                            sc(config.msg("gui-inspect-no-traits-4")))
                    .build());
        }
    }

    private void setupBottomButtons(Player player) {
        setItem(47, new ItemBuilder(Material.BRICKS)
                .name(sc(config.msg("gui-inspect-multiblock-title")))
                .lore(sc(config.msg("gui-inspect-multiblock-1")),
                        sc(config.msg("gui-inspect-multiblock-2")),
                        "",
                        sc(config.msg("gui-inspect-multiblock-3")),
                        sc(config.msg("gui-inspect-multiblock-4")))
                .build());

        setItem(49, new ItemBuilder(Material.BARRIER)
                .name(sc(config.msg("gui-inspect-close")))
                .build(), e -> e.getWhoClicked().closeInventory());

        setItem(51, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(sc("&e&lLivro de Receitas"))
                .lore(sc("&7Abra categorias separadas de preparo"),
                        sc("&7para ver maquinas, bebidas e receitas."))
                .build(), e -> {
            if (e.getWhoClicked() instanceof Player p) {
                new CookbookGui(module).open(p);
            }
        });
    }
}
