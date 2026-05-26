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
import org.bukkit.entity.LivingEntity

@Entry("spawn_mythic_boss_action", "Spawna um boss usando MythicMobs", Colors.RED, "mdi:crown")
/**
 * A ação `Spawn Mythic Boss` spawna um boss definido no MythicMobs.
 *
 * ## Como isso pode ser usado?
 * Crie bosses poderosos configurados no MythicMobs com skills, drops e
 * mecânicas customizadas. O sistema de fases da dungeon rastreia a entidade
 * e sincroniza com a barra de boss e os estados da dungeon.
 * Requer MythicMobs instalado.
 */
class SpawnMythicBossAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("ID do mob no MythicMobs (ex: 'SkeletonKing').")
    val mythicMobId: String = "",
    @Help("Nível do MythicMob (0 = padrão do MythicMobs).")
    val mythicMobLevel: Double = 0.0,
    @Help("Configuração de boss para fases (opcional).")
    val bossConfig: Ref<BossConfigEntry> = emptyRef(),
    @Help("Nome exibido na barra de boss.")
    val displayName: String = "Boss",
    @Help("Cor da barra de boss (RED, YELLOW, PURPLE, etc).")
    val barColor: String = "RED",
    @Help("Se deve mostrar uma barra de boss personalizada.")
    val showBossBar: Boolean = true,
    @Help("Posição de spawn. Ignorada se usePlayerLocation = true.")
    val spawnPosition: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Se deve usar a localização atual do jogador como ponto de spawn.")
    val usePlayerLocation: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!MythicMobsManager.isAvailable()) {
            player.sendMessage(Component.text("MythicMobs não está disponível!", NamedTextColor.RED))
            return
        }

        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: run {
            player.sendMessage(Component.text("Você não está em uma dungeon!", NamedTextColor.RED))
            return
        }

        val spawnLoc = if (usePlayerLocation) {
            player.location
        } else {
            val pos = spawnPosition.get(player)
            Location(instance.world, pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
        }

        val entity = if (mythicMobLevel > 0) {
            MythicMobsManager.spawnMob(mythicMobId, spawnLoc, mythicMobLevel)
        } else {
            MythicMobsManager.spawnMob(mythicMobId, spawnLoc)
        }

        if (entity == null) {
            player.sendMessage(Component.text("Falha ao spawnar MythicMob '$mythicMobId'!", NamedTextColor.RED))
            return
        }

        instance.trackEntity(entity)
        instance.transition(DungeonState.BOSS_FIGHT)

        // Registrar boss no sistema de fases
        val config = bossConfig.get()
        if (config != null && entity is LivingEntity) {
            instance.registerBoss(entity.uniqueId, config)
        }

        // Barra de boss
        if (showBossBar) {
            val color = try {
                BossBar.Color.valueOf(barColor.uppercase())
            } catch (_: Exception) {
                BossBar.Color.RED
            }

            val bossBossBar = BossBar.bossBar(
                Component.text(displayName, NamedTextColor.RED),
                1.0f,
                color,
                BossBar.Overlay.PROGRESS
            )
            instance.setBossBossBar(bossBossBar)
            instance.getOnlinePlayers().forEach { p -> p.showBossBar(bossBossBar) }
        }

        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(
                Component.text("Um boss apareceu: ", NamedTextColor.DARK_RED)
                    .append(Component.text(displayName, NamedTextColor.RED))
            )
        }
    }
}
