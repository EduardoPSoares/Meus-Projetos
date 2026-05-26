package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.MMOCoreManager
import org.bukkit.entity.Player

@Entry("mmocore_level_fact", "Retorna o nível do jogador no MMOCore", Colors.PURPLE, "mdi:arrow-up-bold-circle")
/**
 * O `MMOCore Level Fact` retorna o nível do jogador no MMOCore.
 *
 * ## Como isso pode ser usado?
 * Use como critério para limitar acesso a dungeons pelo nível do MMOCore.
 * Retorna 0 se MMOCore não estiver disponível.
 */
class MMOCoreLevelFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        return FactData(MMOCoreManager.getLevel(player))
    }
}
