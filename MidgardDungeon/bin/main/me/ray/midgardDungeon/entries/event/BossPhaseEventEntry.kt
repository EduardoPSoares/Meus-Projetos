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

@Entry("boss_phase_event", "Acionado quando um boss entra em uma nova fase", Colors.RED, "mdi:fire")
/**
 * O `Boss Phase Event` é disparado quando um boss transiciona entre fases.
 *
 * ## Como isso pode ser usado?
 * Altere mecânicas da arena, spawne mobs adicionais ou exiba avisos.
 */
class BossPhaseEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se definido, só aciona para um número de fase específico (começa em 1). 0 = todas as fases.")
    val phaseNumber: Int = 0,
    @Help("Se definido, só aciona para uma configuração de dungeon específica. Vazio = todas as dungeons.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireBossPhaseEvent(player: Player, instance: DungeonInstance, phaseNumber: Int) {
    val entries = Query.find<BossPhaseEventEntry>().filter { entry ->
        (entry.dungeonFilter.id.isEmpty() || entry.dungeonFilter.id == instance.dungeonId)
            && (entry.phaseNumber == 0 || entry.phaseNumber == phaseNumber)
    }.toList()
    entries.triggerAllFor(player, context())
}
