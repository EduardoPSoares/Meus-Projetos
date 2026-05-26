package me.ray.midgard.nms.v1_21;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.nms.api.NMSHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class NMSHandlerImpl implements NMSHandler {

    @Override
    public void sendPacket(Player player, Object packet) {
        if (player == null) {
            throw new IllegalArgumentException("Player não pode ser nulo");
        }
        if (packet == null) {
            throw new IllegalArgumentException("Packet não pode ser nulo");
        }
        
        try {
            CraftPlayer craftPlayer = (CraftPlayer) player;
            net.minecraft.server.level.ServerPlayer serverPlayer = craftPlayer.getHandle();
            serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
                
        } catch (ClassCastException e) {
            throw new RuntimeException("Tipo de pacote inválido ou instância de player incorreta", e);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar pacote para o player: " + player.getName(), e);
        }
    }

    // ==================== Scoreboard Packets ====================

    @Override
    public void scoreboardCreateObjective(Player player, String id, String titleMiniMessage) {
        try {
            Objective objective = createDummyObjective(id, toNMS(titleMiniMessage));
            sendPacket(player, new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao criar objective scoreboard '" + id + "'", e);
        }
    }

    @Override
    public void scoreboardRemoveObjective(Player player, String id) {
        try {
            Objective objective = createDummyObjective(id, Component.empty());
            sendPacket(player, new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao remover objective scoreboard '" + id + "'", e);
        }
    }

    @Override
    public void scoreboardDisplaySidebar(Player player, String id) {
        try {
            if (id == null || id.isEmpty()) {
                sendPacket(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
            } else {
                Objective objective = createDummyObjective(id, Component.empty());
                sendPacket(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao definir display slot sidebar", e);
        }
    }

    @Override
    public void scoreboardScore(Player player, String objectiveId, String entry, int score, String displayMiniMessage) {
        try {
            Component display = toNMS(displayMiniMessage);
            sendPacket(player, new ClientboundSetScorePacket(
                    entry,
                    objectiveId,
                    score,
                    Optional.of(display),
                    Optional.of(BlankFormat.INSTANCE)
            ));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao enviar score scoreboard '" + objectiveId + "/" + entry + "'", e);
        }
    }

    @Override
    public void scoreboardResetScore(Player player, String objectiveId, String entry) {
        try {
            sendPacket(player, new ClientboundResetScorePacket(entry, objectiveId));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao resetar score '" + objectiveId + "/" + entry + "'", e);
        }
    }

    /**
     * Cria um Objective NMS dummy apenas para construção de pacotes.
     * O Objective em si não é registrado em nenhum server-side scoreboard.
     */
    private Objective createDummyObjective(String name, Component displayName) {
        Scoreboard scoreboard = new Scoreboard();
        return scoreboard.addObjective(
                name,
                ObjectiveCriteria.DUMMY,
                displayName,
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );
    }

    /**
     * Converte texto MiniMessage em NMS Component via Adventure bridge.
     */
    private Component toNMS(String miniMessage) {
        net.kyori.adventure.text.Component adventure = MessageUtils.parse(miniMessage);
        return io.papermc.paper.adventure.PaperAdventure.asVanilla(adventure);
    }

    private void sendMetadataUpdate(Player viewer, Display.TextDisplay display) {
        var values = display.getEntityData().getNonDefaultValues();
        if (values != null) {
            sendPacket(viewer, new ClientboundSetEntityDataPacket(display.getId(), values));
        }
    }

    // Helper using Bukkit API to avoid reflection issues
    private void setInterpolation(Display display, int delay, int duration) {
        try {
            // Use Bukkit API wrapper to handle mappings correctly
            if (display.getBukkitEntity() instanceof org.bukkit.entity.Display bukkitDisplay) {
                bukkitDisplay.setInterpolationDelay(delay);
                bukkitDisplay.setInterpolationDuration(duration);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao configurar interpolação do display", e);
        }
    }
    
    private void setBackgroundColor(Display.TextDisplay display, int color) {
        try {
            if (display.getBukkitEntity() instanceof org.bukkit.entity.TextDisplay bukkitDisplay) {
                // Alpha needs to be handled? Bukkit Color handles ARGB?
                // Bukkit Color.fromARGB accepts (alpha, red, green, blue)
                // The int color here is likely ARGB.
                int a = (color >> 24) & 0xFF;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                bukkitDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(a, r, g, b));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao configurar cor de fundo do display", e);
        }
    }

    @Override
    public void spawnDamageIndicatorPacket(org.bukkit.plugin.Plugin plugin, Player viewer, Location location, String text, int duration, int backgroundColor, boolean shadow, boolean seeThrough) {
        try {
            if (viewer == null || location == null) return;
            
            net.minecraft.server.level.ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
            
            // Create NMS TextDisplay entity
            Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
            display.setPos(location.getX(), location.getY(), location.getZ());
            
            // Set text
            display.setText(Component.literal(text));
            
            // Set basic properties
            display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            display.setViewRange(100.0f);
            
            // Set Visual Properties
            setBackgroundColor(display, backgroundColor);
            
            byte flags = 0;
            if (shadow) flags |= 1;
            if (seeThrough) flags |= 2;
            display.setFlags(flags);

            // Initial Animation State (Invisible/Small)
            // Match old code: Scale 0, Pos 0. Duration 3 ticks (prepared for first phase)
            display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f), 
                new Quaternionf(), 
                new Vector3f(0.0f, 0.0f, 0.0f), 
                new Quaternionf()
            ));
            
            setInterpolation(display, 0, 3);

            // Send Spawn Packet
            ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(display, 0, display.blockPosition());
            sendPacket(viewer, spawnPacket);
            
            // Send Metadata Packet
            sendMetadataUpdate(viewer, display);

            // Animation Task (Using Entity Scheduler for Folia support)
            AtomicInteger tick = new AtomicInteger(0);
            int initialBgAlpha = (backgroundColor >> 24) & 0xFF;

            // Use Paper's EntityScheduler if available (supports Folia)
            viewer.getScheduler().runAtFixedRate(plugin, (task) -> {
                // Check if player is still online/valid (handled by scheduler usually, but double check)
                if (!viewer.isValid()) {
                    task.cancel();
                    return;
                }
                
                int currentTick = tick.getAndIncrement();

                if (currentTick >= duration) {
                    ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(display.getId());
                    sendPacket(viewer, removePacket);
                    task.cancel();
                    return;
                }

                // Phase 1: Pop Up (Tick 1) -> Up 0.6, Scale 1.2
                if (currentTick == 1) {
                    setInterpolation(display, 0, 3);
                    display.setTransformation(new Transformation(
                        new Vector3f(0.0f, 0.6f, 0.0f), 
                        new Quaternionf(), 
                        new Vector3f(1.2f, 1.2f, 1.2f), 
                        new Quaternionf()
                    ));
                    sendMetadataUpdate(viewer, display);
                }

                // Phase 2: Fall (Tick 4) -> Down to -0.4, Scale 1.0
                // Duration: Remaining ticks
                if (currentTick == 4) {
                    int remainingTicks = duration - 4;
                    if (remainingTicks < 1) remainingTicks = 1;

                    setInterpolation(display, 0, remainingTicks);
                    display.setTransformation(new Transformation(
                        new Vector3f(0.0f, -0.4f, 0.0f), 
                        new Quaternionf(), 
                        new Vector3f(1.0f, 1.0f, 1.0f), 
                        new Quaternionf()
                    ));
                    sendMetadataUpdate(viewer, display);
                }

                // Fade Out Logic (Last 5 ticks)
                if (currentTick > duration - 5) {
                    double progress = (double)(duration - currentTick) / 5.0; // 1.0 to 0.0
                    int alpha = (int) (255 * progress);
                    if (alpha < 0) alpha = 0;
                    
                    // Text Opacity
                    display.setTextOpacity((byte) alpha);
                    
                    // Background Opacity
                    int bgAlpha = (int) (initialBgAlpha * progress);
                    int newBgColor = (bgAlpha << 24) | (backgroundColor & 0x00FFFFFF);
                    
                    setBackgroundColor(display, newBgColor);
                    
                    // Send Metadata Update
                    sendMetadataUpdate(viewer, display);
                }
            }, null, 1L, 1L); // Initial delay 1 tick, period 1 tick
            
        } catch (Exception e) {
            MidgardLogger.error("Erro ao enviar indicador de dano via NMS", e);
        }
    }
}
