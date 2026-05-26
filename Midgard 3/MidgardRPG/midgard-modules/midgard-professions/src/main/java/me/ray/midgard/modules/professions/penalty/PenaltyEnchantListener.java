package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Listener de penalidades para encantamento.
 * Afeta: Arcanista.
 *
 * Penalidades aplicáveis:
 * - Custo extra de XP (níveis adicionais consumidos)
 * - Chance de falha (perde XP mas não ganha encantamento)
 *
 * Usa prioridade HIGH para interceptar ANTES do MONITOR (XP listener).
 */
public class PenaltyEnchantListener implements Listener {

    private final ProfessionPenaltyManager penaltyManager;

    public PenaltyEnchantListener(ProfessionPenaltyManager penaltyManager) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }

            Player player = event.getEnchanter();

            // Arcanista é o profissional de enchant
            ProfessionType requiredType = penaltyManager.findProfessionForAction("enchant");
            if (requiredType == null) { return; }

            // Jogador é profissional? Sem penalidade.
            if (penaltyManager.isProfessional(player, requiredType)) { return; }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }

            // 1. Chance de falha total (cancela o encantamento, consome XP e lapis)
            if (config.enchantFailChance() > 0 && penaltyManager.rollChance(config.enchantFailChance())) {
                event.setCancelled(true);

                // Consumir os níveis e lapis manualmente — 1 tick depois para garantir
                int cost = event.getExpLevelCost();
                // whichButton(): 0=topo, 1=meio, 2=fundo → lapis cost = slot + 1
                int lapisCost = event.whichButton() + 1;
                Task.syncLater(player, () -> {
                    if (!player.isOnline()) { return; }
                    // Remover níveis de XP
                    player.setLevel(Math.max(0, player.getLevel() - cost));
                    // Remover lapis lazuli do slot 1 da mesa de encantamento
                    ItemStack lapisSlot = event.getInventory().getItem(1);
                    if (lapisSlot != null && lapisSlot.getType() == Material.LAPIS_LAZULI) {
                        int newAmount = lapisSlot.getAmount() - lapisCost;
                        if (newAmount <= 0) {
                            event.getInventory().setItem(1, null);
                        } else {
                            lapisSlot.setAmount(newAmount);
                        }
                    }
                }, 1L);

                if (config.showMessage()) {
                    penaltyManager.sendPenaltyMessage(player, "enchant_failed", requiredType);
                }
                return;
            }

            // 2. Custo extra de XP — aplica 1 tick depois (após Bukkit consumir os níveis normais)
            int extraCost = config.enchantExtraCost();
            if (extraCost > 0) {
                Task.syncLater(player, () -> {
                    if (!player.isOnline()) { return; }
                    player.setLevel(Math.max(0, player.getLevel() - extraCost));
                }, 1L);

                if (config.showMessage()) {
                    penaltyManager.sendPenaltyMessage(player, "enchant_extra_cost", requiredType);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de encantamento para %s", event.getEnchanter().getName(), e);
        }
    }
}
