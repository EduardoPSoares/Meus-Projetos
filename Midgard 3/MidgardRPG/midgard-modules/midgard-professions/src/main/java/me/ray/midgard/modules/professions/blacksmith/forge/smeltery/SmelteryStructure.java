package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

/**
 * Representa uma Smeltery construída e ativa no mundo.
 * Estrutura multibloco estilo Tinkers' Construct:
 * - Base de nether bricks com lava embaixo
 * - Paredes de nether bricks formando caixa
 * - Controller (blast furnace) em uma parede
 * - Drain (hopper) para escoar metal
 * - Interior aberto onde o metal funde visualmente
 */
public class SmelteryStructure {

    private final UUID smelteryId;
    private final UUID ownerUuid;
    private final String worldName;
    private final int x, y, z; // canto inferior-frontal-esquerdo
    private final SmelteryTier tier;
    private final long createdAt;

    private String name;
    private boolean active;
    private long lastUsed;
    private int totalItemsSmelted;

    // Tanque de metais fundidos
    private final SmelteryTank tank;

    // Localizações dos blocos interativos (cacheados)
    private transient Map<SmelteryBlockType, List<Location>> interactiveLocations;

    // Items na fila de fundição (stack de itens jogados dentro)
    private final List<SmeltingEntry> smeltingQueue;

    // Estado térmico
    private boolean heated; // se tem fuel/lava embaixo
    private int fuelRemaining; // ticks de fuel restante

    public SmelteryStructure(UUID smelteryId, UUID ownerUuid, String worldName,
                              int x, int y, int z, SmelteryTier tier) {
        this.smelteryId = smelteryId;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.createdAt = System.currentTimeMillis();
        this.lastUsed = createdAt;
        this.active = true;
        this.name = tier.getName();
        this.tank = new SmelteryTank(tier.getTankCapacity());
        this.smeltingQueue = new ArrayList<>();
        this.interactiveLocations = new HashMap<>();
    }

    // Full constructor para carregar do DB
    public SmelteryStructure(UUID smelteryId, UUID ownerUuid, String worldName,
                              int x, int y, int z, SmelteryTier tier,
                              long createdAt, long lastUsed, int totalItemsSmelted,
                              boolean active, String name) {
        this.smelteryId = smelteryId;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.createdAt = createdAt;
        this.lastUsed = lastUsed;
        this.totalItemsSmelted = totalItemsSmelted;
        this.active = active;
        this.name = name != null ? name : tier.getName();
        this.tank = new SmelteryTank(tier.getTankCapacity());
        this.smeltingQueue = new ArrayList<>();
        this.interactiveLocations = new HashMap<>();
    }

    // ── Getters ──

    public UUID getSmelteryId() { return smelteryId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public SmelteryTier getTier() { return tier; }
    public long getCreatedAt() { return createdAt; }
    public long getLastUsed() { return lastUsed; }
    public int getTotalItemsSmelted() { return totalItemsSmelted; }
    public boolean isActive() { return active; }
    public String getName() { return name; }
    public SmelteryTank getTank() { return tank; }
    public List<SmeltingEntry> getSmeltingQueue() { return smeltingQueue; }
    public boolean isHeated() { return heated; }
    public int getFuelRemaining() { return fuelRemaining; }

    public void setActive(boolean active) { this.active = active; }
    public void setName(String name) { this.name = name; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
    public void setHeated(boolean heated) { this.heated = heated; }
    public void setFuelRemaining(int fuel) { this.fuelRemaining = fuel; }
    public void incrementItemsSmelted() { this.totalItemsSmelted++; }

    public Map<SmelteryBlockType, List<Location>> getInteractiveLocations() {
        return interactiveLocations;
    }

    public Location getAnchorLocation() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) { return null; }
        return new Location(world, x, y, z);
    }

    // ── Fila de Fundição ──

    /**
     * Adiciona item à fila de fundição.
     */
    public boolean addToSmeltingQueue(Material material, int amount, SmeltingRecipeManager recipeManager) {
        SmeltingRecipe recipe = recipeManager.getRecipe(material);
        if (recipe == null) { return false; }

        // Verificar se cabe no tanque
        int volumeToAdd = recipe.getOutputAmount() * amount;
        if (tank.getFreeSpace() < volumeToAdd) { return false; }

        smeltingQueue.add(new SmeltingEntry(material, recipe.getOutput(),
                amount, recipe.getSmeltTime(), recipe.getSmeltTime()));
        return true;
    }

