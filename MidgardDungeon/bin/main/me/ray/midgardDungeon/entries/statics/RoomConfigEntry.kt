package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.StaticEntry

enum class RoomType {
    SPAWN,
    NORMAL,
    BOSS,
    TREASURE,
    PUZZLE,
    CORRIDOR
}

@Entry("room_config", "Define uma sala dentro de uma dungeon", Colors.CYAN, "mdi:door")
@Tags("room_config")
/**
 * A entry `Room Config` define uma sala dentro de uma dungeon.
 * Cada sala tem um tipo, ponto de spawn e waves e conexões opcionais.
 *
 * ## Como isso pode ser usado?
 * Crie salas para diferentes fases: uma sala de spawn, salas de combate com waves,
 * uma sala de boss e uma sala de tesouro no final.
 */
class RoomConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("O tipo desta sala (SPAWN, NORMAL, BOSS, TREASURE, PUZZLE, CORRIDOR).")
    val roomType: String = "NORMAL",
    @Help("O ponto de spawn dentro desta sala (relativo ao mundo da dungeon).")
    val spawnPoint: Position = Position.ORIGIN,
    @Help("Waves para spawnar nesta sala. Se vazio, a sala não tem combate.")
    val waves: List<Ref<WaveConfigEntry>> = emptyList(),
    @Help("O ID da próxima sala para transição após completar esta.")
    val nextRoomId: String = "",
    @Help("Tabela de loot para esta sala específica (ex: sala de tesouro).")
    val roomLoot: Ref<LootTableEntry> = emptyRef(),
) : StaticEntry {
    fun getRoomType(): RoomType = try {
        RoomType.valueOf(roomType.uppercase())
    } catch (_: Exception) {
        RoomType.NORMAL
    }
}
