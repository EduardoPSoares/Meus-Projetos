package me.ray.midgardspigot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.logging.Level;

public class MaintenanceListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onDamage", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockBreak", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockPlace", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onDrop", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent e) {
        try {
            if (e.getEntity() instanceof org.bukkit.entity.Player && e.getEntity().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onPickup", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        try {
            if (e.getWhoClicked().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onInventoryClick", ex);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onInventoryOpen", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onInteract", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onInteractEntity", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onInteractAtEntity", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onChat", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onCommand", ex);
        }
    }

    // --- NOVAS PROTEÇÕES ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent e) {
        try {
            // Permissões não estão carregadas durante PlayerLoginEvent, usar isOp() ou lista de config
            if (e.getPlayer().isOp()) return;
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, MidgardSpigot.PREFIX + MidgardSpigot.getInstance().getMessage("kick-message"));
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onLogin", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onSwapHand", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEnter(VehicleEnterEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onVehicleEnter", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent e) {
        try {
            e.setCancelled(true); // Para redstone e física de blocos
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockPhysics", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLiquidFlow(BlockFromToEvent e) {
        try {
            e.setCancelled(true); // Para água e lava
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onLiquidFlow", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobTarget(EntityTargetEvent e) {
        try {
            e.setCancelled(true); // Mobs param de focar jogadores
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onMobTarget", ex);
        }
    }

    // --- PROTEÇÕES AMBIENTAIS E FÍSICAS ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        try {
            e.setCancelled(true); // TNT, Creepers, Withers
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onEntityExplode", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        try {
            e.setCancelled(true); // Explosões de blocos (ex: Cama no Nether)
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockExplode", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        try {
            e.setCancelled(true); // Flechas, Tridentes, Snowballs
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onProjectileLaunch", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent e) {
        try {
            e.setCancelled(true); // Impede troca de dimensão (evita desync)
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onPortal", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent e) {
        try {
            e.setCancelled(true); // Impede perda de fome
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onFoodChange", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeavesDecay(LeavesDecayEvent e) {
        try {
            e.setCancelled(true); // Reduz lag de processamento de árvores
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onLeavesDecay", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStructureGrow(StructureGrowEvent e) {
        try {
            e.setCancelled(true); // Impede crescimento de árvores/cogumelos
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onStructureGrow", ex);
        }
    }

    // --- PROTEÇÕES DE SISTEMAS E REDSTONE ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperMove(InventoryMoveItemEvent e) {
        try {
            e.setCancelled(true); // Impede itens movendo em funis (Anti-Dupe Crítico)
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onHopperMove", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        try {
            e.setCancelled(true); // Impede pistões de empurrar
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onPistonExtend", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        try {
            e.setCancelled(true); // Impede pistões de puxar
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onPistonRetract", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDispense(BlockDispenseEvent e) {
        try {
            e.setCancelled(true); // Impede Droppers e Dispensers
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onDispense", ex);
        }
    }

    // --- PROTEÇÕES DE MUNDO ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobSpawn(CreatureSpawnEvent e) {
        try {
            if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                e.setCancelled(true); // Impede spawn natural de mobs
            }
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onMobSpawn", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent e) {
        try {
            e.setCancelled(true); // Impede itens de sumirem do chão (preserva loot)
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onItemDespawn", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent e) {
        try {
            if (e.toWeatherState()) {
                e.setCancelled(true); // Impede chuva/tempestade de começar
            }
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onWeatherChange", ex);
        }
    }

    // --- PROTEÇÕES EXTRAS (TOTAL LOCKDOWN) ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockBurn", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent e) {
        try {
            if (e.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && e.getPlayer() != null && e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockIgnite", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFade(BlockFadeEvent e) {
        try {
            e.setCancelled(true); // Gelo derretendo, coral morrendo
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockFade", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockForm(BlockFormEvent e) {
        try {
            e.setCancelled(true); // Gelo formando, cobble generator
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockForm", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockGrow(BlockGrowEvent e) {
        try {
            e.setCancelled(true); // Plantações crescendo
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBlockGrow", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        try {
            e.setCancelled(true); // Enderman, Ovelha comendo grama
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onEntityChangeBlock", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent e) {
        try {
            e.setCancelled(true); // Mobs pisando em plantação
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onEntityInteract", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBucketEmpty", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBucketFill", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedEnter(PlayerBedEnterEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBedEnter", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onFish", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShear(PlayerShearEntityEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onShear", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent e) {
        try {
            if (e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onConsume", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent e) {
        try {
            if (e instanceof org.bukkit.event.hanging.HangingBreakByEntityEvent) {
                org.bukkit.event.hanging.HangingBreakByEntityEvent entityEvent = (org.bukkit.event.hanging.HangingBreakByEntityEvent) e;
                if (entityEvent.getRemover() instanceof org.bukkit.entity.Player && entityEvent.getRemover().hasPermission("midgard.admin")) return;
            }
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onHangingBreak", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent e) {
        try {
            if (e.getPlayer() != null && e.getPlayer().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onHangingPlace", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnaceBurn(FurnaceBurnEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onFurnaceBurn", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onFurnaceSmelt", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBrew(BrewEvent e) {
        try {
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onBrew", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent e) {
        try {
            if (e.getWhoClicked().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onCraft", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        // Não dá pra cancelar, mas podemos limpar o resultado
        // e.getInventory().setResult(null); 
        // Melhor deixar o CraftItemEvent lidar com o cancelamento real
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent e) {
        try {
            if (e.getEnchanter().hasPermission("midgard.admin")) return;
            e.setCancelled(true);
        } catch (Exception ex) {
            MidgardSpigot.getInstance().getLogger().log(Level.SEVERE, "Erro em onEnchant", ex);
        }
    }
}

