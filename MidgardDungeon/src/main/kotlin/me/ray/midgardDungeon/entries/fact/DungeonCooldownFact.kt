package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.CooldownManager
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import org.bukkit.entity.Player

@Entry("dungeon_cooldown_fact", "Retorna os segundos restantes de cooldown de uma dungeon", Colors.YELLOW, "mdi:timer-sand")
class DungeonCooldownFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("A configuração da dungeon para verificar o cooldown.")
    val dungeon: Ref<DungeonConfigEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val dungeonId = dungeon.id.takeIf { it.isNotEmpty() } ?: return FactData(0)
        return FactData(CooldownManager.getRemainingSeconds(player.uniqueId, dungeonId).toInt())
    }
}
