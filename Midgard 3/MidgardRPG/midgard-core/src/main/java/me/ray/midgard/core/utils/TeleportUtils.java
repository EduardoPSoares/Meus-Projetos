package me.ray.midgard.core.utils;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.CompletableFuture;

public class TeleportUtils {

    /**
     * Verifica se um local é seguro para teleporte (sem lava, void ou sufocamento).
     * @param location O local a verificar.
     * @return true se for seguro.
     */
    public static boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        
        // Check Void
        if (location.getY() < location.getWorld().getMinHeight()) {
            return false;
        }
        
        // Check Suffocation
        if (isSolid(feet) || isSolid(head)) {
            return false;
        }
        
        // Check Dangerous Blocks (Lava, Fire, Magma)
        if (isDangerous(feet) || isDangerous(ground)) {
            return false;
        }
        
        return true;
    }
    
    private static boolean isSolid(Block block) {
        return block.getType().isSolid() && block.getType().isOccluding();
    }
    
    private static boolean isDangerous(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE 
               || type == Material.MAGMA_BLOCK || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE
               || type == Material.SWEET_BERRY_BUSH || type == Material.WITHER_ROSE || type == Material.CACTUS;
    }

    /**
     * Teleporta uma entidade de forma segura, suportando Folia e Bukkit padrão.
     *
     * @param entity A entidade a ser teleportada.
     * @param location O local de destino.
     * @return Um CompletableFuture que completa quando o teleporte termina (true se sucesso, false se falha).
     */
    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (Task.isFolia()) {
            entity.teleportAsync(location).thenAccept(future::complete).exceptionally(ex -> {
                MidgardLogger.error("Falha no teleporte assíncrono de entidade", (Throwable) ex);
                future.complete(false);
                return null;
            });
        } else {
            // Em Bukkit padrão, teleport deve ser na main thread.
            // Se já estivermos na main thread, executamos direto.
            // Se não, agendamos.
            if (org.bukkit.Bukkit.isPrimaryThread()) {
                try {
                    boolean result = entity.teleport(location);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            } else {
                Task.sync(entity, () -> {
                    try {
                        boolean result = entity.teleport(location);
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            }
        }
        return future;
    }

    /**
     * Teleporta uma entidade de forma segura com causa de teleporte.
     *
     * @param entity A entidade.
     * @param location O local.
     * @param cause A causa do teleporte.
     * @return Future com resultado.
     */
    public static CompletableFuture<Boolean> teleport(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (Task.isFolia()) {
            entity.teleportAsync(location, cause).thenAccept(future::complete).exceptionally(ex -> {
                MidgardLogger.error("Falha no teleporte assíncrono com causa " + cause, (Throwable) ex);
                future.complete(false);
                return null;
            });
        } else {
            if (org.bukkit.Bukkit.isPrimaryThread()) {
                try {
                    boolean result = entity.teleport(location, cause);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            } else {
                Task.sync(entity, () -> {
                    try {
                        boolean result = entity.teleport(location, cause);
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            }
        }
        return future;
    }
}
