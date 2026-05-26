package me.ray.midgard.modules.item.task;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.ability.AbilityTrigger;
import me.ray.midgard.modules.item.ability.SpellBinding;
import me.ray.midgard.modules.item.manager.ItemManager;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task that handles passive/timer-based abilities.
 * Runs periodically and checks equipped items for PASSIVE_TIMER triggers.
 * Uses Folia-compatible Task utility.
 */
public class PassiveAbilityTask {

    private final ItemModule module;
    
    // Track tick counts per player per binding
    private final Map<UUID, Map<String, Integer>> tickCounters = new ConcurrentHashMap<>();
    
    // Cache for SpellManager access via reflection
    private Object spellsModule = null;
    private Method castSpellMethod = null;
    private boolean spellsModuleChecked = false;
    
    // Task runs every 20 ticks (1 second)
    private static final int TASK_INTERVAL_TICKS = 20;
    
    // Reference to the running task
    private BukkitTask runningTask = null;

    public PassiveAbilityTask(ItemModule module) {
        this.module = module;
    }

    /**
     * Starts the task using Folia-compatible scheduler.
     */
    public void start() {
        runningTask = Task.syncTimer(this::tick, TASK_INTERVAL_TICKS, TASK_INTERVAL_TICKS);
    }
    
    /**
     * Stops the task.
     */
    public void stop() {
        if (runningTask != null && !runningTask.isCancelled()) {
            runningTask.cancel();
            runningTask = null;
        }
    }

    /**
     * Called every tick interval.
     */
    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Use entity-based scheduling for Folia compatibility
            Task.sync(player, () -> processPlayer(player));
        }
    }

    /**
     * Processes a single player for passive abilities.
     */
    private void processPlayer(Player player) {
        // Check main hand
        processItem(player, player.getInventory().getItemInMainHand(), "main");
        
        // Check off hand
        processItem(player, player.getInventory().getItemInOffHand(), "off");
        
        // Check armor
        ItemStack[] armor = player.getInventory().getArmorContents();
        String[] slots = {"boots", "leggings", "chestplate", "helmet"};
        for (int i = 0; i < armor.length && i < slots.length; i++) {
            processItem(player, armor[i], slots[i]);
        }
    }

    /**
     * Processes a single item for passive abilities.
     */
    private void processItem(Player player, ItemStack itemStack, String slot) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        
        MidgardItem midgardItem = getMidgardItem(itemStack);
        if (midgardItem == null) {
            return;
        }
        
        // Consumíveis não ativam habilidades passivas ao segurar
        if ("CONSUMABLE".equalsIgnoreCase(midgardItem.getCategoryId())) {
            return;
        }
        
        if (!midgardItem.hasSpellBindings()) {
            return;
        }
        
        List<SpellBinding> timerBindings = midgardItem.getSpellBindingsForTrigger(AbilityTrigger.PASSIVE_TIMER);
        if (timerBindings.isEmpty()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String itemId = midgardItem.getId();
        
        for (SpellBinding binding : timerBindings) {
            String bindingKey = itemId + ":" + slot + ":" + binding.getSpellId();
            
            // Get or initialize tick counter
            Map<String, Integer> playerCounters = tickCounters.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            int currentTicks = playerCounters.getOrDefault(bindingKey, 0) + TASK_INTERVAL_TICKS;
            
            // Check if timer interval reached
            int timerInterval = binding.getTimerTicks();
            if (timerInterval <= 0) {
                timerInterval = 20; // Default to 1 second
            }
            
            if (currentTicks >= timerInterval) {
                // Execute the spell
                castSpell(player, binding.getSpellId(), binding);
                currentTicks = 0; // Reset counter
            }
            
            playerCounters.put(bindingKey, currentTicks);
        }
    }

    /**
     * Gets the MidgardItem from an ItemStack.
     */
    private MidgardItem getMidgardItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        try {
            ItemManager itemManager = module.getItemManager();
            if (itemManager == null) {
                return null;
            }
            
            String id = itemManager.getItemId(itemStack);
            if (id == null) {
                return null;
            }
            return itemManager.getMidgardItem(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Casts a spell using the SpellsModule via reflection.
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
                    MidgardLogger.debug("PassiveAbilityTask: Could not initialize SpellsModule bridge - " + e.getMessage());
                }
            }
            
            if (spellsModule == null || castSpellMethod == null) {
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
            MidgardLogger.debug("PassiveAbilityTask: Failed to cast spell '" + spellId + "' - " + e.getMessage());
            return false;
        }
    }

    /**
     * Cleans up data for a player (call on quit).
     */
    public void cleanupPlayer(UUID playerId) {
        tickCounters.remove(playerId);
    }
}
