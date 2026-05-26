package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.BrewStandTracker;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Listener de penalidades para brewing (poções).
 * Afeta: Alquimista.
 *
 * Penalidade: chance de a poção falhar, resultando em glass bottles.
 *
 * Usa o BrewStandTracker compartilhado (que é alimentado pelo ProfessionXpListener)
 * para saber qual jogador está usando cada brewing stand.
 *
 * Prioridade HIGH para interceptar ANTES do MONITOR (XP listener).
 */
public class PenaltyBrewListener implements Listener {

    private final ProfessionPenaltyManager penaltyManager;
    private final BrewStandTracker brewStandTracker;

    public PenaltyBrewListener(ProfessionPenaltyManager penaltyManager, BrewStandTracker brewStandTracker) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
        this.brewStandTracker = Objects.requireNonNull(brewStandTracker);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }

            Location loc = event.getBlock().getLocation();
            UUID uuid = brewStandTracker.getLastUser(loc);
            if (uuid == null) { return; }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) { return; }

            // Alquimista é o profissional de brew
            ProfessionType requiredType = penaltyManager.findProfessionForAction("brew");
            if (requiredType == null) { return; }

            // Jogador é profissional? Sem penalidade.
            if (penaltyManager.isProfessional(player, requiredType)) { return; }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }

            double failChance = config.brewFailChance();
            if (failChance <= 0.0) { return; }

            if (penaltyManager.rollChance(failChance)) {
                // Brew falhou — substituir slots de poção por glass bottles após o brew completar
                Task.syncLater(event.getBlock().getLocation(), () -> {
                    try {
                        if (!(event.getBlock().getState() instanceof BrewingStand stand)) { return; }
                        BrewerInventory inv = stand.getInventory();
                        for (int slot = 0; slot < 3; slot++) {
                            ItemStack potion = inv.getItem(slot);
                            if (potion != null && potion.getType() != Material.AIR) {
                                inv.setItem(slot, new ItemStack(Material.GLASS_BOTTLE));
                            }
                        }
                    } catch (Exception e) {
                        MidgardLogger.error("Erro ao aplicar penalidade de brew falho", e);
                    }
                }, 1L);

                // Mensagem
                if (config.showMessage()) {
                    penaltyManager.sendPenaltyMessage(player, "brew_failed", requiredType);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de brew", e);
        }
    }
}
