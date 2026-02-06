package com.garemat.moonstone_companion.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.garemat.moonstone_companion.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    state: CharacterState,
    viewModel: CharacterViewModel,
    onNavigateBack: () -> Unit,
    onStartGame: () -> Unit,
    onNavigateToAddEditTroupe: () -> Unit
) {
    var showScannerDialogForPlayer by remember { mutableStateOf<Int?>(null) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var showTutorialForcefully by remember { mutableStateOf(false) }
    var tutorialStepIndex by remember { mutableIntStateOf(0) }
    val coordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }

    val shouldShowTutorial = (!state.hasSeenGameSetupTutorial || showTutorialForcefully)

    val session = state.gameSession
    
    // Example Session for Tutorial - Step 12+ switches to this dummy data
    val tutorialSession = remember(shouldShowTutorial, tutorialStepIndex, state.characters) {
        if (shouldShowTutorial && tutorialStepIndex >= 12) {
            val commonWealthChars = state.characters.filter { it.factions.contains(Faction.COMMONWEALTH) }.shuffled().take(3).map { it.id }
            val shadesChars = state.characters.filter { it.factions.contains(Faction.SHADES) }.shuffled().take(3).map { it.id }
            val dominionChars = state.characters.filter { it.factions.contains(Faction.DOMINION) }.shuffled().take(3).map { it.id }

            GameSession(
                sessionId = "EXAMPLE-123",
                isHost = true,
                players = listOf(
                    GamePlayer(name = state.name.ifEmpty { "Host" }, deviceId = state.deviceId, isReady = true),
                    GamePlayer(
                        name = "Players 2",
                        troupe = Troupe(troupeName = "Example Troupe Name", faction = Faction.COMMONWEALTH, characterIds = commonWealthChars, shareCode = "", autoSelectMembers = true),
                        isReady = true
                    ),
                    GamePlayer(
                        name = "Eric",
                        troupe = Troupe(troupeName = "Spooky Troupe", faction = Faction.SHADES, characterIds = shadesChars, shareCode = ""),
                        isReady = true
                    ),
                    GamePlayer(
                        name = "Not a Goblin",
                        troupe = Troupe(troupeName = "Not goblins", faction = Faction.DOMINION, characterIds = dominionChars, shareCode = ""),
                        isReady = true
                    )
                )
            )
        } else null
    }

    val activeSession = tutorialSession ?: session
    
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()
    val context = LocalContext.current

    val nearbyPermissions = remember {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Location is only strictly required by the system for discovery below Android 12
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissions.toTypedArray()
    }

    var pendingNearbyAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pendingNearbyAction?.invoke()
            pendingNearbyAction = null
        } else {
            Toast.makeText(context, "Nearby permissions are required for multiplayer", Toast.LENGTH_LONG).show()
            pendingNearbyAction = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRunNearbyAction(action: () -> Unit) {
        if (nearbyPermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }) {
            action()
        } else {
            pendingNearbyAction = action
            nearbyPermissionLauncher.launch(nearbyPermissions)
        }
    }

    // Listen for start game event from host/session
    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            if (event is CharacterViewModel.UiEvent.GameStarted) {
                onStartGame()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Game Setup") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (activeSession == null) {
                            TextButton(
                                onClick = {
                                    checkAndRunNearbyAction { viewModel.startHosting(state.name.ifEmpty { "Host" }) }
                                },
                                modifier = Modifier.onGloballyPositioned { coordsMap["HostButton"] = it }
                            ) {
                                Text("Host")
                            }
                            TextButton(
                                onClick = { 
                                    checkAndRunNearbyAction {
                                        viewModel.startDiscovering()
                                        showDiscoveryDialog = true 
                                    }
                                },
                                modifier = Modifier.onGloballyPositioned { coordsMap["JoinButton"] = it }
                            ) {
                                Text("Join")
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.leaveSession() },
                                modifier = Modifier.onGloballyPositioned { coordsMap["LeaveButton"] = it }
                            ) {
                                Text("Leave", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (activeSession == null) {
                    OfflineSetupUI(
                        state = state,
                        viewModel = viewModel,
                        onStartGame = onStartGame,
                        onScanRequest = { index ->
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                showScannerDialogForPlayer = index
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        },
                        onNavigateToAddEditTroupe = onNavigateToAddEditTroupe,
                        onPositioned = { name, coords -> coordsMap[name] = coords },
                        isTutorialMode = shouldShowTutorial,
                        tutorialStep = tutorialStepIndex
                    )
                } else {
                    SessionSetupUI(
                        state = state,
                        viewModel = viewModel,
                        session = activeSession,
                        troupes = state.troupes,
                        onSelectTroupe = { troupe -> viewModel.broadcastTroupeSelection(troupe) },
                        onStartGame = {
                            viewModel.broadcastStartGame()
                        },
                        onNavigateToAddEditTroupe = onNavigateToAddEditTroupe,
                        onPositioned = { name, coords -> coordsMap[name] = coords },
                        isTutorialMode = shouldShowTutorial
                    )
                }
            }

            if (showDiscoveryDialog && activeSession == null) {
                AlertDialog(
                    onDismissRequest = { 
                        showDiscoveryDialog = false
                        viewModel.leaveSession()
                    },
                    title = { Text("Available Games") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            if (discoveredEndpoints.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text("Searching for nearby hosts...", modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                            } else {
                                LazyColumn {
                                    items(discoveredEndpoints.toList()) { (id, name) ->
                                        ListItem(
                                            headlineContent = { Text(name) },
                                            supportingContent = { Text("Tap to join session") },
                                            leadingContent = { Icon(Icons.Default.Wifi, contentDescription = null) },
                                            modifier = Modifier.clickable {
                                                viewModel.connectToHost(id, state.name.ifEmpty { "Player" })
                                                showDiscoveryDialog = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { 
                            showDiscoveryDialog = false
                            viewModel.leaveSession()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showScannerDialogForPlayer != null) {
                val playerIndex = showScannerDialogForPlayer!!
                Dialog(
                    onDismissRequest = { showScannerDialogForPlayer = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        QrScanner(
                            onResult = { result ->
                                val importedTroupe = viewModel.importTroupe(result, state.characters)
                                if (importedTroupe != null) {
                                    viewModel.onTroupeScanned(playerIndex, importedTroupe)
                                    showScannerDialogForPlayer = null
                                }
                            }
                        )

                        IconButton(
                            onClick = { showScannerDialogForPlayer = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Help button in top left - Consistent with other screens
        IconButton(
            onClick = { showTutorialForcefully = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Help, contentDescription = "Tutorial")
        }

        if (shouldShowTutorial) {
            Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                TutorialOverlay(
                    steps = gameSetupTutorialSteps,
                    targetCoordinates = coordsMap,
                    onStepChange = { tutorialStepIndex = it },
                    onComplete = {
                        viewModel.onEvent(CharacterEvent.SetHasSeenTutorial("game_setup", true))
                        showTutorialForcefully = false
                        tutorialStepIndex = 0
                    },
                    onSkip = {
                        viewModel.onEvent(CharacterEvent.SetHasSeenTutorial("game_setup", true))
                        showTutorialForcefully = false
                        tutorialStepIndex = 0
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineSetupUI(
    state: CharacterState,
    viewModel: CharacterViewModel,
    onStartGame: () -> Unit,
    onScanRequest: (Int) -> Unit,
    onNavigateToAddEditTroupe: () -> Unit,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    isTutorialMode: Boolean = false,
    tutorialStep: Int = 0
) {
    var playerCount by remember { mutableIntStateOf(2) }
    val selectedTroupes = remember { mutableStateListOf<Troupe?>(null, null, null, null) }
    var troupeToPrune by remember { mutableStateOf<Pair<Int, Troupe>?>(null) }
    var troupeToSaveWithName by remember { mutableStateOf<Troupe?>(null) }
    var customTroupeName by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.scannedTroupeEvent.collect { (index, troupe) ->
            if (index < selectedTroupes.size) {
                selectedTroupes[index] = troupe
            }
        }
    }

    LaunchedEffect(playerCount) {
        viewModel.uiEvent.collect { event ->
            if (event is CharacterViewModel.UiEvent.TroupeCreated) {
                val index = event.playerIndex
                if (index != null && index >= 0 && index < 4) {
                    val maxAllowed = when(playerCount) {
                        3 -> 4
                        4 -> 3
                        else -> 6
                    }
                    if (event.troupe.autoSelectMembers && event.troupe.characterIds.size <= maxAllowed) {
                        selectedTroupes[index] = event.troupe
                    } else {
                        troupeToPrune = index to event.troupe
                    }
                }
            }
        }
    }

    // Tutorial Helper: Auto-populate example troupe when reaching the Save step
    LaunchedEffect(isTutorialMode, tutorialStep) {
        if (isTutorialMode && tutorialStep >= 6 && selectedTroupes[0] == null) {
            val exampleTroupe = Troupe(
                troupeName = "Example Troupe Name",
                faction = Faction.COMMONWEALTH,
                characterIds = state.characters.filter { it.factions.contains(Faction.COMMONWEALTH) }.take(3).map { it.id },
                shareCode = "EXAMPLE",
                autoSelectMembers = true
            )
            selectedTroupes[0] = exampleTroupe
        }
    }

    var showQrForTroupe by remember { mutableStateOf<Troupe?>(null) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onGloballyPositioned { onPositioned("PlayerCount", it) }
        ) {
            Text("Local Game", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.weight(1f))
            Text("Players: ")
            Row {
                listOf(2, 3, 4).forEach { count ->
                    FilterChip(
                        selected = playerCount == count,
                        onClick = { 
                            playerCount = count
                            for (i in count until 4) selectedTroupes[i] = null
                        },
                        label = { Text(count.toString()) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(playerCount) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val name = if (index == 0) state.name.ifEmpty { "Player 1" } else "Player ${index + 1}"
                            Text(name, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            
                            val troupe = selectedTroupes[index]
                            
                            val showSave = troupe != null && state.troupes.none { it.shareCode == troupe.shareCode && troupe.shareCode.isNotEmpty() }
                            val tutorialForceShowIcons = isTutorialMode && index == 0 && (tutorialStep >= 6 && tutorialStep <= 7)
                            
                            if (showSave || (tutorialForceShowIcons && tutorialStep == 6)) {
                                IconButton(
                                    onClick = { 
                                        troupe?.let {
                                            troupeToSaveWithName = it
                                            customTroupeName = it.troupeName
                                        }
                                    },
                                    modifier = Modifier.onGloballyPositioned { if (index == 0) onPositioned("SaveTroupeSetup", it) }
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save Troupe")
                                }
                            }
                            
                            if (troupe != null || (tutorialForceShowIcons && tutorialStep == 7)) {
                                IconButton(
                                    onClick = { showQrForTroupe = troupe },
                                    modifier = Modifier.onGloballyPositioned { if (index == 0) onPositioned("QrCodeDisplayButton", it) }
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = "Show QR")
                                }
                            }

                            IconButton(
                                onClick = { onScanRequest(index) },
                                modifier = Modifier.onGloballyPositioned { if (index == 0) onPositioned("ScanQR", it) }
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                            }
                        }

                        val displayTroupes = if (isTutorialMode && state.troupes.none { it.troupeName == "Example Troupe Name" }) {
                            val exampleTroupe = Troupe(
                                troupeName = "Example Troupe Name",
                                faction = Faction.COMMONWEALTH,
                                characterIds = state.characters.filter { it.factions.contains(Faction.COMMONWEALTH) }.take(3).map { it.id },
                                shareCode = "EXAMPLE",
                                autoSelectMembers = true
                            )
                            listOf(exampleTroupe) + state.troupes
                        } else {
                            state.troupes
                        }

                        TroupeSelector(
                            troupes = displayTroupes,
                            selectedTroupe = selectedTroupes[index],
                            allCharacters = state.characters,
                            onTroupeSelected = { t ->
                                val maxAllowed = when(playerCount) {
                                    3 -> 4
                                    4 -> 3
                                    else -> 6
                                }
                                // Only show pruning dialog if AutoSelect is off OR troupe is over the player count limit
                                if (!t.autoSelectMembers || t.characterIds.size > maxAllowed) {
                                    troupeToPrune = index to t
                                } else {
                                    selectedTroupes[index] = t
                                }
                            },
                            onCreateNewTroupe = {
                                viewModel.editingTroupeId = null
                                viewModel.pendingTroupePlayerIndex = index
                                onNavigateToAddEditTroupe()
                            },
                            onEditTroupe = { t ->
                                viewModel.onEvent(CharacterEvent.EditTroupe(t))
                                viewModel.pendingTroupePlayerIndex = index
                                onNavigateToAddEditTroupe()
                            },
                            modifier = Modifier.onGloballyPositioned { if (index == 0) onPositioned("TroupeSelector", it) },
                            isTutorialMode = isTutorialMode && index == 0,
                            tutorialStep = tutorialStep,
                            onPositioned = onPositioned
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val nonNullTroupes = selectedTroupes.take(playerCount).filterNotNull()
                if (nonNullTroupes.size == playerCount) {
                    viewModel.startNewGame(nonNullTroupes)
                    onStartGame()
                }
            },
            enabled = selectedTroupes.take(playerCount).all { it != null },
            modifier = Modifier.fillMaxWidth().height(56.dp).onGloballyPositioned { onPositioned("BattleButton", it) }
        ) {
            Text("BATTLE!")
        }
    }
    
    if (showQrForTroupe != null) {
        val shareCode = viewModel.generateFullShareCode(showQrForTroupe!!, state.characters)
        QrCodeDialog(
            troupeName = showQrForTroupe!!.troupeName,
            shareCode = shareCode,
            onDismiss = { showQrForTroupe = null }
        )
    }

    if (troupeToPrune != null) {
        val (index, troupe) = troupeToPrune!!
        val maxAllowed = when(playerCount) {
            3 -> 4
            4 -> 3
            else -> 6
        }
        TroupeSelectionDialog(
            troupe = troupe,
            maxSelection = maxAllowed,
            allCharacters = state.characters,
            onConfirmed = { selectedChars ->
                selectedTroupes[index] = troupe.copy(characterIds = selectedChars.map { it.id })
                troupeToPrune = null
            },
            onDismiss = { troupeToPrune = null }
        )
    }

    if (troupeToSaveWithName != null) {
        AlertDialog(
            onDismissRequest = { troupeToSaveWithName = null },
            title = { Text("Save Troupe") },
            text = {
                Column {
                    Text("Enter a name for this troupe:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTroupeName,
                        onValueChange = { customTroupeName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Troupe Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        troupeToSaveWithName?.let {
                            viewModel.saveTroupe(it.copy(troupeName = customTroupeName))
                        }
                        troupeToSaveWithName = null
                    },
                    enabled = customTroupeName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { troupeToSaveWithName = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionSetupUI(
    state: CharacterState,
    viewModel: CharacterViewModel,
    session: GameSession,
    troupes: List<Troupe>,
    onSelectTroupe: (Troupe) -> Unit,
    onStartGame: () -> Unit,
    onNavigateToAddEditTroupe: () -> Unit,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    isTutorialMode: Boolean = false
) {
    var troupeToPrune by remember { mutableStateOf<Troupe?>(null) }
    val playerCount = session.players.size

    LaunchedEffect(playerCount) {
        viewModel.uiEvent.collect { event ->
            if (event is CharacterViewModel.UiEvent.TroupeCreated) {
                if (event.playerIndex == -1) { // Special marker for local session user
                    val maxAllowed = when(playerCount) {
                        3 -> 4
                        4 -> 3
                        else -> 6
                    }
                    if (event.troupe.autoSelectMembers && event.troupe.characterIds.size <= maxAllowed) {
                        onSelectTroupe(event.troupe)
                    } else {
                        troupeToPrune = event.troupe
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Session ID: ${session.sessionId}", 
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.onGloballyPositioned { onPositioned("SessionId", it) }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("${session.players.size}/4 Players", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(session.players.indices.toList()) { index ->
                val player = session.players[index]

                // Identify if this slot belongs to the local user
                val isActuallyLocal = player.deviceId == state.deviceId

                PlayerSlotCard(
                    player = player,
                    troupes = troupes,
                    allCharacters = state.characters,
                    isEditable = isActuallyLocal,
                    onSelectTroupe = { troupe ->
                        val maxAllowed = when(playerCount) {
                            3 -> 4
                            4 -> 3
                            else -> 6
                        }
                        // Skip pruning if AutoSelect is on AND it fits the limits
                        if (!troupe.autoSelectMembers || troupe.characterIds.size > maxAllowed) {
                            troupeToPrune = troupe
                        } else {
                            onSelectTroupe(troupe)
                        }
                    },
                    onCreateNewTroupe = {
                        viewModel.editingTroupeId = null
                        viewModel.pendingTroupePlayerIndex = -1 // Marker for local session user
                        onNavigateToAddEditTroupe()
                    },
                    onEditTroupe = { troupe ->
                        viewModel.onEvent(CharacterEvent.EditTroupe(troupe))
                        viewModel.pendingTroupePlayerIndex = -1
                        onNavigateToAddEditTroupe()
                    },
                    modifier = Modifier.onGloballyPositioned {
                        if (isTutorialMode) {
                            if (index == 0) onPositioned("FirstPlayerSlot", it)
                            if (index == 1) onPositioned("SecondPlayerSlot", it)
                        }
                    }
                )
            }
        }

        Button(
            onClick = onStartGame,
            enabled = (session.isHost && session.players.size >= 2 && session.players.all { it.troupe != null }) || (isTutorialMode && session.sessionId == "EXAMPLE-123"),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp)
                .onGloballyPositioned { onPositioned("StartBattleButton", it) }
        ) {
            Text(if (session.isHost) "START BATTLE" else "WAITING FOR HOST...")
        }
    }

    if (troupeToPrune != null) {
        val maxAllowed = when(playerCount) {
            3 -> 4
            4 -> 3
            else -> 6
        }
        TroupeSelectionDialog(
            troupe = troupeToPrune!!,
            maxSelection = maxAllowed,
            allCharacters = state.characters,
            onConfirmed = { selectedChars ->
                onSelectTroupe(troupeToPrune!!.copy(characterIds = selectedChars.map { it.id }))
                troupeToPrune = null
            },
            onDismiss = { troupeToPrune = null }
        )
    }
}

@Composable
fun PlayerSlotCard(
    player: GamePlayer,
    troupes: List<Troupe>,
    allCharacters: List<Character>,
    isEditable: Boolean,
    onSelectTroupe: (Troupe) -> Unit,
    onCreateNewTroupe: () -> Unit,
    onEditTroupe: (Troupe) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(player.name, fontWeight = FontWeight.Bold)

                if (player.troupe != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(getFactionColor(player.troupe.faction))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FactionSymbol(
                            faction = player.troupe.faction,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            if (isEditable) {
                TroupeSelector(
                    troupes = troupes,
                    selectedTroupe = player.troupe,
                    allCharacters = allCharacters,
                    onTroupeSelected = onSelectTroupe,
                    onCreateNewTroupe = onCreateNewTroupe,
                    onEditTroupe = onEditTroupe
                )
            } else if (player.troupe != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = player.troupe.troupeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Show characters even if we can't edit
                val names = player.troupe.characterIds.mapNotNull { id ->
                    allCharacters.find { it.id == id }?.name
                }
                if (names.isNotEmpty()) {
                    Text(
                        text = names.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Choosing troupe...", color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun QrCodeDialog(
    troupeName: String,
    shareCode: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(troupeName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                val bitmap = remember(shareCode) { BarcodeUtils.generateQrCode(shareCode) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(250.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun TroupeSelector(
    troupes: List<Troupe>,
    selectedTroupe: Troupe?,
    allCharacters: List<Character>,
    onTroupeSelected: (Troupe) -> Unit,
    onCreateNewTroupe: () -> Unit,
    onEditTroupe: (Troupe) -> Unit,
    modifier: Modifier = Modifier,
    isTutorialMode: Boolean = false,
    tutorialStep: Int = 0,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(isTutorialMode, tutorialStep) {
        if (isTutorialMode && (tutorialStep == 4 || tutorialStep == 5)) {
            expanded = true
        } else if (isTutorialMode) {
            expanded = false
        }
    }

    Box(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedTroupe?.troupeName ?: "Select a troupe",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = if (selectedTroupe == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (selectedTroupe != null || (isTutorialMode && tutorialStep >= 9)) {
                        IconButton(
                            onClick = { selectedTroupe?.let { onEditTroupe(it) } },
                            modifier = Modifier.size(24.dp).onGloballyPositioned {
                                if (isTutorialMode) onPositioned("EditTroupeButton", it)
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Troupe",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                if (selectedTroupe != null) {
                    val names = selectedTroupe.characterIds.mapNotNull { id ->
                        allCharacters.find { it.id == id }?.name
                    }
                    if (names.isNotEmpty()) {
                        Text(
                            text = names.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            DropdownMenuItem(
                text = { Text("Create New Troupe", color = MaterialTheme.colorScheme.primary) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onCreateNewTroupe()
                    expanded = false
                },
                modifier = Modifier.onGloballyPositioned {
                    if (isTutorialMode) onPositioned("CreateNewTroupe", it)
                }
            )
            if (troupes.isNotEmpty()) {
                HorizontalDivider()
            }
            troupes.forEach { troupe ->
                DropdownMenuItem(
                    text = { Text(troupe.troupeName) },
                    onClick = {
                        onTroupeSelected(troupe)
                        expanded = false
                    },
                    modifier = Modifier.onGloballyPositioned {
                        if (isTutorialMode && troupe.troupeName == "Example Troupe Name") {
                            onPositioned("ExampleTroupeItem", it)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TroupeSelectionDialog(
    troupe: Troupe,
    maxSelection: Int,
    allCharacters: List<Character>,
    onConfirmed: (List<Character>) -> Unit,
    onDismiss: () -> Unit
) {
    val troupeCharacters = remember(troupe) {
        troupe.characterIds.mapNotNull { id -> allCharacters.find { it.id == id } }
    }
    val selectedCharacters = remember { mutableStateListOf<Character>() }
    var expandedCharacterId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        title = {
            Column {
                Text("Select Your Team")
                Text(
                    "Select up to $maxSelection characters for this game.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(troupeCharacters) { character ->
                    val isSelected = selectedCharacters.contains(character)
                    val isExpanded = expandedCharacterId == character.id

                    SelectionCharacterCard(
                        character = character,
                        isSelected = isSelected,
                        isExpanded = isExpanded,
                        onToggleSelect = {
                            if (isSelected) {
                                selectedCharacters.remove(character)
                            } else if (selectedCharacters.size < maxSelection) {
                                selectedCharacters.add(character)
                            }
                        },
                        onExpandClick = {
                            expandedCharacterId = if (isExpanded) null else character.id
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmed(selectedCharacters.toList()) },
                enabled = selectedCharacters.isNotEmpty() && selectedCharacters.size <= maxSelection
            ) {
                Text("Confirm (${selectedCharacters.size}/$maxSelection)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectionCharacterCard(
    character: Character,
    isSelected: Boolean,
    isExpanded: Boolean,
    onToggleSelect: () -> Unit,
    onExpandClick: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageRes = remember(character.imageName) {
        if (character.imageName != null) {
            val cleanName = character.imageName.substringBeforeLast(".")
            context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        } else 0
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandClick() }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = character.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = character.name.take(1), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = character.tags.joinToString(", "), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }

                IconButton(onClick = onExpandClick) {
                    Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(modifier = Modifier.padding(16.dp)) {
                    if (!isFlipped) {
                        CharacterFront(character = character, searchQuery = "", onFlip = { isFlipped = true })
                    } else {
                        CharacterBack(character = character, searchQuery = "", onFlip = { isFlipped = false })
                    }
                }
            }
        }
    }
}
