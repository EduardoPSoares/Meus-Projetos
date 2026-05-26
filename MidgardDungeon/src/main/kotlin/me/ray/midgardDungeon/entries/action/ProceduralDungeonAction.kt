package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.*
import me.ray.midgardDungeon.entries.event.fireDungeonStartEvent
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import me.ray.midgardDungeon.entries.statics.ProceduralConfigEntry
import me.ray.midgardDungeon.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import kotlin.random.Random

@Entry("procedural_dungeon_action", "Gera e inicia uma dungeon procedural", Colors.MEDIUM_PURPLE, "mdi:dice-multiple")
/**
 * A ação `Procedural Dungeon` gera uma dungeon aleatória e teleporta o grupo.
 *
 * ## Como isso pode ser usado?
 * Vincule a uma interação com NPC para iniciar dungeons procedurais únicas a cada run.
 */
class ProceduralDungeonAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da dungeon base.")
    val dungeonConfig: Ref<DungeonConfigEntry> = emptyRef(),
    @Help("A configuração procedural.")
    val proceduralConfig: Ref<ProceduralConfigEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = dungeonConfig.get() ?: run {
            player.sendMessage(Component.text("Configuração da dungeon não encontrada!", NamedTextColor.RED))
            return
        }
        val procConfig = proceduralConfig.get() ?: run {
            player.sendMessage(Component.text("Configuração procedural não encontrada!", NamedTextColor.RED))
            return
        }

        if (DungeonManager.isPlayerInDungeon(player.uniqueId)) {
            player.sendMessage(Component.text("Você já está em uma dungeon!", NamedTextColor.RED))
            return
        }

        if (PersistentCooldownManager.isOnCooldown(player.uniqueId, config.id)) {
            val remaining = PersistentCooldownManager.getRemainingSeconds(player.uniqueId, config.id)
            val min = remaining / 60
            val sec = remaining % 60
            player.sendMessage(
                Component.text("Dungeon em cooldown! ${min}m ${sec}s restantes.", NamedTextColor.RED)
            )
            return
        }

        val party = PartyManager.getPartyByPlayer(player.uniqueId)
            ?: PartyManager.createParty(player.uniqueId, config.maxPlayers)

        if (party.size < config.minPlayers) {
            player.sendMessage(
                Component.text("Precisa de pelo menos ${config.minPlayers} jogadores!", NamedTextColor.RED)
            )
            return
        }

        val templateWorld = Bukkit.getWorld(config.templateWorldName)
        if (templateWorld == null) {
            player.sendMessage(Component.text("Mundo template não encontrado!", NamedTextColor.RED))
            return
        }

        // Criar instância
        val tempInstanceId = java.util.UUID.randomUUID()
        val clonedWorld = WorldCloneManager.cloneWorld(config.templateWorldName, tempInstanceId)
        if (clonedWorld == null) {
            player.sendMessage(Component.text("Falha ao clonar o mundo!", NamedTextColor.RED))
            return
        }

        // Resolver templates
        val resolvedTemplates = procConfig.roomTemplates.mapNotNull { it.get() }

        // Gerar dungeon procedural
        val generatorConfig = ProceduralGenerator.ProceduralConfig(
            roomCount = procConfig.roomCount,
            roomMinSize = procConfig.roomMinSize,
            roomMaxSize = procConfig.roomMaxSize,
            corridorWidth = procConfig.corridorWidth,
            corridorLength = procConfig.corridorLength,
            wallMaterial = procConfig.getWallMat(),
            floorMaterial = procConfig.getFloorMat(),
            ceilingMaterial = procConfig.getCeilingMat(),
            roomHeight = procConfig.roomHeight,
            hasTorches = procConfig.hasTorches,
            difficulty = procConfig.difficulty,
            seed = Random.nextLong(),
            includeBossRoom = procConfig.includeBossRoom,
            includeTreasureRoom = procConfig.includeTreasureRoom,
            includeSecretRooms = procConfig.includeSecretRooms,
            secretRoomChance = procConfig.secretRoomChance,
            trapDensity = procConfig.trapDensity,
            templates = resolvedTemplates,
            useTemplates = procConfig.useTemplates,
        )

        val baseLocation = clonedWorld.spawnLocation
        val generated = ProceduralGenerator.generate(clonedWorld, baseLocation, generatorConfig)

        val instance = DungeonManager.createInstance(
            dungeonId = config.id,
            party = party,
            world = clonedWorld,
            spawnLocation = generated.spawnLocation,
        )

        // Remapear mundo clonado para o ID real da instância
        WorldCloneManager.remapClonedWorld(tempInstanceId, instance.id)

        instance.isClonedWorld = true
        instance.isProcedural = true

        // Inicializar subsistemas
        LivesManager.initInstance(instance.id, party.memberIds, 3)
        StatsManager.initInstance(instance.id, party.memberIds)
        KeyManager.initInstance(instance.id)

        if (config.timeLimitSeconds > 0) {
            instance.timeLimitSeconds = config.timeLimitSeconds
        }

        // Colocar armadilhas da geração procedural
        for (room in generated.rooms) {
            for (trapLoc in room.trapLocations) {
                TrapManager.placeTrap(instance.id, TrapManager.Trap(
                    type = TrapManager.TrapType.DAMAGE,
                    location = trapLoc,
                    radius = 1.5,
                    damage = 4.0 * generatorConfig.difficulty,
                ))
            }
        }

        instance.transition(DungeonState.STARTING)

        instance.getOnlinePlayers().forEach { p ->
            p.teleport(generated.spawnLocation)
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            p.sendMessage(Component.text("⚔ DUNGEON PROCEDURAL GERADA! ⚔", NamedTextColor.GOLD))
            p.sendMessage(Component.text("Salas: ${generated.rooms.size}", NamedTextColor.YELLOW))
            if (procConfig.useTemplates && resolvedTemplates.isNotEmpty()) {
                p.sendMessage(Component.text("Modo: Modelos de Construtores", NamedTextColor.GREEN))
            }
            p.sendMessage(Component.text("Semente: ${generated.seed}", NamedTextColor.GRAY))
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            fireDungeonStartEvent(p, instance)
        }

        instance.transition(DungeonState.IN_PROGRESS)
    }
}
