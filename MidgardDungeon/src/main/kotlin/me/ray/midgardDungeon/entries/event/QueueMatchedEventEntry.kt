package me.ray.midgardDungeon.entries.event

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.EventEntry
import com.typewritermc.engine.paper.entry.triggerAllFor
import com.typewritermc.core.interaction.context
import org.bukkit.entity.Player

@Entry("queue_matched_event", "Acionado quando o matchmaking forma um grupo", Colors.CYAN, "mdi:account-group-outline")
/**
 * O `Queue Matched Event` é disparado quando o sistema de matchmaking combina jogadores.
 *
 * ## Como isso pode ser usado?
 * Notifique jogadores, teleporte-os para a área de preparação.
 */
class QueueMatchedEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Filtro por ID da dungeon. Vazio = todas.")
    val dungeonIdFilter: String = "",
) : EventEntry

fun fireQueueMatchedEvent(player: Player, dungeonId: String) {
    val entries = Query.find<QueueMatchedEventEntry>().filter { entry ->
        entry.dungeonIdFilter.isEmpty() || entry.dungeonIdFilter == dungeonId
    }.toList()
    entries.triggerAllFor(player, context())
}
