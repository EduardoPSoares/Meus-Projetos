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

@Entry("wave_start_event", "Acionado quando uma nova wave de mobs começa", Colors.ORANGE, "mdi:waves")
/**
 * O `Wave Start Event` é disparado no início de cada wave.
 *
 * ## Como isso pode ser usado?
 * Exiba anúncios de wave, mude a música ou aplique efeitos temporários aos jogadores.
 */
class WaveStartEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se definido, só aciona para um número de wave específico (começa em 1). 0 = todas as waves.")
    val waveNumber: Int = 0,
    @Help("Se definido, só aciona para uma configuração de dungeon específica. Vazio = todas as dungeons.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireWaveStartEvent(player: Player, instance: DungeonInstance) {
    val entries = Query.find<WaveStartEventEntry>().filter { entry ->
        (entry.dungeonFilter.id.isEmpty() || entry.dungeonFilter.id == instance.dungeonId)
            && (entry.waveNumber == 0 || entry.waveNumber == instance.currentWave)
    }.toList()
    entries.triggerAllFor(player, context())
}
