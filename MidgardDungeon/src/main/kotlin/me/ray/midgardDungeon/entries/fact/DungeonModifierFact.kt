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
import me.ray.midgardDungeon.engine.ModifierManager
import org.bukkit.entity.Player

@Entry("dungeon_modifier_fact", "Retorna quantidade de modificadores ativos na dungeon", Colors.PURPLE, "mdi:tune-variant")
class DungeonModifierFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return FactData(0)
        return FactData(ModifierManager.getModifiers(instance.id).size)
    }
}
