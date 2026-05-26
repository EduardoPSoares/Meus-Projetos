package me.ray.midgard.modules.character.listener;

import java.util.ArrayList;
import java.util.List;

import me.ray.midgard.modules.character.CharacterModule;
import me.ray.midgard.modules.character.gui.CharacterMenu;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import net.kyori.adventure.text.Component;

public class HotbarListener implements Listener {

    private final CharacterModule module;
    private static final int SLOT_INDEX = 8; // 9th slot

    public HotbarListener(CharacterModule module) {
        this.module = module;
    }

    private ItemStack getCompass(Player player) {
        String name = module.getMessage("hotbar.compass.name");
        List<String> loreRaw = module.getMessageList("hotbar.compass.lore");

        int attributePoints = 0;
        int skillPoints = 0;
        try {
            var profileManager = MidgardCore.getProfileManager();
            var profile = (profileManager != null) ? profileManager.getProfile(player) : null;
            if (profile != null) {
                ClassData data = profile.getData(ClassData.class);
                if (data != null) {
                    attributePoints = data.getAttributePoints();
                }
            }
        } catch (Exception e) {
            MidgardLogger.debug("HotbarListener: Error getting profile data for compass - " + e.getMessage());
        }

        List<Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            String processed = line
                .replace("%attribute_points%", String.valueOf(attributePoints))
                .replace("%skill_points%", String.valueOf(skillPoints));
            lore.add(MessageUtils.parse(player, processed));
        }

        ItemStack item = new me.ray.midgard.core.utils.ItemBuilder(Material.COMPASS)
            .name(MessageUtils.parse(player, name))
            .lore(lore)
            .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(module.getCompassKey(), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isCompass(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(module.getCompassKey(), PersistentDataType.BYTE);
    }

    public void giveCompass(Player player) {
        if (player == null) {
            return;
        }
        try {
            player.getInventory().setItem(SLOT_INDEX, getCompass(player));
            String message = module.getMessage("hotbar.compass_received");
            if (message != null && !message.isEmpty()) {
                MessageUtils.send(player, message);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao entregar bússola para o jogador: " + player.getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            // Delay giving compass to ensure other plugins don't clear it immediately on join
            Task.syncLater(event.getPlayer(), () -> {
                giveCompass(event.getPlayer());
            }, 10L);
        } catch (Exception e) {
            MidgardLogger.error("Erro no evento de entrada do jogador no HotbarListener", e);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        try {
            // Delay for respawn as well
            Task.syncLater(event.getPlayer(), () -> {
                giveCompass(event.getPlayer());
            }, 10L);
        } catch (Exception e) {
            MidgardLogger.error("Erro no evento de respawn do jogador no HotbarListener", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (isCompass(event.getOldCursor()) || isCompass(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
            for (int rawSlot : event.getRawSlots()) {
                // Check if we are dragging over an existing compass
                if (event.getView() != null && isCompass(event.getView().getItem(rawSlot))) {
                    event.setCancelled(true);
                    return;
                }
                
                // Check if we are dragging into the locked slot 8 of the player's inventory
                if (event.getView() != null && event.getView().getTopInventory() != null) {
                    // Calculate if the slot is in the bottom inventory (Player Inventory)
                    int topSize = event.getView().getTopInventory().getSize();
                    if (rawSlot >= topSize) {
                        // It is in the bottom inventory
                        int slot = event.getView().convertSlot(rawSlot);
                        if (slot == SLOT_INDEX) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar clique na hotbar (Drag)", e);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (event.getClickedInventory() == null) {
                return;
            }
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            
            // Prevent interaction with slot 8 in player inventory
            if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
                if (event.getSlot() == SLOT_INDEX) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            if (event.getHotbarButton() == SLOT_INDEX) {
                event.setCancelled(true);
                return;
            }

            // Prevent moving the compass if somehow selected
            if (isCompass(event.getCurrentItem())) {
                event.setCancelled(true);
            }
            
            if (isCompass(event.getCursor())) {
                // Drop it from cursor if it stuck there
                event.getView().setCursor(null);
                event.setCancelled(true);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar clique na hotbar", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        try {
            if (isCompass(event.getItemDrop().getItemStack())) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar drop de item", e);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        try {
            Player player = event.getPlayer();
            // If mainhand is holding slot 8, cancel swap
            if (player.getInventory().getHeldItemSlot() == SLOT_INDEX) {
                event.setCancelled(true);
            }
            // Double check item content just in case
            if (isCompass(event.getMainHandItem()) || isCompass(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar troca de itens de mão", e);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        try {
            // Prevent drop on death
            event.getDrops().removeIf(this::isCompass);
            // It will be re-given on respawn
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar morte de jogador (Hotbar)", e);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (event.getItem() != null && isCompass(event.getItem())) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    new CharacterMenu(event.getPlayer()).open();
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar interação com a bússola", e);
             MessageUtils.send(event.getPlayer(), module.getMessage("errors.menu_open_failed"));
        }
    }
}
