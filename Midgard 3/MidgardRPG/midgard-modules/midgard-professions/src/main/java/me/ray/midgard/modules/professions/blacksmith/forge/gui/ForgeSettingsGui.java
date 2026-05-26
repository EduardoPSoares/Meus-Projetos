package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * GUI de configurações da forja.
 * Permite ao dono da forja ativar/desativar notificações,
 * efeitos visuais, e controle de acesso público/privado.
 */
public class ForgeSettingsGui extends BaseGui {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.forge_settings." + key); }

    private static final int SLOT_TOGGLE_ACTIVE = 20;
    private static final int SLOT_RENAME = 22;
    private static final int SLOT_INFO = 24;
    private static final int SLOT_BACK = 49;

    private final ForgeStructure forge;
    private Consumer<Player> onBack;

    public ForgeSettingsGui(Player player, ForgeStructure forge) {
        super(player, 6, msg("title"));
        this.forge = forge;
    }

    public void setOnBack(Consumer<Player> onBack) { this.onBack = onBack; }

    @Override
    public void initializeItems() {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) { inventory.setItem(i, border); }

        // Toggle Active
        boolean active = forge.isActive();
        inventory.setItem(SLOT_TOGGLE_ACTIVE, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(active ? msg("toggle_active") : msg("toggle_inactive"))
                .addLore(msg("toggle_lore1"))
                .addLore(msg("toggle_lore2"))
                .addLore("")
                .addLore(active ? msg("toggle_deactivate") : msg("toggle_activate"))
                .build());

        // Rename placeholder
        inventory.setItem(SLOT_RENAME, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("rename"))
                .addLore(msg("rename_current") + (forge.getName() != null ? forge.getName() : forge.getTier().getName()))
                .addLore("")
                .addLore(msg("rename_command"))
                .addLore(msg("rename_usage"))
                .build());

        // General info
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.BOOK)
                .setName(msg("info"))
                .addLore(msg("info_tier") + forge.getTier().getDisplayName())
                .addLore(msg("info_items_forged") + forge.getTotalItemsForged())
                .addLore(msg("info_owner") + org.bukkit.Bukkit.getOfflinePlayer(forge.getOwnerUuid()).getName())
                .addLore(msg("info_world") + forge.getWorldName())
                .build());

        // Back
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .setName(msg("back"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        switch (event.getRawSlot()) {
            case SLOT_TOGGLE_ACTIVE -> {
                forge.setActive(!forge.isActive());
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                initializeItems();
            }
            case SLOT_BACK -> {
                clicker.closeInventory();
                if (onBack != null) { onBack.accept(clicker); }
            }
        }
    }
}
