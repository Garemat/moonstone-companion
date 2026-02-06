package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.Character
import com.garemat.moonstone_companion.CharacterEvent
import com.garemat.moonstone_companion.CharacterPlayState
import com.garemat.moonstone_companion.CharacterState
import com.garemat.moonstone_companion.CharacterViewModel
import com.garemat.moonstone_companion.Troupe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    state: CharacterState,
    viewModel: CharacterViewModel,
    players: List<Pair<Troupe, List<Character>>>,
    onQuitGame: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Game") },
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
        val currentPair = players.getOrNull(selectedTab)
        if (currentPair != null) {
            val characters = currentPair.second
            
            val isLocalPlayer = remember(state.gameSession, selectedTab, state.deviceId) {
                val session = state.gameSession
                if (session == null) {
                    true
                } else {
                    val player = session.players.getOrNull(selectedTab)
                    player?.deviceId == state.deviceId
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(characters) { charIndex, character ->
                    val stateKey = "${selectedTab}_${charIndex}"
                    val playState = state.characterPlayStates[stateKey] ?: CharacterPlayState(currentHealth = character.health)

                    CharacterGameCard(
                        character = character,
                        currentHealth = playState.currentHealth,
                        currentEnergy = playState.currentEnergy,
                        isExpanded = playState.isExpanded,
                        isFlipped = playState.isFlipped,
                        usedAbilities = playState.usedAbilities,
                        isEditable = isLocalPlayer,
                        onHealthChange = { viewModel.onEvent(CharacterEvent.UpdateCharacterHealth(selectedTab, charIndex, it)) },
                        onEnergyChange = { viewModel.onEvent(CharacterEvent.UpdateCharacterEnergy(selectedTab, charIndex, it)) },
                        onExpandToggle = { viewModel.onEvent(CharacterEvent.ToggleCharacterExpanded(selectedTab, charIndex, !playState.isExpanded)) },
                        onFlippedChange = { viewModel.onEvent(CharacterEvent.ToggleCharacterFlipped(selectedTab, charIndex, it)) },
                        onAbilityToggle = { abilityName, used ->
                            viewModel.onEvent(CharacterEvent.ToggleAbilityUsed(selectedTab, charIndex, abilityName, used))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterGameCard(
    character: Character,
    currentHealth: Int,
    currentEnergy: Int,
    isExpanded: Boolean,
    isFlipped: Boolean,
    usedAbilities: Map<String, Boolean>,
    isEditable: Boolean,
    onHealthChange: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onExpandToggle: () -> Unit,
    onFlippedChange: (Boolean) -> Unit,
    onAbilityToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentHealth <= 0) Color.DarkGray else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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
            Column(modifier = Modifier.weight(1f).clickable { onFlip() }) {
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
