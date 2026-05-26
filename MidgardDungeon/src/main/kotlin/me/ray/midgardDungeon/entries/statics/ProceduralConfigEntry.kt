package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry
import org.bukkit.Material

@Entry("procedural_config", "Define configuração de dungeon procedural", Colors.MEDIUM_PURPLE, "mdi:dice-multiple")
@Tags("procedural_config")
/**
 * A entry `Procedural Config` configura a geração procedural de dungeons.
 *
 * ## Como isso pode ser usado?
 * Configure parâmetros como quantidade de salas, tamanho, materiais e densidade
 * de armadilhas para gerar dungeons únicas a cada run.
 *
 * Ative `useTemplates` e adicione referências a `Room Template` para usar
 * salas pré-construídas por builders em vez de geração básica com blocos.
 */
class ProceduralConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Número de salas a gerar.")
    val roomCount: Int = 5,
    @Help("Tamanho mínimo de sala (blocos). Usado quando não há templates.")
    val roomMinSize: Int = 7,
    @Help("Tamanho máximo de sala (blocos). Usado quando não há templates.")
    val roomMaxSize: Int = 15,
    @Help("Largura do corredor.")
    val corridorWidth: Int = 3,
    @Help("Comprimento do corredor.")
    val corridorLength: Int = 5,
    @Help("Material das paredes (nome do Material Bukkit). Usado para corredores e salas sem template.")
    val wallMaterial: String = "STONE_BRICKS",
    @Help("Material do chão (nome do Material Bukkit). Usado para corredores e salas sem template.")
    val floorMaterial: String = "POLISHED_DEEPSLATE",
    @Help("Material do teto (nome do Material Bukkit). Usado para corredores e salas sem template.")
    val ceilingMaterial: String = "STONE_BRICKS",
    @Help("Altura das salas. Usado quando não há templates.")
    val roomHeight: Int = 5,
    @Help("Se deve colocar tochas nas salas sem template.")
    val hasTorches: Boolean = true,
    @Help("Dificuldade da dungeon (1-5).")
    val difficulty: Int = 1,
    @Help("Se deve incluir sala de boss.")
    val includeBossRoom: Boolean = true,
    @Help("Se deve incluir sala de tesouro.")
    val includeTreasureRoom: Boolean = true,
    @Help("Se deve incluir salas secretas.")
    val includeSecretRooms: Boolean = true,
    @Help("Chance de sala secreta (0.0 a 1.0).")
    val secretRoomChance: Double = 0.3,
    @Help("Densidade de armadilhas (0.0 a 1.0). Apenas em salas sem template.")
    val trapDensity: Double = 0.2,
    @Help("Se deve usar templates de schematics feitos por builders. Requer WorldEdit e Room Templates configurados.")
    val useTemplates: Boolean = false,
    @Help("Templates de salas pré-construídas. O gerador seleciona por tipo e peso aleatório.")
    val roomTemplates: List<Ref<RoomTemplateEntry>> = emptyList(),
) : StaticEntry {
    fun getWallMat(): Material = try { Material.valueOf(wallMaterial) } catch (_: Exception) { Material.STONE_BRICKS }
    fun getFloorMat(): Material = try { Material.valueOf(floorMaterial) } catch (_: Exception) { Material.POLISHED_DEEPSLATE }
    fun getCeilingMat(): Material = try { Material.valueOf(ceilingMaterial) } catch (_: Exception) { Material.STONE_BRICKS }
}
