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
import me.ray.midgardDungeon.engine.TrapManager
import me.ray.midgardDungeon.entries.statics.TrapConfigEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location

@Entry("place_trap_action", "Coloca uma armadilha na dungeon", Colors.RED, "mdi:mine")
/**
 * A ação `Place Trap` posiciona uma armadilha em um local específico na dungeon.
 *
 * ## Como isso pode ser usado?
 * Coloque armadilhas em pontos estratégicos para criar desafios adicionais.
 */
class PlaceTrapAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da armadilha.")
    val trapConfig: Ref<TrapConfigEntry> = emptyRef(),
    @Help("Posição da armadilha.")
    val position: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Usar localização do jogador como posição.")
    val usePlayerLocation: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = trapConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val loc = if (usePlayerLocation) {
            player.location
        } else {
            val pos = position.get(player)
            Location(instance.world, pos.x, pos.y, pos.z)
        }

        val trapType = try { TrapManager.TrapType.valueOf(config.trapType.uppercase()) } catch (_: Exception) {
            TrapManager.TrapType.DAMAGE
        }

        val trap = TrapManager.Trap(
            type = trapType,
            location = loc,
            radius = config.radius,
            damage = config.damage,
            effectDuration = config.effectDuration,
            effectAmplifier = config.effectAmplifier,
            oneTime = config.oneTime,
        )

        TrapManager.placeTrap(instance.id, trap)
    }
}
