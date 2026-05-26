package me.ray.midgardDungeon

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import me.ray.midgardDungeon.engine.*
import me.ray.midgardDungeon.party.PartyManager

@Singleton
object MidgardDungeon : Initializable {

    override suspend fun initialize() {
        MidgardPlugin.init()
        DungeonManager.initialize()
        PartyManager.initialize()
        CooldownManager.initialize()
        PersistentCooldownManager.initialize()
        DungeonListener.initialize()
        TrapManager.initialize()
        QueueManager.initialize()
        LeaderboardManager.initialize()
        AchievementManager.initialize()
        StatsManager.initialize()
        DailyDungeonManager.initialize()
        SchematicManager.initialize()
        ProgressionManager.initialize()
        VaultManager.initialize()
        CutsceneManager.initialize()
        MMOCoreManager.initialize()
        MythicMobsManager.initialize()
        ExampleDungeonCommand.register()
    }

    override suspend fun shutdown() {
        DungeonListener.shutdown()
        TrapManager.shutdown()
        QueueManager.shutdown()
        DungeonManager.shutdown()
        WorldCloneManager.shutdown()
        PartyManager.shutdown()
        CooldownManager.shutdown()
        PersistentCooldownManager.shutdown()
        LeaderboardManager.shutdown()
        AchievementManager.shutdown()
        StatsManager.shutdown()
        DailyDungeonManager.shutdown()
        ModifierManager.shutdown()
        KeyManager.shutdown()
        ReadyCheckManager.shutdown()
        PartyChatManager.shutdown()
        SpectatorManager.shutdown()
        ProgressionManager.shutdown()
        SchematicManager.shutdown()
        CutsceneManager.shutdown()
        InventoryManager.shutdown()
        DifficultyVoteManager.shutdown()
        me.ray.midgardDungeon.entries.action.SpawnMiniBossAction.shutdown()
        VaultManager.shutdown()
        MMOCoreManager.shutdown()
        MythicMobsManager.shutdown()
        MidgardPlugin.clear()
    }
}