    /**
     * Processa a fila de fundição (chamado periodicamente).
     * Requer que a smeltery esteja aquecida.
     * @param tickAmount quantidade de ticks a avançar (igual ao intervalo do timer)
     */
    public List<SmeltingResult> tickSmelting(int tickAmount, SmeltingRecipeManager smeltingRecipes, AlloyRecipeManager alloyRecipes) {
        List<SmeltingResult> results = new ArrayList<>();
        if (!heated || !active) { return results; }

        Iterator<SmeltingEntry> it = smeltingQueue.iterator();
        while (it.hasNext()) {
            SmeltingEntry entry = it.next();
            entry.tick(tickAmount);

            if (entry.isComplete()) {
                SmeltingRecipe recipe = smeltingRecipes.getRecipe(entry.getSourceMaterial());
                if (recipe != null) {
                    int added = tank.addMetal(entry.getOutputMetal(), recipe.getOutputAmount());
                    if (added > 0) {
                        entry.decrementRemaining();
                        totalItemsSmelted++;
                        results.add(new SmeltingResult(entry.getOutputMetal(), added));
                        entry.resetProgress();
                    }
                }
                if (entry.getRemainingItems() <= 0) {
                    it.remove();
                }
            }
        }

        // Processar ligas automáticas
        List<SmelteryTank.AlloyResult> alloys = tank.processAlloys(alloyRecipes);
        for (var alloy : alloys) {
            results.add(new SmeltingResult(alloy.recipe().getResult(), alloy.totalProduced()));
        }

        return results;
    }

    /**
     * Consome fuel a cada tick.
     */
    public void consumeFuel(int ticks) {
        fuelRemaining = Math.max(0, fuelRemaining - ticks);
        if (fuelRemaining <= 0) {
            heated = false;
        }
    }

    /**
     * Adiciona fuel à smeltery.
     */
    public void addFuel(int ticks) {
        fuelRemaining += ticks;
        heated = true;
        // Temperatura sobe gradualmente quando aquecida
        int targetTemp = tier.getMaxTemperature();
        int currentTemp = tank.getTemperature();
        if (currentTemp < targetTemp) {
            tank.setTemperature(Math.min(targetTemp, currentTemp + 50));
        }
    }

    // ── Validação do Multibloco ──

