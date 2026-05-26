package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.engine.MMOCoreManager
import org.bukkit.entity.Player

@Entry("mmocore_class_fact", "Verifica a classe do jogador no MMOCore", Colors.PURPLE, "mdi:shield-account")
/**
 * O `MMOCore Class Fact` retorna um valor baseado na classe do jogador no MMOCore.
 *
 * ## Como isso pode ser usado?
 * Use como critério para limitar dungeons a classes específicas.
 * Retorna 1 se o jogador é da classe especificada, 0 caso contrário.
 * Se nenhuma classe for especificada, retorna 1 se o jogador tem qualquer classe.
 */
class MMOCoreClassFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("Nome da classe para verificar (vazio = qualquer classe).")
    val className: String = "",
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        if (!MMOCoreManager.isAvailable()) return FactData(0)

        if (className.isEmpty()) {
            // Retorna 1 se tem qualquer classe
            return FactData(if (MMOCoreManager.getClassName(player).isNotEmpty()) 1 else 0)
        }

        return FactData(if (MMOCoreManager.hasClass(player, className)) 1 else 0)
    }
}
