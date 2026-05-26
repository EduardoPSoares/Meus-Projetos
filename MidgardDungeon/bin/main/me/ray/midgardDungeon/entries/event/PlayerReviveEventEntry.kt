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

@Entry("player_revive_event", "Acionado quando um jogador revive no checkpoint", Colors.GREEN, "mdi:heart-pulse")
/**
 * O `Player Revive Event` é disparado quando um jogador usa uma vida e revive.
 *
 * ## Como isso pode ser usado?
 * Aplique penalidades de revive, restaure buffs ou notifique o grupo.
 */
class PlayerReviveEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : EventEntry

fun firePlayerReviveEvent(player: Player) {
    val entries = Query.find<PlayerReviveEventEntry>().toList()
    entries.triggerAllFor(player, context())
}
