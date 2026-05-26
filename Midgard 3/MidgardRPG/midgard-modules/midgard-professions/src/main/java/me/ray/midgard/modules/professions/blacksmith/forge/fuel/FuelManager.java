package me.ray.midgard.modules.professions.blacksmith.forge.fuel;

import me.ray.midgard.modules.professions.ProfessionsModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages fuel for the forge system.
 * Players physically drop fuel items on the ground in the fuel zone
 * (an invisible area defined in the forge schematic).
 *
 * The system scans for dropped Item entities in the zone and
 * consumes them as needed during forging.
 */
public class FuelManager {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Registered fuel types
    private final Map<Material, ForgeFuel> fuelRegistry = new LinkedHashMap<>();

    // Active fuel per forge: forgeId → deposited fuel
    private final Map<UUID, FuelDeposit> activeDeposits = new ConcurrentHashMap<>();

    // Search radius around each fuel zone block to find dropped items
    private static final double PICKUP_RADIUS = 1.5;

    // Maximum accumulated burn time per forge (default ~2h13m)
    private int maxBurnTime = 160000;

    public FuelManager() {
        registerDefaultFuels();
    }

    public int getMaxBurnTime() { return maxBurnTime; }
    public void setMaxBurnTime(int maxBurnTime) { this.maxBurnTime = Math.max(1600, maxBurnTime); }

    private void registerDefaultFuels() {
        // Basic fuels
        register(new ForgeFuel(Material.COAL, "Carvão", 1.0, 1600, 0.0, 0));
        register(new ForgeFuel(Material.CHARCOAL, "Carvão Vegetal", 1.0, 1600, 0.01, 0));
        register(new ForgeFuel(Material.COAL_BLOCK, "Bloco de Carvão", 1.3, 16000, 0.02, 0));

        // Intermediate fuels
        register(new ForgeFuel(Material.BLAZE_ROD, "Bastão de Blaze", 1.5, 2400, 0.03, 3));
        register(new ForgeFuel(Material.DRIED_KELP_BLOCK, "Bloco de Kelp Seco", 0.9, 4000, 0.0, 0));
        register(new ForgeFuel(Material.LAVA_BUCKET, "Balde de Lava", 1.8, 20000, 0.04, 5));

        // Advanced fuels
        register(new ForgeFuel(Material.BLAZE_POWDER, "Pó de Blaze", 1.6, 1200, 0.05, 5));
        register(new ForgeFuel(Material.MAGMA_CREAM, "Creme de Magma", 1.7, 2000, 0.06, 7));

        // Premium fuels
        register(new ForgeFuel(Material.FIRE_CHARGE, "Carga de Fogo", 2.0, 3200, 0.08, 8));
        register(new ForgeFuel(Material.NETHER_STAR, "Estrela do Nether", 3.0, 40000, 0.15, 10));
    }

    public void register(ForgeFuel fuel) {
        fuelRegistry.put(fuel.getMaterial(), fuel);
    }

    /**
     * Checks if a material is a valid fuel.
     */
    public boolean isFuel(Material material) {
        return fuelRegistry.containsKey(material);
    }

    /**
     * Gets the fuel data for a given material.
     */
    public ForgeFuel getFuel(Material material) {
        return fuelRegistry.get(material);
    }

    /**
     * Gets all registered fuel types.
     */
    public Collection<ForgeFuel> getAllFuels() {
        return Collections.unmodifiableCollection(fuelRegistry.values());
    }

    /**
     * Scans the fuel zone for dropped Item entities and collects one valid fuel item.
     * The item is removed from the world and a FuelDeposit is created.
     *
     * @param forgeId The forge UUID
     * @param fuelZoneLocations The list of fuel zone block locations
     * @param player The player (for messages and level check), may be null
     * @return true if fuel was collected
     */
    public boolean collectFuelFromZone(UUID forgeId, List<Location> fuelZoneLocations, Player player, int forgeLevel) {
        if (fuelZoneLocations == null || fuelZoneLocations.isEmpty()) { return false; }

        FuelDeposit existing = activeDeposits.get(forgeId);

        // Check if deposit is already at max capacity
        if (existing != null && existing.getRemainingBurnTime() >= maxBurnTime) { return true; }

        boolean collected = false;

        // Scan for dropped items in the fuel zone
        for (Location zoneLoc : fuelZoneLocations) {
            World world = zoneLoc.getWorld();
            if (world == null) { continue; }

            Location center = zoneLoc.clone().add(0.5, 0.5, 0.5);
            Collection<Entity> nearby = world.getNearbyEntities(center, PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS);

            for (Entity entity : nearby) {
                if (!(entity instanceof Item itemEntity)) { continue; }
                if (!itemEntity.isValid() || itemEntity.isDead()) { continue; }

                ItemStack stack = itemEntity.getItemStack();
                ForgeFuel fuel = fuelRegistry.get(stack.getType());
                if (fuel == null) { continue; }

                // Check forge profession level
                if (forgeLevel < fuel.getMinForgeLevel()) { continue; }

                // Consume items from the stack (as many as fit)
                existing = activeDeposits.get(forgeId);
                int currentBurn = existing != null ? existing.getRemainingBurnTime() : 0;
                int spaceLeft = maxBurnTime - currentBurn;
                if (spaceLeft <= 0) { break; }

                int itemsToConsume = Math.min(stack.getAmount(), Math.max(1, spaceLeft / fuel.getBurnTime()));

                int burnTime = fuel.getBurnTime() * itemsToConsume;

                if (stack.getAmount() > itemsToConsume) {
                    stack.setAmount(stack.getAmount() - itemsToConsume);
                    itemEntity.setItemStack(stack);
                } else {
                    itemEntity.remove();
                }

                // Create or stack deposit
                if (existing != null) {
                    existing.addBurnTime(burnTime);
                    existing.updateFuel(fuel);
                } else {
                    existing = new FuelDeposit(fuel, burnTime);
                    activeDeposits.put(forgeId, existing);
                }

                // Visual feedback
                playCollectEffect(center);
                collected = true;
            }
        }

        if (collected && player != null) {
            existing = activeDeposits.get(forgeId);
            int seconds = existing != null ? existing.getRemainingBurnTime() / 20 : 0;
            player.sendMessage(mm.deserialize(msg("forge.fuel.collected").replace("%time%", formatTime(seconds))));
        }

        return collected || (existing != null && existing.getRemainingBurnTime() > 0);
    }

