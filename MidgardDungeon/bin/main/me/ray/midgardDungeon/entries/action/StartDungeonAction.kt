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
import me.ray.midgardDungeon.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

@Entry("start_dungeon_action", "Inicia uma dungeon para o grupo do jogador", Colors.RED, "mdi:play")
/**
 * A ação `Start Dungeon` cria uma nova instância de dungeon e teleporta o grupo.
 *
 * ## Como isso pode ser usado?
 * Vincule esta ação a uma interação com NPC ou comando para iniciar uma dungeon.
 */
class StartDungeonAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da dungeon para iniciar.")
    val dungeonConfig: Ref<DungeonConfigEntry> = emptyRef(),
    @Help("Clonar o mundo para instância isolada.")
    val cloneWorld: Boolean = false,
    @Help("Número máximo de vidas por jogador (0 = infinito).")
    val maxLives: Int = 0,
    @Help("Custo de entrada em moeda (0 = grátis).")
    val entryCost: Double = 0.0,
    @Help("Nível mínimo do MMOCore para entrar (0 = sem restrição, requer MMOCore).")
    val mmocoreMinLevel: Int = 0,
    @Help("Classe do MMOCore necessária para entrar (vazio = sem restrição, requer MMOCore).")
    val mmocoreRequiredClass: String = "",
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = dungeonConfig.get() ?: run {
            player.sendMessage(Component.text("Configuração da dungeon não encontrada!", NamedTextColor.RED))
            return
        }

        if (DungeonManager.isPlayerInDungeon(player.uniqueId)) {
            player.sendMessage(Component.text("Você já está em uma dungeon!", NamedTextColor.RED))
            return
        }

        // Verificar cooldown persistente primeiro, fallback para memória
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

        // Verificar nível mínimo do MMOCore
        if (mmocoreMinLevel > 0 && MMOCoreManager.isAvailable()) {
            val underLevel = party.memberIds.mapNotNull { memberId ->
                val member = Bukkit.getPlayer(memberId) ?: return@mapNotNull null
                if (!MMOCoreManager.hasMinLevel(member, mmocoreMinLevel)) member.name else null
            }
            if (underLevel.isNotEmpty()) {
                player.sendMessage(
                    Component.text("Nível mínimo MMOCore: $mmocoreMinLevel. Jogadores abaixo: ${underLevel.joinToString(", ")}", NamedTextColor.RED)
                )
                return
            }
        }

        // Verificar classe do MMOCore
        if (mmocoreRequiredClass.isNotEmpty() && MMOCoreManager.isAvailable()) {
            val wrongClass = party.memberIds.mapNotNull { memberId ->
                val member = Bukkit.getPlayer(memberId) ?: return@mapNotNull null
                if (!MMOCoreManager.hasClass(member, mmocoreRequiredClass)) member.name else null
            }
            if (wrongClass.isNotEmpty()) {
                player.sendMessage(
                    Component.text("Classe necessária: $mmocoreRequiredClass. Jogadores sem: ${wrongClass.joinToString(", ")}", NamedTextColor.RED)
                )
                return
            }
        }

        // Verificar custo de entrada (Vault)
        if (entryCost > 0 && VaultManager.isAvailable()) {
            val allCanAfford = party.memberIds.all { memberId ->
                val member = Bukkit.getPlayer(memberId)
                member != null && VaultManager.has(member, entryCost)
            }
            if (!allCanAfford) {
                player.sendMessage(
                    Component.text("Nem todos os membros t\u00eam ${"%.2f".format(entryCost)} moedas para entrar!", NamedTextColor.RED)
                )
                return
            }
            // Cobrar de todos
            party.memberIds.forEach { memberId ->
                val member = Bukkit.getPlayer(memberId) ?: return@forEach
                VaultManager.charge(member, entryCost)
            }
        }

        if (party.size < config.minPlayers) {
            player.sendMessage(
                Component.text("Precisa de pelo menos ${config.minPlayers} jogadores para iniciar!", NamedTextColor.RED)
            )
            return
        }

        val templateWorld = Bukkit.getWorld(config.templateWorldName)
        if (templateWorld == null) {
            player.sendMessage(Component.text("Mundo da dungeon não encontrado!", NamedTextColor.RED))
            return
        }

        // Clonagem de mundo para instâncias isoladas
        val tempCloneId = java.util.UUID.randomUUID()
        val (dungeonWorld, spawnLoc, isCloned) = if (cloneWorld) {
            val cloned = WorldCloneManager.cloneWorld(config.templateWorldName, tempCloneId)
            if (cloned == null) {
                player.sendMessage(Component.text("Falha ao clonar mundo da dungeon!", NamedTextColor.RED))
                return
            }
            Triple(cloned, cloned.spawnLocation, true)
        } else {
            Triple(templateWorld, templateWorld.spawnLocation, false)
        }

        val instance = DungeonManager.createInstance(
            dungeonId = config.id,
            party = party,
            world = dungeonWorld,
            spawnLocation = spawnLoc,
        )

        // Remapear mundo clonado para o ID real da instância
        if (isCloned) {
            WorldCloneManager.remapClonedWorld(tempCloneId, instance.id)
        }

        instance.isClonedWorld = isCloned
        instance.maxLives = maxLives

        if (config.timeLimitSeconds > 0) {
            instance.timeLimitSeconds = config.timeLimitSeconds
        }

        // Inicializar subsistemas
        if (maxLives > 0) {
            LivesManager.initInstance(instance.id, party.memberIds, maxLives)
        }
        StatsManager.initInstance(instance.id, party.memberIds)

        instance.transition(DungeonState.STARTING)
        instance.getOnlinePlayers().forEach { p ->
            InventoryManager.saveAndClear(p)
            p.teleport(instance.spawnLocation)
            p.sendMessage(Component.text("Dungeon iniciando!", NamedTextColor.GOLD))
            fireDungeonStartEvent(p, instance)
        }
        instance.transition(DungeonState.IN_PROGRESS)
    }
}
