package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.fuel.FuelManager;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Main forge GUI — the hub shown when a player right-clicks a smithing table
 * on a forge structure. Allows access to recipe book, forging, info, etc.
 */
public class ForgeMainGui extends BaseGui {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.forge_main." + key); }

    private static final int SLOT_FORGE_INFO = 4;
    private static final int SLOT_FUEL_INFO = 10;
    private static final int SLOT_RECIPE_BOOK = 20;
    private static final int SLOT_START_FORGING = 22;
    private static final int SLOT_REPAIR = 24;
    private static final int SLOT_PROFESSION_INFO = 38;
    private static final int SLOT_UPGRADES = 40;
    private static final int SLOT_SETTINGS = 42;
    private static final int SLOT_CLOSE = 49;

    private final ForgeStructure forge;
    private final int playerLevel;
    private int fuelBurnTime;
    private int fuelMaxBurnTime;
    private String fuelName;

    private Consumer<Player> onOpenRecipeBook;
    private BiConsumer<Player, ForgeStructure> onStartForging;
    private Consumer<Player> onOpenProfessionInfo;
    private Consumer<Player> onOpenForgeMainGui;

    public ForgeMainGui(Player player, ForgeStructure forge, int playerLevel) {
        super(player, 6, msg("title") + forge.getTier().getDisplayName());
        this.forge = forge;
        this.playerLevel = playerLevel;
    }

    public void setFuelInfo(FuelManager fuelManager) {
        FuelManager.FuelDeposit deposit = fuelManager.getDeposit(forge.getForgeId());
        this.fuelBurnTime = deposit != null ? deposit.getRemainingBurnTime() : 0;
        this.fuelMaxBurnTime = fuelManager.getMaxBurnTime();
        this.fuelName = deposit != null ? deposit.getFuel().getDisplayName() : msg("fuel_none");
    }

    @Override
    public void initializeItems() {
        // Fill border
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ForgeTier tier = forge.getTier();

        // Forge info (center top)
        inventory.setItem(SLOT_FORGE_INFO, new ItemBuilder(Material.SMITHING_TABLE)
                .setName("<gold><bold>" + tier.getDisplayName())
                .addLore(msg("forge_level") + tier.getLevel())
                .addLore(msg("items_forged") + forge.getTotalItemsForged())
                .addLore(msg("owner") + org.bukkit.Bukkit.getOfflinePlayer(forge.getOwnerUuid()).getName())
                .addLore("")
                .addLore(msg("dimension") + tier.getWidth() + "x" + tier.getHeight() + "x" + tier.getDepth())
                .build());

        // Fuel info (left side)
        int fuelPercent = fuelMaxBurnTime > 0 ? (int)((long) fuelBurnTime * 100 / fuelMaxBurnTime) : 0;
        String fuelColor = fuelPercent >= 60 ? "<green>" : fuelPercent >= 30 ? "<yellow>" : "<red>";
        Material fuelIcon = fuelPercent >= 60 ? Material.CAMPFIRE : fuelPercent > 0 ? Material.SOUL_CAMPFIRE : Material.DEAD_FIRE_CORAL;
        inventory.setItem(SLOT_FUEL_INFO, new ItemBuilder(fuelIcon)
                .setName(msg("fuel_name"))
                .addLore(msg("fuel_type") + fuelName)
                .addLore(msg("fuel_stock") + fuelColor + formatTime(fuelBurnTime / 20) + " <dark_gray>/ " + formatTime(fuelMaxBurnTime / 20))
                .addLore(fuelColor + fuelPercent + "%")
                .addLore("")
                .addLore(msg("fuel_deposit_1"))
                .addLore(msg("fuel_deposit_2"))
                .build());

        // Recipe Book
        inventory.setItem(SLOT_RECIPE_BOOK, new ItemBuilder(Material.BOOK)
                .setName(msg("recipe_book"))
                .addLore(msg("recipe_book_lore1"))
                .addLore(msg("recipe_book_lore2"))
                .addLore("")
                .addLore(msg("recipe_book_click"))
                .glow().build());

        // Start Forging
        boolean canForge = playerLevel >= 1;
        inventory.setItem(SLOT_START_FORGING, new ItemBuilder(canForge ? Material.ANVIL : Material.BARRIER)
                .setName(canForge ? msg("start_forging") : msg("start_forging_unavailable"))
                .addLore(canForge
                        ? msg("start_forging_lore")
                        : msg("start_forging_unavailable_lore"))
                .addLore("")
                .addLore(canForge ? msg("start_forging_click") : msg("start_forging_unavailable_click"))
                .build());

        // Repair
        inventory.setItem(SLOT_REPAIR, new ItemBuilder(Material.DAMAGED_ANVIL)
                .setName(msg("repair"))
                .addLore(msg("repair_lore1"))
                .addLore(msg("repair_lore2"))
                .addLore("")
                .addLore(msg("repair_click"))
                .build());

        // Profession Info
        inventory.setItem(SLOT_PROFESSION_INFO, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("profession"))
                .addLore(msg("profession_level") + playerLevel)
                .addLore(msg("profession_lore1"))
                .addLore(msg("profession_lore2"))
                .addLore("")
                .addLore(msg("profession_click"))
                .build());

