package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Sistema de armadilhas para dungeons.
 */
object TrapManager {

    enum class TrapType {
        DAMAGE,
        SLOWNESS,
        POISON,
        BLINDNESS,
        TELEPORT,
        MOB_SPAWN,
    }

    data class Trap(
        val id: UUID = UUID.randomUUID(),
        val type: TrapType,
        val location: Location,
        val radius: Double = 2.0,
        val damage: Double = 4.0,
        val effectDuration: Int = 60, // tiques
        val effectAmplifier: Int = 0,
        val oneTime: Boolean = true,
        var triggered: Boolean = false,
    )

    private val instanceTraps = ConcurrentHashMap<UUID, CopyOnWriteArrayList<Trap>>()
    private var checkTaskId: Int = -1

    fun initialize() {
        val plugin = MidgardPlugin.instance ?: return
        checkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            checkTraps()
        }, 5L, 5L) // Verificar a cada 5 ticks
    }

    fun shutdown() {
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId)
            checkTaskId = -1
        }
        instanceTraps.clear()
    }

    fun placeTrap(instanceId: UUID, trap: Trap) {
        instanceTraps.getOrPut(instanceId) { CopyOnWriteArrayList() }.add(trap)
    }

    fun removeInstance(instanceId: UUID) {
        instanceTraps.remove(instanceId)
    }

    fun getTraps(instanceId: UUID): List<Trap> {
        return instanceTraps[instanceId] ?: emptyList()
    }

    private fun checkTraps() {
        for (instance in DungeonManager.getActiveInstances()) {
            if (instance.state.isFinished()) continue
            val traps = instanceTraps[instance.id] ?: continue

            for (player in instance.getOnlinePlayers()) {
                for (trap in traps) {
                    if (trap.oneTime && trap.triggered) continue
                    // Ensure same world before distance check to avoid IllegalArgumentException
                    if (player.world != trap.location.world) continue
                    if (player.location.distanceSquared(trap.location) <= trap.radius * trap.radius) {
                        triggerTrap(player, trap, instance)
                    }
                }
            }
        }
    }

    private fun triggerTrap(player: Player, trap: Trap, instance: DungeonInstance) {
        if (trap.oneTime && trap.triggered) return
        trap.triggered = true

        player.world.spawnParticle(Particle.SMOKE, trap.location, 30, 0.5, 0.5, 0.5, 0.02)
        player.playSound(trap.location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f)

        when (trap.type) {
            TrapType.DAMAGE -> {
                player.damage(trap.damage)
                player.sendMessage(Component.text("Você ativou uma armadilha!", NamedTextColor.RED))
            }
            TrapType.SLOWNESS -> {
                player.addPotionEffect(
                    org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS,
                        trap.effectDuration, trap.effectAmplifier
                    )
                )
                player.sendMessage(Component.text("Armadilha de lentidão ativada!", NamedTextColor.YELLOW))
            }
            TrapType.POISON -> {
                player.addPotionEffect(
                    org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.POISON,
                        trap.effectDuration, trap.effectAmplifier
                    )
                )
                player.sendMessage(Component.text("Armadilha de veneno ativada!", NamedTextColor.DARK_GREEN))
            }
            TrapType.BLINDNESS -> {
                player.addPotionEffect(
                    org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS,
                        trap.effectDuration, trap.effectAmplifier
                    )
                )
                player.sendMessage(Component.text("Armadilha de cegueira ativada!", NamedTextColor.DARK_GRAY))
            }
            TrapType.TELEPORT -> {
                player.teleport(instance.getCheckpoint())
                player.sendMessage(Component.text("Armadilha de teleporte! Voltou ao checkpoint!", NamedTextColor.LIGHT_PURPLE))
            }
            TrapType.MOB_SPAWN -> {
                player.sendMessage(Component.text("Armadilha ativou mobs!", NamedTextColor.RED))
                // Spawn de mobs gerenciado pelo TrapTriggeredEvent no Typewriter
            }
        }
    }
}
