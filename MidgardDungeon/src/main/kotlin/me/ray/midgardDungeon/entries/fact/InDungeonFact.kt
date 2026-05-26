package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import org.bukkit.entity.Player

@Entry("in_dungeon_fact", "Verifica se o jogador está atualmente dentro de uma dungeon", Colors.PURPLE, "mdi:castle")
class InDungeonFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("Se definido, só corresponde a esta dungeon específica. Vazio = qualquer dungeon.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return FactData(0)
        val filterId = dungeonFilter.id
        if (filterId.isNotEmpty() && instance.dungeonId != filterId) return FactData(0)
        return FactData(1)
    }
}