    /**
     * Deposits fuel from the player's hand into the forge's fuel buffer.
     * Called when right-clicking a fuel zone block with fuel in hand.
     *
     * @return true if fuel was deposited
     */
    public boolean depositFuelFromHand(UUID forgeId, Player player, int forgeLevel) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { return false; }

        ForgeFuel fuel = fuelRegistry.get(hand.getType());
        if (fuel == null) { return false; }

        if (forgeLevel < fuel.getMinForgeLevel()) {
            player.sendMessage(mm.deserialize(msg("forge.fuel.level_insufficient")
                    .replace("%name%", fuel.getDisplayName())
                    .replace("%level%", String.valueOf(fuel.getMinForgeLevel()))));
            return false;
        }

        FuelDeposit existing = activeDeposits.get(forgeId);
        int currentBurn = existing != null ? existing.getRemainingBurnTime() : 0;

        // Calculate how many items fit up to the cap
        int spaceLeft = maxBurnTime - currentBurn;
        if (spaceLeft <= 0) {
            int maxSeconds = maxBurnTime / 20;
            player.sendMessage(mm.deserialize(msg("forge.fuel.stock_full").replace("%time%", formatTime(maxSeconds))));
            return false;
        }
        int itemsToConsume = Math.min(hand.getAmount(), Math.max(1, spaceLeft / fuel.getBurnTime()));
        int burnTime = fuel.getBurnTime() * itemsToConsume;

        // Consume from hand
        if (hand.getAmount() > itemsToConsume) {
            hand.setAmount(hand.getAmount() - itemsToConsume);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Create or stack deposit (capped at max)
        if (existing != null) {
            existing.addBurnTime(burnTime);
            if (existing.getRemainingBurnTime() > maxBurnTime) {
                existing.setRemainingBurnTime(maxBurnTime);
            }
            existing.updateFuel(fuel);
        } else {
            activeDeposits.put(forgeId, new FuelDeposit(fuel, Math.min(burnTime, maxBurnTime)));
        }

        int totalSeconds = activeDeposits.get(forgeId).getRemainingBurnTime() / 20;
        int maxSeconds = maxBurnTime / 20;
        player.sendMessage(mm.deserialize(msg("forge.fuel.deposited")
                .replace("%amount%", String.valueOf(itemsToConsume))
                .replace("%name%", fuel.getDisplayName())
                .replace("%current%", formatTime(totalSeconds))
                .replace("%max%", formatTime(maxSeconds))));

        playCollectEffect(player.getLocation());
        return true;
    }

