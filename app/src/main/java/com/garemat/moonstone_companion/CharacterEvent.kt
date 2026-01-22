package com.garemat.moonstone_companion

sealed interface CharacterEvent {
    data object SaveCharacter : CharacterEvent
    data class DeleteCharacter(val character: Character) : CharacterEvent
    
    data object SaveTroupe : CharacterEvent
    data class DeleteTroupe(val troupe: Troupe) : CharacterEvent
    data class EditTroupe(val troupe: Troupe) : CharacterEvent

    data class SortCharacters(val sortType: SortType) : CharacterEvent
    
    // UI visibility events
    data object ShowCharacterDialog : CharacterEvent
    data object HideCharacterDialog : CharacterEvent
    data object ShowTroupeDialog : CharacterEvent
    data object HideTroupeDialog : CharacterEvent

    data object DismissError : CharacterEvent

    data class UpdateUserName(val name: String) : CharacterEvent
}
