package me.ray.midgardDiscord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * Sistema de fila para transferência gradual de jogadores ao servidor RPG.
 * Evita sobrecarga ao enviar jogadores um a um com intervalo configurável.
 * 
 * Suporta bypass direto (Staff) e 4 tiers de prioridade VIP:
 * - Staff: transferência direta, sem entrar na fila
 * - VIP 4: prioridade máxima na fila
 * - VIP 3: prioridade alta
 * - VIP 2: prioridade média
 * - VIP 1: prioridade baixa
 * - Normal (tier 0): fila padrão FIFO
 */
public class QueueManager {

    /** Número de tiers VIP (1 a MAX_TIER). Tier 0 = normal. */
    public static final int MAX_TIER = 4;

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    private final org.slf4j.Logger logger;

    // Índice 0 = normal, 1 = VIP1, 2 = VIP2, 3 = VIP3, 4 = VIP4
    private final List<ConcurrentLinkedQueue<UUID>> tierQueues;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private String targetServer = "rpg";
    private int delayBetweenPlayers = 2; // segundos
    private int maxQueueSize = 200;

    public QueueManager(MidgardVelocity plugin, ProxyServer server, org.slf4j.Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;

        // Inicializa filas: índice 0 = normal, 1..4 = VIP tiers
        this.tierQueues = new ArrayList<>(MAX_TIER + 1);
        for (int i = 0; i <= MAX_TIER; i++) {
            tierQueues.add(new ConcurrentLinkedQueue<>());
        }
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public void setDelayBetweenPlayers(int delay) {
        this.delayBetweenPlayers = Math.max(1, delay);
    }

    public void setMaxQueueSize(int max) {
        this.maxQueueSize = Math.max(1, max);
    }

    public String getTargetServer() {
        return targetServer;
    }

    public int getDelayBetweenPlayers() {
        return delayBetweenPlayers;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    /**
     * Transfere um jogador diretamente ao servidor, sem entrar na fila (bypass de staff).
     * @return 0 se iniciou a transferência, -3 se já conectado, -4 se servidor indisponível.
     */
    public int directTransfer(Player player) {
        Optional<com.velocitypowered.api.proxy.ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isPresent() && currentServer.get().getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            return -3;
        }

        Optional<RegisteredServer> targetOpt = server.getServer(targetServer);
        if (targetOpt.isEmpty()) {
            logger.warn("Servidor alvo da fila '{}' não encontrado!", targetServer);
            return -4;
        }

        RegisteredServer target = targetOpt.get();
        MessagesManager msg = plugin.getMessagesManager();

        player.showTitle(Title.title(
            msg.get("queue.connecting-title"),
            msg.get("queue.connecting-subtitle", "server", targetServer),
            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));

        player.createConnectionRequest(target).connectWithIndication().thenAccept(success -> {
            if (success) {
                logger.info("Staff {} transferido diretamente para {} (bypass de fila)", player.getUsername(), targetServer);
            } else {
                player.sendActionBar(msg.get("queue.transfer-failed"));
                logger.warn("Falha ao transferir staff {} para {}", player.getUsername(), targetServer);
            }
        });

        return 0;
    }

    /**
     * Adiciona um jogador à fila normal (tier 0).
     * @return posição na fila (1-based), ou -1 se a fila estiver cheia,
     *         -2 se já estiver na fila, -3 se já estiver no servidor alvo.
     */
    public int addToQueue(Player player) {
        return addToQueue(player, 0);
    }

    /**
     * Adiciona um jogador à fila com tier de prioridade.
     * @param tier nível de prioridade (0 = normal, 1-4 = VIP tiers, maior = mais prioritário)
     * @return posição na fila (1-based), ou -1 se a fila estiver cheia,
     *         -2 se já estiver na fila, -3 se já estiver no servidor alvo.
     */
    public int addToQueue(Player player, int tier) {
        // Verifica se já está no servidor alvo
        Optional<com.velocitypowered.api.proxy.ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isPresent() && currentServer.get().getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            return -3;
        }

        // Verifica se já está em alguma fila
        UUID uuid = player.getUniqueId();
        if (isInQueue(uuid)) {
            return -2;
        }

        // Verifica tamanho máximo (todas as filas combinadas)
        if (getQueueSize() >= maxQueueSize) {
            return -1;
        }

        // Garante tier válido
        int safeTier = Math.max(0, Math.min(tier, MAX_TIER));
        tierQueues.get(safeTier).add(uuid);

        int position = getPosition(uuid);

        // Inicia processamento se não estiver rodando
        if (processing.compareAndSet(false, true)) {
            scheduleNextTransfer();
        }

        return position;
    }

    /**
     * Remove um jogador de qualquer fila.
     */
    public boolean removeFromQueue(UUID uuid) {
        for (ConcurrentLinkedQueue<UUID> q : tierQueues) {
            if (q.remove(uuid)) return true;
        }
        return false;
    }

    /**
     * Retorna a posição do jogador na fila combinada (1-based), ou 0 se não estiver.
     * Tiers mais altos aparecem primeiro (VIP4 > VIP3 > VIP2 > VIP1 > Normal).
     */
    public int getPosition(UUID uuid) {
        int pos = 0;
        // Itera do tier mais alto ao mais baixo
        for (int t = MAX_TIER; t >= 0; t--) {
            for (UUID id : tierQueues.get(t)) {
                pos++;
                if (id.equals(uuid)) return pos;
            }
        }
        return 0;
    }

    /**
     * Retorna o tamanho total de todas as filas.
     */
    public int getQueueSize() {
        int total = 0;
        for (ConcurrentLinkedQueue<UUID> q : tierQueues) {
            total += q.size();
        }
        return total;
    }

