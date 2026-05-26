package me.ray.midgard.core.scoreboard;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.nms.api.NMSHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador global de scoreboards do MidgardRPG.
 *
 * Funciona como um sistema de pilha por prioridade usando pacotes NMS
 * para compatibilidade total com Folia (sem Bukkit Scoreboard API).
 *
 * - Módulos chamam {@link #show(Player, MidgardScoreboard)} para aplicar uma scoreboard.
 * - Se já existe uma ativa com menor prioridade, ela é empilhada e restaurada
 *   automaticamente quando a de maior prioridade é removida.
 * - {@link #hide(Player, String)} remove por ID — restaura a próxima da pilha.
 */
public class ScoreboardManager {

    private static volatile ScoreboardManager instance;

    // Pilha de scoreboards ativas por jogador (ordenada por prioridade desc)
    private final Map<UUID, Deque<ActiveBoard>> playerStacks = new ConcurrentHashMap<>();

    // Update task
    private BukkitTask updateTask;
    private int updateInterval = 10; // ticks between updates (0.5s)

    private ScoreboardManager() {}

    public static ScoreboardManager getInstance() {
        if (instance == null) {
            synchronized (ScoreboardManager.class) {
                if (instance == null) {
                    instance = new ScoreboardManager();
                }
            }
        }
        return instance;
    }

    /**
     * Inicia o loop de atualização periódica.
     */
    public void start() {
        if (updateTask != null) {
            return;
        }
        updateTask = Task.syncTimer(() -> {
            for (Map.Entry<UUID, Deque<ActiveBoard>> entry : playerStacks.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    continue;
                }

                ActiveBoard top = entry.getValue().peekFirst();
                if (top != null) {
                    try {
                        top.board.onUpdate(player, top.lines);
                    } catch (Exception e) {
                        MidgardLogger.error("Erro ao atualizar scoreboard '" + top.board.getId() + "'", e);
                    }
                }
            }
        }, updateInterval, updateInterval);
    }

    /**
     * Encerra o manager — cancela tasks e limpa tudo.
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remove objectives via packets para jogadores online
        NMSHandler nms = MidgardCore.getNMSHandler();
        for (Map.Entry<UUID, Deque<ActiveBoard>> entry : playerStacks.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && nms != null) {
                for (ActiveBoard ab : entry.getValue()) {
                    try {
                        ab.board.onRemove(player);
                        nms.scoreboardRemoveObjective(player, ab.board.getId());
                    } catch (Exception e) {
                        MidgardLogger.warn("Erro ao remover scoreboard '%s' durante shutdown", ab.board.getId());
                    }
                }
            }
        }

        playerStacks.clear();
    }

    // ==================== API Pública ====================

    /**
     * Aplica uma scoreboard a um jogador via pacotes NMS.
     * Se já existe uma ativa, a de maior prioridade fica visível.
     */
    public void show(Player player, MidgardScoreboard scoreboard) {
        UUID uuid = player.getUniqueId();
        NMSHandler nms = MidgardCore.getNMSHandler();
        if (nms == null) {
            return;
        }

        Deque<ActiveBoard> stack = playerStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());

        // Verifica se já está na pilha
        for (ActiveBoard ab : stack) {
            if (ab.board.getId().equals(scoreboard.getId())) {
                return; // Já ativa, não duplica
            }
        }

        // Cria o objective no client
        nms.scoreboardCreateObjective(player, scoreboard.getId(), scoreboard.getTitle());

        // Constrói o layout (onApply envia score packets via ScoreboardLines.set)
        ScoreboardLines lines = new ScoreboardLines(player, scoreboard.getId());
        ActiveBoard newBoard = new ActiveBoard(scoreboard, lines);

        try {
            scoreboard.onApply(player, lines);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao aplicar scoreboard '" + scoreboard.getId() + "'", e);
            nms.scoreboardRemoveObjective(player, scoreboard.getId());
            return;
        }

        // Identifica o topo atual
        ActiveBoard currentTop = stack.peekFirst();

        // Insere na posição correta da pilha (maior prioridade no topo)
        Deque<ActiveBoard> newStack = new ArrayDeque<>();
        boolean inserted = false;
        for (ActiveBoard existing : stack) {
            if (!inserted && scoreboard.getPriority() >= existing.board.getPriority()) {
                newStack.addLast(newBoard);
                inserted = true;
            }
            newStack.addLast(existing);
        }
        if (!inserted) {
            newStack.addLast(newBoard);
        }

        playerStacks.put(uuid, newStack);

        // Exibe a sidebar do topo da pilha
        ActiveBoard newTop = newStack.peekFirst();
        if (newTop != null) {
            nms.scoreboardDisplaySidebar(player, newTop.board.getId());
        }
    }

    /**
     * Remove uma scoreboard específica de um jogador por ID.
     * Se era a visível, restaura a próxima da pilha (ou limpa a sidebar).
     */
    public void hide(Player player, String boardId) {
        UUID uuid = player.getUniqueId();
        Deque<ActiveBoard> stack = playerStacks.get(uuid);
        if (stack == null || stack.isEmpty()) {
            return;
        }

        NMSHandler nms = MidgardCore.getNMSHandler();

        ActiveBoard currentTop = stack.peekFirst();
        boolean wasTop = currentTop != null && currentTop.board.getId().equals(boardId);

        // Remove da pilha
        ActiveBoard removed = null;
        Iterator<ActiveBoard> it = stack.iterator();
        while (it.hasNext()) {
            ActiveBoard ab = it.next();
            if (ab.board.getId().equals(boardId)) {
                removed = ab;
                it.remove();
                break;
            }
        }

        if (removed != null) {
            try {
                removed.board.onRemove(player);
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao remover scoreboard '%s'", boardId);
            }
            if (nms != null && player.isOnline()) {
                nms.scoreboardRemoveObjective(player, boardId);
            }
        }

        if (wasTop && nms != null && player.isOnline()) {
            ActiveBoard newTop = stack.peekFirst();
            if (newTop != null) {
                // Mostra a próxima board (objective já existe no client)
                nms.scoreboardDisplaySidebar(player, newTop.board.getId());
            } else {
                // Sem boards — limpa a sidebar
                nms.scoreboardDisplaySidebar(player, null);
            }
        }

        if (stack.isEmpty()) {
            playerStacks.remove(uuid);
        }
    }

    /**
     * Remove TODAS as scoreboards de um jogador e limpa a sidebar.
     */
    public void hideAll(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<ActiveBoard> stack = playerStacks.remove(uuid);
        if (stack == null) {
            return;
        }

        NMSHandler nms = MidgardCore.getNMSHandler();
        for (ActiveBoard ab : stack) {
            try {
                ab.board.onRemove(player);
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao remover scoreboard '%s' em hideAll", ab.board.getId());
            }
            if (nms != null && player.isOnline()) {
                nms.scoreboardRemoveObjective(player, ab.board.getId());
            }
        }
    }

    /**
     * Retorna o ScoreboardLines de uma scoreboard ativa para atualização sob demanda.
     */
    public ScoreboardLines getLines(Player player, String boardId) {
        Deque<ActiveBoard> stack = playerStacks.get(player.getUniqueId());
        if (stack == null) {
            return null;
        }
        for (ActiveBoard ab : stack) {
            if (ab.board.getId().equals(boardId)) {
                return ab.lines;
            }
        }
        return null;
    }

    /**
     * Verifica se um jogador tem uma scoreboard específica na pilha.
     */
    public boolean hasBoard(Player player, String boardId) {
        if (player == null) {
            return false;
        }
        Deque<ActiveBoard> stack = playerStacks.get(player.getUniqueId());
        if (stack == null) {
            return false;
        }
        for (ActiveBoard ab : stack) {
            if (ab.board.getId().equals(boardId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna o ID da scoreboard atualmente visível (topo da pilha).
     */
    public String getVisibleBoardId(UUID playerId) {
        Deque<ActiveBoard> stack = playerStacks.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ActiveBoard top = stack.peekFirst();
        return top != null ? top.board.getId() : null;
    }

    /**
     * Limpa todos os dados de um jogador (chamado no quit).
     */
    public void cleanup(UUID playerId) {
        Deque<ActiveBoard> stack = playerStacks.remove(playerId);
        if (stack != null) {
            Player player = Bukkit.getPlayer(playerId);
            NMSHandler nms = MidgardCore.getNMSHandler();
            for (ActiveBoard ab : stack) {
                try {
                    ab.board.onRemove(player);
                } catch (Exception e) {
                    MidgardLogger.warn("Erro ao remover scoreboard '%s' durante cleanup", ab.board.getId());
                }
                if (player != null && player.isOnline() && nms != null) {
                    try {
                        nms.scoreboardRemoveObjective(player, ab.board.getId());
                    } catch (Exception e) {
                        MidgardLogger.warn("Erro ao remover objective '%s' durante cleanup", ab.board.getId());
                    }
                }
            }
        }
    }

    /**
     * Configura o intervalo de atualização automática (em ticks).
     */
    public void setUpdateInterval(int ticks) {
        this.updateInterval = Math.max(1, ticks);
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
            start();
        }
    }

    // ==================== Internal ====================

    private static class ActiveBoard {
        final MidgardScoreboard board;
        final ScoreboardLines lines;

        ActiveBoard(MidgardScoreboard board, ScoreboardLines lines) {
            this.board = board;
            this.lines = lines;
        }
    }
}
