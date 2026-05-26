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
import me.ray.midgardDungeon.engine.KeyManager
import org.bukkit.entity.Player

@Entry("has_key_fact", "Verifica se o jogador ou grupo possui uma chave específica", Colors.PURPLE, "mdi:key-variant")
class HasKeyFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("ID da chave a verificar.")
    val keyId: String = "",
    @Help("Se deve verificar em todo o grupo (true) ou só no jogador (false).")
    val checkParty: Boolean = true,
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return FactData(0)
        val hasKey = if (checkParty) {
            KeyManager.partyHasKey(instance.id, keyId)
        } else {
            KeyManager.hasKey(instance.id, player.uniqueId, keyId)
        }
        return FactData(if (hasKey) 1 else 0)
    }
}
