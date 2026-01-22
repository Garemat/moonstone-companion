package com.garemat.moonstone_companion.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.garemat.moonstone_companion.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    state: CharacterState,
    viewModel: CharacterViewModel,
    onNavigateBack: () -> Unit,
    onStartGame: () -> Unit
) {
    var showScannerDialogForPlayer by remember { mutableStateOf<Int?>(null) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }

    val session = state.gameSession
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()
    val context = LocalContext.current

    val nearbyPermissions = remember {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissions.toTypedArray()
    }

    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Permissions granted! Try again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Nearby permissions are required for local multiplayer", Toast.LENGTH_LONG).show()
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
            nearbyPermissionLauncher.launch(nearbyPermissions)
        }
    }

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
                    if (session == null) {
                        TextButton(onClick = { 
                            checkAndRunNearbyAction { viewModel.startHosting(state.name.ifEmpty { "Host" }) }
                        }) {
                            Text("Host")
                        }
                        TextButton(onClick = { 
                            checkAndRunNearbyAction {
                                viewModel.startDiscovering()
                                showDiscoveryDialog = true 
                            }
                        }) {
                            Text("Join")
                        }
                    } else {
                        TextButton(onClick = { viewModel.leaveSession() }) {
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
            if (session == null) {
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
                    }
                )
            } else {
                SessionSetupUI(
                    state = state,
                    session = session,
                    troupes = state.troupes,
                    onSelectTroupe = { troupe -> viewModel.broadcastTroupeSelection(troupe) },
                    onStartGame = {
                        viewModel.broadcastStartGame()
                        onStartGame()
                    }
                )
            }
        }

        if (showDiscoveryDialog && session == null) {
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
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSetupUI(
    state: CharacterState,
    viewModel: CharacterViewModel,
    onStartGame: () -> Unit,
    onScanRequest: (Int) -> Unit
) {
    var playerCount by remember { mutableStateOf(2) }
    val selectedTroupes = remember { mutableStateListOf<Troupe?>(null, null, null, null) }
    
    LaunchedEffect(Unit) {
        viewModel.scannedTroupeEvent.collect { (index, troupe) ->
            if (index < selectedTroupes.size) {
                selectedTroupes[index] = troupe
            }
        }
    }

    var showQrForTroupe by remember { mutableStateOf<Troupe?>(null) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val name = if (index == 0) state.name.ifEmpty { "Player 1" } else "Player ${index + 1}"
                            Text(name, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            if (selectedTroupes[index] != null) {
                                val troupe = selectedTroupes[index]!!
                                if (state.troupes.none { it.shareCode == troupe.shareCode && troupe.shareCode.isNotEmpty() }) {
                                    IconButton(onClick = { viewModel.saveTroupe(troupe) }) {
                                        Icon(Icons.Default.Save, contentDescription = "Save Troupe")
                                    }
                                }
                                
                                IconButton(onClick = { showQrForTroupe = troupe }) {
                                    Icon(Icons.Default.QrCode, contentDescription = "Show QR")
                                }
                            }
                            IconButton(onClick = { onScanRequest(index) }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                            }
                        }

                        TroupeSelector(
                            troupes = state.troupes,
                            selectedTroupe = selectedTroupes[index],
                            allCharacters = state.characters,
                            onTroupeSelected = { selectedTroupes[index] = it }
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
            modifier = Modifier.fillMaxWidth().height(56.dp)
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
}

@Composable
fun SessionSetupUI(
    state: CharacterState,
    session: GameSession,
    troupes: List<Troupe>,
    onSelectTroupe: (Troupe) -> Unit,
    onStartGame: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Session ID: ${session.sessionId}", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.weight(1f))
            Text("${session.players.size}/4 Players", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(session.players.indices.toList()) { index ->
                val player = session.players[index]
                
                val isActuallyLocal = if (session.isHost) {
                    player.deviceId == "HOST"
                } else {
                    player.name == state.name.ifEmpty { "Player" }
                }
                
                PlayerSlotCard(
                    player = player,
                    troupes = troupes,
                    allCharacters = state.characters,
                    isEditable = isActuallyLocal,
                    onSelectTroupe = onSelectTroupe
                )
            }
        }

        Button(
            onClick = onStartGame,
            enabled = session.isHost && session.players.size >= 2 && session.players.all { it.troupe != null },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
        ) {
            Text(if (session.isHost) "START BATTLE" else "WAITING FOR HOST...")
        }
    }
}

@Composable
fun PlayerSlotCard(
    player: GamePlayer,
    troupes: List<Troupe>,
    allCharacters: List<Character>,
    isEditable: Boolean,
    onSelectTroupe: (Troupe) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        Icon(
                            imageVector = getFactionIcon(player.troupe.faction),
                            contentDescription = player.troupe.faction.name,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isEditable) {
                TroupeSelector(
                    troupes = troupes,
                    selectedTroupe = player.troupe,
                    allCharacters = allCharacters,
                    onTroupeSelected = onSelectTroupe
                )
            } else if (player.troupe != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = player.troupe.troupeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
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
    onTroupeSelected: (Troupe) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
            troupes.forEach { troupe ->
                DropdownMenuItem(
                    text = { Text(troupe.troupeName) },
                    onClick = {
                        onTroupeSelected(troupe)
                        expanded = false
                    }
                )
            }
        }
    }
}