        // Upgrades
        inventory.setItem(SLOT_UPGRADES, new ItemBuilder(Material.NETHER_STAR)
                .setName(msg("upgrades"))
                .addLore(msg("upgrades_lore1"))
                .addLore(msg("upgrades_lore2"))
                .addLore("")
                .addLore(msg("upgrades_click"))
                .build());

        // Settings
        inventory.setItem(SLOT_SETTINGS, new ItemBuilder(Material.COMPARATOR)
                .setName(msg("settings"))
                .addLore(msg("settings_lore1"))
                .addLore(msg("settings_lore2"))
                .addLore("")
                .addLore(msg("settings_click"))
                .build());

        // Close
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(msg("close"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        switch (event.getRawSlot()) {
            case SLOT_RECIPE_BOOK -> {
                clicker.closeInventory();
                if (onOpenRecipeBook != null) { onOpenRecipeBook.accept(clicker); }
            }
            case SLOT_START_FORGING -> {
                if (playerLevel >= 1) {
                    clicker.closeInventory();
                    if (onStartForging != null) { onStartForging.accept(clicker, forge); }
                }
            }
            case SLOT_PROFESSION_INFO -> {
                clicker.closeInventory();
                if (onOpenProfessionInfo != null) { onOpenProfessionInfo.accept(clicker); }
            }
            case SLOT_REPAIR -> {
                clicker.closeInventory();
                ForgeRepairGui repairGui = new ForgeRepairGui(clicker, forge);
                repairGui.setOnBack(onOpenForgeMainGui);
                repairGui.open();
            }
            case SLOT_UPGRADES -> {
                clicker.closeInventory();
                ForgeUpgradeGui upgradeGui = new ForgeUpgradeGui(clicker, forge, playerLevel);
                upgradeGui.setOnBack(onOpenForgeMainGui);
                upgradeGui.open();
            }
            case SLOT_SETTINGS -> {
                clicker.closeInventory();
                ForgeSettingsGui settingsGui = new ForgeSettingsGui(clicker, forge);
                settingsGui.setOnBack(onOpenForgeMainGui);
                settingsGui.open();
            }
            case SLOT_CLOSE -> clicker.closeInventory();
        }
    }

    public void setOnOpenRecipeBook(Consumer<Player> cb) { this.onOpenRecipeBook = cb; }
    public void setOnStartForging(BiConsumer<Player, ForgeStructure> cb) { this.onStartForging = cb; }
    public void setOnOpenProfessionInfo(Consumer<Player> cb) { this.onOpenProfessionInfo = cb; }
    public void setOnOpenForgeMainGui(Consumer<Player> cb) { this.onOpenForgeMainGui = cb; }

    private String formatTime(int totalSeconds) {
        if (totalSeconds >= 60) {
            int min = totalSeconds / 60;
            int sec = totalSeconds % 60;
            return min + "min " + sec + "s";
        }
        return totalSeconds + "s";
    }
}
