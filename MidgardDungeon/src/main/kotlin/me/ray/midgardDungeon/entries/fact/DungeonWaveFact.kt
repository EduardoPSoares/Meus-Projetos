package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.DungeonManager
import org.bukkit.entity.Player

@Entry("dungeon_wave_fact", "Retorna o número da wave atual da dungeon", Colors.ORANGE, "mdi:waves")
class DungeonWaveFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: com.typewritermc.core.entries.Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            ?: return FactData(0)
        return FactData(instance.currentWave)
    }
}
