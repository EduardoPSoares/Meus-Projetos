package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Listener de penalidades para extração de fornalha.
 * Afeta: Ferreiro (lingotes), Cozinheiro (comida).
 *
 * Penalidades aplicáveis:
 * - Chance de o resultado queimar (perder parte ou todo o output)
 * - Item de falha substituto (ex: carvão ao invés de comida)
 *
 * FurnaceExtractEvent é quando o jogador RETIRA o item da fornalha.
 * O item já foi transferido para o cursor/inventário neste ponto, então
 * a remoção precisa ser agendada para 1 tick depois.
 *
 * Usa prioridade HIGH para interceptar ANTES do MONITOR (XP listener).
 */
public class PenaltyFurnaceListener implements Listener {

    private final ProfessionPenaltyManager penaltyManager;

    public PenaltyFurnaceListener(ProfessionPenaltyManager penaltyManager) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }

            Player player = event.getPlayer();
            Material resultType = event.getItemType();
            int amount = event.getItemAmount();

            // Descobrir qual profissão protege esse smelt
            ProfessionType requiredType = penaltyManager.findProfessionForMaterial("smelt", resultType);
            if (requiredType == null) { return; }

            // Jogador é profissional de qualquer profissão que dá XP para este smelt? Sem penalidade.
            if (penaltyManager.isPlayerExemptForMaterial(player, "smelt", resultType)) { return; }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }

            double failChance = config.smeltFailChance();
            if (failChance <= 0.0) { return; }

            // Calcular quantos itens são "queimados"
            int burned = 0;
            for (int i = 0; i < amount; i++) {
                if (penaltyManager.rollChance(failChance)) {
                    burned++;
                }
            }

            if (burned <= 0) { return; }

            // Agendar remoção para o próximo tick — o item é transferido ao inventário
            // durante o processamento do evento, então precisamos esperar
            final int burnedFinal = burned;
            final Material failResult = config.smeltFailResult();
            final boolean showMsg = config.showMessage();

            Task.syncLater(player, () -> {
                try {
                    if (!player.isOnline()) { return; }

                    // Remover itens queimados do inventário
                    removeItems(player, resultType, burnedFinal);

                    // Dar item de falha se configurado
                    if (failResult != null) {
                        var overflow = player.getInventory().addItem(new ItemStack(failResult, burnedFinal));
                        for (ItemStack leftover : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }

                    // Mensagem
                    if (showMsg) {
                        penaltyManager.sendPenaltyMessage(player, "smelt_failed", requiredType);
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao aplicar penalidade de fornalha para %s", player.getName(), e);
                }
            }, 1L);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de fornalha para %s", event.getPlayer().getName(), e);
        }
    }

    /**
     * Remove uma quantidade de itens de um material do inventário e cursor do jogador.
     */
    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;

        // Verificar cursor primeiro (item pode estar no cursor após click normal na furnace)
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() == material && remaining > 0) {
            int remove = Math.min(remaining, cursor.getAmount());
            if (remove >= cursor.getAmount()) {
                player.setItemOnCursor(null);
            } else {
                cursor.setAmount(cursor.getAmount() - remove);
            }
            remaining -= remove;
        }

        // Verificar inventário
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                int remove = Math.min(remaining, stack.getAmount());
                if (remove >= stack.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - remove);
                }
                remaining -= remove;
            }
        }
    }
}
