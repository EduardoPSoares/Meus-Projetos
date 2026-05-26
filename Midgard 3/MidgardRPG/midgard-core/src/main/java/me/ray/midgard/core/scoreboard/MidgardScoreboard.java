package me.ray.midgard.core.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Interface que todas as scoreboards do MidgardRPG devem implementar.
 *
 * Cada scoreboard representa um "estado visual" da sidebar para um jogador.
 * Exemplos: Forja, Combate Debug, Lobby, HUD Padrão, Dungeon, etc.
 *
 * O {@link ScoreboardManager} controla qual scoreboard está visível para cada
 * jogador usando um sistema de prioridade com pilha — quando uma scoreboard de
 * maior prioridade é removida, a anterior é restaurada automaticamente.
 */
public interface MidgardScoreboard {

    /**
     * Identificador único desta scoreboard (ex: "forge", "combat_debug", "lobby").
     */
    String getId();

    /**
     * Título exibido no topo da sidebar.
     * Parseado como MiniMessage pelo manager.
     */
    String getTitle();

    /**
     * Prioridade desta scoreboard. Maior valor = maior prioridade.
     * Quando duas scoreboards competem pelo mesmo jogador,
     * a de maior prioridade é exibida.
     *
     * Guia de prioridades:
     *   0-9    = Padrão/ambient (HUD geral, lobby)
     *  10-49   = Gameplay (forja, dungeon, quest)
     *  50-89   = Temporário/evento (boss fight, arena)
     *  90-100  = Debug/Admin
     */
    int getPriority();

    /**
     * Chamado quando esta scoreboard é aplicada a um jogador.
     * Deve configurar todas as linhas iniciais no {@link Scoreboard} fornecido.
     *
     * O Scoreboard já vem com um Objective registrado no DisplaySlot.SIDEBAR.
     * Use o helper {@link ScoreboardLines} para criar/atualizar linhas.
     *
     * @param player   O jogador alvo.
     * @param lines    Helper para criar e atualizar linhas.
     */
    void onApply(Player player, ScoreboardLines lines);

    /**
     * Chamado quando esta scoreboard é removida de um jogador
     * (seja por troca de scoreboard ou por cleanup).
     * Utilize para liberar recursos, cancelar timers, etc.
     *
     * @param player O jogador (pode ser null se offline).
     */
    default void onRemove(Player player) {}

    /**
     * Chamado periodicamente pelo manager (a cada tick configurado) para
     * atualizar linhas dinâmicas. Retorne false se não há nada a atualizar.
     *
     * @param player O jogador.
     * @param lines  Helper para atualizar linhas.
     * @return true se alguma linha foi atualizada.
     */
    default boolean onUpdate(Player player, ScoreboardLines lines) {
        return false;
    }
}
