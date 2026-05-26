package me.ray.midgard.modules.item.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.ability.AbilityTrigger;
import me.ray.midgard.modules.item.ability.SpellBinding;
import me.ray.midgard.modules.item.manager.ItemManager;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for item ability triggers.
 * Detects player actions and executes bound spells from items.
 */
public class ItemAbilityListener implements Listener {

    private final ItemModule module;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    // Cache for SpellManager access via reflection (avoids hard dependency)
    private Object spellsModule = null;
    private Method castSpellMethod = null;
    private boolean spellsModuleChecked = false;

    public ItemAbilityListener(ItemModule module) {
        this.module = module;
    }

    /**
     * Gets the MidgardItem from an ItemStack.
     * Returns null for consumable items (they don't trigger abilities while held).
     */
    private MidgardItem getMidgardItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) { return null; }
        try {
            ItemManager itemManager = module.getItemManager();
            if (itemManager == null) { return null; }
            
            String id = itemManager.getItemId(itemStack);
            if (id == null) { return null; }
            MidgardItem item = itemManager.getMidgardItem(id);
            if (item != null && "CONSUMABLE".equalsIgnoreCase(item.getCategoryId())) { return null; }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Handles LEFT_CLICK, RIGHT_CLICK, SHIFT+CLICK triggers.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignore offhand events to prevent double-firing
        if (event.getHand() == EquipmentSlot.OFF_HAND) { return; }
        // Respect cancellation from other listeners (e.g., ItemRestrictionListener)
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) { return; }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        MidgardItem midgardItem = getMidgardItem(item);
        if (midgardItem == null) { return; }
        
        if (!midgardItem.hasSpellBindings()) { return; }
        
        AbilityTrigger trigger = determineTrigger(event.getAction(), player.isSneaking());
        if (trigger == null) { return; }
        
        List<SpellBinding> bindings = midgardItem.getSpellBindingsForTrigger(trigger);
        if (bindings.isEmpty()) { return; }
        
        // Execute all bindings for this trigger
        for (SpellBinding binding : bindings) {
            executeSpellBinding(player, binding, midgardItem.getId());
        }
    }

    /**
     * Handles SNEAK (toggle sneak) trigger.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) { return; } // Only trigger when starting to sneak
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        MidgardItem midgardItem = getMidgardItem(item);
        if (midgardItem == null) { return; }
        
        if (!midgardItem.hasSpellBindings()) { return; }
        
        List<SpellBinding> bindings = midgardItem.getSpellBindingsForTrigger(AbilityTrigger.SNEAK);
        for (SpellBinding binding : bindings) {
            executeSpellBinding(player, binding, midgardItem.getId());
        }
    }

    /**
     * Handles ON_DAMAGE_DEALT trigger (when player attacks).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) { return; }
        
        Player player = (Player) event.getDamager();
        
        ItemStack item = player.getInventory().getItemInMainHand();
        MidgardItem midgardItem = getMidgardItem(item);
        if (midgardItem == null) { return; }
        
        if (!midgardItem.hasSpellBindings()) { return; }
        
        // ON_DAMAGE_DEALT trigger (when player hits)
        List<SpellBinding> damageBindings = midgardItem.getSpellBindingsForTrigger(AbilityTrigger.ON_DAMAGE_DEALT);
        for (SpellBinding binding : damageBindings) {
            executeSpellBinding(player, binding, midgardItem.getId());
        }
    }
    
    /**
     * Handles ON_DAMAGE_TAKEN trigger (when player takes damage).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) { return; }
        
        Player player = (Player) event.getEntity();
        
        // Check main hand item
        ItemStack item = player.getInventory().getItemInMainHand();
        MidgardItem midgardItem = getMidgardItem(item);
        
        if (midgardItem != null && midgardItem.hasSpellBindings()) {
            List<SpellBinding> bindings = midgardItem.getSpellBindingsForTrigger(AbilityTrigger.ON_DAMAGE_TAKEN);
            for (SpellBinding binding : bindings) {
                executeSpellBinding(player, binding, midgardItem.getId());
            }
        }
        
        // Also check armor pieces for ON_DAMAGE_TAKEN
        checkArmorBindings(player, AbilityTrigger.ON_DAMAGE_TAKEN);
    }

    /**
     * Handles ON_KILL trigger.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null) { return; }
        
        ItemStack item = killer.getInventory().getItemInMainHand();
        MidgardItem midgardItem = getMidgardItem(item);
        if (midgardItem == null) { return; }
        
        if (!midgardItem.hasSpellBindings()) { return; }
        
        List<SpellBinding> bindings = midgardItem.getSpellBindingsForTrigger(AbilityTrigger.ON_KILL);
        for (SpellBinding binding : bindings) {
            executeSpellBinding(killer, binding, midgardItem.getId());
        }
    }

    /**
     * Checks armor pieces for bindings with the specified trigger.
     */
    private void checkArmorBindings(Player player, AbilityTrigger trigger) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armor) {
            MidgardItem midgardItem = getMidgardItem(armorPiece);
            if (midgardItem == null) { continue; }
            
            if (!midgardItem.hasSpellBindings()) { continue; }
            
            List<SpellBinding> bindings = midgardItem.getSpellBindingsForTrigger(trigger);
            for (SpellBinding binding : bindings) {
                executeSpellBinding(player, binding, midgardItem.getId());
            }
        }
    }

    /**
     * Determines the trigger type based on action and sneak state.
     */
    private AbilityTrigger determineTrigger(Action action, boolean sneaking) {
        switch (action) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return sneaking ? AbilityTrigger.SHIFT_LEFT_CLICK : AbilityTrigger.LEFT_CLICK;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                return sneaking ? AbilityTrigger.SHIFT_RIGHT_CLICK : AbilityTrigger.RIGHT_CLICK;
            default:
                return null;
        }
    }

    /**
     * Executes a spell binding, handling cooldowns.
     */
    private void executeSpellBinding(Player player, SpellBinding binding, String itemId) {
        String spellId = binding.getSpellId();
        String cooldownKey = itemId + ":" + spellId;
        
        // Check cooldown
        long cooldownMs = (long) (binding.getCooldownOverride() * 1000);
        if (cooldownMs > 0 && isOnCooldown(player.getUniqueId(), cooldownKey, cooldownMs)) {
            // On cooldown - skip execution
            return;
        }
        
        // Set cooldown
        if (cooldownMs > 0) {
            setCooldown(player.getUniqueId(), cooldownKey);
        }
        
        // Cast the spell via SpellsModule
        boolean success = castSpell(player, spellId, binding);
        
        if (!success) {
            // Remove cooldown if cast failed
            removeCooldown(player.getUniqueId(), cooldownKey);
        }
    }

    /**
     * Casts a spell using the SpellsModule via reflection.
     * This avoids hard compile-time dependency on the spells module.
     */
    private boolean castSpell(Player player, String spellId, SpellBinding binding) {
        try {
            // Lazy-load SpellsModule reference
            if (!spellsModuleChecked) {
                spellsModuleChecked = true;
                try {
                    RPGModule spellsMod = MidgardCore.getModuleManager().getModule("Spells");
                    if (spellsMod != null) {
                        this.spellsModule = spellsMod;
                        
                        // Get SpellManager and castSpell method
                        Method getSpellManager = spellsMod.getClass().getMethod("getSpellManager");
                        Object spellManager = getSpellManager.invoke(spellsMod);
                        
                        if (spellManager != null) {
                            this.castSpellMethod = spellManager.getClass().getMethod("castSpell", Player.class, String.class);
                        }
                    }
                } catch (Exception e) {
                    MidgardLogger.debug("ItemAbilityListener: Could not initialize SpellsModule bridge - " + e.getMessage());
                }
            }
            
            if (spellsModule == null || castSpellMethod == null) {
                MidgardLogger.warn("ItemAbilityListener: SpellsModule not available. Cannot cast spell: " + spellId);
                return false;
            }
            
            // Get SpellManager
            Method getSpellManager = spellsModule.getClass().getMethod("getSpellManager");
            Object spellManager = getSpellManager.invoke(spellsModule);
            
            if (spellManager == null) {
                return false;
            }
            
            // Call castSpell
            Object result = castSpellMethod.invoke(spellManager, player, spellId);
            return result instanceof Boolean && (Boolean) result;
            
        } catch (Exception e) {
            MidgardLogger.warn("ItemAbilityListener: Failed to cast spell '" + spellId + "' - " + e.getMessage());
            return false;
        }
    }

    // ========== Cooldown Management ==========
    
    private boolean isOnCooldown(UUID playerId, String key, long cooldownMs) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return false;
        }
        
        Long lastUse = playerCooldowns.get(key);
        if (lastUse == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastUse < cooldownMs;
    }
    
    private void setCooldown(UUID playerId, String key) {
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis());
    }
    
    private void removeCooldown(UUID playerId, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
    }
    
    /**
     * Cleans up cooldown data for a player (call on quit).
     */
    public void cleanupPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
