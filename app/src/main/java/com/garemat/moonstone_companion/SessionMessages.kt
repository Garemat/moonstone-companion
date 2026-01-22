package com.garemat.moonstone_companion

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class SessionMessage {
    @Serializable
    data class JoinRequest(val playerName: String) : SessionMessage()
    
    @Serializable
    data class Welcome(val deviceId: String) : SessionMessage()
    
    @Serializable
    data class TroupeSelected(
        val deviceId: String, 
        val troupeName: String, 
        val faction: Faction, 
        val characterIds: List<Int>
    ) : SessionMessage()
    
    @Serializable
    data class SessionSync(val players: List<GamePlayer>, val sessionId: String) : SessionMessage()
    
    @Serializable
    data object StartGame : SessionMessage()
}

object MessageParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(message: SessionMessage): String = json.encodeToString(message)
    fun decode(jsonString: String): SessionMessage = json.decodeFromString(jsonString)
}
