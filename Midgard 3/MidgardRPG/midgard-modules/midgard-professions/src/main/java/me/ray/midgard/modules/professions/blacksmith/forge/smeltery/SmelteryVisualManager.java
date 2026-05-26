package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia toda a visualização da Smeltery usando BlockDisplay entities.
 * <ul>
 *   <li>Nível de metal fundido no interior (camadas coloridas)</li>
 *   <li>Animação de despejo (pouring) do drain para table/basin</li>
 *   <li>Efeitos de partículas ambientes aprimorados</li>
 *   <li>Bolhas e brilho durante fundição ativa</li>
 * </ul>
 */
public class SmelteryVisualManager {

    private final JavaPlugin plugin;
    private final SmelteryRegistry registry;

    // Estado visual por smeltery
    private final Map<UUID, MetalDisplayState> displayStates = new ConcurrentHashMap<>();

    // Animações de despejo ativas
    private final Map<UUID, BlockDisplay> pouringDisplays = new ConcurrentHashMap<>();

    private BukkitTask updateTask;
    private BukkitTask particleTask;

    // Constantes visuais
    private static final float VIEW_RANGE = 0.4f;
    private static final int INTERPOLATION_TICKS = 8;
    private static final int UPDATE_INTERVAL = 20; // 1s
    private static final int PARTICLE_INTERVAL = 8; // 0.4s

