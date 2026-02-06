package com.garemat.moonstone_companion

import kotlinx.serialization.Serializable

enum class AppTheme {
    DEFAULT, MOONSTONE
}

data class CharacterState(
    val characters: List<Character> = emptyList(),
    val troupes: List<Troupe> = emptyList(),
    val name: String = "",
    val deviceId: String = "",
    val theme: AppTheme = AppTheme.DEFAULT,
    val isAddingCharacter: Boolean = false,
    val isAddingTroupe: Boolean = false,
    val sortType: SortType = SortType.NAME,
    val errorMessage: String? = null,
    
    // Tutorial State
    val hasSeenHomeTutorial: Boolean = false,
    val hasSeenTroupesTutorial: Boolean = false,
    val hasSeenCharactersTutorial: Boolean = false,
    val hasSeenRulesTutorial: Boolean = false,
    val hasSeenSettingsTutorial: Boolean = false,
    val hasSeenGameSetupTutorial: Boolean = false,
    
    // Game Session State (Nearby)
    val gameSession: GameSession? = null,

    // Active Game Play State
    // Key: "playerIndex_characterIndex"
    val characterPlayStates: Map<String, CharacterPlayState> = emptyMap()
)

@Serializable
data class CharacterPlayState(
    val currentHealth: Int,
    val currentEnergy: Int = 0,
    val usedAbilities: Map<String, Boolean> = emptyMap(),
    val isFlipped: Boolean = false,
    val isExpanded: Boolean = false
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
