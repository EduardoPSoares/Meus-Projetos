package me.ray.midgard.modules.professions.xp;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastreia blocos colocados por jogadores para prevenir exploits de XP.
 * Blocos player-placed não concedem XP ao serem quebrados.
 *
 * Cenários cobertos:
 * - Minerador: silk touch → place → mine loop
 * - Carpinteiro: cortar log → place → cortar loop
 * - Agricultor: plantar → colher imaturo → replantar loop
 * - Alquimista: nether wart/cogumelos → place → break loop
 *
 * Armazenamento é in-memory — limpo no disable/restart.
 * Localizações são removidas quando o bloco é quebrado (auto-cleanup).
 */
public final class PlacedBlockTracker {

    private final Set<Location> placedBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Marca um bloco como colocado por jogador.
     */
    public void track(Block block) {
        placedBlocks.add(block.getLocation());
    }

    /**
     * Verifica se o bloco foi colocado por jogador e remove do tracking.
     * Retorna true se foi player-placed (não deve dar XP).
     */
    public boolean isPlayerPlaced(Block block) {
        return placedBlocks.remove(block.getLocation());
    }

    /**
     * Limpa todos os dados. Chamado no onDisable.
     */
    public void clear() {
        placedBlocks.clear();
    }

    /**
     * Tamanho atual do tracker (para debug/monitoring).
     */
    public int size() {
        return placedBlocks.size();
    }
}
