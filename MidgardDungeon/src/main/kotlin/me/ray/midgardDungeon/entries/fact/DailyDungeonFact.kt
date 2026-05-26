package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.DailyDungeonManager
import org.bukkit.entity.Player

@Entry("daily_dungeon_fact", "Verifica se o jogador já completou a dungeon diária (0=não, 1=sim)", Colors.PURPLE, "mdi:calendar-check")
class DailyDungeonFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("Verificar diária (true) ou semanal (false).")
    val checkDaily: Boolean = true,
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val completed = if (checkDaily) {
            DailyDungeonManager.hasDailyCompleted(player.uniqueId.toString())
        } else {
            DailyDungeonManager.hasWeeklyCompleted(player.uniqueId.toString())
        }
        return FactData(if (completed) 1 else 0)
    }
}
