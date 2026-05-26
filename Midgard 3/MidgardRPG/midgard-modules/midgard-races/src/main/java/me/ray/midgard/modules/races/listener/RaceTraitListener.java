package me.ray.midgard.modules.races.listener;

import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class RaceTraitListener implements Listener {

    private final RacesModule module;

    public RaceTraitListener(RacesModule module) {
        this.module = module;
    }

    private void processTrigger(Player player, TraitTrigger trigger, Map<String, Object> context) {
        module.getRaceManager().processTrigger(player, trigger, context);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getPlayer().isSneaking()) {
                    Map<String, Object> context = new HashMap<>();
                    context.put("event", event);
                    context.put("action", event.getAction());
                    context.put("item", event.getItem());

                    processTrigger(event.getPlayer(), TraitTrigger.ON_ACTIVE, context);
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar interact trait para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onExp(PlayerExpChangeEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("event", event);
            context.put("amount", event.getAmount());

            processTrigger(event.getPlayer(), TraitTrigger.ON_EXP_GAIN, context);
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar exp trait para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            processTrigger(event.getPlayer(), TraitTrigger.ON_JOIN, new HashMap<>());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar join trait para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            processTrigger(event.getPlayer(), TraitTrigger.ON_QUIT, new HashMap<>());
            module.getTraitRunnable().removePlayer(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar quit trait para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        try {
            if (event.getDamager() instanceof Player player) {
                Map<String, Object> context = new HashMap<>();
                context.put("event", event);
                context.put("damage", event.getDamage());
                context.put("target", event.getEntity());
                
                processTrigger(player, TraitTrigger.ON_ATTACK, context);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar attack trait", e);
        }
    }

    @EventHandler
    public void onDefend(EntityDamageByEntityEvent event) {
        try {
            if (event.getEntity() instanceof Player player) {
                Map<String, Object> context = new HashMap<>();
                context.put("event", event);
                context.put("damage", event.getDamage());
                context.put("attacker", event.getDamager());

                processTrigger(player, TraitTrigger.ON_DEFEND, context);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar defend trait", e);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        try {
            if (event.getEntity() instanceof Player player) {
                Map<String, Object> context = new HashMap<>();
                context.put("event", event);
                context.put("damage", event.getDamage());
                context.put("cause", event.getCause());

                processTrigger(player, TraitTrigger.ON_DAMAGE, context);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar damage trait", e);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        try {
            processTrigger(event.getEntity(), TraitTrigger.ON_DEATH, new HashMap<>());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar death trait para %s", event.getEntity().getName(), e);
        }
    }
    
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        try {
            if (event.getEntity().getKiller() != null) {
                Player killer = event.getEntity().getKiller();
                Map<String, Object> context = new HashMap<>();
                context.put("event", event);
                context.put("victim", event.getEntity());
                
                processTrigger(killer, TraitTrigger.ON_KILL, context);
                
                // Rastrear kills para requisitos de evolução
                module.getRaceManager().addKill(killer, event.getEntity().getType().name());
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar kill trait", e);
        }
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("event", event);
            context.put("item", event.getItem());
            
            processTrigger(event.getPlayer(), TraitTrigger.ON_EAT, context);
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar eat trait para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onRegen(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        try {
            if (event.getEntity() instanceof Player player) {
                Map<String, Object> context = new HashMap<>();
                context.put("event", event);
                context.put("amount", event.getAmount());
                context.put("reason", event.getRegainReason());

                processTrigger(player, TraitTrigger.ON_REGEN, context);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar regen trait", e);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("event", event);
            context.put("block", event.getBlock());

            processTrigger(event.getPlayer(), TraitTrigger.ON_BLOCK_BREAK, context);
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar block break trait para %s", event.getPlayer().getName(), e);
        }
    }
}
