package me.ray.midgardDiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Comando /fila - Adiciona jogadores à fila do servidor RPG.
 * Pode ser usado pelo Typewriter ao clicar em um NPC.
 * 
 * Uso:
 *   /fila          - Entra na fila
 *   /fila sair     - Sai da fila
 *   /fila posicao  - Mostra posição atual
 *   /fila status   - (Admin) Mostra status da fila
 *   /fila limpar   - (Admin) Limpa a fila
 */
public class QueueCommand implements SimpleCommand {

    private final MidgardVelocity plugin;
    private final QueueManager queueManager;
    private final Map<UUID, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public QueueCommand(MidgardVelocity plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;

        // Limpeza periódica do mapa de cooldowns para evitar memory leak
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            long now = System.currentTimeMillis();
            cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_MS * 2);
        }).repeat(5, java.util.concurrent.TimeUnit.MINUTES).schedule();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        String[] args = invocation.arguments();

        // Cooldown
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < COOLDOWN_MS) {
            return; // Ignora silenciosamente para evitar spam do Typewriter
        }
        cooldowns.put(player.getUniqueId(), now);

        MessagesManager msg = plugin.getMessagesManager();

        if (args.length == 0) {
            // Entrar na fila
            handleJoinQueue(player, msg);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "sair":
            case "leave":
                handleLeaveQueue(player, msg);
                break;

            case "posicao":
            case "pos":
            case "position":
                handlePosition(player, msg);
                break;

            case "status":
                if (player.hasPermission("midgard.admin") || player.hasPermission("midgard.op")) {
                    handleStatus(player, msg);
                }
                break;

            case "limpar":
            case "clear":
                if (player.hasPermission("midgard.admin") || player.hasPermission("midgard.op")) {
                    handleClear(player, msg);
                }
                break;

            default:
                handleJoinQueue(player, msg);
                break;
        }
    }

    private static final Title.Times TITLE_TIMES = Title.Times.times(
        Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500));

    private static final Title.Times TITLE_TIMES_SHORT = Title.Times.times(
        Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300));

    private void handleJoinQueue(Player player, MessagesManager msg) {
        // Verifica manutenção antes de entrar na fila (staff bypassa)
        boolean isStaff = player.hasPermission("midgard.staff")
                || player.hasPermission("midgard.admin")
                || player.hasPermission("midgard.op")
                || player.hasPermission("midgard.maintenance.bypass")
                || plugin.isAdmin(player.getUsername());

        if (plugin.isMaintenance(queueManager.getTargetServer()) && !isStaff) {
            player.showTitle(Title.title(
                msg.get("queue.server-maintenance-title"),
                msg.get("queue.server-maintenance-subtitle", "server", queueManager.getTargetServer()),
                TITLE_TIMES_SHORT));
            return;
        }

        // Staff: bypass total, transferência direta sem fila
        if (isStaff) {
            int result = queueManager.directTransfer(player);
            switch (result) {
                case -3:
                    player.sendActionBar(msg.get("queue.already-connected", "server", queueManager.getTargetServer()));
                    break;
                case -4:
                    player.showTitle(Title.title(
                        msg.get("queue.server-unavailable-title"),
                        msg.get("queue.server-unavailable-subtitle"),
                        TITLE_TIMES_SHORT));
                    break;
                default:
                    player.sendActionBar(msg.get("queue.staff-bypass", "server", queueManager.getTargetServer()));
                    break;
            }
            return;
        }

        // Determina o tier VIP do jogador (4 = máximo, 0 = normal)
        int tier = getVipTier(player);
        int result = queueManager.addToQueue(player, tier);

        switch (result) {
            case -3:
                player.sendActionBar(msg.get("queue.already-connected", "server", queueManager.getTargetServer()));
                break;
            case -2:
                int currentPos = queueManager.getPosition(player.getUniqueId());
                player.sendActionBar(msg.get("queue.already-in-queue",
                    "position", String.valueOf(currentPos),
                    "total", String.valueOf(queueManager.getQueueSize())));
                break;
            case -1:
                player.sendActionBar(msg.get("queue.full"));
                break;
            default:
                if (tier > 0) {
                    player.showTitle(Title.title(
                        msg.get("queue.vip-priority-title"),
                        msg.get("queue.vip-priority-subtitle",
                            "position", String.valueOf(result),
                            "total", String.valueOf(queueManager.getQueueSize()),
                            "tier", String.valueOf(tier)),
                        TITLE_TIMES));
                } else {
                    player.showTitle(Title.title(
                        msg.get("queue.joined-title"),
                        msg.get("queue.joined-subtitle",
                            "position", String.valueOf(result),
                            "total", String.valueOf(queueManager.getQueueSize())),
                        TITLE_TIMES));
                }
                break;
        }
    }

    /**
     * Determina o tier VIP do jogador.
     * Verifica do mais alto ao mais baixo (midgard.vip.4 > midgard.vip.3 > midgard.vip.2 > midgard.vip.1).
     * @return tier 0-4 (0 = sem VIP)
     */
    private int getVipTier(Player player) {
        for (int t = QueueManager.MAX_TIER; t >= 1; t--) {
            if (player.hasPermission("midgard.vip." + t)) {
                return t;
            }
        }
        return 0;
    }

    private void handleLeaveQueue(Player player, MessagesManager msg) {
        if (queueManager.removeFromQueue(player.getUniqueId())) {
            player.sendActionBar(msg.get("queue.left"));
        } else {
            player.sendActionBar(msg.get("queue.not-in-queue"));
        }
    }

    private void handlePosition(Player player, MessagesManager msg) {
        int pos = queueManager.getPosition(player.getUniqueId());
        if (pos > 0) {
            player.sendActionBar(msg.get("queue.position",
                "position", String.valueOf(pos),
                "total", String.valueOf(queueManager.getQueueSize())));
        } else {
            player.sendActionBar(msg.get("queue.not-in-queue"));
        }
    }

    private void handleStatus(Player player, MessagesManager msg) {
        player.sendMessage(msg.get("queue.status",
            "size", String.valueOf(queueManager.getQueueSize()),
            "max", String.valueOf(queueManager.getMaxQueueSize()),
            "server", queueManager.getTargetServer(),
            "delay", String.valueOf(queueManager.getDelayBetweenPlayers())));
    }

    private void handleClear(Player player, MessagesManager msg) {
        int size = queueManager.getQueueSize();
        queueManager.clear();
        player.sendMessage(msg.get("queue.cleared", "count", String.valueOf(size)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String current = args.length == 0 ? "" : args[0];
            List<String> suggestions = new java.util.ArrayList<>(List.of("sair", "posicao"));
            CommandSource source = invocation.source();
            if (source.hasPermission("midgard.admin") || source.hasPermission("midgard.op")) {
                suggestions.add("status");
                suggestions.add("limpar");
            }
            if (current.isEmpty()) return suggestions;
            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }
}
