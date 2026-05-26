package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.event.AlloyFormedEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.event.SmelteryActivateEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.event.SmeltingCompleteEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.fuel.ForgeFuel;
import me.ray.midgard.modules.professions.blacksmith.forge.fuel.FuelManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui.DrainSelectGui;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui.SmelteryGui;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central do sistema de Smeltery.
 * Controla registro, ticking, fundição, efeitos visuais e interação com jogadores.
 * Inspirado no Tinkers' Construct.
 */
public class SmelteryManager {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private final ProfessionsModule module;
    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final SmelteryRegistry registry;
    private final SmeltingRecipeManager smeltingRecipeManager;
    private final AlloyRecipeManager alloyRecipeManager;
    private SmelteryRepository repository;
    private SmelteryVisualManager visualManager;
    private BukkitTask tickTask;

    // BossBars ativos para jogadores visualizando smelteries
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    // Jogadores atualmente visualizando uma smeltery (UUID jogador → UUID smeltery)
    private final Map<UUID, UUID> viewingPlayers = new ConcurrentHashMap<>();

    // Auto-pour: drain Location serializada → task ativo
    private final Map<String, BukkitTask> autoPourTasks = new ConcurrentHashMap<>();

    public SmelteryManager(ProfessionsModule module) {
        this.module = module;
        this.plugin = module.getPlugin();
        this.registry = new SmelteryRegistry();
        this.smeltingRecipeManager = new SmeltingRecipeManager();
        this.alloyRecipeManager = new AlloyRecipeManager();
    }

    // ── Ciclo de Vida ──

    public void initialize() {
        MidgardLogger.info("Inicializando sistema de Fundição (Smeltery)...");

        // Inicializar repositório (sem carregar do banco ainda — mundo não acessível no onEnable do Folia)
        DatabaseManager dbManager = MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new SmelteryRepository(dbManager);
        } else {
            MidgardLogger.warn("DatabaseManager não disponível — smelteries não serão persistidas.");
        }

        // Load smeltery recipes from config
        loadSmelteryRecipes();

        // Tick de processamento: a cada 10 ticks (0.5s)
        tickTask = Task.syncTimer(this::tickAllSmelteries, 10L, 10L);

        // Sistema visual com BlockDisplay e partículas
        this.visualManager = new SmelteryVisualManager(plugin, registry);
        this.visualManager.initialize();

        // Carregar smelteries do banco e agendar validação na region thread correta de cada uma.
        // No Folia, acessar blocos exige estar na region thread da localização.
        if (repository != null) {
            try {
                List<SmelteryStructure> loaded = repository.loadAllSmelteries();
                MidgardLogger.info("Encontradas %d smelteries no banco. Agendando validação por região...", loaded.size());
                for (SmelteryStructure smeltery : loaded) {
                    Location anchor = smeltery.getAnchorLocation();
                    if (anchor == null || anchor.getWorld() == null) {
                        MidgardLogger.warn("Smeltery %s — mundo '%s' não encontrado, ignorando.",
                                smeltery.getSmelteryId(), smeltery.getWorldName());
                        continue;
                    }
                    // Agendar validação na region thread da localização da smeltery
                    Task.syncLater(anchor, () -> {
                        try {
                            if (smeltery.validateStructure()) {
                                registry.register(smeltery);
                                MidgardLogger.debug("Smeltery %s validada e registrada.", smeltery.getSmelteryId());
                            } else {
                                MidgardLogger.warn("Smeltery %s falhou na validação de estrutura, ignorando.", smeltery.getSmelteryId());
                            }
                        } catch (Exception e) {
                            MidgardLogger.error("Erro ao validar smeltery %s", smeltery.getSmelteryId(), e);
                        }
                    }, 1L);
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar smelteries do banco de dados", e);
            }
        }

        MidgardLogger.info("Sistema de Fundição inicializado! Smelteries serão validadas no próximo tick.");
    }

    public void shutdown() {
        MidgardLogger.info("Desligando sistema de Fundição...");
        if (tickTask != null) { tickTask.cancel(); }
        if (visualManager != null) { visualManager.shutdown(); }

        // Parar todos os auto-pours
        for (BukkitTask task : autoPourTasks.values()) {
            task.cancel();
        }
        autoPourTasks.clear();

        // Esconder todos os BossBars
        for (var entry : activeBossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) { p.hideBossBar(entry.getValue()); }
        }
        activeBossBars.clear();
        viewingPlayers.clear();

