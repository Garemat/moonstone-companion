package com.garemat.moonstone_companion

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class CharacterViewModel(
    application: Application,
    private val dao: CharacterDAO
) : AndroidViewModel(application) {

    private val nearbyManager = NearbyManager(application)

    private val _state = MutableStateFlow(CharacterState())
    private val _characters = dao.getCharactersOrderedByName()
    private val _troupes = dao.getTroupes()

    val state = combine(_state, _characters, _troupes) { state, characters, troupes ->
        state.copy(
            characters = characters,
            troupes = troupes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CharacterState())

    val discoveredEndpoints = nearbyManager.discoveredEndpoints

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _scannedTroupeEvent = MutableSharedFlow<Pair<Int, Troupe>>()
    val scannedTroupeEvent = _scannedTroupeEvent.asSharedFlow()

    sealed class UiEvent {
        data object GameStarted : UiEvent()
    }

    init {
        nearbyManager.setPayloadListener { endpointId, message ->
            handleSessionMessage(endpointId, message)
        }
        
        nearbyManager.setConnectionListener { endpointId ->
            val currentSession = _state.value.gameSession
            if (currentSession == null || !currentSession.isHost) {
                val joinMsg = SessionMessage.JoinRequest(_state.value.name.ifEmpty { "Player" })
                nearbyManager.sendPayload(endpointId, MessageParser.encode(joinMsg))
            }
        }
    }

    // Troupe Draft State
    var editingTroupeId by mutableStateOf<Int?>(null)
    var newTroupeName by mutableStateOf("")
    var selectedTroupeFaction by mutableStateOf(Faction.COMMONWEALTH)
    var selectedCharacterIds by mutableStateOf(setOf<Int>())

    // Active Game State
    private val _activeTroupes = MutableStateFlow<List<Troupe>>(emptyList())
    
    val playersWithCharacters = _activeTroupes.flatMapLatest { troupes ->
        if (troupes.isEmpty()) return@flatMapLatest flowOf(emptyList<Pair<Troupe, List<Character>>>())
        
        val flows = troupes.map { troupe ->
            dao.getCharactersByIds(troupe.characterIds).map { troupe to it }
        }
        combine(flows) { it.toList() }
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
            }
            CharacterEvent.SaveTroupe -> {
                val troupe = Troupe(
                    id = editingTroupeId ?: 0,
                    troupeName = newTroupeName,
                    faction = selectedTroupeFaction,
                    characterIds = selectedCharacterIds.toList(),
                    shareCode = ""
                )
                viewModelScope.launch { dao.upsertTroupe(troupe) }
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
            }
            else -> {}
        }
    }

    fun startNewGame(troupes: List<Troupe>) {
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
        val rawCode = "${troupe.troupeName}|$factionCode$selectedCodes"
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
            val characterCodes = codeBody.substring(1).chunked(3)
            val faction = when (codeBody[0]) {
                'A' -> Faction.COMMONWEALTH
                'B' -> Faction.DOMINION
                'C' -> Faction.LESHAVULT
                'D' -> Faction.SHADES
                else -> return null
            }

            val characterIds = characterCodes.mapNotNull { code ->
                allCharacters.find { it.shareCode == code }?.id
            }
            
            return Troupe(0, name, faction, characterIds, fullCode)
        } catch (e: Exception) {
            return null
        }
    }

    fun startHosting(hostName: String) {
        nearbyManager.stopAll()
        val sessionId = UUID.randomUUID().toString().take(8)
        val actualName = _state.value.name.ifEmpty { hostName }
        _state.update { it.copy(
            gameSession = GameSession(
                players = listOf(GamePlayer(name = actualName, deviceId = "HOST")),
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
                if (currentSession.isHost && currentSession.players.size < 4) {
                    val newPlayer = GamePlayer(name = message.playerName, deviceId = endpointId)
                    _state.update { it.copy(
                        gameSession = currentSession.copy(players = currentSession.players + newPlayer)
                    )}
                    syncSessionToAll()
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
                    val isTarget = if (currentSession.isHost) {
                        player.deviceId == endpointId || (player.deviceId == "HOST" && message.deviceId == "HOST")
                    } else {
                        player.deviceId == message.deviceId
                    }
                    
                    if (isTarget) {
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
        val deviceId = if (session.isHost) "HOST" else "CLIENT" 
        
        val msg = SessionMessage.TroupeSelected(
            deviceId = deviceId,
            troupeName = troupe.troupeName,
            faction = troupe.faction,
            characterIds = troupe.characterIds
        )
        val json = MessageParser.encode(message = msg)
        
        if (session.isHost) {
            val updatedPlayers = session.players.map { 
                if (it.deviceId == "HOST") it.copy(troupe = troupe) else it 
            }
            _state.update { it.copy(gameSession = session.copy(players = updatedPlayers)) }
            nearbyManager.sendPayloadToAll(json)
        } else {
            val updatedPlayers = session.players.map { 
                if (it.name == state.value.name || it.name == _state.value.name) it.copy(troupe = troupe) else it
            }
            _state.update { it.copy(gameSession = session.copy(players = updatedPlayers)) }
            nearbyManager.sendPayloadToAll(json) 
        }
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
