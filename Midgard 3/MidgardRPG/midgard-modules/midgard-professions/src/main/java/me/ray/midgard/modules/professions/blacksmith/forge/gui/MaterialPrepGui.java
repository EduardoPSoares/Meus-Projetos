package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Material preparation GUI. Shown after selecting a recipe, lets
 * the player place materials into slots and confirm to start forging.
 */
public class MaterialPrepGui extends BaseGui {

    private static final int SLOT_RECIPE_INFO = 4;
    private static final int SLOT_PRIMARY_METAL = 20;
    private static final int SLOT_SECONDARY_1 = 22;
    private static final int SLOT_SECONDARY_2 = 24;
    private static final int SLOT_CONFIRM = 40;
    private static final int SLOT_CANCEL = 49;

    // Material deposit slots (where players PUT items)
    private static final int[] DEPOSIT_SLOTS = {29, 31, 33};

    private final ForgeRecipe recipe;
    private final ForgeSession session;

    /**
     * Materials the player has deposited.
     * This is a simplified model — in production you'd validate actual ItemStacks.
     */
    private boolean primaryDeposited;
    private int secondaryDeposited;

    private BiConsumer<Player, ForgeSession> onConfirm;

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.material_prep." + key); }

    public MaterialPrepGui(Player player, ForgeRecipe recipe, ForgeSession session) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.material_prep.title") + recipe.getDisplayName());
        this.recipe = recipe;
        this.session = session;
    }

    @Override
    public void initializeItems() {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Recipe info
        inventory.setItem(SLOT_RECIPE_INFO, new ItemBuilder(Material.ANVIL)
                .setName("<gold>" + recipe.getDisplayName())
                .addLore(msg("deposit_instruction"))
                .build());

        // Primary metal needed
        inventory.setItem(SLOT_PRIMARY_METAL, new ItemBuilder(Material.IRON_INGOT)
                .setName(msg("primary_metal"))
                .addLore(msg("item_label") + recipe.getPrimaryMetal())
                .addLore(msg("quantity_label") + recipe.getPrimaryMetalAmount())
                .addLore("")
                .addLore(primaryDeposited ? msg("deposited") : msg("pending"))
                .build());

        // Secondary materials
        int secIdx = 0;
        for (Map.Entry<String, Integer> mat : recipe.getSecondaryMaterials().entrySet()) {
            int slot = secIdx == 0 ? SLOT_SECONDARY_1 : SLOT_SECONDARY_2;
            boolean deposited = secIdx < secondaryDeposited;
            inventory.setItem(slot, new ItemBuilder(Material.GOLD_NUGGET)
                    .setName(msg("secondary_material"))
                    .addLore(msg("item_label") + mat.getKey())
                    .addLore(msg("quantity_label") + mat.getValue())
                    .addLore("")
                    .addLore(deposited ? msg("deposited") : msg("pending"))
                    .build());
            secIdx++;
            if (secIdx >= 2) { break; }
        }

        // Deposit slots (allow interaction)
        for (int dSlot : DEPOSIT_SLOTS) {
            inventory.setItem(dSlot, new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .setName(msg("deposit_here"))
                    .build());
        }

        // Confirm button
        boolean ready = primaryDeposited && secondaryDeposited >= recipe.getSecondaryMaterials().size();
        inventory.setItem(SLOT_CONFIRM, new ItemBuilder(ready ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(ready ? msg("confirm") : msg("deposit_all"))
                .build());

        // Cancel
        inventory.setItem(SLOT_CANCEL, new ItemBuilder(Material.BARRIER)
                .setName(msg("cancel")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        int slot = event.getRawSlot();

        // Deposit slots — in a production implementation you'd validate the items
        // the player puts in. For now, we simulate auto-deposit on click.
        if (slot == DEPOSIT_SLOTS[0] && !primaryDeposited) {
            // Check if player has the required materials in their inventory
            primaryDeposited = true; // Simplified — real impl checks inventory
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            initializeItems();
        } else if ((slot == DEPOSIT_SLOTS[1] || slot == DEPOSIT_SLOTS[2])
                && secondaryDeposited < recipe.getSecondaryMaterials().size()) {
            secondaryDeposited++;
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            initializeItems();
        } else if (slot == SLOT_CONFIRM) {
            boolean ready = primaryDeposited && secondaryDeposited >= recipe.getSecondaryMaterials().size();
            if (ready) {
                session.setMaterialsConsumed(true);
                clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                clicker.closeInventory();
                if (onConfirm != null) { onConfirm.accept(clicker, session); }
            }
        } else if (slot == SLOT_CANCEL) {
            clicker.closeInventory();
        }
    }

    public void setOnConfirm(BiConsumer<Player, ForgeSession> callback) {
        this.onConfirm = callback;
    }
}
