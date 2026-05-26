package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("loot_table", "Define uma tabela de loot com itens ponderados", Colors.YELLOW, "mdi:treasure-chest")
@Tags("loot_table")
/**
 * A entry `Loot Table` define um conjunto de recompensas possíveis.
 *
 * ## Como isso pode ser usado?
 * Crie tabelas de loot para completar salas, matar bosses ou completar dungeons.
 * Cada item tem um peso/probabilidade e quantidade mínima/máxima.
 */
class LootTableEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Os itens nesta tabela de loot.")
    val items: List<LootItem> = emptyList(),
    @Help("Quantos itens sortear desta tabela.")
    val rollCount: Int = 1,
    @Help("Se cada jogador recebe seu próprio sorteio (true) ou loot compartilhado (false).")
    val perPlayer: Boolean = true,
) : StaticEntry

data class LootItem(
    @Help("O nome do material do item (ex: DIAMOND_SWORD).")
    val material: String = "STONE",
    @Help("Nome de exibição personalizado (formato MiniMessage).")
    val displayName: String = "",
    @Help("Linhas de lore (formato MiniMessage).")
    val lore: List<String> = emptyList(),
    @Help("Peso deste item no pool de loot. Maior = mais provável.")
    val weight: Double = 1.0,
    @Help("Quantidade mínima.")
    val minAmount: Int = 1,
    @Help("Quantidade máxima.")
    val maxAmount: Int = 1,
    @Help("Custom model data para resource packs.")
    val customModelData: Int = 0,
)
