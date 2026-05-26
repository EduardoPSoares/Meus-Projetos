package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.ProgressionManager
import org.bukkit.entity.Player

@Entry("dungeon_unlock_fact", "Verifica se o jogador desbloqueou uma dungeon", Colors.PURPLE, "mdi:lock-open")
/**
 * O `Dungeon Unlock Fact` retorna 1 se a dungeon está desbloqueada, 0 se não.
 *
 * ## Como isso pode ser usado?
 * Use como critério para bloquear acesso a dungeons que exigem pré-requisitos.
 */
class DungeonUnlockFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("O ID da dungeon para verificar desbloqueio.")
    val dungeonId: String = "",
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val unlocked = ProgressionManager.isUnlocked(player.uniqueId, dungeonId)
        return FactData(if (unlocked) 1 else 0)
    }
}
