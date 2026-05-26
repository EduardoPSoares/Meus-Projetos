package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("room_template", "Define um template de sala feito por builders", Colors.CYAN, "mdi:floor-plan")
@Tags("room_template")
/**
 * A entry `Room Template` define uma sala pré-construída por builders que pode ser
 * usada pelo gerador procedural.
 *
 * ## Como isso pode ser usado?
 * Builders constroem salas bonitas, salvam como schematic com WorldEdit
 * (//copy + //schematic save nome), e referenciam aqui pelo nome do arquivo.
 * O gerador procedural cola essas salas aleatoriamente para criar dungeons únicas
 * mas visualmente bonitas.
 */
class RoomTemplateEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Nome do arquivo schematic (sem extensão). Ex: 'sala_boss_dragao'")
    val schematicName: String = "",
    @Help("Tipo de sala que este template representa.")
    val roomType: String = "NORMAL",
    @Help("Largura do template em blocos (eixo X).")
    val width: Int = 10,
    @Help("Profundidade do template em blocos (eixo Z).")
    val depth: Int = 10,
    @Help("Altura do template em blocos (eixo Y).")
    val height: Int = 6,
    @Help("Offset X do ponto de spawn dentro da sala (relativo à origem).")
    val spawnOffsetX: Double = 5.0,
    @Help("Offset Y do ponto de spawn dentro da sala (relativo à origem).")
    val spawnOffsetY: Double = 1.0,
    @Help("Offset Z do ponto de spawn dentro da sala (relativo à origem).")
    val spawnOffsetZ: Double = 5.0,
    @Help("Offset X do ponto de conexão de entrada (onde o corredor chega).")
    val entryOffsetX: Int = 0,
    @Help("Offset Y do ponto de conexão de entrada.")
    val entryOffsetY: Int = 1,
    @Help("Offset Z do ponto de conexão de entrada.")
    val entryOffsetZ: Int = 5,
    @Help("Offset X do ponto de conexão de saída (onde o corredor sai).")
    val exitOffsetX: Int = 9,
    @Help("Offset Y do ponto de conexão de saída.")
    val exitOffsetY: Int = 1,
    @Help("Offset Z do ponto de conexão de saída.")
    val exitOffsetZ: Int = 5,
    @Help("Peso para seleção aleatória (maior = mais chance de ser escolhido).")
    val weight: Double = 1.0,
    @Help("Descrição visual para identificação no painel.")
    val description: String = "",
) : StaticEntry {
    fun getRoomType(): RoomType = try {
        RoomType.valueOf(roomType.uppercase())
    } catch (_: Exception) {
        RoomType.NORMAL
    }
}
