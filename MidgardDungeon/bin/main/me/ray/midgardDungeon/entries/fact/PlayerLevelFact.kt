package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.ProgressionManager
import org.bukkit.entity.Player

@Entry("player_level_fact", "Retorna o nível de progressão do jogador", Colors.PURPLE, "mdi:arrow-up-bold-circle")
/**
 * O `Player Level Fact` retorna o nível de progressão da dungeon do jogador.
 *
 * ## Como isso pode ser usado?
 * Use como critério para limitar acesso a dungeons por nível.
 */
class PlayerLevelFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        return FactData(ProgressionManager.getLevel(player.uniqueId))
    }
}
