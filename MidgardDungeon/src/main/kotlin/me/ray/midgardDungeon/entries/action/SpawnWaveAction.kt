package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.MythicMobsManager
import me.ray.midgardDungeon.entries.event.fireWaveStartEvent
import me.ray.midgardDungeon.entries.statics.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

@Entry("spawn_wave_action", "Spawna uma wave de mobs na dungeon atual", Colors.RED, "mdi:sword-cross")
/**
 * A ação `Spawn Wave` spawna mobs para a wave atual ou especificada.
 *
 * ## Como isso pode ser usado?
 * Spawne mobs automaticamente quando uma sala é entrada ou encadeie múltiplas waves.
 */
class SpawnWaveAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da wave para spawnar. Se vazio, usa a próxima wave da sala atual.")
    val waveConfig: Ref<WaveConfigEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: run {
            player.sendMessage(Component.text("Você não está em uma dungeon!", NamedTextColor.RED))
            return
        }

        val wave = waveConfig.get() ?: return
        val center = instance.spawnLocation

        for (mobRef in wave.mobs) {
            val mobConfig = mobRef.get() ?: continue
            repeat(mobConfig.spawnCount) {
                val offsetX = Random.nextDouble(-wave.spawnRadius, wave.spawnRadius)
                val offsetZ = Random.nextDouble(-wave.spawnRadius, wave.spawnRadius)
                val spawnLoc = Location(
                    center.world, center.x + offsetX, center.y, center.z + offsetZ
                )

                // Integração com MythicMobs (centralizada)
                if (mobConfig.mythicMobId.isNotEmpty() && MythicMobsManager.isAvailable()) {
                    val spawned = MythicMobsManager.spawnMob(mobConfig.mythicMobId, spawnLoc)
                    if (spawned != null) {
                        instance.trackEntity(spawned)
                        return@repeat
                    }
                    // Fallback para vanilla se o mob não existir no MythicMobs
                }

                val entity = center.world.spawnEntity(spawnLoc, mobConfig.entityType)
                instance.trackEntity(entity)

                if (entity is LivingEntity) {
                    if (mobConfig.displayName.isNotEmpty()) {
                        entity.customName(Component.text(mobConfig.displayName))
                        entity.isCustomNameVisible = true
                    }
                    entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = mobConfig.maxHealth
                    entity.health = mobConfig.maxHealth
                    entity.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                        it.baseValue = it.baseValue * mobConfig.speedMultiplier
                    }
                    entity.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = mobConfig.attackDamage
                }
            }
        }

        instance.advanceWave()

        instance.getOnlinePlayers().forEach { p ->
            fireWaveStartEvent(p, instance)
        }
    }

}