        // Salvar todas as smelteries no banco antes de limpar
        if (repository != null) {
            repository.saveAll(registry.getAll());
            MidgardLogger.info("Salvas " + registry.size() + " smelteries no banco de dados.");
        }

        registry.clear();
    }

    // ── Tick Principal ──

    private void tickAllSmelteries() {
        for (SmelteryStructure smeltery : registry.getAll()) {
            if (!smeltery.isActive()) { continue; }

            // Consumir fuel e aquecer gradualmente
            if (smeltery.isHeated()) {
                smeltery.consumeFuel(10);

                // Temperatura sobe gradualmente enquanto aquecida (+10°C a cada 0.5s = 20°C/s)
                int currentTemp = smeltery.getTank().getTemperature();
                int maxTemp = smeltery.getTier().getMaxTemperature();
                if (currentTemp < maxTemp) {
                    smeltery.getTank().setTemperature(Math.min(maxTemp, currentTemp + 10));
                }
            }

            // Processar fundição
            if (smeltery.isHeated() && !smeltery.getSmeltingQueue().isEmpty()) {
                List<SmelteryStructure.SmeltingResult> results = smeltery.tickSmelting(10, smeltingRecipeManager, alloyRecipeManager);

                // Notificar jogadores próximos sobre resultados
                for (var result : results) {
                    notifyNearbyPlayers(smeltery, result);

                    // Fire SmeltingCompleteEvent
                    Bukkit.getPluginManager().callEvent(
                            new SmeltingCompleteEvent(smeltery, result.metal(), result.amountProduced()));

                    // Efeito visual especial para ligas formadas
                    if (visualManager != null && result.metal().isAlloy()) {
                        // Fire AlloyFormedEvent
                        Bukkit.getPluginManager().callEvent(
                                new AlloyFormedEvent(smeltery, result.metal(), result.amountProduced()));

                        Location center = smeltery.getInteriorCenter();
                        if (center != null && center.getWorld() != null) {
                            Task.sync(center, () ->
                                    visualManager.playAlloyFormationEffect(smeltery, result.metal()));
                        }
                    }
                }
            }

            // Resfriar gradualmente se sem fuel
            if (!smeltery.isHeated() && smeltery.getTank().getTemperature() > 0) {
                int temp = smeltery.getTank().getTemperature();
                smeltery.getTank().setTemperature(temp - 5);
            }

            // Atualizar BossBars de jogadores visualizando
            updateBossBarsForSmeltery(smeltery);
        }
    }

    // ── Interação do Jogador ──

    /**
     * Jogador clicou no Controller da smeltery → Abre GUI principal.
     */
    public void onControllerInteract(Player player, SmelteryStructure smeltery) {
        smeltery.setLastUsed(System.currentTimeMillis());
        SmelteryGui gui = new SmelteryGui(this, player, smeltery);
        gui.open();
    }

    /**
     * Jogador jogou/colocou item no Item Input da smeltery.
     */
    public void onItemInput(Player player, SmelteryStructure smeltery, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) { return; }

        SmeltingRecipe recipe = smeltingRecipeManager.getRecipe(item.getType());
        if (recipe == null) {
            player.sendMessage(mm.deserialize(msg("smeltery.input.invalid_item")));
            return;
        }

        // Verificar temperatura mínima
        if (smeltery.getTank().getTemperature() < recipe.getMinTemperature()) {
            player.sendMessage(mm.deserialize(msg("smeltery.input.temp_too_low")
                    .replace("%temp%", String.valueOf(recipe.getMinTemperature()))
                    .replace("%current%", String.valueOf(smeltery.getTank().getTemperature()))));
            return;
        }

        // Verificar espaço no tanque
        int volumeNeeded = recipe.getOutputAmount() * item.getAmount();
        if (smeltery.getTank().getFreeSpace() < volumeNeeded) {
            player.sendMessage(mm.deserialize(msg("smeltery.input.tank_full")
                    .replace("%needed%", String.valueOf(volumeNeeded))
                    .replace("%free%", String.valueOf(smeltery.getTank().getFreeSpace()))));
            return;
        }

        int amount = item.getAmount();
        if (smeltery.addToSmeltingQueue(item.getType(), amount, smeltingRecipeManager)) {
            item.setAmount(0);
            player.sendMessage(mm.deserialize(msg("smeltery.input.smelting_started")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%name%", recipe.getOutput().getDisplayName())));
            player.sendMessage(mm.deserialize(msg("smeltery.input.smelting_time")
                    .replace("%time%", String.format("%.1f", (recipe.getSmeltTime() * amount) / 20.0))));

            // Efeito sonoro e visual
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.2f);
            if (visualManager != null) {
                visualManager.playItemInputEffect(smeltery, recipe.getOutput());
            } else {
                playInputEffect(smeltery);
            }
        }
    }

    /**
     * Jogador depositou fuel no Fuel Input.
     */
    public void onFuelInput(Player player, SmelteryStructure smeltery, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) { return; }

        int fuelTicks = getFuelTicks(item.getType());
        if (fuelTicks <= 0) {
            player.sendMessage(mm.deserialize(msg("smeltery.fuel.invalid")));
            return;
        }

        smeltery.addFuel(fuelTicks);
        boolean isLavaBucket = item.getType() == Material.LAVA_BUCKET;
        item.setAmount(item.getAmount() - 1);
        // Atualizar o item real no inventário do jogador
        if (isLavaBucket) {
            // Lava bucket retorna balde vazio
            player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
        } else {
            player.getInventory().setItemInMainHand(item.getAmount() > 0 ? item : null);
        }

        player.sendMessage(mm.deserialize(msg("smeltery.fuel.added").replace("%seconds%", String.valueOf(fuelTicks / 20))));
        player.sendMessage(mm.deserialize(msg("smeltery.fuel.temperature").replace("%temp%", String.valueOf(smeltery.getTank().getTemperature()))));

        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 1.0f, 0.8f);

        if (visualManager != null) {
            visualManager.playFuelAddedEffect(smeltery);
        }
    }

    /**
     * Jogador clicou no Drain → Abre menu de seleção de metal.
     */
    public void onDrainInteract(Player player, SmelteryStructure smeltery, Location drainLoc) {
        SmelteryTank tank = smeltery.getTank();
        if (tank.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("smeltery.drain.tank_empty")));
            return;
        }

        new DrainSelectGui(this, player, smeltery, drainLoc).open();
    }

    // ── Auto-Pour (Lever/Alavanca) ──

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Ativa/desativa o despejo automático de um drain via alavanca.
     * Quando ativo, despeja o metal dominante a cada 40 ticks (2s).
     */
    public void toggleAutoPour(Player player, SmelteryStructure smeltery, Location drainLoc, boolean leverOn) {
        String key = locKey(drainLoc);

        if (!leverOn) {
            stopAutoPour(drainLoc);
            player.sendMessage(mm.deserialize(msg("smeltery.autopour.disabled")));
            player.playSound(drainLoc, Sound.BLOCK_LEVER_CLICK, 0.8f, 0.8f);
            return;
        }

        SmelteryTank tank = smeltery.getTank();
        if (tank.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("smeltery.drain.tank_empty")));
            return;
        }

        // Já tem auto-pour ativo nesse drain
        if (autoPourTasks.containsKey(key)) {
            return;
        }

        player.sendMessage(mm.deserialize(msg("smeltery.autopour.enabled")));
        player.playSound(drainLoc, Sound.BLOCK_LEVER_CLICK, 0.8f, 1.2f);

        BukkitTask task = Task.syncTimer(drainLoc, () -> tickAutoPour(smeltery, drainLoc), 20L, 40L);
        autoPourTasks.put(key, task);
    }

    public void stopAutoPour(Location drainLoc) {
        String key = locKey(drainLoc);
        BukkitTask task = autoPourTasks.remove(key);
        if (task != null) { task.cancel(); }
    }

    public boolean isAutoPourActive(Location drainLoc) {
        return autoPourTasks.containsKey(locKey(drainLoc));
    }

    private void tickAutoPour(SmelteryStructure smeltery, Location drainLoc) {
        // Verificar se a alavanca adjacente ainda está ligada
        if (!isLeverPoweredNear(drainLoc)) {
            stopAutoPour(drainLoc);
            return;
        }

        // Verificar smeltery ativa
        if (!smeltery.isActive()) {
            stopAutoPour(drainLoc);
            return;
        }

        SmelteryTank tank = smeltery.getTank();
        if (tank.isEmpty()) {
            stopAutoPour(drainLoc);
            notifyNearbyPlayers(smeltery, msg("smeltery.drain.tank_emptied"));
            return;
        }

        MoltenMetal metal = tank.getDominantMetal();
        if (metal == null) {
            stopAutoPour(drainLoc);
            return;
        }

        // Tentar mesa primeiro (lingote), depois bacia (bloco)
        Location targetTable = findAdjacentCastingBlock(drainLoc, SmelteryBlockType.CASTING_TABLE);
        Location targetBasin = findAdjacentCastingBlock(drainLoc, SmelteryBlockType.CASTING_BASIN);

        boolean poured = false;

        if (targetTable != null && tank.getAmount(metal) >= 144) {
            tank.removeMetal(metal, 144);
            ItemStack ingot = SmelteryOutputItem.createIngot(metal);
            dropOrStore(drainLoc, ingot);
            playAutoPourEffects(drainLoc, targetTable, metal);
            poured = true;
        } else if (targetBasin != null && tank.getAmount(metal) >= 1296) {
            tank.removeMetal(metal, 1296);
            ItemStack block = SmelteryOutputItem.createBlock(metal);
            dropOrStore(drainLoc, block);
            playAutoPourEffects(drainLoc, targetBasin, metal);
            poured = true;
        }

        if (!poured) {
            // Sem mesa/bacia ou metal insuficiente para qualquer output
            stopAutoPour(drainLoc);
            notifyNearbyPlayers(smeltery, msg("smeltery.drain.tank_emptied"));
        }
    }

    private void dropOrStore(Location drainLoc, ItemStack item) {
        // Dropar o item no chão ao lado do drain (perto da mesa/bacia)
        World world = drainLoc.getWorld();
        if (world != null) {
            Location dropLoc = drainLoc.clone().add(0.5, 0, 0.5);
            world.dropItemNaturally(dropLoc, item);
        }
    }

    private void playAutoPourEffects(Location drain, Location target, MoltenMetal metal) {
        if (visualManager != null) {
            visualManager.playPouringAnimation(drain, target, metal);
        }
        World world = drain.getWorld();
        if (world != null) {
            world.playSound(drain, Sound.BLOCK_LAVA_POP, 0.6f, 0.5f);
        }
    }

    /**
     * Verifica se há uma alavanca ligada adjacente ao drain.
     */
    public boolean isLeverPoweredNear(Location drainLoc) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = drainLoc.clone().add(dx, dy, dz).getBlock();
                    if (block.getType() == Material.LEVER) {
                        if (block.getBlockData() instanceof Switch sw && sw.isPowered()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Encontra a localização do drain mais próximo de uma alavanca.
     */
    public Location findDrainNearLever(Location leverLoc, SmelteryStructure smeltery) {
        Map<SmelteryBlockType, List<Location>> interactives = smeltery.getInteractiveLocations();
        List<Location> drains = interactives.get(SmelteryBlockType.DRAIN);
        if (drains == null) { return null; }

        Location closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Location drain : drains) {
            double dist = leverLoc.distanceSquared(drain.clone().add(0.5, 0.5, 0.5));
            if (dist <= 4.0 && dist < closestDist) { // <= 2 blocos
                closestDist = dist;
                closest = drain;
            }
        }
        return closest;
    }

    private void notifyNearbyPlayers(SmelteryStructure smeltery, String message) {
        Location center = smeltery.getInteriorCenter();
        if (center == null || center.getWorld() == null) { return; }
        Task.sync(center, () -> {
            for (Player p : center.getWorld().getNearbyPlayers(center, 15)) {
                p.sendMessage(mm.deserialize(message));
            }
        });
    }

    // ── Fuel ──

    private int getFuelTicks(Material material) {
        FuelManager fuelManager = module.getForgeManager() != null
                ? module.getForgeManager().getFuelManager() : null;
        if (fuelManager != null) {
            ForgeFuel fuel = fuelManager.getFuel(material);
            if (fuel != null) {
                return fuel.getBurnTime();
            }
        }
        return 0;
    }

    // ── Efeitos Visuais ──

    private void playInputEffect(SmelteryStructure smeltery) {
        Location center = smeltery.getInteriorCenter();
        if (center == null) { return; }
        World world = center.getWorld();
        world.spawnParticle(Particle.SMOKE, center, 10, 0.3, 0.5, 0.3, 0.05);
        world.playSound(center, Sound.ENTITY_GENERIC_BURN, 0.8f, 0.6f);
    }

    /**
     * Escaneia blocos próximos ao drain procurando casting table ou basin no mundo.
     * Casting tables/basins ficam FORA da smeltery, ao lado do drain.
     * Raio de busca: 3 blocos (mesmo que o findAdjacentCasting original).
     */
    public Location findAdjacentCastingBlock(Location drain, SmelteryBlockType type) {
        if (drain == null || drain.getWorld() == null) { return null; }

        Location closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) { continue; }
                    Location check = drain.clone().add(dx, dy, dz);
                    if (isCastingBlockType(check.getBlock().getType(), type)) {
                        double dist = drain.distanceSquared(check);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = check;
                        }
                    }
                }
            }
        }
        return closest;
    }

    private boolean isCastingBlockType(Material mat, SmelteryBlockType type) {
        if (type == SmelteryBlockType.CASTING_TABLE) {
            return mat == Material.SMOOTH_STONE_SLAB
                    || mat == Material.STONE_SLAB
                    || mat == Material.COBBLESTONE_SLAB
                    || mat == Material.POLISHED_DEEPSLATE_SLAB;
        } else if (type == SmelteryBlockType.CASTING_BASIN) {
            return mat == Material.CAULDRON
                    || mat == Material.WATER_CAULDRON
                    || mat == Material.LAVA_CAULDRON
                    || mat == Material.POWDER_SNOW_CAULDRON;
        }
        return mat == type.getDefaultMaterial();
    }

    // ── BossBars ──

    /**
     * Mostra BossBar de status da smeltery para um jogador.
     */
    public void showStatusBar(Player player, SmelteryStructure smeltery) {
        hideBossBar(player);

        BossBar bar = BossBar.bossBar(
                mm.deserialize(buildBossBarText(smeltery)),
                smeltery.getTank().getFillPercent(),
                smeltery.isHeated() ? BossBar.Color.RED : BossBar.Color.BLUE,
                BossBar.Overlay.NOTCHED_10
        );

        player.showBossBar(bar);
        activeBossBars.put(player.getUniqueId(), bar);
        viewingPlayers.put(player.getUniqueId(), smeltery.getSmelteryId());
    }

    public void hideBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) { player.hideBossBar(bar); }
        viewingPlayers.remove(player.getUniqueId());
    }

    private void updateBossBarsForSmeltery(SmelteryStructure smeltery) {
        for (var entry : viewingPlayers.entrySet()) {
            if (!entry.getValue().equals(smeltery.getSmelteryId())) { continue; }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                viewingPlayers.remove(entry.getKey());
                activeBossBars.remove(entry.getKey());
                continue;
            }

            BossBar bar = activeBossBars.get(entry.getKey());
            if (bar == null) { continue; }

            bar.name(mm.deserialize(buildBossBarText(smeltery)));
            bar.progress(Math.min(1.0f, smeltery.getTank().getFillPercent()));
            bar.color(smeltery.isHeated() ? BossBar.Color.RED : BossBar.Color.BLUE);
        }
    }

    private String buildBossBarText(SmelteryStructure smeltery) {
        SmelteryTank tank = smeltery.getTank();
        StringBuilder sb = new StringBuilder();

        if (smeltery.isHeated()) {
            sb.append("<red>🔥 ");
        } else {
            sb.append("<blue>❄ ");
        }

        sb.append("<white>").append(tank.getTemperature()).append("°C");
        sb.append(" <gray>| ");
        sb.append("<white>").append(tank.getTotalVolume()).append("/").append(tank.getCapacity()).append("mb");

        if (!smeltery.getSmeltingQueue().isEmpty()) {
            sb.append(" <gray>| <yellow>").append(msg("smeltery.bossbar.smelting_count").replace("%count%", String.valueOf(smeltery.getSmeltingQueue().size())));
        }

        MoltenMetal dominant = tank.getDominantMetal();
        if (dominant != null) {
            sb.append(" <gray>| ").append(dominant.getFormattedName());
        }

        return sb.toString();
    }

    // ── Notificações ──

    private void notifyNearbyPlayers(SmelteryStructure smeltery, SmelteryStructure.SmeltingResult result) {
        Location center = smeltery.getInteriorCenter();
        if (center == null || center.getWorld() == null) { return; }

        // getNearbyPlayers requer execução na region thread (Folia)
        Task.sync(center, () -> {
            for (Player player : center.getWorld().getNearbyPlayers(center, 15)) {
                player.sendActionBar(mm.deserialize(msg("smeltery.smelting.produced_actionbar")
                        .replace("%metal%", result.metal().getFormattedName())
                        .replace("%amount%", String.valueOf(result.amountProduced()))));
            }
        });
    }

    // ── Construção / Validação ──

    /**
     * Tenta detectar e registrar uma smeltery ao redor de um controller.
     * Chamado quando jogador clica direito em um blast furnace.
     */
    public SmelteryStructure detectAndRegister(Player player, Location controllerLoc) {
        // Tentar detectar o multibloco ao redor do controller
        for (SmelteryTier tier : SmelteryTier.values()) {
            SmelteryStructure candidate = tryDetectStructure(player, controllerLoc, tier);
            if (candidate != null) {
                registry.register(candidate);
                // Persistir nova smeltery no banco
                if (repository != null) {
                    repository.saveSmeltery(candidate);
                }
                // Fire SmelteryActivateEvent
                Bukkit.getPluginManager().callEvent(new SmelteryActivateEvent(player, candidate));
                return candidate;
            }
        }
        return null;
    }

    private SmelteryStructure tryDetectStructure(Player player, Location controllerLoc, SmelteryTier tier) {
        int tw = tier.getTotalWidth();
        int th = tier.getTotalHeight();
        int td = tier.getTotalDepth();

        // O controller pode estar em qualquer parede.
        // Tentamos localizar o canto inferior-frontal-esquerdo testando offsets.
        int cx = controllerLoc.getBlockX();
        int cy = controllerLoc.getBlockY();
        int cz = controllerLoc.getBlockZ();

        // Testar o controller em cada posição possível das paredes
        for (int wallX = 0; wallX < tw; wallX++) {
            for (int wallY = 0; wallY < th; wallY++) {
                // Controller na parede frontal (z = 0)
                SmelteryStructure s = tryAtAnchor(player, cx - wallX, cy - wallY, cz, tier);
                if (s != null) {
                    return s;
                }

                // Controller na parede traseira (z = td - 1)
                s = tryAtAnchor(player, cx - wallX, cy - wallY, cz - (td - 1), tier);
                if (s != null) {
                    return s;
                }
            }
            for (int wallY = 0; wallY < th; wallY++) {
                // Controller na parede esquerda (x = 0)
                SmelteryStructure s = tryAtAnchor(player, cx, cy - wallY, cz - wallX, tier);
                if (s != null) {
                    return s;
                }

                // Controller na parede direita (x = tw - 1)
                s = tryAtAnchor(player, cx - (tw - 1), cy - wallY, cz - wallX, tier);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private SmelteryStructure tryAtAnchor(Player player, int ax, int ay, int az, SmelteryTier tier) {
        SmelteryStructure candidate = new SmelteryStructure(
                UUID.randomUUID(), player.getUniqueId(),
                player.getWorld().getName(), ax, ay, az, tier
        );
        if (candidate.validateStructure()) {
            return candidate;
        }
        return null;
    }

    // ── Getters ──

    public SmelteryRegistry getRegistry() { return registry; }
    public SmelteryRepository getRepository() { return repository; }
    public SmeltingRecipeManager getSmeltingRecipeManager() { return smeltingRecipeManager; }
    public AlloyRecipeManager getAlloyRecipeManager() { return alloyRecipeManager; }
    public ProfessionsModule getModule() { return module; }
    public JavaPlugin getPlugin() { return plugin; }
    public SmelteryVisualManager getVisualManager() { return visualManager; }

    /**
     * Loads or reloads smelting and alloy recipes from config.
     */
    public void loadSmelteryRecipes() {
        smeltingRecipeManager.loadFromConfig(
                module.getConfig().getConfigurationSection("smeltery.smelting_recipes"));
        alloyRecipeManager.loadFromConfig(
                module.getConfig().getConfigurationSection("smeltery.alloy_recipes"));
    }
}
