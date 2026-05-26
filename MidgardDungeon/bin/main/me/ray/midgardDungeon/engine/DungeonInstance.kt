package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.entries.statics.BossConfigEntry
import me.ray.midgardDungeon.party.Party
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class DungeonInstance(
    val id: UUID = UUID.randomUUID(),
    val dungeonId: String,
    val party: Party,
    val world: World,
    val spawnLocation: Location,
) {
    var state: DungeonState = DungeonState.WAITING
        private set

    var currentWave: Int = 0
        private set
    var currentRoom: Int = 0
        private set
    var startTime: Long = 0L
        private set

    var timeLimitSeconds: Int = 0
    var maxLives: Int = 3
    var isClonedWorld: Boolean = false
    var isProcedural: Boolean = false

    private val spawnedEntities = ConcurrentHashMap.newKeySet<UUID>()
    private var checkpointLocation: Location = spawnLocation

    // Rastreamento de boss
    private var bossEntityId: UUID? = null
    private var bossConfig: BossConfigEntry? = null
    private var bossBossBar: BossBar? = null
    var currentBossPhase: Int = -1
        private set

    val bossBar: BossBar = BossBar.bossBar(
        Component.text("Masmorra: $dungeonId", NamedTextColor.GOLD),
        1.0f,
        BossBar.Color.PURPLE,
        BossBar.Overlay.PROGRESS
    )

    fun transition(newState: DungeonState) {
        val oldState = state
        state = newState

        when (newState) {
            DungeonState.STARTING -> {
                startTime = System.currentTimeMillis()
                showBossBar()
                // Efeitos visuais: título + som
                getOnlinePlayers().forEach { p ->
                    p.showTitle(Title.title(
                        Component.text("⚔ PREPARAR!", NamedTextColor.GOLD),
                        Component.text("A dungeon vai começar...", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
                    ))
                    p.playSound(p.location, Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 1.2f)
                }
            }
            DungeonState.IN_PROGRESS -> {
                currentWave = 1
                updateBossBar("Onda $currentWave", BossBar.Color.GREEN)
                // Efeitos visuais: título + som + partículas
                getOnlinePlayers().forEach { p ->
                    p.showTitle(Title.title(
                        Component.text("⚔ COMEÇOU!", NamedTextColor.GREEN),
                        Component.text("Onda $currentWave", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(500))
                    ))
                    p.playSound(p.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f)
                    p.world.spawnParticle(Particle.TOTEM_OF_UNDYING, p.location.clone().add(0.0, 1.0, 0.0), 30, 1.0, 1.0, 1.0, 0.1)
                }
            }
            DungeonState.BOSS_FIGHT -> {
                updateBossBar("LUTA DE BOSS", BossBar.Color.RED)
                // Efeitos visuais: título dramático + som + partículas
                getOnlinePlayers().forEach { p ->
                    p.showTitle(Title.title(
                        Component.text("💀 BOSS!", NamedTextColor.DARK_RED),
                        Component.text("Prepare-se para a batalha!", NamedTextColor.RED),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                    ))
                    p.playSound(p.location, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.8f)
                    p.world.spawnParticle(Particle.FLAME, p.location.clone().add(0.0, 0.5, 0.0), 50, 2.0, 0.5, 2.0, 0.05)
                }
            }
            DungeonState.COMPLETED -> {
                updateBossBar("COMPLETA!", BossBar.Color.BLUE)
                cleanupEntities()
                // Efeitos visuais: celebração
                getOnlinePlayers().forEach { p ->
                    p.showTitle(Title.title(
                        Component.text("🏆 VITÓRIA!", NamedTextColor.GOLD),
                        Component.text("Dungeon completa!", NamedTextColor.GREEN),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    ))
                    p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                    p.world.spawnParticle(Particle.TOTEM_OF_UNDYING, p.location.clone().add(0.0, 1.0, 0.0), 100, 1.5, 2.0, 1.5, 0.2)
                    p.world.spawnParticle(Particle.FIREWORK, p.location.clone().add(0.0, 2.0, 0.0), 50, 2.0, 1.0, 2.0, 0.1)
                }
            }
            DungeonState.FAILED -> {
                updateBossBar("FALHOU", BossBar.Color.WHITE)
                cleanupEntities()
                // Efeitos visuais: derrota
                getOnlinePlayers().forEach { p ->
                    p.showTitle(Title.title(
                        Component.text("✗ DERROTA", NamedTextColor.RED),
                        Component.text("A dungeon falhou...", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofSeconds(1))
                    ))
                    p.playSound(p.location, Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f)
                    p.world.spawnParticle(Particle.SMOKE, p.location.clone().add(0.0, 1.0, 0.0), 40, 1.0, 1.0, 1.0, 0.05)
                }
            }
            else -> {}
        }
    }

    fun advanceWave() {
        currentWave++
        updateBossBar("Onda $currentWave", BossBar.Color.GREEN)
        // Efeitos visuais de nova onda
        getOnlinePlayers().forEach { p ->
            p.showTitle(Title.title(
                Component.text("Onda $currentWave", NamedTextColor.YELLOW),
                Component.text("Inimigos se aproximam!", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(500))
            ))
            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
        }
    }

    fun advanceRoom() {
        currentRoom++
    }

    fun setCheckpoint(location: Location) {
        checkpointLocation = location
    }

    fun getCheckpoint(): Location = checkpointLocation

    fun trackEntity(entity: Entity) {
        spawnedEntities.add(entity.uniqueId)
        DungeonManager.trackEntity(entity.uniqueId, id)
    }

    fun untrackEntity(entityId: UUID) {
        spawnedEntities.remove(entityId)
        DungeonManager.untrackEntity(entityId)
    }

    fun getAliveTrackedEntities(): List<Entity> {
        return spawnedEntities.mapNotNull { Bukkit.getEntity(it) }.filter { !it.isDead }
    }

    fun hasAliveEntities(): Boolean = getAliveTrackedEntities().isNotEmpty()

    fun getOnlinePlayers(): List<Player> = party.getOnlineMembers()

    fun getElapsedSeconds(): Long {
        if (startTime == 0L) return 0
        return (System.currentTimeMillis() - startTime) / 1000
    }

    private fun showBossBar() {
        getOnlinePlayers().forEach { it.showBossBar(bossBar) }
    }

    private fun updateBossBar(title: String, color: BossBar.Color) {
        bossBar.name(Component.text(title))
        bossBar.color(color)
    }

    private fun cleanupEntities() {
        spawnedEntities.forEach { DungeonManager.untrackEntity(it) }
        spawnedEntities.mapNotNull { Bukkit.getEntity(it) }.forEach { it.remove() }
        spawnedEntities.clear()
    }

    // Gerenciamento de luta contra boss
    fun registerBoss(entityId: UUID, config: BossConfigEntry) {
        bossEntityId = entityId
        bossConfig = config
        currentBossPhase = -1
    }

    fun setBossBossBar(bar: BossBar) {
        bossBossBar = bar
    }

    fun getBossEntity(): LivingEntity? {
        val id = bossEntityId ?: return null
        return Bukkit.getEntity(id) as? LivingEntity
    }

    fun isBossAlive(): Boolean {
        val boss = getBossEntity() ?: return false
        return !boss.isDead
    }

    fun getBossConfig(): BossConfigEntry? = bossConfig

    fun updateBossPhase(): Int? {
        val boss = getBossEntity() ?: return null
        val config = bossConfig ?: return null
        if (config.phases.isEmpty()) return null

        val healthPercent = boss.health / (boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.baseValue ?: boss.health)

        val sortedPhases = config.phases.sortedByDescending { it.healthThreshold }
        for ((index, phase) in sortedPhases.withIndex()) {
            if (healthPercent <= phase.healthThreshold && index > currentBossPhase) {
                currentBossPhase = index

                boss.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE)?.let {
                    val baseMob = config.baseMob.get()
                    if (baseMob != null) {
                        it.baseValue = baseMob.attackDamage * phase.damageMultiplier
                    }
                }
                boss.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)?.let {
                    it.baseValue = it.baseValue * phase.speedMultiplier
                }

                return currentBossPhase
            }
        }
        return null
    }

    fun updateBossBossBar() {
        val boss = getBossEntity() ?: return
        val bar = bossBossBar ?: return
        val maxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.baseValue ?: return
        bar.progress(maxOf(0f, minOf(1f, (boss.health / maxHealth).toFloat())))
    }

    fun isTimedOut(): Boolean {
        if (timeLimitSeconds <= 0) return false
        return getElapsedSeconds() >= timeLimitSeconds
    }

    fun cleanup() {
        getOnlinePlayers().forEach { it.hideBossBar(bossBar) }
        bossBossBar?.let { bar -> getOnlinePlayers().forEach { it.hideBossBar(bar) } }
        cleanupEntities()
    }
}
