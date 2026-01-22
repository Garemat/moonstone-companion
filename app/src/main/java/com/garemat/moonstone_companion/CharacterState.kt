package com.garemat.moonstone_companion

import kotlinx.serialization.Serializable

data class CharacterState(
    val characters: List<Character> = emptyList(),
    val troupes: List<Troupe> = emptyList(),
    val name: String = "",
    val isAddingCharacter: Boolean = false,
    val isAddingTroupe: Boolean = false,
    val sortType: SortType = SortType.NAME,
    val errorMessage: String? = null,
    
    // Game Session State
    val gameSession: GameSession? = null
)

@Serializable
data class GameSession(
    val players: List<GamePlayer> = emptyList(),
    val isHost: Boolean = false,
    val sessionId: String = ""
)

@Serializable
data class GamePlayer(
    val name: String,
    val troupe: Troupe? = null,
    val isReady: Boolean = false,
    val deviceId: String = ""
)
