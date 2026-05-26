package me.ray.midgardDungeon.engine

import com.typewritermc.core.extension.Initializable
import me.ray.midgardDungeon.MidgardPlugin
import me.ray.midgardDungeon.entries.event.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object DungeonListener : Initializable, Listener {

    private var timerId: Int = -1

    override suspend fun initialize() {
        val plugin = MidgardPlugin.instance ?: return
        Bukkit.getPluginManager().registerEvents(this, plugin)

        timerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            tickInstances()
        }, 20L, 20L)
    }

    override suspend fun shutdown() {
        if (timerId != -1) {
            Bukkit.getScheduler().cancelTask(timerId)
            timerId = -1
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val entityId = entity.uniqueId

        // Rastrear estatísticas de kills
        val killer = entity.killer
        if (killer != null) {
            val killerInstance = DungeonManager.getInstanceByPlayer(killer.uniqueId)
            if (killerInstance != null) {
                StatsManager.recordKill(killerInstance.id, killer.uniqueId)
            }
        }

        // Busca otimizada via mapa reverso de entidade → instância
        val instance = DungeonManager.getInstanceByEntity(entityId)
        if (instance != null) {
            instance.untrackEntity(entityId)

            // Disparar evento de MythicMob morto se aplicável
            if (MythicMobsManager.isAvailable() && MythicMobsManager.isMythicMob(entity)) {
                val mythicId = MythicMobsManager.getMythicMobId(entity) ?: ""
                instance.getOnlinePlayers().forEach { p ->
                    fireMythicMobDeathEvent(p, instance, mythicId)
                }
            }

            // Verificar se era um mini-boss
            if (me.ray.midgardDungeon.entries.action.SpawnMiniBossAction.activeMiniBosses.containsKey(entityId)) {
                me.ray.midgardDungeon.entries.action.SpawnMiniBossAction.onMiniBossDeath(entityId, entity.location)
            }

            // Verificar se este era o boss
            if (instance.getBossEntity()?.uniqueId == entityId || (!instance.isBossAlive() && instance.state == DungeonState.BOSS_FIGHT)) {
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("O boss foi derrotado!", NamedTextColor.GOLD))
                }
                instance.getOnlinePlayers().forEach { p ->
                    fireWaveCompleteEvent(p, instance)
                }
            }

            // Verificar se todos os mobs da onda morreram
            if (instance.state == DungeonState.IN_PROGRESS && !instance.hasAliveEntities()) {
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Onda eliminada!", NamedTextColor.GREEN))
                    fireWaveCompleteEvent(p, instance)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damaged = event.entity
        val damager = event.damager

        // Rastrear estatísticas de dano
        if (damager is Player) {
            val instance = DungeonManager.getInstanceByPlayer(damager.uniqueId)
            if (instance != null) {
                StatsManager.recordDamageDealt(instance.id, damager.uniqueId, event.finalDamage)
            }
        }
        if (damaged is Player) {
            val instance = DungeonManager.getInstanceByPlayer(damaged.uniqueId)
            if (instance != null) {
                StatsManager.recordDamageTaken(instance.id, damaged.uniqueId, event.finalDamage)
            }
        }

        // Atualizar barra do boss quando sofre dano (busca otimizada)
        val entityInstance = DungeonManager.getInstanceByEntity(damaged.uniqueId)
        if (entityInstance != null) {
            // Atualizar barra de mini-boss
            if (me.ray.midgardDungeon.entries.action.SpawnMiniBossAction.activeMiniBosses.containsKey(damaged.uniqueId)) {
                val livingDamaged = damaged as? org.bukkit.entity.LivingEntity
                if (livingDamaged != null) {
                    val healthAfter = (livingDamaged.health - event.finalDamage).coerceAtLeast(0.0)
                    me.ray.midgardDungeon.entries.action.SpawnMiniBossAction.onMiniBossDamage(damaged.uniqueId, healthAfter)
                }
            }

            // Atualizar barra do boss principal
            if (entityInstance.state == DungeonState.BOSS_FIGHT) {
                val boss = entityInstance.getBossEntity()
                if (boss != null && boss.uniqueId == damaged.uniqueId) {
                    val plugin = MidgardPlugin.instance ?: return
                    Bukkit.getScheduler().runTask(
                        plugin,
                        Runnable {
                            entityInstance.updateBossBossBar()
                            val newPhase = entityInstance.updateBossPhase()
                            if (newPhase != null) {
                                val config = entityInstance.getBossConfig()
                                val phaseName = config?.phases?.getOrNull(newPhase)?.phaseName ?: "Fase $newPhase"
                                entityInstance.getOnlinePlayers().forEach { p ->
                                    p.sendMessage(
                                        Component.text("Boss entra em: $phaseName", NamedTextColor.DARK_RED)
                                    )
                                    fireBossPhaseEvent(p, entityInstance, newPhase + 1)
                                }
                                val phase = config?.phases?.getOrNull(newPhase)
                                phase?.getAdditionalMobs()?.forEach { mob ->
                                    repeat(mob.spawnCount) {
                                        val loc = boss.location.clone().add(
                                            (Math.random() * 6 - 3), 0.0, (Math.random() * 6 - 3)
                                        )
                                        val spawned = entityInstance.world.spawnEntity(loc, mob.entityType)
                                        entityInstance.trackEntity(spawned)
                                        if (spawned is org.bukkit.entity.LivingEntity) {
                                            if (mob.displayName.isNotEmpty()) {
                                                spawned.customName(Component.text(mob.displayName))
                                                spawned.isCustomNameVisible = true
                                            }
                                            spawned.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.baseValue = mob.maxHealth
                                            spawned.health = mob.maxHealth
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerDamageByPlayer(event: EntityDamageByEntityEvent) {
        val damaged = event.entity as? Player ?: return
        val damager = event.damager as? Player ?: return

        val damagedInstance = DungeonManager.getInstanceByPlayer(damaged.uniqueId)
        val damagerInstance = DungeonManager.getInstanceByPlayer(damager.uniqueId)

        if (damagedInstance != null && damagerInstance != null && damagedInstance.id == damagerInstance.id) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        // Lidar com comando /ready
        if (message.equals("/ready", ignoreCase = true)) {
            event.isCancelled = true
            Bukkit.getScheduler().runTask(MidgardPlugin.instance ?: return, Runnable {
                ReadyCheckManager.markReady(player)
            })
            return
        }
        // Lidar com votação de dificuldade
        if (DifficultyVoteManager.hasPendingVote(player.uniqueId)) {
            val voted = DifficultyVoteManager.vote(player, message)
            if (voted) {
                event.isCancelled = true
                return
            }
        }
        // Lidar com chat de grupo
        if (PartyChatManager.handleChat(player, message)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        if (CutsceneManager.isInCutscene(player.uniqueId)) {
            CutsceneManager.skipCutscene(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (CutsceneManager.isInCutscene(player.uniqueId)) {
            CutsceneManager.stopCutscene(player, skipEvent = true)
        }
        PartyChatManager.removePlayer(player.uniqueId)
        QueueManager.leaveQueue(player)

        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return
        player.hideBossBar(instance.bossBar)

        val remaining = instance.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }
        if (remaining.isEmpty()) {
            instance.transition(DungeonState.FAILED)
            StatsManager.finishRun(instance.id, instance.dungeonId, instance.startTime, false)
            cleanupInstance(instance)
        } else {
            remaining.forEach { p ->
                p.sendMessage(
                    Component.text("${player.name} saiu da dungeon!", NamedTextColor.YELLOW)
                )
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        if (!instance.state.isFinished()) {
            // Reconectar jogador à dungeon ativa
            val plugin = MidgardPlugin.instance ?: return
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    player.teleport(instance.getCheckpoint())
                    player.showBossBar(instance.bossBar)
                    player.sendMessage(
                        Component.text("Você foi reconectado à dungeon!", NamedTextColor.GREEN)
                    )
                    instance.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { p ->
                        p.sendMessage(
                            Component.text("${player.name} reconectou à dungeon!", NamedTextColor.GREEN)
                        )
                    }
                }
            }, 20L)
        }

        // Notificar sobre dungeon diária/semanal
        val plugin = MidgardPlugin.instance ?: return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            val daily = DailyDungeonManager.getCurrentDaily()
            val weekly = DailyDungeonManager.getCurrentWeekly()
            val playerId = player.uniqueId.toString()

            if (daily != null && !DailyDungeonManager.hasDailyCompleted(playerId)) {
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                player.sendMessage(
                    Component.text("📅 Dungeon Diária: ", NamedTextColor.GOLD)
                        .append(Component.text(daily.dungeonId, NamedTextColor.YELLOW))
                )
                player.sendMessage(
                    Component.text("   Bônus: ${daily.bonusExpMultiplier}x EXP, ${daily.bonusLootMultiplier}x Loot", NamedTextColor.GRAY)
                )
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            }
            if (weekly != null && !DailyDungeonManager.hasWeeklyCompleted(playerId)) {
                player.sendMessage(
                    Component.text("🏆 Dungeon Semanal: ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(weekly.dungeonId, NamedTextColor.YELLOW))
                        .append(Component.text(" (${weekly.bonusExpMultiplier}x EXP)", NamedTextColor.GRAY))
                )
            }
        }, 60L)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return
        event.respawnLocation = instance.getCheckpoint()
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerDeath(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        if (player.health - event.finalDamage <= 0) {
            StatsManager.recordDeath(instance.id, player.uniqueId)

            // Sistema de vidas - tentar consumir uma vida
            val revived = LivesManager.handleDeath(instance, player)
            if (revived) {
                firePlayerReviveEvent(player)
                return
            }

            // Sem vidas restantes - modo espectador
            SpectatorManager.enterSpectator(player, instance)

            // Verificar se TODOS os jogadores estão em espectador
            val alivePlayers = SpectatorManager.getAlivePlayersCount(instance)
            if (alivePlayers == 0) {
                val plugin = MidgardPlugin.instance ?: return
                Bukkit.getScheduler().runTaskLater(
                    plugin,
                    Runnable {
                        instance.transition(DungeonState.FAILED)
                        StatsManager.showEndScreen(instance, false)
                        StatsManager.finishRun(instance.id, instance.dungeonId, instance.startTime, false)
                        instance.getOnlinePlayers().forEach { p ->
                            p.sendMessage(Component.text("Todos os jogadores caíram! Dungeon falhou!", NamedTextColor.RED))
                            fireDungeonFailEvent(p, instance)
                        }
                        cleanupInstance(instance)
                    },
                    40L
                )
            }
        }
    }

    private fun tickInstances() {
        for (instance in DungeonManager.getActiveInstances()) {
            if (instance.state.isFinished()) continue

            // Verificar limite de tempo
            if (instance.isTimedOut()) {
                instance.transition(DungeonState.FAILED)
                StatsManager.showEndScreen(instance, false)
                StatsManager.finishRun(instance.id, instance.dungeonId, instance.startTime, false)
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Tempo esgotado! Dungeon falhou!", NamedTextColor.RED))
                    fireDungeonFailEvent(p, instance)
                }
                cleanupInstance(instance)
            }
        }
    }

    private fun cleanupInstance(instance: DungeonInstance) {
        DungeonManager.fullCleanup(instance)
    }
}
