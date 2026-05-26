package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("dungeon_config", "Define a configuração completa de uma dungeon", Colors.BLUE, "mdi:castle")
@Tags("dungeon_config")
/**
 * A entry `Dungeon Config` define todas as propriedades de uma dungeon.
 * Este é o nó principal de configuração — vincule salas, waves, bosses e loot a partir daqui.
 *
 * ## Como isso pode ser usado?
 * Configure uma dungeon com contagem mínima/máxima de jogadores, cooldowns, dificuldade,
 * e vincule a entries de Sala, Wave, Boss e Loot para criar uma experiência completa de dungeon.
 */
class DungeonConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Número mínimo de jogadores para iniciar.")
    val minPlayers: Int = 1,
    @Help("Número máximo de jogadores permitidos.")
    val maxPlayers: Int = 4,
    @Help("Nível mínimo do jogador para entrar.")
    val minLevel: Int = 1,
    @Help("Cooldown entre runs para o mesmo jogador (em segundos).")
    val cooldownSeconds: Int = 3600,
    @Help("Limite de tempo da dungeon em segundos. 0 = sem limite.")
    val timeLimitSeconds: Int = 0,
    @Help("A lista de salas em ordem.")
    val rooms: List<Ref<RoomConfigEntry>> = emptyList(),
    @Help("A tabela de loot concedida ao completar.")
    val completionLoot: Ref<LootTableEntry> = emptyRef(),
    @Help("Nome do mundo usado para instanciar esta dungeon.")
    val templateWorldName: String = "",
    @Help("Configuração de escalonamento de dificuldade. Deixe vazio para usar o padrão.")
    val difficulty: Ref<DifficultyConfigEntry> = emptyRef(),
    @Help("Classe do MMOCore necessária para entrar (vazio = sem restrição).")
    val mmocoreRequiredClass: String = "",
    @Help("Nível mínimo do MMOCore para entrar (0 = sem restrição).")
    val mmocoreMinLevel: Int = 0,
) : StaticEntry
