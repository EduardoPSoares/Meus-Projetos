package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.DungeonState
import me.ray.midgardDungeon.engine.MythicMobsManager
import me.ray.midgardDungeon.entries.statics.BossConfigEntry
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

@Entry("spawn_boss_action", "Spawna um mob boss com gerenciamento de fases", Colors.RED, "mdi:crown")
class SpawnBossAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração do boss para spawnar.")
    val bossConfig: Ref<BossConfigEntry> = emptyRef(),
    @Help("Posição de spawn. Se vazio, usa a localização atual do jogador.")
    val spawnPosition: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Se deve usar a localização atual do jogador como ponto de spawn.")
    val usePlayerLocation: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = bossConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return
        val baseMob = config.baseMob.get() ?: return

        val spawnLoc = if (usePlayerLocation) {
            player.location
        } else {
            val pos = spawnPosition.get(player)
            Location(instance.world, pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
        }

        // Tentar spawnar via MythicMobs se o mob tiver mythicMobId definido
        val entity = if (baseMob.mythicMobId.isNotEmpty() && MythicMobsManager.isAvailable()) {
            MythicMobsManager.spawnMob(baseMob.mythicMobId, spawnLoc)
                ?: instance.world.spawnEntity(spawnLoc, baseMob.entityType) // Fallback vanilla
        } else {
            instance.world.spawnEntity(spawnLoc, baseMob.entityType)
        }
        instance.trackEntity(entity)

        if (entity is LivingEntity) {
            if (baseMob.displayName.isNotEmpty()) {
                entity.customName(Component.text(baseMob.displayName))
                entity.isCustomNameVisible = true
            }
            entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = baseMob.maxHealth
            entity.health = baseMob.maxHealth
            entity.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                it.baseValue = it.baseValue * baseMob.speedMultiplier
            }
            entity.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = baseMob.attackDamage

            instance.transition(DungeonState.BOSS_FIGHT)
            instance.registerBoss(entity.uniqueId, config)

            if (config.showBossBar) {
                val barColor = try {
                    BossBar.Color.valueOf(config.bossBarColor.uppercase())
                } catch (_: Exception) {
                    BossBar.Color.RED
                }

                val bossBossBar = BossBar.bossBar(
                    Component.text(baseMob.displayName.ifEmpty { "Chefe" }, NamedTextColor.RED),
                    1.0f,
                    barColor,
                    BossBar.Overlay.PROGRESS
                )
                instance.setBossBossBar(bossBossBar)
                instance.getOnlinePlayers().forEach { p -> p.showBossBar(bossBossBar) }
            }

            instance.getOnlinePlayers().forEach { p ->
                p.sendMessage(Component.text("Um boss apareceu!", NamedTextColor.DARK_RED))
            }
        }
    }
}
