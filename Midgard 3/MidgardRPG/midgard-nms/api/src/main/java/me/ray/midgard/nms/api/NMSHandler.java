package me.ray.midgard.nms.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NMSHandler {
    
    void sendPacket(Player player, Object packet);
    
    /**
     * Spawna um indicador de dano usando pacotes NMS.
     * @param plugin Plugin instance for scheduling tasks.
     * @param viewer O jogador que verá o indicador.
     * @param location A localização do indicador.
     * @param text O texto do indicador (formato JSON ou raw).
     * @param duration Duração em ticks.
     * @param backgroundColor Cor de fundo (ARGB).
     * @param shadow Se deve ter sombra.
     * @param seeThrough Se deve ser visível através de blocos.
     */
    void spawnDamageIndicatorPacket(org.bukkit.plugin.Plugin plugin, Player viewer, Location location, String text, int duration, int backgroundColor, boolean shadow, boolean seeThrough);

    // ==================== Scoreboard Packets (Folia-compatible) ====================

    /**
     * Cria um objective de scoreboard no client do jogador via pacote.
     * @param player Jogador alvo.
     * @param id Nome do objective (ex: "forge", "combat_debug").
     * @param titleMiniMessage Título em formato MiniMessage.
     */
    default void scoreboardCreateObjective(Player player, String id, String titleMiniMessage) {}

    /**
     * Remove um objective de scoreboard do client do jogador.
     * @param player Jogador alvo.
     * @param id Nome do objective a remover.
     */
    default void scoreboardRemoveObjective(Player player, String id) {}

    /**
     * Define qual objective é exibido na sidebar do jogador.
     * @param player Jogador alvo.
     * @param id Nome do objective, ou null/vazio para limpar a sidebar.
     */
    default void scoreboardDisplaySidebar(Player player, String id) {}

    /**
     * Envia/atualiza uma entrada de score no client do jogador.
     * Usa NumberFormat BLANK para esconder números.
     * @param player Jogador alvo.
     * @param objectiveId Nome do objective dono.
     * @param entry Identificador único da entrada (score owner).
     * @param score Valor numérico (posição vertical: 15=topo, 0=base).
     * @param displayMiniMessage Texto exibido em formato MiniMessage.
     */
    default void scoreboardScore(Player player, String objectiveId, String entry, int score, String displayMiniMessage) {}

    /**
     * Remove uma entrada de score do client do jogador.
     * @param player Jogador alvo.
     * @param objectiveId Nome do objective.
     * @param entry Identificador da entrada a remover.
     */
    default void scoreboardResetScore(Player player, String objectiveId, String entry) {}
}