    /**
     * Verifica se o jogador está em qualquer fila.
     */
    public boolean isInQueue(UUID uuid) {
        for (ConcurrentLinkedQueue<UUID> q : tierQueues) {
            if (q.contains(uuid)) return true;
        }
        return false;
    }

    private void scheduleNextTransfer() {
        server.getScheduler().buildTask(plugin, this::processNext)
            .delay(delayBetweenPlayers, TimeUnit.SECONDS)
            .schedule();
    }

    private void processNext() {
        // Prioridade: processa do tier mais alto (VIP4) ao mais baixo (normal)
        UUID nextUuid = null;
        for (int t = MAX_TIER; t >= 0; t--) {
            nextUuid = tierQueues.get(t).poll();
            if (nextUuid != null) break;
        }

        // Se todas as filas esvaziaram, para o processamento
        if (nextUuid == null) {
            processing.set(false);
            return;
        }

        Optional<Player> playerOpt = server.getPlayer(nextUuid);

        // Jogador saiu do servidor enquanto estava na fila
        if (playerOpt.isEmpty()) {
            // Tenta o próximo
            if (getQueueSize() > 0) {
                scheduleNextTransfer();
            } else {
                processing.set(false);
                // Double-check: previne race condition onde addToQueue() adiciona
                // um jogador entre o getQueueSize() e o set(false)
                if (getQueueSize() > 0 && processing.compareAndSet(false, true)) {
                    scheduleNextTransfer();
                }
            }
            return;
        }

        Player player = playerOpt.get();

        // Verifica se o servidor alvo existe e está disponível
        Optional<RegisteredServer> targetOpt = server.getServer(targetServer);
        if (targetOpt.isEmpty()) {
            logger.warn("Servidor alvo da fila '{}' não encontrado!", targetServer);
            MessagesManager msg = plugin.getMessagesManager();
            player.showTitle(Title.title(
                msg.get("queue.server-unavailable-title"),
                msg.get("queue.server-unavailable-subtitle"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300))));
            processing.set(false);
            // Double-check: previne race condition com addToQueue()
            if (getQueueSize() > 0 && processing.compareAndSet(false, true)) {
                scheduleNextTransfer();
            }
            return;
        }

        // Verifica manutenção (staff bypassa)
        if (plugin.isMaintenance(targetServer)) {
            boolean canBypass = player.hasPermission("midgard.staff")
                    || player.hasPermission("midgard.admin")
                    || player.hasPermission("midgard.op")
                    || player.hasPermission("midgard.maintenance.bypass")
                    || plugin.isAdmin(player.getUsername());
            if (!canBypass) {
                MessagesManager msg = plugin.getMessagesManager();
                Component maintenanceTitle = msg.get("queue.server-maintenance-title");
                Component maintenanceSubtitle = msg.get("queue.server-maintenance-subtitle", "server", targetServer);
                Title.Times times = Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300));
                player.showTitle(Title.title(maintenanceTitle, maintenanceSubtitle, times));
                clearQueueWithTitle(maintenanceTitle, maintenanceSubtitle, times);
                processing.set(false);
                // Double-check: previne race condition com addToQueue()
                if (getQueueSize() > 0 && processing.compareAndSet(false, true)) {
                    scheduleNextTransfer();
                }
                return;
            }
        }

        RegisteredServer target = targetOpt.get();
        MessagesManager msg = plugin.getMessagesManager();

        // Envia título de "conectando"
        player.showTitle(Title.title(
            msg.get("queue.connecting-title"),
            msg.get("queue.connecting-subtitle", "server", targetServer),
            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));

        // Transfere o jogador
        player.createConnectionRequest(target).connectWithIndication().thenAccept(success -> {
            if (success) {
                logger.info("Jogador {} transferido da fila para {}", player.getUsername(), targetServer);
            } else {
                player.sendActionBar(msg.get("queue.transfer-failed"));
                logger.warn("Falha ao transferir {} para {}", player.getUsername(), targetServer);
            }
        });

        // Notifica jogadores restantes sobre atualização de posição
        notifyQueuePositions();

        // Continua processando se houver mais na fila
        if (getQueueSize() > 0) {
            scheduleNextTransfer();
        } else {
            processing.set(false);
            // Double-check: previne race condition onde addToQueue() adiciona
            // um jogador entre o getQueueSize() e o set(false)
            if (getQueueSize() > 0 && processing.compareAndSet(false, true)) {
                scheduleNextTransfer();
            }
        }
    }

    /**
     * Notifica todos os jogadores na fila sobre sua posição atual.
     */
    private void notifyQueuePositions() {
        MessagesManager msg = plugin.getMessagesManager();
        int total = getQueueSize();
        int pos = 0;
        // Itera do tier mais alto ao mais baixo
        for (int t = MAX_TIER; t >= 0; t--) {
            for (UUID uuid : tierQueues.get(t)) {
                pos++;
                Optional<Player> pOpt = server.getPlayer(uuid);
                if (pOpt.isPresent()) {
                    pOpt.get().sendActionBar(msg.get("queue.position-update",
                        "position", String.valueOf(pos),
                        "total", String.valueOf(total)));
                }
            }
        }
    }

    /**
     * Limpa a fila enviando um título para todos os jogadores.
     */
    private void clearQueueWithTitle(Component title, Component subtitle, Title.Times times) {
        for (ConcurrentLinkedQueue<UUID> q : tierQueues) {
            UUID uuid;
            while ((uuid = q.poll()) != null) {
                server.getPlayer(uuid).ifPresent(p -> p.showTitle(Title.title(title, subtitle, times)));
            }
        }
    }

    /**
     * Limpa a fila (ex: quando o plugin é desligado).
     */
    public void clear() {
        for (ConcurrentLinkedQueue<UUID> q : tierQueues) {
            q.clear();
        }
        processing.set(false);
    }
}
