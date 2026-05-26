package me.ray.midgard.core.scoreboard;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.nms.api.NMSHandler;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper para criar e atualizar linhas em uma scoreboard sidebar via pacotes NMS.
 *
 * Cada linha é identificada por um ID de string (entry) e tem um score (posição vertical)
 * e um texto MiniMessage que é exibido ao jogador.
 *
 * Compatível com Folia — usa pacotes diretos em vez da API Bukkit Scoreboard.
 */
public class ScoreboardLines {

    private final Player player;
    private final String objectiveId;
    private final Map<String, LineData> lines = new LinkedHashMap<>();

    public ScoreboardLines(Player player, String objectiveId) {
        this.player = player;
        this.objectiveId = objectiveId;
    }

    /**
     * Cria uma nova linha na scoreboard (envia pacote imediatamente).
     *
     * @param id    Identificador único da linha (usado como score entry).
     * @param score Posição vertical (15 = topo, 0 = base).
     * @param text  Texto da linha em formato MiniMessage.
     */
    public void set(String id, int score, String text) {
        if (score < 0 || score > 15) {
            return;
        }
        lines.put(id, new LineData(id, score, text));

        NMSHandler nms = MidgardCore.getNMSHandler();
        if (nms != null && player.isOnline()) {
            nms.scoreboardScore(player, objectiveId, id, score, text);
        }
    }

    /**
     * Atualiza o texto de uma linha existente (reenvia pacote de score).
     *
     * @param id   Identificador da linha.
     * @param text Novo texto em MiniMessage.
     */
    public void update(String id, String text) {
        LineData line = lines.get(id);
        if (line == null) {
            return;
        }
        line.text = text;

        NMSHandler nms = MidgardCore.getNMSHandler();
        if (nms != null && player.isOnline()) {
            nms.scoreboardScore(player, objectiveId, id, line.score, text);
        }
    }

    /**
     * Remove uma linha da scoreboard.
     *
     * @param id Identificador da linha.
     */
    public void remove(String id) {
        LineData removed = lines.remove(id);
        if (removed == null) {
            return;
        }

        NMSHandler nms = MidgardCore.getNMSHandler();
        if (nms != null && player.isOnline()) {
            nms.scoreboardResetScore(player, objectiveId, id);
        }
    }

    /**
     * Reenvia todas as linhas ao jogador (usado ao restaurar uma board empilhada).
     */
    void resendAll() {
        NMSHandler nms = MidgardCore.getNMSHandler();
        if (nms == null || !player.isOnline()) {
            return;
        }
        for (LineData line : lines.values()) {
            nms.scoreboardScore(player, objectiveId, line.id, line.score, line.text);
        }
    }

    /** Retorna o Player associado. */
    public Player getPlayer() {
        return player;
    }

    /** Retorna o ID do objective. */
    public String getObjectiveId() {
        return objectiveId;
    }

    private static class LineData {
        final String id;
        final int score;
        String text;

        LineData(String id, int score, String text) {
            this.id = id;
            this.score = score;
            this.text = text;
        }
    }
}
