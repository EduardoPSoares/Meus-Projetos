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
import me.ray.midgardDungeon.entries.event.fireRoomEnterEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("teleport_room_action", "Teleporta o grupo para uma localização específica na dungeon", Colors.CYAN, "mdi:map-marker")
/**
 * A ação `Teleport Room` move todos os membros do grupo para uma localização.
 *
 * ## Como isso pode ser usado?
 * Faça a transição do grupo entre salas, teleporte para checkpoints ou arenas de boss.
 */
class TeleportToRoomAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Posição alvo para teleportar o grupo.")
    val targetPosition: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Se deve teleportar todo o grupo ou apenas o jogador que acionou.")
    val teleportParty: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
        val position = targetPosition.get(player)
        val world = instance?.world ?: player.world
        val location = org.bukkit.Location(world, position.x, position.y, position.z, position.yaw, position.pitch)

        if (teleportParty && instance != null) {
            instance.getOnlinePlayers().forEach { p -> p.teleport(location) }
            instance.advanceRoom()
            instance.getOnlinePlayers().forEach { p -> fireRoomEnterEvent(p, instance) }
        } else {
            player.teleport(location)
        }
    }
}
