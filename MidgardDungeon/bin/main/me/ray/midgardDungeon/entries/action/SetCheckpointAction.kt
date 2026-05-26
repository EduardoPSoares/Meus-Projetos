package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("set_checkpoint_action", "Define o checkpoint de respawn para o grupo", Colors.GREEN, "mdi:flag-checkered")
/**
 * A ação `Set Checkpoint` salva uma localização de respawn para o grupo.
 *
 * ## Como isso pode ser usado?
 * Coloque checkpoints nas entradas das salas para que os jogadores respawnem lá ao morrer.
 */
class SetCheckpointAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A posição do checkpoint. Se vazio, usa a posição atual do jogador.")
    val checkpointPosition: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Se deve usar a localização atual do jogador ao invés da posição configurada.")
    val usePlayerLocation: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val location = if (usePlayerLocation) {
            player.location
        } else {
            val pos = checkpointPosition.get(player)
            org.bukkit.Location(instance.world, pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
        }

        instance.setCheckpoint(location)
        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(Component.text("Checkpoint salvo!", NamedTextColor.GREEN))
        }
    }
}
