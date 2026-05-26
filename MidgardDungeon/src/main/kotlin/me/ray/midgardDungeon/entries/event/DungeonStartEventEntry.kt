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

@Entry("dungeon_start_event", "Acionado quando uma dungeon inicia", Colors.YELLOW, "mdi:play-circle")
/**
 * O `Dungeon Start Event` é acionado quando uma instância de dungeon começa.
 *
 * ## Como isso pode ser usado?
 * Acione cinemáticas, mensagens ou efeitos quando a dungeon começar.
 */
class DungeonStartEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se definido, só aciona para uma configuração de dungeon específica. Vazio = todas as dungeons.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireDungeonStartEvent(player: Player, instance: DungeonInstance) {
    val entries = Query.find<DungeonStartEventEntry>().filter {
        it.dungeonFilter.id.isEmpty() || it.dungeonFilter.id == instance.dungeonId
    }.toList()
    entries.triggerAllFor(player, context())
}
