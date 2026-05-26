package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * GUI informativa mostrando a progressão de tiers da forja.
 * Mostra o tier atual, o próximo tier e os requisitos para upgrade.
 */
public class ForgeUpgradeGui extends BaseGui {

    private static final int SLOT_CURRENT = 20;
    private static final int SLOT_ARROW = 22;
    private static final int SLOT_NEXT = 24;
    private static final int SLOT_BACK = 49;

    private final ForgeStructure forge;
    private final int playerLevel;
    private Consumer<Player> onBack;

    public ForgeUpgradeGui(Player player, ForgeStructure forge, int playerLevel) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.upgrade.title"));
        this.forge = forge;
        this.playerLevel = playerLevel;
    }

    public void setOnBack(Consumer<Player> onBack) { this.onBack = onBack; }

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.upgrade." + key); }

    @Override
    public void initializeItems() {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) { inventory.setItem(i, border); }

        ForgeTier current = forge.getTier();
        ForgeTier next = ForgeTier.fromLevel(current.getLevel() + 1);

        // Current tier info
        inventory.setItem(SLOT_CURRENT, new ItemBuilder(getMaterialForTier(current))
                .setName(current.getDisplayName())
                .addLore(msg("current_tier_label"))
                .addLore("")
                .addLore(msg("level") + current.getLevel())
                .addLore(msg("size") + current.getWidth() + "x" + current.getHeight() + "x" + current.getDepth())
                .addLore(msg("items_forged") + forge.getTotalItemsForged())
                .glow()
                .build());

        // Arrow
        inventory.setItem(SLOT_ARROW, new ItemBuilder(Material.SPECTRAL_ARROW)
                .setName("<yellow>→")
                .build());

        // Next tier
        if (next != null) {
            boolean meetsLevel = playerLevel >= next.getRequiredProfessionLevel();
            inventory.setItem(SLOT_NEXT, new ItemBuilder(getMaterialForTier(next))
                    .setName(next.getDisplayName())
                    .addLore(msg("next_tier_label"))
                    .addLore("")
                    .addLore(msg("level") + next.getLevel())
                    .addLore(msg("size") + next.getWidth() + "x" + next.getHeight() + "x" + next.getDepth())
                    .addLore(msg("required_level") + (meetsLevel ? "<green>" : "<red>") + next.getRequiredProfessionLevel())
                    .addLore("")
                    .addLore(msg("improve_line1"))
                    .addLore(msg("improve_line2"))
                    .addLore(msg("improve_line3"))
                    .build());
        } else {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.NETHER_STAR)
                    .setName(msg("max_tier_title"))
                    .addLore(msg("max_tier_line1"))
                    .addLore(msg("max_tier_line2"))
                    .glow()
                    .build());
        }

        // Tier progression display (row 4, slots 37-43)
        int slot = 37;
        for (ForgeTier tier : ForgeTier.values()) {
            if (slot > 43) { break; }
            boolean unlocked = tier.getLevel() <= current.getLevel();
            boolean isCurrent = tier == current;
            inventory.setItem(slot, new ItemBuilder(unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                    .setName((isCurrent ? "<gold><bold>" : unlocked ? "<green>" : "<red>") + tier.getName())
                    .addLore(isCurrent ? msg("current_marker") : unlocked ? msg("unlocked") : msg("locked_level") + tier.getRequiredProfessionLevel())
                    .build());
            slot++;
        }

        // Back
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .setName(msg("back"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        if (event.getRawSlot() == SLOT_BACK) {
            clicker.closeInventory();
            if (onBack != null) { onBack.accept(clicker); }
        }
    }

    private Material getMaterialForTier(ForgeTier tier) {
        return switch (tier) {
            case BASIC -> Material.CRAFTING_TABLE;
            case INTERMEDIATE -> Material.SMITHING_TABLE;
            case ADVANCED -> Material.ANVIL;
            case MASTER -> Material.ENCHANTING_TABLE;
            case LEGENDARY -> Material.BEACON;
        };
    }
}
