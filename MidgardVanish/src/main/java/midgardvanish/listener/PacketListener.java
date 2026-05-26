package midgardvanish.listener;

import io.netty.channel.*;
import midgardvanish.manager.VanishManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class PacketListener implements Listener {

    private static final Unsafe UNSAFE;
    private static final long COLOR_OFFSET;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            Field colorField = ClientboundSetPlayerTeamPacket.Parameters.class.getDeclaredField("color");
            COLOR_OFFSET = UNSAFE.objectFieldOffset(colorField);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup reflection for PacketListener", e);
        }
    }

    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private static final String HANDLER_NAME = "midgardvanish_packet_handler";

    public PacketListener(JavaPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;

        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> injectPlayer(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        uninjectPlayer(event.getPlayer());
    }

    private void injectPlayer(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;

        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }
            if (!channel.isOpen()) return;

            channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
                private final Set<String> vanishedTeams = new HashSet<>();

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    try {
                        // === Sound filtering ===
                        if (msg instanceof ClientboundSoundPacket soundPacket) {
                            String soundName = soundPacket.getSound().value().location().getPath();

                            if (soundName.contains("door") || soundName.contains("gate")
                                    || soundName.contains("lever") || soundName.contains("button")
                                    || soundName.contains("trapdoor")) {
                                int sx = (int) Math.floor(soundPacket.getX());
                                int sy = (int) Math.floor(soundPacket.getY());
                                int sz = (int) Math.floor(soundPacket.getZ());
                                if (vanishManager.isSilentInteraction(BlockPos.asLong(sx, sy, sz))) {
                                    if (!vanishManager.canSeeAny(player)) {
                                        return;
                                    }
                                }
                            }

                            if (soundName.contains("chest") || soundName.contains("barrel")
                                    || soundName.contains("shulker")) {
                                for (var uuid : vanishManager.getVanishedPlayers()) {
                                    Player vanished = Bukkit.getPlayer(uuid);
                                    if (vanished == null) continue;

                                    double dx = vanished.getLocation().getX() - soundPacket.getX();
                                    double dy = vanished.getLocation().getY() - soundPacket.getY();
                                    double dz = vanished.getLocation().getZ() - soundPacket.getZ();

                                    if (dx * dx + dy * dy + dz * dz < 25.0) {
                                        if (!vanishManager.canSee(player, vanished) && !player.getUniqueId().equals(uuid)) {
                                            return;
                                        }
                                    }
                                }
                            }

                            if (soundName.contains("step") || soundName.contains("footstep")
                                    || soundName.contains("swim") || soundName.contains("splash")) {
                                for (var uuid : vanishManager.getVanishedPlayers()) {
                                    Player vanished = Bukkit.getPlayer(uuid);
                                    if (vanished == null) continue;

                                    double dx = vanished.getLocation().getX() - soundPacket.getX();
                                    double dy = vanished.getLocation().getY() - soundPacket.getY();
                                    double dz = vanished.getLocation().getZ() - soundPacket.getZ();

                                    if (dx * dx + dy * dy + dz * dz < 4.0) {
                                        if (!vanishManager.canSee(player, vanished)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        // === Silent chest animation ===
                        if (msg instanceof ClientboundBlockEventPacket blockEventPacket) {
                            if (blockEventPacket.getBlock() == Blocks.CHEST
                                    || blockEventPacket.getBlock() == Blocks.TRAPPED_CHEST
                                    || blockEventPacket.getBlock() == Blocks.ENDER_CHEST
                                    || blockEventPacket.getBlock() == Blocks.BARREL
                                    || blockEventPacket.getBlock() instanceof ShulkerBoxBlock) {

                                for (var uuid : vanishManager.getVanishedPlayers()) {
                                    Player vanished = Bukkit.getPlayer(uuid);
                                    if (vanished == null) continue;

                                    double dx = vanished.getLocation().getX() - blockEventPacket.getPos().getX();
                                    double dy = vanished.getLocation().getY() - blockEventPacket.getPos().getY();
                                    double dz = vanished.getLocation().getZ() - blockEventPacket.getPos().getZ();

                                    if (dx * dx + dy * dy + dz * dz < 25.0) {
                                        if (!vanishManager.canSee(player, vanished) && !player.getUniqueId().equals(uuid)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        // === Block state update interception ===
                        if (msg instanceof ClientboundBlockUpdatePacket blockUpdatePacket) {
                            long packedPos = blockUpdatePacket.getPos().asLong();
                            if (vanishManager.isSilentInteraction(packedPos)) {
                                if (!vanishManager.canSeeAny(player) && !vanishManager.isVanished(player)) {
                                    return;
                                }
                            }
                        }

                    } catch (Exception ignored) {
                    }

                    // === Team glow color control (RED for vanished players) ===
                    try {
                        if (msg instanceof ClientboundSetPlayerTeamPacket teamPacket) {
                            if (!teamPacket.getName().startsWith("mv_")) {
                                if (!teamPacket.getPlayers().isEmpty()) {
                                    boolean hasVanished = teamPacket.getPlayers().stream().anyMatch(name -> {
                                        Player p = Bukkit.getPlayerExact(name);
                                        return p != null && vanishManager.isVanished(p);
                                    });
                                    if (hasVanished) {
                                        vanishedTeams.add(teamPacket.getName());
                                    } else {
                                        vanishedTeams.remove(teamPacket.getName());
                                    }
                                }

                                if (vanishedTeams.contains(teamPacket.getName())) {
                                    teamPacket.getParameters().ifPresent(params -> {
                                        UNSAFE.putObject(params, COLOR_OFFSET, ChatFormatting.RED);
                                    });
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    super.write(ctx, msg, promise);
                }
            });
        });
    }

    public void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninjectPlayer(player);
        }
    }

    private void uninjectPlayer(Player player) {
        try {
            Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
            channel.eventLoop().execute(() -> {
                try {
                    if (channel.pipeline().get(HANDLER_NAME) != null) {
                        channel.pipeline().remove(HANDLER_NAME);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}