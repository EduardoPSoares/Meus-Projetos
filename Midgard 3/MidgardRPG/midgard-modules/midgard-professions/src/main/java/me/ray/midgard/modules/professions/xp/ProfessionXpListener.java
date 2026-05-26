package me.ray.midgard.modules.professions.xp;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.BrewStandTracker;
import me.ray.midgard.modules.professions.ProfessionManager;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Listener central de XP de profissões.
 * Captura eventos do Bukkit e distribui XP conforme configurado nas YMLs.
 *
 * Todos os handlers usam MONITOR + ignoreCancelled para só premiar XP
 * em eventos que realmente aconteceram (não cancelados por proteção, etc.).
 */
public class ProfessionXpListener implements Listener {

    private final ProfessionManager manager;
    private final ProfessionXpBar xpBar;
    private final PlacedBlockTracker blockTracker;
    private final BrewStandTracker brewStandTracker;
    private volatile double xpMultiplier = 1.0;

    public ProfessionXpListener(ProfessionManager manager, ProfessionXpBar xpBar,
                                PlacedBlockTracker blockTracker, BrewStandTracker brewStandTracker) {
        this.manager = Objects.requireNonNull(manager);
        this.xpBar = Objects.requireNonNull(xpBar);
        this.blockTracker = Objects.requireNonNull(blockTracker);
        this.brewStandTracker = Objects.requireNonNull(brewStandTracker);
        reloadMultiplier();
    }

    public void reloadMultiplier() {
        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module != null) {
            this.xpMultiplier = module.getConfig().getDouble("professions.xp-multiplier", 1.0);
        }
    }

    // ==========================================
    // BLOCK BREAK — Colher, Minerar, Cortar
    // Anti-exploit: ignora blocos player-placed e crops imaturos
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Block block = event.getBlock();

            // Anti-exploit: bloco colocado por jogador não dá XP
            if (blockTracker.isPlayerPlaced(block)) { return; }

            // Anti-exploit: crop imaturo não dá XP
            if (!isMatureCropOrNonCrop(block)) { return; }

            awardMaterialXp(event.getPlayer(), "block-break", block.getType(), 1);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de block-break para %s", event.getPlayer().getName(), e);
        }
    }

    // ==========================================
    // BLOCK PLACE — Plantar + rastrear anti-exploit
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        try {
            Block block = event.getBlock();

            // Rastrear todos os blocos colocados para anti-exploit de block-break
            blockTracker.track(block);

            awardMaterialXp(event.getPlayer(), "block-place", block.getType(), 1);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de block-place para %s", event.getPlayer().getName(), e);
        }
    }

    // ==========================================
    // CRAFT — Fabricar itens
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) { return; }
            ItemStack result = event.getRecipe().getResult();
            if (result.getType() == Material.AIR) { return; }

            int craftCount = 1;
            if (event.isShiftClick()) {
                craftCount = getShiftCraftCount(event);
            }

            awardMaterialXp(player, "craft", result.getType(), result.getAmount() * craftCount);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de craft", e);
        }
    }

    /**
     * Calcula quantas vezes o craft será executado em shift-click.
     * Baseia-se no menor stack de ingredientes na grid.
     */
    private int getShiftCraftCount(CraftItemEvent event) {
        int min = 64;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                min = Math.min(min, ingredient.getAmount());
            }
        }
        return Math.max(1, min);
    }

    // ==========================================
    // SMELT — Fundir / Cozinhar na fornalha
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        try {
            awardMaterialXp(event.getPlayer(), "smelt", event.getItemType(), event.getItemAmount());
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de smelt para %s", event.getPlayer().getName(), e);
        }
    }

    // ==========================================
    // FISH — Pescar
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        try {
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) { return; }
            if (!(event.getCaught() instanceof Item item)) { return; }
            awardMaterialXp(event.getPlayer(), "fish", item.getItemStack().getType(), 1);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de fish para %s", event.getPlayer().getName(), e);
        }
    }

    // ==========================================
    // ENCHANT — Encantar itens
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        try {
            Player player = event.getEnchanter();
            int totalLevels = event.getEnchantsToAdd().values().stream()
                    .mapToInt(Integer::intValue).sum();

            for (var config : ProfessionXpConfig.all().values()) {
                if (!config.hasAction("enchant")) { continue; }
                double baseXp = config.getParam("enchant", "base-xp");
                double xpPerLevel = config.getParam("enchant", "xp-per-level");
                double xp = baseXp + (totalLevels * xpPerLevel);
                if (xp > 0) {
                    awardXp(player, config.type(), xp);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de enchant para %s", event.getEnchanter().getName(), e);
        }
    }

    // ==========================================
    // BREW — Criar poções (rastreia último usuário)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        try {
            if (!(event.getPlayer() instanceof Player player)) { return; }
            if (!(event.getInventory() instanceof BrewerInventory brewer)) { return; }
            if (!(brewer.getHolder() instanceof BrewingStand stand)) { return; }
            brewStandTracker.track(stand.getLocation(), player.getUniqueId());
        } catch (Exception e) {
            MidgardLogger.error("Erro ao rastrear uso de brewing stand", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        try {
            Location loc = event.getBlock().getLocation();
            UUID uuid = brewStandTracker.getLastUser(loc);
            if (uuid == null) { return; }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) { return; }

            for (var config : ProfessionXpConfig.all().values()) {
                if (!config.hasAction("brew")) { continue; }
                double baseXp = config.getParam("brew", "base-xp");
                if (baseXp > 0) {
                    awardXp(player, config.type(), baseXp);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de brew", e);
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private void awardMaterialXp(Player player, String action, Material material, int amount) {
        for (var config : ProfessionXpConfig.all().values()) {
            double xpPer = config.getMaterialXp(action, material);
            if (xpPer > 0) {
                awardXp(player, config.type(), xpPer * amount);
            }
        }
    }

    private void awardXp(Player player, ProfessionType type, double xp) {
        double finalXp = xp * xpMultiplier;
        if (finalXp <= 0) { return; }

        int levelsGained = manager.addXp(player, type, finalXp);
        // addXp retorna 0 e não premia se não for a profissão ativa — nesse caso, não mostra nada
        if (levelsGained < 0) { return; }
        // Verificar se XP foi realmente premiado (profissão ativa)
        ProfessionType active = manager.getActiveProfession(player);
        if (active == null || active != type) { return; }

        Task.sync(player, () -> {
            xpBar.show(player, type, finalXp);
            if (levelsGained > 0) {
                notifyLevelUp(player, type);
            }
        });
    }

    private void notifyLevelUp(Player player, ProfessionType type) {
        if (!player.isOnline()) { return; }

        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null) { return; }

        int newLevel = manager.getLevel(player, type);
        String msg = module.getMessage("professions.level_up");
        if (msg == null || msg.isEmpty()) { return; }

        msg = msg.replace("%profession%", type.getDisplayName())
                 .replace("%level%", String.valueOf(newLevel))
                 .replace("%symbol%", type.getSymbol());
        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    public void shutdown() {
        brewStandTracker.clear();
        blockTracker.clear();
    }

    // ==========================================
    // Anti-Exploit: Verificação de maturidade de crops
    // ==========================================

    /**
     * Retorna true se o bloco NÃO é crop, ou se é crop totalmente maduro.
     * Crops imaturos retornam false (sem XP).
     */
    private boolean isMatureCropOrNonCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return true; // Não é crop — mineiro, carpinteiro, etc.
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }
}
