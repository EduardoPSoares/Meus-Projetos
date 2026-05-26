package me.ray.midgardDungeon.entries.event

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.EventEntry
import com.typewritermc.engine.paper.entry.triggerAllFor
import com.typewritermc.core.interaction.context
import me.ray.midgardDungeon.engine.DungeonInstance
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import org.bukkit.entity.Player

@Entry("room_enter_event", "Acionado quando o grupo entra em uma nova sala", Colors.CYAN, "mdi:location-enter")
/**
 * O `Room Enter Event` é disparado quando o grupo se move para uma nova sala.
 *
 * ## Como isso pode ser usado?
 * Exiba nomes de salas, tranque portas atrás ou inicie efeitos específicos da sala.
 */
class RoomEnterEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se definido, só aciona para um número de sala específico (começa em 0). -1 = todas as salas.")
    val roomNumber: Int = -1,
    @Help("Se definido, só aciona para uma configuração de dungeon específica. Vazio = todas as dungeons.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireRoomEnterEvent(player: Player, instance: DungeonInstance) {
    val entries = Query.find<RoomEnterEventEntry>().filter { entry ->
        (entry.dungeonFilter.id.isEmpty() || entry.dungeonFilter.id == instance.dungeonId)
            && (entry.roomNumber == -1 || entry.roomNumber == instance.currentRoom)
    }.toList()
    entries.triggerAllFor(player, context())
}
