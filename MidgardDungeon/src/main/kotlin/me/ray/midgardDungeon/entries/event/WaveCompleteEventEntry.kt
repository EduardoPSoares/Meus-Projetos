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

@Entry("wave_complete_event", "Acionado quando uma wave de mobs é eliminada", Colors.GREEN, "mdi:check-all")
/**
 * O `Wave Complete Event` é disparado quando todos os mobs de uma wave são mortos.
 *
 * ## Como isso pode ser usado?
 * Recompense jogadores entre waves, abra portas ou acione diálogos.
 */
class WaveCompleteEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se definido, só aciona para um número de wave específico (começa em 1). 0 = todas as waves.")
    val waveNumber: Int = 0,
    @Help("Se definido, só aciona para uma configuração de dungeon específica. Vazio = todas as dungeons.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireWaveCompleteEvent(player: Player, instance: DungeonInstance) {
    val entries = Query.find<WaveCompleteEventEntry>().filter { entry ->
        (entry.dungeonFilter.id.isEmpty() || entry.dungeonFilter.id == instance.dungeonId)
            && (entry.waveNumber == 0 || entry.waveNumber == instance.currentWave)
    }.toList()
    entries.triggerAllFor(player, context())
}
