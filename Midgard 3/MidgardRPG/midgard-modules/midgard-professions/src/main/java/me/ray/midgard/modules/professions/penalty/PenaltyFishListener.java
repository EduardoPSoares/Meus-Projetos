package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Listener de penalidades para pesca.
 * Afeta: Pescador.
 *
 * Penalidade: multiplica a chance de pescar lixo.
 * Na prática, substitui itens bons por lixo com base no multiplicador.
 *
 * Usa prioridade HIGH para interceptar ANTES do MONITOR (XP listener).
 */
public class PenaltyFishListener implements Listener {

    private static final List<Material> JUNK_ITEMS = List.of(
            Material.ROTTEN_FLESH, Material.STICK, Material.STRING,
            Material.BONE, Material.INK_SAC, Material.TRIPWIRE_HOOK,
            Material.LEATHER_BOOTS, Material.BOWL
    );

    private final ProfessionPenaltyManager penaltyManager;

    public PenaltyFishListener(ProfessionPenaltyManager penaltyManager) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) { return; }
            if (!(event.getCaught() instanceof Item item)) { return; }

            Player player = event.getPlayer();

            // Descobrir profissão de pesca
            ProfessionType requiredType = penaltyManager.findProfessionForMaterial("fish", item.getItemStack().getType());
            if (requiredType != null) {
                // Material registrado em alguma profissão — verificar todas as que dão XP
                if (penaltyManager.isPlayerExemptForMaterial(player, "fish", item.getItemStack().getType())) { return; }
            } else {
                // Material não registrado (ex: BOWL) — fallback para Pescador
                requiredType = ProfessionType.FISHER;
                if (penaltyManager.isProfessional(player, requiredType)) { return; }
            }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }

            double junkMultiplier = config.fishJunkMultiplier();
            if (junkMultiplier <= 1.0) { return; }

            // Chance de substituir por lixo = 1 - (1/multiplier)
            // Ex: multiplier 3.0 → 66% chance de virar lixo
            double junkChance = 1.0 - (1.0 / junkMultiplier);
            if (penaltyManager.rollChance(junkChance)) {
                // Substituir item por lixo aleatório
                Material junk = JUNK_ITEMS.get(ThreadLocalRandom.current().nextInt(JUNK_ITEMS.size()));
                item.setItemStack(new ItemStack(junk));

                // Mensagem
                if (config.showMessage()) {
                    penaltyManager.sendPenaltyMessage(player, "fish_junk", requiredType);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de pesca para %s", event.getPlayer().getName(), e);
        }
    }
}
