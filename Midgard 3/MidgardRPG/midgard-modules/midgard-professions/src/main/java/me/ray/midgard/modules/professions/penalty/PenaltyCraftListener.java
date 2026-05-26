package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Listener de penalidades para craft de itens.
 * Afeta: Ferreiro, Carpinteiro, Cozinheiro, Médico, Cartógrafo, Alquimista.
 *
 * Penalidades aplicáveis:
 * - Chance de falhar o craft (perder materiais, receber item de falha ou nada)
 * - Efeito de ação (slowness após craft falho)
 *
 * Usa prioridade HIGH para interceptar ANTES do MONITOR (XP listener).
 */
public class PenaltyCraftListener implements Listener {

    private final ProfessionPenaltyManager penaltyManager;

    public PenaltyCraftListener(ProfessionPenaltyManager penaltyManager) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }
            if (!(event.getWhoClicked() instanceof Player player)) { return; }

            ItemStack result = event.getRecipe().getResult();
            if (result.getType() == Material.AIR) { return; }

            // Descobrir qual profissão protege esse craft
            ProfessionType requiredType = penaltyManager.findProfessionForMaterial("craft", result.getType());
            if (requiredType == null) { return; }

            // Jogador é profissional de qualquer profissão que dá XP para este craft? Sem penalidade.
            if (penaltyManager.isPlayerExemptForMaterial(player, "craft", result.getType())) { return; }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }

            // Verificar se o material do resultado está na lista de afetados
            if (!config.affectsMaterial(result.getType())) { return; }

            // Chance de falhar o craft
            double failChance = config.craftFailChance();
            if (failChance <= 0.0) { return; }

            MidgardLogger.debug("[PenaltyCraft] Aplicando penalidade para %s (failChance=%.2f)", player.getName(), failChance);

            // Shift-click: forçar craft unitário para evitar exploit
            // (sem isso, shift-click rola 1x para o lote inteiro, sendo muito mais seguro)
            if (event.isShiftClick()) {
                event.setCancelled(true);
                processSingleCraft(event, player, result, config, failChance, requiredType);
                return;
            }

            if (penaltyManager.rollChance(failChance)) {
                applyCraftFailure(event, player, result, config, requiredType);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de craft para %s",
                    event.getWhoClicked().getName(), e);
        }
    }

    /**
     * Processa shift-click como craft unitário para não-profissionais.
     * Evita exploit onde shift-click é mais seguro que click individual.
     */
    private void processSingleCraft(CraftItemEvent event, Player player, ItemStack result,
                                     ProfessionPenaltyConfig config, double failChance,
                                     ProfessionType requiredType) {
        if (penaltyManager.rollChance(failChance)) {
            // Falhou — consome ingredientes, dá item de falha
            consumeIngredients(event);
            giveCraftFailResult(player, result, config);

            // Efeito de ação apenas na falha
            if (config.actionEffect() != null) {
                penaltyManager.applyEffect(player, config.actionEffect(),
                        config.actionEffectDuration(), config.actionEffectAmplifier());
            }

            // Mensagem de falha
            if (config.showMessage()) {
                penaltyManager.sendPenaltyMessage(player, "craft_failed", requiredType);
            }
        } else {
            // Sucesso — consome ingredientes, dá resultado normal
            consumeIngredients(event);
            var overflow = player.getInventory().addItem(result.clone());
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /**
     * Aplica falha de craft: cancela evento, consome ingredientes, dá item de falha.
     */
    private void applyCraftFailure(CraftItemEvent event, Player player, ItemStack result,
                                    ProfessionPenaltyConfig config, ProfessionType requiredType) {
        event.setCancelled(true);
        consumeIngredients(event);
        giveCraftFailResult(player, result, config);

        // Efeito de ação
        if (config.actionEffect() != null) {
            penaltyManager.applyEffect(player, config.actionEffect(),
                    config.actionEffectDuration(), config.actionEffectAmplifier());
        }

        // Mensagem com cooldown
        if (config.showMessage()) {
            penaltyManager.sendPenaltyMessage(player, "craft_failed", requiredType);
        }
    }

    /**
     * Dá item de falha ao jogador se configurado.
     */
    private void giveCraftFailResult(Player player, ItemStack result, ProfessionPenaltyConfig config) {
        Material failResult = config.craftFailResult();
        if (failResult != null) {
            var overflow = player.getInventory().addItem(new ItemStack(failResult, result.getAmount()));
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /**
     * Consome os ingredientes da grid de craft manualmente.
     * Necessário porque cancelamos o evento (Bukkit não consome automaticamente).
     */
    private void consumeIngredients(CraftItemEvent event) {
        var matrix = event.getInventory().getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack ingredient = matrix[i];
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                if (ingredient.getAmount() > 1) {
                    ingredient.setAmount(ingredient.getAmount() - 1);
                } else {
                    matrix[i] = null;
                }
            }
        }
        event.getInventory().setMatrix(matrix);
    }
}
