package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.garemat.moonstone_companion.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    state: CharacterState,
    viewModel: CharacterViewModel,
    players: List<Pair<Troupe, List<Character>>>,
    onQuitGame: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    // Drag and Drop State
    var draggingStoneSource by remember { mutableStateOf<StoneSource?>(null) }
    var draggingStoneIndex by remember { mutableIntStateOf(-1) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    
    val characterBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val potBounds = remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val stateRef = rememberUpdatedState(state)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Active Game")
                        Text(
                            text = if (state.currentTurn > 4) "Round: Sudden Death" else "Round: ${state.currentTurn}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onQuitGame) {
                        Icon(Icons.Default.Close, contentDescription = "End Game")
                    }
                }
            )
        },
        bottomBar = {
            if (players.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    players.forEachIndexed { index, (troupe, _) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(troupe.troupeName) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootLayoutCoordinates = it }
        ) {
            val currentPair = players.getOrNull(selectedTab)
            val isLocalPlayer = remember(state.gameSession, selectedTab, state.deviceId) {
                val session = state.gameSession
                if (session == null) true
                else session.players.getOrNull(selectedTab)?.deviceId == state.deviceId
            }

            if (currentPair != null) {
                val characters = currentPair.second
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(characters) { charIndex, character ->
                        val stateKey = "${selectedTab}_${charIndex}"
                        val playState = state.characterPlayStates[stateKey] ?: CharacterPlayState(currentHealth = character.health)

                        DisposableEffect(stateKey) {
                            onDispose { characterBounds.remove(stateKey) }
                        }

                        Box(
                            modifier = Modifier.onGloballyPositioned { 
                                characterBounds[stateKey] = it.boundsInWindow()
                            }
                        ) {
                            CharacterGameCard(
                                character = character,
                                currentHealth = playState.currentHealth,
                                currentEnergy = playState.currentEnergy,
                                moonstones = playState.moonstones,
                                isExpanded = playState.isExpanded,
                                isFlipped = playState.isFlipped,
                                usedAbilities = playState.usedAbilities,
                                isEditable = isLocalPlayer,
                                draggingStoneIndex = if (draggingStoneSource is StoneSource.Character && 
                                    (draggingStoneSource as StoneSource.Character).playerIndex == selectedTab && 
                                    (draggingStoneSource as StoneSource.Character).charIndex == charIndex) draggingStoneIndex else -1,
                                onHealthChange = { viewModel.onEvent(CharacterEvent.UpdateCharacterHealth(selectedTab, charIndex, it)) },
                                onEnergyChange = { viewModel.onEvent(CharacterEvent.UpdateCharacterEnergy(selectedTab, charIndex, it)) },
                                onExpandToggle = { viewModel.onEvent(CharacterEvent.ToggleCharacterExpanded(selectedTab, charIndex, !playState.isExpanded)) },
                                onFlippedChange = { viewModel.onEvent(CharacterEvent.ToggleCharacterFlipped(selectedTab, charIndex, it)) },
                                onAbilityToggle = { abilityName, used ->
                                    viewModel.onEvent(CharacterEvent.ToggleAbilityUsed(selectedTab, charIndex, abilityName, used))
                                },
                                onStoneDragStart = { index, globalPos ->
                                    draggingStoneIndex = index
                                    draggingStoneSource = StoneSource.Character(selectedTab, charIndex)
                                    dragPosition = globalPos
                                },
                                onStoneDrag = { amount -> dragPosition += amount },
                                onStoneDragEnd = {
                                    val source = draggingStoneSource as? StoneSource.Character
                                    if (source != null) {
                                        val currentState = stateRef.value
                                        
                                        if (potBounds.value?.contains(dragPosition) == true) {
                                            val sourceStones = currentState.characterPlayStates["${source.playerIndex}_${source.charIndex}"]?.moonstones ?: 0
                                            if (sourceStones > 0) {
                                                viewModel.onEvent(CharacterEvent.UpdateCharacterMoonstones(source.playerIndex, source.charIndex, sourceStones - 1))
                                            }
                                        } 
                                        else {
                                            val dropTarget = characterBounds.entries.find { it.value.contains(dragPosition) }
                                            if (dropTarget != null) {
                                                val (targetPIdx, targetCIdx) = dropTarget.key.split("_").map { it.toInt() }
                                                if (targetPIdx != source.playerIndex || targetCIdx != source.charIndex) {
                                                    val sourceStones = currentState.characterPlayStates["${source.playerIndex}_${source.charIndex}"]?.moonstones ?: 0
                                                    val targetStones = currentState.characterPlayStates[dropTarget.key]?.moonstones ?: 0
                                                    
                                                    if (targetStones < 7) {
                                                        viewModel.onEvent(CharacterEvent.UpdateCharacterMoonstones(source.playerIndex, source.charIndex, sourceStones - 1))
                                                        viewModel.onEvent(CharacterEvent.UpdateCharacterMoonstones(targetPIdx, targetCIdx, targetStones + 1))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    draggingStoneSource = null
                                    draggingStoneIndex = -1
                                }
                            )
                        }
                    }
                }
            }

            // Interactive Layer: Pot and Next Turn Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Moonstone Pot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(60.dp)
                        .onGloballyPositioned { potBounds.value = it.boundsInWindow() }
                        .pointerInput(isLocalPlayer) {
                            if (isLocalPlayer) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggingStoneSource = StoneSource.Pot
                                        dragPosition = potBounds.value!!.topLeft + offset
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragPosition += dragAmount
                                    },
                                    onDragEnd = {
                                        val dropTarget = characterBounds.entries.find { it.value.contains(dragPosition) }
                                        if (dropTarget != null) {
                                            val (pIdx, cIdx) = dropTarget.key.split("_").map { it.toInt() }
                                            val currentState = stateRef.value.characterPlayStates[dropTarget.key]?.moonstones ?: 0
                                            if (currentState < 7) {
                                                viewModel.onEvent(CharacterEvent.UpdateCharacterMoonstones(pIdx, cIdx, currentState + 1))
                                            }
                                        }
                                        draggingStoneSource = null
                                    },
                                    onDragCancel = { draggingStoneSource = null }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MoonstoneIcon(size = 48.dp, modifier = Modifier.alpha(if (isLocalPlayer) 1f else 0.3f))
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rewind Button
                    if (state.turnHistory.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { viewModel.onEvent(CharacterEvent.RewindTurn) },
                            enabled = isLocalPlayer
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Rewind Turn")
                        }
                    }

                    // Next Turn Button
                    Button(
                        onClick = { viewModel.onEvent(CharacterEvent.NextTurn) },
                        enabled = isLocalPlayer
                    ) {
                        Text("Next Turn")
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            // Dragging stone overlay
            if (draggingStoneSource != null) {
                val localPos = rootLayoutCoordinates?.windowToLocal(dragPosition) ?: dragPosition
                Box(
                    modifier = Modifier
                        .offset { IntOffset(localPos.x.roundToInt() - 20.dp.toPx().toInt(), localPos.y.roundToInt() - 20.dp.toPx().toInt()) }
                        .zIndex(1000f)
                ) {
                    MoonstoneIcon(size = 40.dp)
                }
            }
        }
    }
}

sealed class StoneSource {
    data object Pot : StoneSource()
    data class Character(val playerIndex: Int, val charIndex: Int) : StoneSource()
}

@Composable
fun MoonstoneIcon(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            moveTo(size.toPx() / 2f, 0f)
            lineTo(size.toPx(), size.toPx())
            lineTo(0f, size.toPx())
            close()
        }
        drawPath(path, color = Color(0xFF2196F3))
    }
}

@Composable
fun CharacterGameCard(
    character: Character,
    currentHealth: Int,
    currentEnergy: Int,
    moonstones: Int,
    isExpanded: Boolean,
    isFlipped: Boolean,
    usedAbilities: Map<String, Boolean>,
    isEditable: Boolean,
    draggingStoneIndex: Int,
    onHealthChange: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onExpandToggle: () -> Unit,
    onFlippedChange: (Boolean) -> Unit,
    onAbilityToggle: (String, Boolean) -> Unit,
    onStoneDragStart: (Int, Offset) -> Unit,
    onStoneDrag: (Offset) -> Unit,
    onStoneDragEnd: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onStoneDragStart)
    val currentOnDrag by rememberUpdatedState(onStoneDrag)
    val currentOnDragEnd by rememberUpdatedState(onStoneDragEnd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentHealth <= 0) Color.DarkGray else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isFlipped) {
                    FrontSide(
                        character = character,
                        currentHealth = currentHealth,
                        currentEnergy = currentEnergy,
                        isExpanded = isExpanded,
                        usedAbilities = usedAbilities,
                        isEditable = isEditable,
                        onHealthChange = onHealthChange,
                        onEnergyChange = onEnergyChange,
                        onExpandToggle = onExpandToggle,
                        onAbilityToggle = onAbilityToggle,
                        onFlip = { onFlippedChange(true) }
                    )
                } else {
                    CharacterBack(character = character, searchQuery = "", onFlip = { onFlippedChange(false) })
                }
            }

            // Moonstone Pinned View
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val stoneCoords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
                
                repeat(moonstones) { i ->
                    val isThisStoneDragging = draggingStoneIndex == i
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { stoneCoords[i] = it }
                            .alpha(if (isThisStoneDragging) 0f else 1f)
                            .pointerInput(i, isEditable) {
                                if (isEditable) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val globalPos = stoneCoords[i]?.boundsInWindow()?.topLeft?.plus(offset) ?: Offset.Zero
                                            currentOnDragStart(i, globalPos)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentOnDrag(dragAmount)
                                        },
                                        onDragEnd = { currentOnDragEnd() },
                                        onDragCancel = { currentOnDragEnd() }
                                    )
                                }
                            }
                    ) {
                        MoonstoneIcon(size = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun FrontSide(
    character: Character,
    currentHealth: Int,
    currentEnergy: Int,
    isExpanded: Boolean,
    usedAbilities: Map<String, Boolean>,
    isEditable: Boolean,
    onHealthChange: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onExpandToggle: () -> Unit,
    onAbilityToggle: (String, Boolean) -> Unit,
    onFlip: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).clickable { onFlip() }.padding(start = 24.dp)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (character.signatureMove.upgradeFrom.isNotEmpty()) {
                    Text(
                        text = character.signatureMove.upgradeFrom,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            IconButton(onClick = onFlip) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Show Signature Move",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    CommonStatBox("MELEE", "${character.melee} / ${character.meleeRange}\"", modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start)
                    CommonStatBox("ARCANE", character.arcane.toString(), modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                    CommonStatBox("EVADE", character.evade, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModifierDisplay(character, isOffense = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    ModifierDisplay(character, isOffense = false, modifier = Modifier.weight(1f))
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ENERGY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (currentEnergy > 0) onEnergyChange(currentEnergy - 1) }, 
                        modifier = Modifier.size(32.dp),
                        enabled = isEditable
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Use", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "$currentEnergy",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onEnergyChange(currentEnergy + 1) }, 
                        modifier = Modifier.size(32.dp),
                        enabled = isEditable
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Gain", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            HealthTracker(
                totalHealth = character.health,
                currentHealth = currentHealth,
                energyTrack = character.energyTrack,
                onHealthChange = onHealthChange,
                isEditable = isEditable,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onExpandToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand abilities"
                )
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            if (character.passiveAbilities.isNotEmpty()) {
                Text("PASSIVE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.passiveAbilities.forEach { passive ->
                    CommonAbilityItem(
                        name = passive.name,
                        description = passive.description,
                        oncePerTurn = passive.oncePerTurn,
                        oncePerGame = passive.oncePerGame
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (character.activeAbilities.isNotEmpty()) {
                Text("ACTIVE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.activeAbilities.forEach { ability ->
                    val rangeText = if (ability.range.isNotEmpty()) " ${ability.range}" else ""
                    val header = "${ability.name} (${ability.cost})$rangeText"
                    CommonAbilityItem(
                        name = header, 
                        description = ability.description,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        isUsed = usedAbilities[ability.name] ?: false,
                        onUsedChange = { onAbilityToggle(ability.name, it) },
                        isEditable = isEditable
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (character.arcaneAbilities.isNotEmpty()) {
                Text("ARCANE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.arcaneAbilities.forEach { ability ->
                    val rangeText = if (ability.range.isNotEmpty()) " ${ability.range}" else ""
                    val header = "${ability.name} (${ability.cost})$rangeText"
                    CommonAbilityItem(
                        name = header, 
                        description = ability.description,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        reloadable = ability.reloadable,
                        isUsed = usedAbilities[ability.name] ?: false,
                        onUsedChange = { onAbilityToggle(ability.name, it) },
                        isEditable = isEditable
                    )
                }
            }
        }
    }
}