    public SmelteryVisualManager(JavaPlugin plugin, SmelteryRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    // ── Ciclo de Vida ──

    public void initialize() {
        // Timer para atualizar displays de metal (a cada 1s)
        updateTask = Task.syncTimer(this::updateAllDisplays, UPDATE_INTERVAL, UPDATE_INTERVAL);

        // Timer para partículas ambientes (a cada 0.4s)
        particleTask = Task.syncTimer(this::tickParticles, PARTICLE_INTERVAL, PARTICLE_INTERVAL);

        MidgardLogger.info("[SmelteryVisual] Sistema visual inicializado.");
    }

    public void shutdown() {
        if (updateTask != null) { updateTask.cancel(); }
        if (particleTask != null) { particleTask.cancel(); }

        // Remover todas as entidades de display
        for (var state : displayStates.values()) {
            removeAllLayers(state);
        }
        displayStates.clear();

        // Remover animações de despejo
        for (BlockDisplay display : pouringDisplays.values()) {
            if (display != null && display.isValid()) { display.remove(); }
        }
        pouringDisplays.clear();
    }

    // ── Atualização do Display de Metal ──

    private void updateAllDisplays() {
        Set<UUID> activeIds = new HashSet<>();

        for (SmelteryStructure smeltery : registry.getAll()) {
            if (!smeltery.isActive()) { continue; }
            activeIds.add(smeltery.getSmelteryId());

            Location center = smeltery.getInteriorCenter();
            if (center == null || center.getWorld() == null) { continue; }

            Task.sync(center, () -> {
                try {
                    updateMetalDisplay(smeltery);
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao atualizar display visual da smeltery", e);
                }
            });
        }

        // Limpar displays de smelteries que não existem mais
        var it = displayStates.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!activeIds.contains(entry.getKey())) {
                removeAllLayers(entry.getValue());
                it.remove();
            }
        }
    }

    /**
     * Atualiza o display de metal fundido dentro da smeltery.
     * Cria camadas proporcionais ao volume de cada metal.
     */
    private void updateMetalDisplay(SmelteryStructure smeltery) {
        UUID id = smeltery.getSmelteryId();
        SmelteryTank tank = smeltery.getTank();
        SmelteryTier tier = smeltery.getTier();

        MetalDisplayState state = displayStates.computeIfAbsent(id, k -> new MetalDisplayState());

        if (tank.isEmpty()) {
            // Tanque vazio → remover todos os displays
            if (!state.layers.isEmpty()) {
                removeAllLayers(state);
            }
            return;
        }

        List<Map.Entry<MoltenMetal, Integer>> sorted = tank.getSortedContents();
        int totalVolume = tank.getTotalVolume();
        float fillPercent = tank.getFillPercent();
        double totalMetalHeight = tier.getInteriorHeight() * fillPercent;

        if (totalMetalHeight < 0.03) {
            removeAllLayers(state);
            return;
        }

        // Verificar se a composição de metais mudou
        List<MoltenMetal> currentMetals = new ArrayList<>();
        for (var entry : sorted) { currentMetals.add(entry.getKey()); }

        boolean metalsChanged = !currentMetals.equals(state.lastMetalOrder);
        boolean needsRebuild = metalsChanged || state.layers.isEmpty();

        World world = Bukkit.getWorld(smeltery.getWorldName());
        if (world == null) { return; }

        // Coordenadas do interior
        double ix = smeltery.getX() + 1;
        double iy = smeltery.getY() + 1;
        double iz = smeltery.getZ() + 1;
        int iw = tier.getInteriorWidth();
        int iDepth = tier.getInteriorDepth();

        if (needsRebuild) {
            // Reconstruir todas as camadas
            removeAllLayers(state);
            state.lastMetalOrder = new ArrayList<>(currentMetals);

            double currentY = iy;
            for (var entry : sorted) {
                MoltenMetal metal = entry.getKey();
                double fraction = (double) entry.getValue() / totalVolume;
                double layerHeight = Math.max(0.05, totalMetalHeight * fraction);

                Location spawnLoc = new Location(world, ix, currentY, iz);
                BlockDisplay display = spawnMetalLayer(spawnLoc, metal, iw, (float) layerHeight, iDepth);
                state.layers.add(new MetalLayer(metal, display, (float) layerHeight));
                currentY += layerHeight;
            }

            // Camada de brilho superior (superfície do metal)
            if (!sorted.isEmpty()) {
                MoltenMetal topMetal = sorted.get(0).getValue() >= (sorted.size() > 1 ? sorted.get(1).getValue() : 0)
                        ? sorted.get(0).getKey() : sorted.get(0).getKey();
                Location surfaceLoc = new Location(world, ix, currentY - 0.05, iz);
                BlockDisplay surfaceDisplay = spawnSurfaceGlow(surfaceLoc, topMetal, iw, iDepth);
                if (surfaceDisplay != null) {
                    state.surfaceGlow = surfaceDisplay;
                }
            }
        } else {
            // Apenas atualizar alturas com interpolação suave
            double currentY = 0;
            for (int i = 0; i < state.layers.size() && i < sorted.size(); i++) {
                MetalLayer layer = state.layers.get(i);
                double fraction = (double) sorted.get(i).getValue() / totalVolume;
                float newHeight = (float) Math.max(0.05, totalMetalHeight * fraction);

                if (Math.abs(layer.height - newHeight) > 0.01f) {
                    updateLayerHeight(layer, iw, newHeight, iDepth, (float) currentY);
                    layer.height = newHeight;
                }
                currentY += newHeight;
            }

            // Atualizar posição do brilho de superfície
            if (state.surfaceGlow != null && state.surfaceGlow.isValid()) {
                float surfaceY = (float) (iy + currentY - 0.05 - state.surfaceGlow.getLocation().getY());
                state.surfaceGlow.setTransformation(new Transformation(
                        new Vector3f(0, Math.max(0, surfaceY), 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(iw, 0.06f, iDepth),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                state.surfaceGlow.setInterpolationDelay(0);
                state.surfaceGlow.setInterpolationDuration(INTERPOLATION_TICKS);
            }
        }
    }

    private BlockDisplay spawnMetalLayer(Location loc, MoltenMetal metal, int width, float height, int depth) {
        World world = loc.getWorld();
        if (world == null) { return null; }

        return world.spawn(loc, BlockDisplay.class, entity -> {
            entity.setBlock(metal.getVisualBlock().createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(width, height, depth),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setViewRange(VIEW_RANGE);
            entity.setPersistent(false);
            entity.setGlowing(true);
            entity.setGlowColorOverride(metal.getColor());
            entity.setInterpolationDuration(INTERPOLATION_TICKS);
        });
    }

    private BlockDisplay spawnSurfaceGlow(Location loc, MoltenMetal metal, int width, int depth) {
        World world = loc.getWorld();
        if (world == null) { return null; }

        return world.spawn(loc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.MAGMA_BLOCK.createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(width, 0.06f, depth),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setViewRange(VIEW_RANGE);
            entity.setPersistent(false);
            entity.setGlowing(true);
            entity.setGlowColorOverride(metal.getColor());
            entity.setInterpolationDuration(INTERPOLATION_TICKS);
        });
    }

    private void updateLayerHeight(MetalLayer layer, int width, float newHeight, int depth, float yOffset) {
        BlockDisplay display = layer.display;
        if (display == null || !display.isValid()) { return; }

        display.setTransformation(new Transformation(
                new Vector3f(0, yOffset, 0),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(width, newHeight, depth),
                new AxisAngle4f(0, 0, 1, 0)
        ));
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
    }

    private void removeAllLayers(MetalDisplayState state) {
        for (MetalLayer layer : state.layers) {
            if (layer.display != null && layer.display.isValid()) {
                layer.display.remove();
            }
        }
        state.layers.clear();
        state.lastMetalOrder.clear();

        if (state.surfaceGlow != null && state.surfaceGlow.isValid()) {
            state.surfaceGlow.remove();
            state.surfaceGlow = null;
        }
    }

    // ── Animação de Despejo (Pouring) ──

    /**
     * Anima o despejo de metal do drain para a mesa/bacia.
     * Cria um BlockDisplay que desliza suavemente e cria partículas de gotejamento.
     */
    public void playPouringAnimation(Location drain, Location target, MoltenMetal metal) {
        if (drain == null || target == null || drain.getWorld() == null) { return; }

        World world = drain.getWorld();
        UUID animId = UUID.randomUUID();

        // Ponto de partida (saída do drain)
        Location start = drain.clone().add(0.5, 0.3, 0.5);

        // Criar display do "jato" de metal
        BlockDisplay pourDisplay = world.spawn(start, BlockDisplay.class, entity -> {
            entity.setBlock(metal.getVisualBlock().createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f(-0.1f, -0.1f, -0.1f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.2f, 0.2f, 0.2f),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setViewRange(VIEW_RANGE);
            entity.setPersistent(false);
            entity.setGlowing(true);
            entity.setGlowColorOverride(metal.getColor());
            entity.setInterpolationDuration(15);
        });

        pouringDisplays.put(animId, pourDisplay);

        // Animar para o target usando interpolação
        Location targetCenter = target.clone().add(0.5, 0.8, 0.5);
        float dx = (float) (targetCenter.getX() - start.getX());
        float dy = (float) (targetCenter.getY() - start.getY());
        float dz = (float) (targetCenter.getZ() - start.getZ());

        Task.syncLater(drain, () -> {
            if (pourDisplay.isValid()) {
                pourDisplay.setTransformation(new Transformation(
                        new Vector3f(dx - 0.1f, dy - 0.1f, dz - 0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 0.2f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                pourDisplay.setInterpolationDelay(0);
                pourDisplay.setInterpolationDuration(15);
            }
        }, 2L);

        // Partículas de gotejamento durante a animação
        Particle.DustOptions dustOpt = new Particle.DustOptions(metal.getColor(), 0.8f);
        for (int tick = 0; tick <= 15; tick++) {
            final int t = tick;
            Task.syncLater(drain, () -> {
                if (world.getPlayers().isEmpty()) { return; }
                double progress = t / 15.0;
                Location particleLoc = start.clone().add(
                        dx * progress, dy * progress, dz * progress);
                world.spawnParticle(Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, 0, dustOpt);
                world.spawnParticle(Particle.DRIPPING_LAVA, particleLoc, 1, 0.08, 0.08, 0.08, 0);
            }, 2L + t);
        }

        // Splash no destino + remoção
        Task.syncLater(drain, () -> {
            BlockDisplay display = pouringDisplays.remove(animId);
            if (display != null && display.isValid()) {
                display.remove();
            }

            // Efeito de splash no destino
            Location splash = target.clone().add(0.5, 1.0, 0.5);
            world.spawnParticle(Particle.DUST, splash, 12, 0.3, 0.2, 0.3, 0, dustOpt);
            world.spawnParticle(Particle.LAVA, splash, 4, 0.2, 0.1, 0.2, 0);
            world.spawnParticle(Particle.FLAME, splash, 6, 0.2, 0.3, 0.2, 0.02);
            world.playSound(splash, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.4f);
        }, 20L);
    }

    // ── Partículas Ambientes Aprimoradas ──

    private void tickParticles() {
        for (SmelteryStructure smeltery : registry.getAll()) {
            if (!smeltery.isActive()) { continue; }

            Location center = smeltery.getInteriorCenter();
            Location bottom = smeltery.getInteriorBottom();
            if (center == null || bottom == null) { continue; }
            World world = center.getWorld();
            if (world == null) { continue; }

            Task.sync(center, () -> {
                try {
                    tickSmelteryParticles(smeltery, world, center, bottom);
                } catch (Exception e) {
                    // silently ignore particle errors
                }
            });
        }
    }

    private void tickSmelteryParticles(SmelteryStructure smeltery, World world,
                                        Location center, Location bottom) {
        SmelteryTank tank = smeltery.getTank();
        SmelteryTier tier = smeltery.getTier();
        boolean heated = smeltery.isHeated();
        boolean hasMetal = !tank.isEmpty();
        boolean smelting = !smeltery.getSmeltingQueue().isEmpty() && heated;

        double halfW = tier.getInteriorWidth() / 2.0;
        double halfD = tier.getInteriorDepth() / 2.0;

        // ── Partículas de calor ──
        if (heated) {
            // Ondas de calor subindo das bordas
            for (int i = 0; i < 2; i++) {
                double rx = center.getX() + (Math.random() - 0.5) * halfW * 2;
                double rz = center.getZ() + (Math.random() - 0.5) * halfD * 2;
                Location heatLoc = new Location(world, rx, bottom.getY() + 0.2, rz);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, heatLoc, 0,
                        0, 0.05, 0, 0.01);
            }
        }

        // ── Bolhas na superfície do metal ──
        if (hasMetal && heated) {
            MoltenMetal dominant = tank.getDominantMetal();
            if (dominant != null) {
                float fillPercent = tank.getFillPercent();
                double surfaceY = bottom.getY() + tier.getInteriorHeight() * fillPercent;
                Particle.DustOptions metalDust = new Particle.DustOptions(dominant.getColor(), 1.2f);

                // Bolhas subindo pela superfície
                for (int i = 0; i < 3; i++) {
                    double rx = center.getX() + (Math.random() - 0.5) * halfW * 1.6;
                    double rz = center.getZ() + (Math.random() - 0.5) * halfD * 1.6;
                    Location bubbleLoc = new Location(world, rx, surfaceY, rz);
                    world.spawnParticle(Particle.DUST, bubbleLoc, 1,
                            0.05, 0.1, 0.05, 0, metalDust);
                }

                // Brilho sutil na superfície
                world.spawnParticle(Particle.SMALL_FLAME, new Location(world,
                                center.getX() + (Math.random() - 0.5) * halfW,
                                surfaceY + 0.1,
                                center.getZ() + (Math.random() - 0.5) * halfD),
                        1, 0.1, 0.02, 0.1, 0.005);
            }
        }

        // ── Fundição ativa: partículas mais intensas ──
        if (smelting) {
            // Faíscas subindo do fogo
            for (int i = 0; i < 4; i++) {
                double rx = center.getX() + (Math.random() - 0.5) * halfW * 1.5;
                double rz = center.getZ() + (Math.random() - 0.5) * halfD * 1.5;
                world.spawnParticle(Particle.LAVA, new Location(world, rx, bottom.getY() + 0.3, rz),
                        1, 0.1, 0.15, 0.1, 0);
            }

            // Fumaça densa subindo
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 1, 0),
                    2, halfW * 0.5, 0.2, halfD * 0.5, 0.01);

            // Crepitar do fogo (som sutil)
            if (Math.random() < 0.3) {
                world.playSound(bottom, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.3f, 0.7f);
            }
        }

        // ── Fogo na base (sempre que aquecido) ──
        if (heated) {
            world.spawnParticle(Particle.FLAME, bottom, 2,
                    halfW * 0.6, 0.05, halfD * 0.6, 0.01);
        }
    }

    // ── Efeitos Especiais ──

    /**
     * Efeito visual quando um item é colocado para fundir.
     */
    public void playItemInputEffect(SmelteryStructure smeltery, MoltenMetal metal) {
        Location center = smeltery.getInteriorCenter();
        if (center == null || center.getWorld() == null) { return; }

        World world = center.getWorld();
        Particle.DustOptions dustOpt = new Particle.DustOptions(metal.getColor(), 1.5f);

        // Explosão de partículas do item queimando
        world.spawnParticle(Particle.SMOKE, center, 15, 0.4, 0.6, 0.4, 0.05);
        world.spawnParticle(Particle.DUST, center, 10, 0.3, 0.5, 0.3, 0, dustOpt);
        world.spawnParticle(Particle.FLAME, center, 8, 0.3, 0.4, 0.3, 0.03);
        world.playSound(center, Sound.ENTITY_GENERIC_BURN, 0.8f, 0.6f);
        world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.5f);
    }

    /**
     * Efeito visual quando uma liga é formada automaticamente.
     */
    public void playAlloyFormationEffect(SmelteryStructure smeltery, MoltenMetal alloy) {
        Location center = smeltery.getInteriorCenter();
        if (center == null || center.getWorld() == null) { return; }

        World world = center.getWorld();
        Particle.DustOptions alloyDust = new Particle.DustOptions(alloy.getColor(), 2.0f);

        // Espiral ascendente de partículas da liga
        for (int tick = 0; tick < 20; tick++) {
            final int t = tick;
            Task.syncLater(center, () -> {
                double angle = t * Math.PI / 5;
                double radius = 0.5 + t * 0.03;
                double px = center.getX() + Math.cos(angle) * radius;
                double py = center.getY() - 0.5 + t * 0.08;
                double pz = center.getZ() + Math.sin(angle) * radius;
                Location spiralLoc = new Location(world, px, py, pz);
                world.spawnParticle(Particle.DUST, spiralLoc, 2, 0.05, 0.05, 0.05, 0, alloyDust);
                world.spawnParticle(Particle.ENCHANT, spiralLoc, 1, 0.1, 0.1, 0.1, 0.5);
            }, t);
        }

        // Som místico
        world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.3f);
        Task.syncLater(center, () -> {
            world.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 1.5f);
        }, 10L);
    }

    /**
     * Efeito visual quando combustível é adicionado.
     */
    public void playFuelAddedEffect(SmelteryStructure smeltery) {
        Location bottom = smeltery.getInteriorBottom();
        if (bottom == null || bottom.getWorld() == null) { return; }

        World world = bottom.getWorld();
        world.spawnParticle(Particle.FLAME, bottom, 20, 0.5, 0.2, 0.5, 0.04);
        world.spawnParticle(Particle.LAVA, bottom, 5, 0.3, 0.1, 0.3, 0);
    }

    // ── Chamado quando smeltery é removida/desativada ──

    public void removeDisplays(UUID smelteryId) {
        MetalDisplayState state = displayStates.remove(smelteryId);
        if (state != null) {
            removeAllLayers(state);
        }
    }

    // ── Classes internas ──

    private static class MetalDisplayState {
        final List<MetalLayer> layers = new ArrayList<>();
        List<MoltenMetal> lastMetalOrder = new ArrayList<>();
        BlockDisplay surfaceGlow;
    }

    private static class MetalLayer {
        final MoltenMetal metal;
        final BlockDisplay display;
        float height;

        MetalLayer(MoltenMetal metal, BlockDisplay display, float height) {
            this.metal = metal;
            this.display = display;
            this.height = height;
        }
    }
}