    /**
     * Tries to collect more fuel from the zone when the current deposit runs out.
     * Silent version — no player messages.
     */
    public boolean tryCollectMoreFuel(UUID forgeId, List<Location> fuelZoneLocations) {
        if (fuelZoneLocations == null || fuelZoneLocations.isEmpty()) { return false; }

        FuelDeposit existing = activeDeposits.get(forgeId);
        if (existing != null && existing.getRemainingBurnTime() >= maxBurnTime) { return true; }

        boolean collected = false;

        for (Location zoneLoc : fuelZoneLocations) {
            World world = zoneLoc.getWorld();
            if (world == null) { continue; }

            Location center = zoneLoc.clone().add(0.5, 0.5, 0.5);
            Collection<Entity> nearby = world.getNearbyEntities(center, PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS);

            for (Entity entity : nearby) {
                if (!(entity instanceof Item itemEntity)) { continue; }
                if (!itemEntity.isValid() || itemEntity.isDead()) { continue; }

                ItemStack stack = itemEntity.getItemStack();
                ForgeFuel fuel = fuelRegistry.get(stack.getType());
                if (fuel == null) { continue; }

                existing = activeDeposits.get(forgeId);
                int currentBurn = existing != null ? existing.getRemainingBurnTime() : 0;
                int spaceLeft = maxBurnTime - currentBurn;
                if (spaceLeft <= 0) { break; }

                int itemsToConsume = Math.min(stack.getAmount(), Math.max(1, spaceLeft / fuel.getBurnTime()));
                int burnTime = fuel.getBurnTime() * itemsToConsume;

                if (stack.getAmount() > itemsToConsume) {
                    stack.setAmount(stack.getAmount() - itemsToConsume);
                    itemEntity.setItemStack(stack);
                } else {
                    itemEntity.remove();
                }

                if (existing != null) {
                    existing.addBurnTime(burnTime);
                    existing.updateFuel(fuel);
                } else {
                    existing = new FuelDeposit(fuel, burnTime);
                    activeDeposits.put(forgeId, existing);
                }

                playCollectEffect(center);
                collected = true;
            }
        }

        return collected || (existing != null && existing.getRemainingBurnTime() > 0);
    }

    /**
     * Gets the active fuel deposit for a forge, if any.
     */
    public FuelDeposit getDeposit(UUID forgeId) {
        return activeDeposits.get(forgeId);
    }

    /**
     * Checks if a forge has active fuel.
     */
    public boolean hasFuel(UUID forgeId) {
        FuelDeposit deposit = activeDeposits.get(forgeId);
        return deposit != null && deposit.getRemainingBurnTime() > 0;
    }

    /**
     * Checks if there are valid fuel items dropped in the fuel zone.
     */
    public boolean hasFuelInZone(List<Location> fuelZoneLocations) {
        if (fuelZoneLocations == null || fuelZoneLocations.isEmpty()) { return false; }

        for (Location zoneLoc : fuelZoneLocations) {
            World world = zoneLoc.getWorld();
            if (world == null) { continue; }

            Location center = zoneLoc.clone().add(0.5, 0.5, 0.5);
            Collection<Entity> nearby = world.getNearbyEntities(center, PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS);

            for (Entity entity : nearby) {
                if (!(entity instanceof Item itemEntity)) { continue; }
                if (!itemEntity.isValid() || itemEntity.isDead()) { continue; }

                if (fuelRegistry.containsKey(itemEntity.getItemStack().getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Consumes some burn time from the forge's fuel.
     * Called during the heating phase.
     *
     * @return true if fuel is still available
     */
    public boolean consumeFuel(UUID forgeId, int ticks) {
        FuelDeposit deposit = activeDeposits.get(forgeId);
        if (deposit == null) { return false; }
        deposit.consumeBurnTime(ticks);
        if (deposit.getRemainingBurnTime() <= 0) {
            activeDeposits.remove(forgeId);
            return false;
        }
        return true;
    }

    /**
     * Gets the heating power multiplier for the active fuel.
     */
    public double getHeatingPower(UUID forgeId) {
        FuelDeposit deposit = activeDeposits.get(forgeId);
        if (deposit == null) { return 0.5; } // No fuel = half speed
        return deposit.getFuel().getHeatingPower();
    }

    /**
     * Gets the quality bonus from fuel.
     */
    public double getQualityBonus(UUID forgeId) {
        FuelDeposit deposit = activeDeposits.get(forgeId);
        if (deposit == null) { return 0.0; }
        return deposit.getFuel().getQualityBonus();
    }

    /**
     * Clears fuel for a forge.
     */
    public void clearFuel(UUID forgeId) {
        activeDeposits.remove(forgeId);
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds >= 60) {
            int min = totalSeconds / 60;
            int sec = totalSeconds % 60;
            return min + "min " + sec + "s";
        }
        return totalSeconds + "s";
    }

    private void playCollectEffect(Location loc) {
        World world = loc.getWorld();
        if (world == null) { return; }

        world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.2f);
        world.spawnParticle(Particle.FLAME, loc, 8, 0.3, 0.2, 0.3, 0.02);
        world.spawnParticle(Particle.SMOKE, loc, 4, 0.1, 0.3, 0.1, 0.01);
    }

    /**
     * Tracks a single fuel deposit at a forge.
     */
    public static class FuelDeposit {
        private ForgeFuel fuel;
        private int remainingBurnTime;

        public FuelDeposit(ForgeFuel fuel, int burnTime) {
            this.fuel = fuel;
            this.remainingBurnTime = burnTime;
        }

        public ForgeFuel getFuel() { return fuel; }
        public int getRemainingBurnTime() { return remainingBurnTime; }

        public void addBurnTime(int ticks) {
            this.remainingBurnTime += ticks;
        }

        public void updateFuel(ForgeFuel fuel) {
            this.fuel = fuel;
        }

        public void setRemainingBurnTime(int ticks) {
            this.remainingBurnTime = ticks;
        }

        public void consumeBurnTime(int ticks) {
            this.remainingBurnTime = Math.max(0, this.remainingBurnTime - ticks);
        }
    }
}
