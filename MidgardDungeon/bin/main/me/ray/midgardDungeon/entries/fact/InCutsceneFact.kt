package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.CutsceneManager
import org.bukkit.entity.Player

@Entry("in_cutscene_fact", "Verifica se o jogador está em uma cutscene (0=não, 1=sim)", Colors.PURPLE, "mdi:movie-open-check")
class InCutsceneFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val value = if (CutsceneManager.isInCutscene(player.uniqueId)) 1 else 0
        return FactData(value)
    }
}
