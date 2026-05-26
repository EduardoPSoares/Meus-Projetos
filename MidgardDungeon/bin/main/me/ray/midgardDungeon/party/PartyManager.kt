package me.ray.midgardDungeon.party

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PartyManager {
    private val parties = ConcurrentHashMap<UUID, Party>()
    private val playerPartyMap = ConcurrentHashMap<UUID, UUID>()

    fun initialize() {
        parties.clear()
        playerPartyMap.clear()
    }

    fun shutdown() {
        parties.clear()
        playerPartyMap.clear()
    }

    fun createParty(leaderId: UUID, maxSize: Int = 4): Party {
        if (playerPartyMap.containsKey(leaderId)) return getPartyByPlayer(leaderId)!!
        val party = Party(leader = leaderId, maxSize = maxSize)
        parties[party.id] = party
        playerPartyMap[leaderId] = party.id
        return party
    }

    fun disbandParty(partyId: UUID) {
        val party = parties.remove(partyId) ?: return
        party.memberIds.forEach { playerPartyMap.remove(it) }
    }

    fun joinParty(playerId: UUID, partyId: UUID): Boolean {
        val party = parties[partyId] ?: return false
        if (playerPartyMap.containsKey(playerId)) return false
        if (!party.addMember(playerId)) return false
        playerPartyMap[playerId] = partyId
        return true
    }

    fun leaveParty(playerId: UUID): Boolean {
        val partyId = playerPartyMap[playerId] ?: return false
        val party = parties[partyId] ?: return false

        if (party.isLeader(playerId)) {
            disbandParty(partyId)
            return true
        }

        party.removeMember(playerId)
        playerPartyMap.remove(playerId)
        return true
    }

    fun getPartyByPlayer(playerId: UUID): Party? {
        val partyId = playerPartyMap[playerId] ?: return null
        return parties[partyId]
    }

    fun getParty(partyId: UUID): Party? = parties[partyId]
}
