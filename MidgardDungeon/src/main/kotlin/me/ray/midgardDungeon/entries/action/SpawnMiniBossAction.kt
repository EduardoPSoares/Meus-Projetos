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
import me.ray.midgardDungeon.engine.StatsManager
import me.ray.midgardDungeon.engine.MythicMobsManager
import me.ray.midgardDungeon.entries.statics.MobConfigEntry
import me.ray.midgardDungeon.entries.statics.LootTableEntry
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Entry("spawn_mini_boss_action", "Spawna um mini-boss com barra de vida e loot especial", Colors.RED, "mdi:sword-cross")
class SpawnMiniBossAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração do mob para o mini-boss.")
    val mobConfig: Ref<MobConfigEntry> = emptyRef(),
    @Help("Multiplicador de vida (aplicado sobre a config do mob).")
    val healthMultiplier: Double = 3.0,
    @Help("Multiplicador de dano.")
    val damageMultiplier: Double = 1.5,
    @Help("Nome exibido na barra de boss.")
    val displayTitle: String = "Mini-Boss",
    @Help("Cor da barra de boss (RED, YELLOW, PURPLE, etc).")
    val barColor: String = "YELLOW",
    @Help("Se deve mostrar uma barra de boss.")
    val showBossBar: Boolean = true,
    @Help("Loot especial ao derrotar o mini-boss.")
    val miniBossLoot: Ref<LootTableEntry> = emptyRef(),
    @Help("Posição de spawn. Ignorada se usePlayerLocation = true.")
    val spawnPosition: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Se deve usar a localização do jogador como ponto de spawn.")
    val usePlayerLocation: Boolean = true,
) : ActionEntry {

    companion object {
        // Rastreia mini-bosses ativos: entityUUID -> MiniBossData
        val activeMiniBosses = ConcurrentHashMap<UUID, MiniBossData>()

        data class MiniBossData(
            val instanceId: UUID,
            val bossBar: BossBar?,
            val lootRef: Ref<LootTableEntry>,
            val maxHealth: Double,
        )

        fun onMiniBossDamage(entityId: UUID, currentHealth: Double) {
            val data = activeMiniBosses[entityId] ?: return
            data.bossBar?.progress(
                (currentHealth / data.maxHealth).toFloat().coerceIn(0f, 1f)
            )
        }

        fun onMiniBossDeath(entityId: UUID, killerLocation: Location?) {
            val data = activeMiniBosses.remove(entityId) ?: return

            // Remover barra de boss
            val instance = DungeonManager.getInstance(data.instanceId)
            if (data.bossBar != null && instance != null) {
                instance.getOnlinePlayers().forEach { p ->
                    p.hideBossBar(data.bossBar)
                    p.sendMessage(
                        Component.text("✦ Mini-boss derrotado!", NamedTextColor.GOLD)
                    )
                    p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f)
                }
            }

            // Distribuir loot
            val lootTable = data.lootRef.get()
            if (lootTable != null && killerLocation != null) {
                lootTable.items.forEach { lootItem ->
                    if (Math.random() <= lootItem.weight) {
                        val amount = if (lootItem.minAmount >= lootItem.maxAmount) lootItem.minAmount
                                     else (lootItem.minAmount..lootItem.maxAmount).random()
                        val mat = org.bukkit.Material.valueOf(lootItem.material.uppercase())
                        val stack = org.bukkit.inventory.ItemStack(mat, amount)
                        killerLocation.world.dropItemNaturally(killerLocation, stack)
                    }
                }
            }
        }

        fun shutdown() {
            activeMiniBosses.clear()
        }
    }

    override fun ActionTrigger.execute() {
        val config = mobConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val spawnLoc = if (usePlayerLocation) {
            player.location
        } else {
            val pos = spawnPosition.get(player)
            Location(instance.world, pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
        }

        // Tentar spawnar via MythicMobs se configurado
        val entity = if (config.mythicMobId.isNotEmpty() && MythicMobsManager.isAvailable()) {
            MythicMobsManager.spawnMob(config.mythicMobId, spawnLoc)
                ?: instance.world.spawnEntity(spawnLoc, config.entityType) // Fallback vanilla
        } else {
            instance.world.spawnEntity(spawnLoc, config.entityType)
        }
        instance.trackEntity(entity)

        if (entity is LivingEntity) {
            val finalHealth = config.maxHealth * healthMultiplier
            val displayName = displayTitle.ifEmpty { config.displayName.ifEmpty { "Mini-Boss" } }

            entity.customName(Component.text("✦ $displayName", NamedTextColor.GOLD))
            entity.isCustomNameVisible = true
            entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = finalHealth
            entity.health = finalHealth
            entity.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                it.baseValue = it.baseValue * config.speedMultiplier
            }
            entity.getAttribute(Attribute.ATTACK_DAMAGE)?.let {
                it.baseValue = config.attackDamage * damageMultiplier
            }

            // Barra de boss
            var bossBar: BossBar? = null
            if (showBossBar) {
                val color = try {
                    BossBar.Color.valueOf(barColor.uppercase())
                } catch (_: Exception) {
                    BossBar.Color.YELLOW
                }
                bossBar = BossBar.bossBar(
                    Component.text("✦ $displayName", NamedTextColor.GOLD),
                    1.0f,
                    color,
                    BossBar.Overlay.PROGRESS,
                )
                instance.getOnlinePlayers().forEach { p -> p.showBossBar(bossBar) }
            }

            // Registrar mini-boss
            activeMiniBosses[entity.uniqueId] = MiniBossData(
                instanceId = instance.id,
                bossBar = bossBar,
                lootRef = miniBossLoot,
                maxHealth = finalHealth,
            )

            // Efeitos visuais
            instance.getOnlinePlayers().forEach { p ->
                p.sendMessage(
                    Component.text("✦ Um mini-boss apareceu: ", NamedTextColor.GOLD)
                        .append(Component.text(displayName, NamedTextColor.RED))
                )
                p.showTitle(
                    Title.title(
                        Component.text("✦ MINI-BOSS", NamedTextColor.GOLD),
                        Component.text(displayName, NamedTextColor.RED),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500)),
                    )
                )
                p.playSound(p.location, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f)
            }

            // Partículas no local de spawn
            entity.world.spawnParticle(Particle.FLAME, spawnLoc, 40, 1.0, 1.0, 1.0, 0.05)
        }
    }
}
