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

@Entry("key_used_event", "Acionado quando uma chave é usada", Colors.YELLOW, "mdi:key-change")
/**
 * O `Key Used Event` é disparado quando uma chave é consumida para desbloquear algo.
 *
 * ## Como isso pode ser usado?
 * Acione abertura de portas, ativação de mecanismos ou diálogos especiais.
 */
class KeyUsedEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Filtro por ID da chave. Vazio = todas as chaves.")
    val keyIdFilter: String = "",
) : EventEntry

fun fireKeyUsedEvent(player: Player, keyId: String) {
    val entries = Query.find<KeyUsedEventEntry>().filter { entry ->
        entry.keyIdFilter.isEmpty() || entry.keyIdFilter == keyId
    }.toList()
    entries.triggerAllFor(player, context())
}
