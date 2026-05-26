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

@Entry("ready_check_complete_event", "Acionado quando a verificação de prontidão é completada", Colors.GREEN, "mdi:check-circle-outline")
/**
 * O `Ready Check Complete Event` é disparado quando todos os jogadores confirmam prontidão.
 *
 * ## Como isso pode ser usado?
 * Acione o início da dungeon após todos confirmarem.
 */
class ReadyCheckCompleteEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : EventEntry

fun fireReadyCheckCompleteEvent(player: Player) {
    val entries = Query.find<ReadyCheckCompleteEventEntry>().toList()
    entries.triggerAllFor(player, context())
}