    /**
     * Valida se a estrutura multibloco está correta no mundo.
     */
    public boolean validateStructure() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) { return false; }

        int tw = tier.getTotalWidth();
        int th = tier.getTotalHeight();
        int td = tier.getTotalDepth();

        boolean hasController = false;
        boolean hasDrain = false;
        interactiveLocations.clear();

        for (int dx = 0; dx < tw; dx++) {
            for (int dy = 0; dy < th; dy++) {
                for (int dz = 0; dz < td; dz++) {
                    Location loc = new Location(world, x + dx, y + dy, z + dz);
                    Block block = loc.getBlock();
                    boolean isEdge = dx == 0 || dx == tw - 1 || dz == 0 || dz == td - 1;
                    boolean isBottom = dy == 0;
                    boolean isTop = dy == th - 1;
                    boolean isInterior = !isEdge && !isBottom && !isTop;

                    if (isInterior) {
                        // Interior deve ser ar
                        if (!block.getType().isAir()) { return false; }
                        continue;
                    }

                    Material mat = block.getType();

                    // Checar blocos interativos
                    if (mat == SmelteryBlockType.CONTROLLER.getDefaultMaterial()) {
                        hasController = true;
                        addInteractiveLocation(SmelteryBlockType.CONTROLLER, loc);
                    } else if (mat == SmelteryBlockType.DRAIN.getDefaultMaterial() && isEdge && !isBottom && !isTop) {
                        hasDrain = true;
                        addInteractiveLocation(SmelteryBlockType.DRAIN, loc);
                    } else if (mat == SmelteryBlockType.ITEM_INPUT.getDefaultMaterial()) {
                        addInteractiveLocation(SmelteryBlockType.ITEM_INPUT, loc);
                    } else if (mat == SmelteryBlockType.FUEL_INPUT.getDefaultMaterial() && isBottom) {
                        addInteractiveLocation(SmelteryBlockType.FUEL_INPUT, loc);
                    } else if (mat == SmelteryBlockType.CASTING_TABLE.getDefaultMaterial()) {
                        addInteractiveLocation(SmelteryBlockType.CASTING_TABLE, loc);
                    } else if (mat == SmelteryBlockType.CASTING_BASIN.getDefaultMaterial()) {
                        addInteractiveLocation(SmelteryBlockType.CASTING_BASIN, loc);
                    } else if (mat == SmelteryBlockType.TANK_WINDOW.getDefaultMaterial()) {
                        addInteractiveLocation(SmelteryBlockType.TANK_WINDOW, loc);
                    } else if (isWallMaterial(mat)) {
                        // Bloco de parede válido
                    } else if (isBottom && mat == Material.LAVA) {
                        // Lava na base é válida como fonte de calor infinita
                        heated = true;
                        fuelRemaining = Math.max(fuelRemaining, 32000);
                    } else {
                        // Bloco inválido na estrutura
                        return false;
                    }
                }
            }
        }

        return hasController && hasDrain;
    }

    private boolean isWallMaterial(Material mat) {
        return mat == Material.NETHER_BRICKS ||
                mat == Material.RED_NETHER_BRICKS ||
                mat == Material.CHISELED_NETHER_BRICKS ||
                mat == Material.CRACKED_NETHER_BRICKS ||
                mat == Material.NETHER_BRICK_SLAB ||
                mat == Material.TINTED_GLASS ||
                mat == Material.POLISHED_BLACKSTONE_BRICKS ||
                mat == Material.POLISHED_BLACKSTONE;
    }

    private void addInteractiveLocation(SmelteryBlockType type, Location loc) {
        interactiveLocations.computeIfAbsent(type, k -> new ArrayList<>()).add(loc);
    }

    /**
     * Verifica se uma localização está dentro desta smeltery.
     */
    public boolean containsLocation(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) { return false; }
        int tw = tier.getTotalWidth();
        int th = tier.getTotalHeight();
        int td = tier.getTotalDepth();
        int lx = loc.getBlockX();
        int ly = loc.getBlockY();
        int lz = loc.getBlockZ();
        return lx >= x && lx < x + tw &&
                ly >= y && ly < y + th &&
                lz >= z && lz < z + td;
    }

    /**
     * Detecta o tipo de bloco interativo nessa posição.
     */
    public SmelteryBlockType getBlockTypeAt(Location loc) {
        for (var entry : interactiveLocations.entrySet()) {
            for (Location interLoc : entry.getValue()) {
                if (interLoc.getBlockX() == loc.getBlockX() &&
                        interLoc.getBlockY() == loc.getBlockY() &&
                        interLoc.getBlockZ() == loc.getBlockZ()) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Retorna a localização central do interior (usado para partículas/displays).
     */
    public Location getInteriorCenter() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) { return null; }
        return new Location(world,
                x + tier.getTotalWidth() / 2.0,
                y + 1 + tier.getInteriorHeight() / 2.0,
                z + tier.getTotalDepth() / 2.0);
    }

    /**
     * Retorna a localização do fundo do interior (para partículas de lava/fire).
     */
    public Location getInteriorBottom() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) { return null; }
        return new Location(world,
                x + tier.getTotalWidth() / 2.0,
                y + 1.0,
                z + tier.getTotalDepth() / 2.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        SmelteryStructure that = (SmelteryStructure) o;
        return smelteryId.equals(that.smelteryId);
    }

    @Override
    public int hashCode() { return smelteryId.hashCode(); }

    // ── Classes internas ──

    /**
     * Entry na fila de fundição.
     */
    public static class SmeltingEntry {
        private final Material sourceMaterial;
        private final MoltenMetal outputMetal;
        private int remainingItems;
        private final int smeltTimePerItem; // ticks
        private int currentProgress; // ticks decorridos

        public SmeltingEntry(Material source, MoltenMetal output,
                             int amount, int smeltTime, int smeltTimePerItem) {
            this.sourceMaterial = source;
            this.outputMetal = output;
            this.remainingItems = amount;
            this.smeltTimePerItem = smeltTimePerItem;
            this.currentProgress = 0;
        }

        public Material getSourceMaterial() { return sourceMaterial; }
        public MoltenMetal getOutputMetal() { return outputMetal; }
        public int getRemainingItems() { return remainingItems; }
        public int getSmeltTimePerItem() { return smeltTimePerItem; }
        public int getCurrentProgress() { return currentProgress; }

        public float getProgressPercent() {
            if (smeltTimePerItem <= 0) { return 1.0f; }
            return (float) currentProgress / smeltTimePerItem;
        }

        public void tick(int amount) { currentProgress += amount; }
        public boolean isComplete() { return currentProgress >= smeltTimePerItem; }
        public void decrementRemaining() { remainingItems--; }
        public void resetProgress() { currentProgress = 0; }
    }

    /**
     * Resultado de uma fundição completada.
     */
    public record SmeltingResult(MoltenMetal metal, int amountProduced) {}
}
