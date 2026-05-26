package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GUI de Progressão de Linhagem
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 4 linhas (36 slots) - XP e Progressão
 */
public class RaceProgressGui extends BaseGui {

    private final RacesModule module;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    // Layout 4 linhas (36 slots)
    private static final int SLOT_PROFILE = 13;
    private static final int SLOT_XP = 20;
    private static final int SLOT_UNLOCKS = 24;
    private static final int SLOT_BACK = 27;
    private static final int SLOT_CLOSE = 35;

    public RaceProgressGui(Player player, BaseGui parentGui) {
        super(player, 4, getTitle());
        this.module = RacesModule.getInstance();
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("progression.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("progression." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillBottomRow(inventory, pane);

        if (raceData == null || !raceData.hasRace()) {
            inventory.setItem(13, new ItemBuilder(Material.BARRIER)
                    .setName(gui("no_race.name"))
                    .setLoreMultiline(gui("no_race.lore"))
                    .build());

            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
            inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                    .setName(module.getGuiMessage("general.close"))
                    .build());
            return;
        }

        // Itens de progressão
        inventory.setItem(SLOT_PROFILE, createProfileItem());
        inventory.setItem(SLOT_XP, createXpItem());
        inventory.setItem(SLOT_UNLOCKS, createUnlocksItem());

        // Navegação
        if (parentGui != null) {
            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
        }

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    private ItemStack createProfileItem() {
        var race = module.getRaceManager().getRace(raceData.getRaceId());
        String raceName = (race != null) ? race.getDisplayName() : raceData.getRaceId();

        int level = raceData.getLevel();
        double xp = raceData.getExperience();
        double required = module.getLevelManager().getRequiredExperience(level);
        double percent = (required > 0) ? (xp / required) * 100 : 100;
        String progressBar = RaceGuiTheme.progressBar(percent, 14);

        return new ItemBuilder(race != null ? race.getIcon() : new ItemStack(Material.PLAYER_HEAD))
                .setName(gui("profile.name").replace("%race%", raceName))
                .setLoreMultiline(gui("profile.lore")
                        .replace("%level%", String.valueOf(level))
                        .replace("%xp%", String.format("%.0f", xp))
                        .replace("%required%", String.format("%.0f", required))
                        .replace("%percent%", String.format("%.1f", percent))
                        .replace("%progressbar%", progressBar))
                .glow()
                .build();
    }

    private ItemStack createXpItem() {
        int level = raceData.getLevel();
        double xp = raceData.getExperience();
        double required = module.getLevelManager().getRequiredExperience(level);
        double missing = Math.max(0, required - xp);
        String percent = (required > 0) ? String.format("%.1f", (xp / required) * 100) : "100.0";

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(gui("xp.name"))
                .setLoreMultiline(gui("xp.lore")
                        .replace("%current%", String.valueOf(level))
                        .replace("%next%", String.valueOf(level + 1))
                        .replace("%xp%", String.format("%.0f", xp))
                        .replace("%required%", String.format("%.0f", required))
                        .replace("%missing%", String.format("%.0f", missing))
                        .replace("%percent%", percent))
                .glow()
                .build();
    }

    private ItemStack createUnlocksItem() {
        var race = module.getRaceManager().getRace(raceData.getRaceId());

        if (race == null || race.getTraits() == null || race.getTraits().isEmpty()) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .setName(gui("unlocks.name"))
                    .setLoreMultiline(gui("unlocks.none"))
                    .build();
        }

        int nextLevel = raceData.getLevel() + 1;
        var nextUnlocks = race.getTraits().stream()
                .filter(t -> t.getMinLevel() == nextLevel)
                .toList();

        if (nextUnlocks.isEmpty()) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .setName(gui("unlocks.name"))
                    .setLoreMultiline(gui("unlocks.none"))
                    .build();
        }

        StringBuilder lore = new StringBuilder("\n");
        lore.append(gui("unlocks.next_header"));
        for (var trait : nextUnlocks) {
            lore.append(String.format("<dark_gray>▸ <gradient:#a855f7:#ec4899>%s</gradient>\n", trait.getId()));
        }
        lore.append("\n").append(gui("unlocks.keep_evolving"));

        return new ItemBuilder(Material.NETHER_STAR)
                .setName(gui("unlocks.name"))
                .setLoreMultiline(lore.toString())
                .glow()
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            switch (slot) {
                case SLOT_BACK -> {
                    if (parentGui != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        parentGui.open();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceProgressGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
