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
import java.util.*

@Entry("cutscene_complete_event", "Acionado quando uma cutscene termina", Colors.MEDIUM_PURPLE, "mdi:movie-check")
/**
 * O `Cutscene Complete Event` é disparado quando uma cutscene cinematográfica termina.
 *
 * ## Como isso pode ser usado?
 * Use para acionar ações após a cutscene: spawnar o boss, iniciar combate,
 * abrir portas, ou iniciar a próxima fase da dungeon.
 */
class CutsceneCompleteEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Filtrar por ID da cutscene. Vazio = qualquer cutscene.")
    val cutsceneFilter: String = "",
    @Help("Tipo de cutscene para filtrar (BOSS_INTRO, ROOM_ENTER, NARRATIVE, CUSTOM). Vazio = todos.")
    val cutsceneTypeFilter: String = "",
) : EventEntry

fun fireCutsceneCompleteEvent(player: Player, cutsceneId: String, instanceId: UUID?) {
    val entries = Query.find<CutsceneCompleteEventEntry>().filter { entry ->
        (entry.cutsceneFilter.isEmpty() || entry.cutsceneFilter == cutsceneId)
    }.toList()
    entries.triggerAllFor(player, context())
}
