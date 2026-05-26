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
import me.ray.midgardDungeon.engine.TrapManager
import org.bukkit.entity.Player

@Entry("trap_triggered_event", "Acionado quando um jogador ativa uma armadilha", Colors.RED, "mdi:mine")
/**
 * O `Trap Triggered Event` é disparado quando um jogador ativa uma armadilha.
 *
 * ## Como isso pode ser usado?
 * Spawne mobs extras, toque sons de alerta ou aplique efeitos adicionais.
 */
class TrapTriggeredEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Filtro por tipo de armadilha. Se vazio, aciona para todos os tipos.")
    val trapTypeFilter: String = "",
) : EventEntry

fun fireTrapTriggeredEvent(player: Player, trapType: TrapManager.TrapType) {
    val entries = Query.find<TrapTriggeredEventEntry>().filter { entry ->
        entry.trapTypeFilter.isEmpty() || entry.trapTypeFilter == trapType.name
    }.toList()
    entries.triggerAllFor(player, context())
}
