package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui.AlloyBookGui;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui.DrainSelectGui;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui.SmelteryGui;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener de interação com o sistema de Fundição (Smeltery).
 * Conecta cliques no mundo com SmelteryManager.
 */
public class SmelteryListener implements Listener {

    private final SmelteryManager smelteryManager;

    public SmelteryListener(SmelteryManager smelteryManager) {
        this.smelteryManager = smelteryManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getClickedBlock() == null) { return; }

        Player player = event.getPlayer();
        Location clickedLoc = event.getClickedBlock().getLocation();
        Material mat = event.getClickedBlock().getType();

        // Tentar encontrar smeltery que contém este bloco
        SmelteryStructure smeltery = findSmelteryAt(clickedLoc);

        if (smeltery != null && smeltery.isActive()) {
            SmelteryBlockType blockType = smeltery.getBlockTypeAt(clickedLoc);
            if (blockType == null) { return; }

            event.setCancelled(true);

            switch (blockType) {
                case CONTROLLER -> smelteryManager.onControllerInteract(player, smeltery);
                case ITEM_INPUT -> {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    smelteryManager.onItemInput(player, smeltery, held);
                    if (held.getAmount() <= 0) {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                case FUEL_INPUT -> {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    smelteryManager.onFuelInput(player, smeltery, held);
                }
                case DRAIN -> smelteryManager.onDrainInteract(player, smeltery, clickedLoc);
                default -> {
                    // Blocos não interativos — ignorar
                }
            }
            return;
        }

        // Alavanca próxima a um drain → toggle auto-pour
        if (mat == Material.LEVER) {
            handleLeverToggle(player, event.getClickedBlock());
            return;
        }

        // Tentar detectar nova smeltery ao clicar em blast furnace
        if (mat == SmelteryBlockType.CONTROLLER.getDefaultMaterial()) {
            SmelteryStructure detected = smelteryManager.detectAndRegister(player, clickedLoc);
            if (detected != null) {
                event.setCancelled(true);
                smelteryManager.onControllerInteract(player, detected);
            }
        }
    }

    private void handleLeverToggle(Player player, org.bukkit.block.Block leverBlock) {
        Location leverLoc = leverBlock.getLocation();

        // Procurar smeltery que tenha um drain próximo dessa alavanca
        for (SmelteryStructure smeltery : smelteryManager.getRegistry().getAll()) {
            if (!smeltery.isActive()) { continue; }

            Location drainLoc = smelteryManager.findDrainNearLever(leverLoc, smeltery);
            if (drainLoc == null) { continue; }

            // Verificar estado da alavanca (após o toggle do Minecraft)
            // O evento é ANTES do toggle, então invertemos
            boolean willBePowered;
            if (leverBlock.getBlockData() instanceof Switch sw) {
                willBePowered = !sw.isPowered();
            } else {
                return;
            }

            smelteryManager.toggleAutoPour(player, smeltery, drainLoc, willBePowered);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) { return; }

        // SmelteryGui handling — permite interação no slot de input e inventário do jogador
        if (event.getInventory().getHolder() instanceof SmelteryGui gui) {
            gui.handleClick(event);
            return;
        }

        // AlloyBookGui handling — bloqueia toda interação (display only)
        if (event.getInventory().getHolder() instanceof AlloyBookGui gui) {
            gui.handleClick(event);
            return;
        }

        // DrainSelectGui handling
        if (event.getInventory().getHolder() instanceof DrainSelectGui gui) {
            gui.handleClick(event);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) { return; }

        var holder = event.getInventory().getHolder();

        // Bloquear drag em GUIs display-only
        if (holder instanceof AlloyBookGui || holder instanceof DrainSelectGui) {
            event.setCancelled(true);
            return;
        }

        // SmelteryGui — só permitir drag no INPUT_SLOT
        if (holder instanceof SmelteryGui) {
            int guiSize = event.getInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < guiSize && slot != 31) { // 31 = INPUT_SLOT
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) { return; }

        // Smeltery: processar input e esconder bossbar
        if (event.getInventory().getHolder() instanceof SmelteryGui gui) {
            gui.handleClose(event);
        }

        // AlloyBook: parar auto-refresh
        if (event.getInventory().getHolder() instanceof AlloyBookGui gui) {
            gui.handleClose(event);
        }
    }

    // ── Utilitários ──

    private SmelteryStructure findSmelteryAt(Location loc) {
        return smelteryManager.getRegistry().getAtLocation(loc);
    }
}
