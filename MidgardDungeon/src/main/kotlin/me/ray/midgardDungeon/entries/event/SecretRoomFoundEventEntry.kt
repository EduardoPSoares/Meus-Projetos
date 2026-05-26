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

@Entry("secret_room_found_event", "Acionado quando uma sala secreta é descoberta", Colors.MEDIUM_PURPLE, "mdi:map-marker-question")
/**
 * O `Secret Room Found Event` é disparado quando o grupo descobre uma sala secreta.
 *
 * ## Como isso pode ser usado?
 * Revele loot especial, toque efeitos ou desbloqueie conquistas.
 */
class SecretRoomFoundEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : EventEntry

fun fireSecretRoomFoundEvent(player: Player) {
    val entries = Query.find<SecretRoomFoundEventEntry>().toList()
    entries.triggerAllFor(player, context())
}
