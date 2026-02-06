package com.garemat.moonstone_companion

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class CharacterViewModel(
    application: Application,
    private val dao: CharacterDAO
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("moonstone_prefs", Context.MODE_PRIVATE)
    private val nearbyManager = NearbyManager(application)

    // Persistent Unique Device ID for session rejoin
    private val persistentDeviceId: String = prefs.getString("persistent_device_id", null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("persistent_device_id", newId).apply()
        newId
    }

    private val _state = MutableStateFlow(CharacterState(
        name = prefs.getString("player_name", "") ?: "",
        deviceId = persistentDeviceId,
        theme = AppTheme.valueOf(prefs.getString("app_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name),
        hasSeenHomeTutorial = prefs.getBoolean("has_seen_home_tutorial", false),
        hasSeenTroupesTutorial = prefs.getBoolean("has_seen_troupes_tutorial", false),
        hasSeenCharactersTutorial = prefs.getBoolean("has_seen_characters_tutorial", false),
        hasSeenRulesTutorial = prefs.getBoolean("has_seen_rules_tutorial", false),
        hasSeenSettingsTutorial = prefs.getBoolean("has_seen_settings_tutorial", false),
        hasSeenGameSetupTutorial = prefs.getBoolean("has_seen_game_setup_tutorial", false)
    ))
    
    private val _characters = dao.getCharactersOrderedByName()
    private val _troupes = dao.getTroupes()

    // Rules logic
    private val _rules = MutableStateFlow<List<RuleSection>>(emptyList())
    val rules = _rules.asStateFlow()

    val state = combine(_state, _characters, _troupes) { state, characters, troupes ->
        state.copy(
            characters = characters,
            troupes = troupes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    val discoveredEndpoints = nearbyManager.discoveredEndpoints

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _scannedTroupeEvent = MutableSharedFlow<Pair<Int, Troupe>>()
    val scannedTroupeEvent = _scannedTroupeEvent.asSharedFlow()

    sealed class UiEvent {
        data object GameStarted : UiEvent()
        data class TroupeCreated(val troupe: Troupe, val playerIndex: Int?) : UiEvent()
    }

    init {
        loadRules()
        nearbyManager.setPayloadListener { endpointId, message ->
            handleSessionMessage(endpointId, message)
        }
        
        nearbyManager.setConnectionListener { endpointId ->
            val currentSession = _state.value.gameSession
            if (currentSession == null || !currentSession.isHost) {
                val joinMsg = SessionMessage.JoinRequest(
                    playerName = _state.value.name.ifEmpty { "Player" },
                    deviceId = persistentDeviceId
                )
                nearbyManager.sendPayload(endpointId, MessageParser.encode(joinMsg))
            }
        }
    }

    private fun loadRules() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets.open("rules.json").bufferedReader().use { it.readText() }
                val loadedRules = Json.decodeFromString<List<RuleSection>>(jsonString)
                _rules.value = loadedRules
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Troupe Draft State
    var editingTroupeId by mutableStateOf<Int?>(null)
    var newTroupeName by mutableStateOf("")
    var selectedTroupeFaction by mutableStateOf(Faction.COMMONWEALTH)
    var selectedCharacterIds by mutableStateOf(setOf<Int>())
    var autoSelectMembers by mutableStateOf(false)
    var pendingTroupePlayerIndex by mutableStateOf<Int?>(null)

    // Active Game State
    private val _activeTroupes = MutableStateFlow<List<Troupe>>(emptyList())
    
    val playersWithCharacters = _activeTroupes.flatMapLatest { troupes ->
        if (troupes.isEmpty()) return@flatMapLatest flowOf(emptyList<Pair<Troupe, List<Character>>>())
        
        val flows = troupes.map { troupe ->
            dao.getCharactersByIds(troupe.characterIds).map { troupe to it }
        }
        combine(flows) { troupePairs ->
            troupePairs.toList().map { (troupe, characters) ->
                // Character summmons logic:
                // Find any characters in the troupe that have 'summonsCharacterIds'
                val summonIds = characters.flatMap { it.summonsCharacterIds }
                if (summonIds.isNotEmpty()) {
                    // Fetch summoned characters but exclude those already present in the troupe
                    val allCharacters = state.value.characters
                    val currentIdsInTroupe = characters.map { it.id }.toSet()
                    
                    val summonedCharacters = summonIds.filter { sId ->
                        // Only add as a summon if they aren't already manually selected in the troupe
                        !currentIdsInTroupe.contains(sId)
                    }.mapNotNull { sId ->
                        allCharacters.find { it.id == sId }
                    }
                    
                    troupe to (characters + summonedCharacters)
                } else {
                    troupe to characters
                }
            }
        }
    }.onEach { players ->
        // When characters are loaded for a new game, initialize their play states if not already done
        if (players.isNotEmpty() && _state.value.characterPlayStates.isEmpty()) {
            val initialStates = mutableMapOf<String, CharacterPlayState>()
            players.forEachIndexed { pIdx, (_, characters) ->
                characters.forEachIndexed { cIdx, character ->
                    initialStates["${pIdx}_${cIdx}"] = CharacterPlayState(currentHealth = character.health)
                }
            }
            _state.update { it.copy(characterPlayStates = initialStates) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEvent(event: CharacterEvent) {
        when (event) {
            is CharacterEvent.DeleteTroupe -> {
                viewModelScope.launch { dao.deleteTroupe(event.troupe) }
            }
            is CharacterEvent.EditTroupe -> {
                editingTroupeId = event.troupe.id
                newTroupeName = event.troupe.troupeName
                selectedTroupeFaction = event.troupe.faction
                selectedCharacterIds = event.troupe.characterIds.toSet()
                autoSelectMembers = event.troupe.autoSelectMembers
            }
            CharacterEvent.SaveTroupe -> {
                val troupe = Troupe(
                    id = editingTroupeId ?: 0,
                    troupeName = newTroupeName,
                    faction = selectedTroupeFaction,
                    characterIds = selectedCharacterIds.toList(),
                    shareCode = "",
                    autoSelectMembers = autoSelectMembers
                )
                viewModelScope.launch { 
                    val id = dao.upsertTroupe(troupe)
                    val savedTroupe = troupe.copy(id = id.toInt())
                    _uiEvent.emit(UiEvent.TroupeCreated(savedTroupe, pendingTroupePlayerIndex))
                    pendingTroupePlayerIndex = null
                }
                resetNewTroupeFields()
            }
            is CharacterEvent.SortCharacters -> {
                _state.update { it.copy(sortType = event.sortType) }
            }
            CharacterEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is CharacterEvent.UpdateUserName -> {
                _state.update { it.copy(name = event.name) }
                prefs.edit().putString("player_name", event.name).apply()
            }
            is CharacterEvent.ChangeTheme -> {
                _state.update { it.copy(theme = event.theme) }
                prefs.edit().putString("app_theme", event.theme.name).apply()
            }
            is CharacterEvent.SetHasSeenTutorial -> {
                val prefKey = "has_seen_${event.tutorialKey}_tutorial"
                _state.update { 
                    when(event.tutorialKey) {
                        "home" -> it.copy(hasSeenHomeTutorial = event.seen)
                        "troupes" -> it.copy(hasSeenTroupesTutorial = event.seen)
                        "characters" -> it.copy(hasSeenCharactersTutorial = event.seen)
                        "rules" -> it.copy(hasSeenRulesTutorial = event.seen)
                        "settings" -> it.copy(hasSeenSettingsTutorial = event.seen)
                        "game_setup" -> it.copy(hasSeenGameSetupTutorial = event.seen)
                        else -> it
                    }
                }
                prefs.edit().putBoolean(prefKey, event.seen).apply()
            }
            
            // Gameplay Events
            is CharacterEvent.UpdateCharacterHealth -> {
                updateCharacterState(event.playerIndex, event.charIndex) { 
                    it.copy(currentHealth = event.health) 
                }
                broadcastGameplayUpdate(SessionMessage.GameplayUpdate(event.playerIndex, event.charIndex, health = event.health))
            }
            is CharacterEvent.UpdateCharacterEnergy -> {
                updateCharacterState(event.playerIndex, event.charIndex) { 
                    it.copy(currentEnergy = event.energy) 
                }
                broadcastGameplayUpdate(SessionMessage.GameplayUpdate(event.playerIndex, event.charIndex, energy = event.energy))
            }
            is CharacterEvent.ToggleAbilityUsed -> {
                updateCharacterState(event.playerIndex, event.charIndex) { 
                    val newAbilities = it.usedAbilities.toMutableMap()
                    newAbilities[event.abilityName] = event.used
                    it.copy(usedAbilities = newAbilities)
                }
                broadcastGameplayUpdate(SessionMessage.GameplayUpdate(event.playerIndex, event.charIndex, abilityName = event.abilityName, abilityUsed = event.used))
            }
            is CharacterEvent.ToggleCharacterFlipped -> {
                updateCharacterState(event.playerIndex, event.charIndex) { 
                    it.copy(isFlipped = event.flipped) 
                }
                // Removed flip synchronization
            }
            is CharacterEvent.ToggleCharacterExpanded -> {
                updateCharacterState(event.playerIndex, event.charIndex) { 
                    it.copy(isExpanded = event.expanded) 
                }
                // Removed expand synchronization
            }
            CharacterEvent.ResetGamePlayState -> {
                _state.update { it.copy(characterPlayStates = emptyMap()) }
            }
            else -> {}
        }
    }

    private fun updateCharacterState(playerIndex: Int, charIndex: Int, update: (CharacterPlayState) -> CharacterPlayState) {
        val key = "${playerIndex}_$charIndex"
        _state.update { currentState ->
            val currentPlayStates = currentState.characterPlayStates.toMutableMap()
            val charState = currentPlayStates[key] ?: CharacterPlayState(currentHealth = 0)
            currentPlayStates[key] = update(charState)
            currentState.copy(characterPlayStates = currentPlayStates)
        }
    }

    fun startNewGame(troupes: List<Troupe>) {
        _state.update { it.copy(characterPlayStates = emptyMap()) } // Clear previous game state
        _activeTroupes.value = troupes
    }

    fun saveTroupe(troupe: Troupe) {
        viewModelScope.launch {
            dao.upsertTroupe(troupe.copy(id = 0))
        }
    }

    fun onTroupeScanned(playerIndex: Int, troupe: Troupe) {
        viewModelScope.launch {
            _scannedTroupeEvent.emit(playerIndex to troupe)
        }
    }

    private fun resetNewTroupeFields() {
        editingTroupeId = null
        newTroupeName = ""
        selectedTroupeFaction = Faction.COMMONWEALTH
        selectedCharacterIds = emptySet()
        autoSelectMembers = false
    }

    fun generateFullShareCode(troupe: Troupe, characters: List<Character>): String {
        val factionCode = when (troupe.faction) {
            Faction.COMMONWEALTH -> "A"
            Faction.DOMINION -> "B"
            Faction.LESHAVULT -> "C"
            Faction.SHADES -> "D"
        }
        val selectedCodes = troupe.characterIds.mapNotNull { id ->
            characters.find { it.id == id }?.shareCode
        }.joinToString("")
        val autoSelectFlag = if (troupe.autoSelectMembers) "1" else "0"
        val rawCode = "${troupe.troupeName}|$factionCode$autoSelectFlag$selectedCodes"
        return Base64.encodeToString(rawCode.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun importTroupe(fullCode: String, allCharacters: List<Character>): Troupe? {
        try {
            val decodedBytes = Base64.decode(fullCode, Base64.DEFAULT)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            val parts = decodedString.split("|")
            if (parts.size != 2 || parts[1].isEmpty()) return null
            
            val name = parts[0]
            val codeBody = parts[1]
            val faction = when (codeBody[0]) {
                'A' -> Faction.COMMONWEALTH
                'B' -> Faction.DOMINION
                'C' -> Faction.LESHAVULT
                'D' -> Faction.SHADES
                else -> return null
            }
            val autoSelect = codeBody[1] == '1'
            val characterCodes = codeBody.substring(2).chunked(3)

            val characterIds = characterCodes.mapNotNull { code ->
                allCharacters.find { it.shareCode == code }?.id
            }
            
            return Troupe(0, name, faction, characterIds, fullCode, autoSelectMembers = autoSelect)
        } catch (e: Exception) {
            return null
        }
    }

    // --- Nearby Session Logic ---

    fun startHosting(hostName: String) {
        nearbyManager.stopAll()
        val sessionId = UUID.randomUUID().toString().take(8)
        val actualName = _state.value.name.ifEmpty { hostName }
        _state.update { it.copy(
            gameSession = GameSession(
                players = listOf(GamePlayer(name = actualName, deviceId = persistentDeviceId)),
                isHost = true,
                sessionId = sessionId
            )
        )}
        nearbyManager.startAdvertising(actualName)
    }

    fun startDiscovering() {
        nearbyManager.startDiscovery()
    }

    fun connectToHost(endpointId: String, playerName: String) {
        nearbyManager.requestConnection(_state.value.name.ifEmpty { playerName }, endpointId)
    }

    private fun handleSessionMessage(endpointId: String, jsonString: String) {
        val message = try { MessageParser.decode(jsonString) } catch (e: Exception) { return }
        val currentSession = _state.value.gameSession ?: run {
            if (message is SessionMessage.SessionSync) {
                val newSession = GameSession(
                    players = message.players,
                    isHost = false,
                    sessionId = message.sessionId
                )
                _state.update { it.copy(gameSession = newSession) }
            }
            return
        }

        when (message) {
            is SessionMessage.JoinRequest -> {
                if (currentSession.isHost) {
                    val existingPlayerIndex = currentSession.players.indexOfFirst { it.deviceId == message.deviceId }
                    
                    if (existingPlayerIndex != -1) {
                        syncSessionToAll()
                    } else if (currentSession.players.size < 4) {
                        val newPlayer = GamePlayer(name = message.playerName, deviceId = message.deviceId)
                        _state.update { it.copy(
                            gameSession = currentSession.copy(players = currentSession.players + newPlayer)
                        )}
                        syncSessionToAll()
                    }
                }
            }
            is SessionMessage.SessionSync -> {
                if (!currentSession.isHost) {
                    _state.update { it.copy(gameSession = currentSession.copy(
                        players = message.players,
                        sessionId = message.sessionId
                    ))}
                }
            }
            is SessionMessage.TroupeSelected -> {
                val newTroupe = Troupe(
                    id = 0,
                    troupeName = message.troupeName,
                    faction = message.faction,
                    characterIds = message.characterIds,
                    shareCode = ""
                )
                
                val updatedPlayers = currentSession.players.map { player ->
                    // Match based on persistent deviceId
                    if (player.deviceId == message.deviceId) {
                        player.copy(troupe = newTroupe)
                    } else player
                }
                _state.update { it.copy(gameSession = currentSession.copy(players = updatedPlayers)) }
                if (currentSession.isHost) syncSessionToAll()
            }
            is SessionMessage.StartGame -> {
                val troupes = currentSession.players.mapNotNull { it.troupe }
                startNewGame(troupes)
                viewModelScope.launch { _uiEvent.emit(UiEvent.GameStarted) }
            }
            is SessionMessage.GameplayUpdate -> {
                updateCharacterState(message.playerIndex, message.charIndex) { currentState ->
                    var newState = currentState
                    message.health?.let { newState = newState.copy(currentHealth = it) }
                    message.energy?.let { newState = newState.copy(currentEnergy = it) }
                    message.abilityName?.let { name ->
                        message.abilityUsed?.let { used ->
                            val newAbilities = newState.usedAbilities.toMutableMap()
                            newAbilities[name] = used
                            newState = newState.copy(usedAbilities = newAbilities)
                        }
                    }
                    newState
                }
                // If host, rebroadcast to others
                if (currentSession.isHost) {
                    nearbyManager.sendPayloadToAll(MessageParser.encode(message))
                }
            }
            else -> {}
        }
    }

    private fun syncSessionToAll() {
        val session = _state.value.gameSession ?: return
        if (session.isHost) {
            val syncMsg = SessionMessage.SessionSync(session.players, session.sessionId)
            nearbyManager.sendPayloadToAll(MessageParser.encode(syncMsg))
        }
    }

    fun broadcastTroupeSelection(troupe: Troupe) {
        val session = _state.value.gameSession ?: return
        
        val msg = SessionMessage.TroupeSelected(
            deviceId = persistentDeviceId,
            troupeName = troupe.troupeName,
            faction = troupe.faction,
            characterIds = troupe.characterIds
        )
        val json = MessageParser.encode(message = msg)
        
        if (session.isHost) {
            val updatedPlayers = session.players.map { 
                if (it.deviceId == persistentDeviceId) it.copy(troupe = troupe) else it 
            }
            _state.update { it.copy(gameSession = session.copy(players = updatedPlayers)) }
            nearbyManager.sendPayloadToAll(json)
        } else {
            // Locally update client state immediately for responsiveness
            val updatedPlayers = session.players.map { 
                if (it.deviceId == persistentDeviceId) it.copy(troupe = troupe) else it
            }
            _state.update { it.copy(gameSession = session.copy(players = updatedPlayers)) }
            nearbyManager.sendPayloadToAll(json) 
        }
    }

    fun broadcastGameplayUpdate(update: SessionMessage.GameplayUpdate) {
        val session = _state.value.gameSession ?: return
        nearbyManager.sendPayloadToAll(MessageParser.encode(update))
    }

    fun broadcastStartGame() {
        val session = _state.value.gameSession ?: return
        if (session.isHost) {
            val msg = SessionMessage.StartGame
            nearbyManager.sendPayloadToAll(MessageParser.encode(msg))
            handleSessionMessage("HOST", MessageParser.encode(msg))
        }
    }

    fun leaveSession() {
        nearbyManager.stopAll()
        _state.update { it.copy(gameSession = null) }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.stopAll()
    }
}
