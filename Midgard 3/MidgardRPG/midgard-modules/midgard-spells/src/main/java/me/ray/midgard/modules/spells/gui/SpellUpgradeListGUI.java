package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menu de upgrade de spells — Cards Minimalistas
 *
 * ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
 * │   │   │   │   │ 📈│   │   │   │   │  Row 0  Header
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │   │  Row 1  Grid limpo
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │ 8 │ 9 │10 │11 │12 │13 │14 │   │  Row 2  7 spells/linha
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │15 │16 │17 │18 │19 │20 │21 │   │  Row 3  Com XP bars
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │   │   │   │   │   │  Row 4  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │ ← │   │   │   │ ⌂ │   │   │   │ → │  Row 5  Nav + Home
 * └───┴───┴───┴───┴───┴───┴───┴───┴───┘
 */
public class SpellUpgradeListGUI extends PaginatedGui<Spell> {

    private final SpellsModule module;
    private final SpellProfile profile;
    private static final DecimalFormat DF = new DecimalFormat("0.#");
    private static final DecimalFormat DF_INT = new DecimalFormat("#,##0");

    // Layout - Grid limpo 7x3
    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int HOME_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,  // Row 1
            19, 20, 21, 22, 23, 24, 25,  // Row 2
            28, 29, 30, 31, 32, 33, 34   // Row 3
    };

    public SpellUpgradeListGUI(Player player, SpellsModule module) {
        super(player, msg(module, "title"), buildSpellList(player, module));
        this.module = module;
        this.profile = module.getSpellManager().getProfile(player);
        if (this.profile == null) {
            MessageUtils.sendError(player, module.getMessage("errors.profile_not_loaded"));
            me.ray.midgard.core.utils.Task.syncLater(player, player::closeInventory, 1L);
        }
    }

    private static String msg(SpellsModule module, String key) {
        return module.getMessage("upgrade_gui." + key);
    }

    /**
     * Builds a sorted list of spells the player has unlocked.
     * Order: PASSIVE first, then COMMON, then ULTIMATE, sorted by level desc.
     */
    private static List<Spell> buildSpellList(Player player, SpellsModule module) {
        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) { return new ArrayList<>(); }

        List<Spell> spells = new ArrayList<>();
        for (String spellId : profile.getUnlockedSpells()) {
            Spell spell = module.getSpellManager().getSpell(spellId);
            if (spell != null) {
                spells.add(spell);
            }
        }

        // Sort: type order (PASSIVE=0, COMMON=1, ULTIMATE=2), then by level descending
        spells.sort(Comparator
                .<Spell, Integer>comparing(s -> s.getSpellType().ordinal())
                .thenComparing((s1, s2) -> {
                    int l1 = profile.getSpellLevel(s1.getId());
                    int l2 = profile.getSpellLevel(s2.getId());
                    return Integer.compare(l2, l1); // descending
                }));

        return spells;
    }

    @Override
    public ItemStack createItem(Spell spell) {
        String spellId = spell.getId();
        int level = profile.getSpellLevel(spellId);
        int maxLevel = spell.getMaxLevel();
        double xp = profile.getSpellXP(spellId);
        boolean mastered = profile.isMastered(spellId);
        boolean isMaxLevel = level >= maxLevel;

        int xpNeeded = isMaxLevel ? 1 : module.getSpellManager().getXPManager().getXPForLevel(level + 1);
        double percent = isMaxLevel ? 100.0 : Math.min(100.0, (xp / xpNeeded) * 100.0);

        // Build lore minimalista
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Nível
        lore.add(MessageUtils.parse(msg("item.level")
                .replace("%level%", String.valueOf(level))
                .replace("%max%", String.valueOf(maxLevel))));

        // XP (apenas se não for max level)
        if (!isMaxLevel) {
            lore.add(MessageUtils.parse(msg("item.xp")
                    .replace("%current%", DF_INT.format(xp))
                    .replace("%needed%", DF_INT.format(xpNeeded))));

            // XP Bar compacto
            String bar = buildProgressBar(xp, xpNeeded, 10);
            lore.add(MessageUtils.parse(msg("item.bar")
                    .replace("%bar%", bar)
                    .replace("%percent%", DF.format(percent))));
        }

        lore.add(Component.empty());

        // Status badge
        if (mastered) {
            lore.add(MessageUtils.parse(msg("item.mastered")));
        } else if (isMaxLevel) {
            lore.add(MessageUtils.parse(msg("item.max_level")));
        } else {
            lore.add(MessageUtils.parse(msg("item.click_to_view")));
        }

        // Build icon
        return buildSpellIcon(spell, lore, mastered || spell.isUltimate());
    }

    @Override
    public void initializeItems() {
        // Grid limpo - sem background

        // Header
        inventory.setItem(HEADER_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(MessageUtils.parse(msg("header.name")))
                .lore(parse(List.of("", msg("header.lore"))))
                .glow()
                .build());

        // Renderizar spells no grid
        int startIndex = page * GRID_SLOTS.length;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < items.size()) {
                inventory.setItem(GRID_SLOTS[i], createItem(items.get(itemIndex)));
            }
        }

        // Navegação
        if (page > 0) {
            inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.previous")))
                    .build());
        }

        if ((page + 1) * GRID_SLOTS.length < items.size()) {
            inventory.setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.next")))
                    .build());
        }

        // Home button
        inventory.setItem(HOME_SLOT, new ItemBuilder(Material.COMPASS)
                .name(MessageUtils.parse(msg("nav.home")))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Navegação
        if (slot == BACK_SLOT && page > 0) {
            page--;
            initializeItems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            return;
        }

        if (slot == NEXT_SLOT && (page + 1) * GRID_SLOTS.length < items.size()) {
            page++;
            initializeItems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }

        // Home
        if (slot == HOME_SLOT) {
            new MainSpellGUI(player, module).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Click em spell
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (slot == GRID_SLOTS[i]) {
                int itemIndex = page * GRID_SLOTS.length + i;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    Spell spell = items.get(itemIndex);
                    new SpellUpgradeDetailGUI(player, module, spell).open();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                }
                return;
            }
        }
    }

    // ───────── Util ─────────

    private String msg(String key) {
        return module.getMessage("upgrade_gui." + key);
    }

    private ItemStack buildSpellIcon(Spell spell, List<Component> lore, boolean glow) {
        String mat = spell.getIconMaterial(false);
        int md = spell.getIconModelData(false);
        Material fallback = switch (spell.getSpellType()) {
            case PASSIVE  -> Material.TOTEM_OF_UNDYING;
            case COMMON   -> Material.ENCHANTED_BOOK;
            case ULTIMATE -> Material.NETHER_STAR;
        };

        ItemBuilder b = (mat != null && !mat.isEmpty()) ? ItemBuilder.smart(mat) : new ItemBuilder(fallback);
        if (md > 0) { b.customModelData(md); }

        String color = switch (spell.getSpellType()) {
            case PASSIVE  -> module.getMessage("main_gui.spell_color.passive");
            case COMMON   -> module.getMessage("main_gui.spell_color.common");
            case ULTIMATE -> module.getMessage("main_gui.spell_color.ultimate");
        };

        int level = profile.getSpellLevel(spell.getId());
        b.name(MessageUtils.parse(color + spell.getDisplayName() + module.getMessage("upgrade_gui.level_suffix").replace("%level%", String.valueOf(level))))
         .lore(lore);
        if (glow) { b.glow(); }
        return b.build();
    }

    private String buildProgressBar(double current, double max, int totalBars) {
        double percent = Math.min(1.0, Math.max(0.0, current / max));
        int filled = (int) (totalBars * percent);
        int empty = totalBars - filled;

        String filledColor = "<green>";
        String emptyColor = "<dark_gray>";
        String barChar = "█";

        return filledColor + barChar.repeat(Math.max(0, filled))
                + emptyColor + barChar.repeat(Math.max(0, empty));
    }

    private List<Component> parse(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) { out.add(MessageUtils.parse(l)); }
        return out;
    }
}
