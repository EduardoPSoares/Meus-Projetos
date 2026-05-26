package me.ray.midgardDungeon.party

import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Party(
    val id: UUID = UUID.randomUUID(),
    val leader: UUID,
    val maxSize: Int = 4,
) {
    private val members = ConcurrentHashMap.newKeySet<UUID>().apply { add(leader) }

    val memberIds: Set<UUID> get() = Collections.unmodifiableSet(members)
    val size: Int get() = members.size
    val isFull: Boolean get() = members.size >= maxSize

    fun addMember(playerId: UUID): Boolean {
        if (isFull) return false
        return members.add(playerId)
    }

    fun removeMember(playerId: UUID): Boolean {
        if (playerId == leader) return false
        return members.remove(playerId)
    }

    fun isMember(playerId: UUID): Boolean = playerId in members
    fun isLeader(playerId: UUID): Boolean = playerId == leader

    fun getOnlineMembers(): List<Player> {
        return members.mapNotNull { org.bukkit.Bukkit.getPlayer(it) }
    }
}
