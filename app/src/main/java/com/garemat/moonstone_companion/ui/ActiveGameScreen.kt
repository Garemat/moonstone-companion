package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
            
            // Determine if the current tab belongs to the local player
            val isLocalPlayer = remember(state.gameSession, selectedTab, state.deviceId) {
                val session = state.gameSession
                if (session == null) {
                    // In offline mode, everything is editable
                    true
                } else {
                    // In multiplayer, check if the player at selectedTab matches the local deviceId
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
                // Front Side
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
                // Back Side (Signature Move)
                CharacterBack(character = character, onFlip = { onFlippedChange(false) })
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
        // Header Row: Name/Upgrade and Flip Icon
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

        // Main Row: Stats area and Energy Column
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Stats Area Column
            Column(modifier = Modifier.weight(2f)) {
                // Core Stats Row - Perfectly aligned with weight(1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatBoxPlay("MELEE", "${character.melee} / ${character.meleeRange}\"")
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        StatBoxPlay("ARCANE", character.arcane.toString(), centerAlign = true)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        StatBoxPlay("EVADE", character.evade, centerAlign = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buffs Row: Offensive and Defensive combined with fixed spacing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OffenseModifiersDisplay(character)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        DefenseModifiersDisplay(character)
                    }
                }
            }

            // Energy Column
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

        // Health Grid and Expand Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HealthGrid(
                    totalHealth = character.health,
                    currentHealth = currentHealth,
                    energyTrack = character.energyTrack,
                    onHealthChange = onHealthChange,
                    isEditable = isEditable
                )
            }
            
            IconButton(onClick = onExpandToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand abilities"
                )
            }
        }

        // Expanded Abilities
        if (isExpanded) {
            val inlineContent = getMoonstoneInlineContent()
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            if (character.passiveAbilities.isNotEmpty()) {
                Text("PASSIVE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.passiveAbilities.forEach { passive ->
                    AbilityItemPlay(
                        name = passive.name,
                        description = passive.description,
                        oncePerTurn = passive.oncePerTurn,
                        oncePerGame = passive.oncePerGame,
                        inlineContent = inlineContent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (character.activeAbilities.isNotEmpty()) {
                Text("ACTIVE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.activeAbilities.forEach { ability ->
                    val rangeText = if (ability.range.isNotEmpty()) " ${ability.range}" else ""
                    val header = "${ability.name} (${ability.cost})$rangeText"
                    AbilityItemPlay(
                        name = header, 
                        description = ability.description,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        isUsed = usedAbilities[ability.name] ?: false,
                        onUsedChange = { onAbilityToggle(ability.name, it) },
                        isEditable = isEditable,
                        inlineContent = inlineContent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (character.arcaneAbilities.isNotEmpty()) {
                Text("ARCANE ABILITIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                character.arcaneAbilities.forEach { ability ->
                    val rangeText = if (ability.range.isNotEmpty()) " ${ability.range}" else ""
                    val header = "${ability.name} (${ability.cost})$rangeText"
                    AbilityItemPlay(
                        name = header, 
                        description = ability.description,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        reloadable = ability.reloadable,
                        isUsed = usedAbilities[ability.name] ?: false,
                        onUsedChange = { onAbilityToggle(ability.name, it) },
                        isEditable = isEditable,
                        inlineContent = inlineContent
                    )
                }
            }
        }
    }
}

@Composable
fun AbilityItemPlay(
    name: String,
    description: String,
    oncePerTurn: Boolean = false,
    oncePerGame: Boolean = false,
    reloadable: Boolean = false,
    isUsed: Boolean = false,
    onUsedChange: (Boolean) -> Unit = {},
    isEditable: Boolean = true,
    inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent> = emptyMap()
) {
    val fullTitle = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("$name: ")
        }
        if (oncePerTurn) {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                append(" - Once per turn")
            }
        }
        if (oncePerGame) {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                if (reloadable) {
                    append(" - Once per game, unless reloaded")
                } else {
                    append(" - Once per game")
                }
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = fullTitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            
            if (oncePerGame) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isUsed) Color.Gray else Color.Transparent)
                        .border(1.2.dp, if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                        .then(if (isEditable) Modifier.clickable { onUsedChange(!isUsed) } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUsed) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        Text(
            text = parseAbilityDescription(description, ""),
            style = MaterialTheme.typography.bodySmall,
            inlineContent = inlineContent
        )
    }
}

@Composable
fun StatBoxPlay(
    label: String, 
    value: String, 
    centerAlign: Boolean = false,
    endAlign: Boolean = false
) {
    Column(
        horizontalAlignment = when {
            centerAlign -> Alignment.CenterHorizontally
            endAlign -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(
            text = value, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            textAlign = when {
                centerAlign -> TextAlign.Center
                endAlign -> TextAlign.End
                else -> TextAlign.Start
            }
        )
    }
}

@Composable
fun HealthGrid(
    totalHealth: Int,
    currentHealth: Int,
    energyTrack: List<Int>,
    onHealthChange: (Int) -> Unit,
    isEditable: Boolean = true
) {
    // Left-aligned row that naturally fits up to 12 items
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        modifier = Modifier.padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until totalHealth) {
            val healthValue = index + 1
            val isLost = healthValue > currentHealth
            val isEnergyPoint = energyTrack.contains(healthValue)
            
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isLost -> Color.Transparent
                            isEnergyPoint -> if (isEditable) Color(0xFF2196F3) else Color(0xFF90CAF9)
                            else -> if (isEditable) Color(0xFF4CAF50) else Color(0xFFA5D6A7)
                        }
                    )
                    .border(1.dp, if (isEnergyPoint) Color(0xFF1565C0) else Color.DarkGray, CircleShape)
                    .then(if (isEditable) Modifier.clickable {
                        if (currentHealth == healthValue) onHealthChange(healthValue - 1)
                        else onHealthChange(healthValue)
                    } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isLost) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun OffenseModifiersDisplay(character: Character) {
    val offensiveModifiers = mutableListOf<@Composable () -> Unit>()
    
    fun addModifier(prefix: String, value: String) {
        if (value == "Null") {
            offensiveModifiers.add {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$prefix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    NullSymbol(size = 12.dp, modifier = Modifier.padding(horizontal = 1.dp))
                }
            }
        } else if (value.toIntOrNull() != 0) {
            offensiveModifiers.add {
                Text("$prefix+$value", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    addModifier("I", character.impactDamageBuff)
    addModifier("S", character.slicingDamageBuff)
    addModifier("P", character.piercingDamageBuff)
    
    if (offensiveModifiers.isNotEmpty() || character.dealsMagicalDamage) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Hardware, contentDescription = "Offense", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            if (character.dealsMagicalDamage) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Magical", modifier = Modifier.size(14.dp), tint = Color(0xFF00B0FF))
                Spacer(modifier = Modifier.width(2.dp))
            }
            offensiveModifiers.forEachIndexed { index, modifier ->
                modifier()
                if (index < offensiveModifiers.size - 1) {
                    Text(", ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Empty placeholder to maintain height
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun DefenseModifiersDisplay(character: Character) {
    val defensiveModifiers = mutableListOf<@Composable () -> Unit>()
    
    fun addModifier(prefix: String, value: String) {
        if (value == "Null") {
            defensiveModifiers.add {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$prefix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    NullSymbol(size = 12.dp, modifier = Modifier.padding(horizontal = 1.dp))
                }
            }
        } else if (value.toIntOrNull() != 0) {
            defensiveModifiers.add {
                Text("$prefix-$value", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (character.allDamageMitigation != "0") {
        addModifier("ALL", character.allDamageMitigation)
    } else {
        addModifier("I", character.impactDamageMitigation)
        addModifier("S", character.slicingDamageMitigation)
        addModifier("P", character.piercingDamageMitigation)
    }
    addModifier("M", character.magicalDamageMitigation)

    if (defensiveModifiers.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = "Defense", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            defensiveModifiers.forEachIndexed { index, modifier ->
                modifier()
                if (index < defensiveModifiers.size - 1) {
                    Text(", ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Empty placeholder to maintain height
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun CharacterBack(character: Character, onFlip: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = character.signatureMove.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onFlip) {
                Icon(Icons.Default.Refresh, contentDescription = "Flip Back")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (character.signatureMove.damageType != null) {
            Text(
                text = "Damage Type: ${character.signatureMove.damageType}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Signature Results Table
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opponent Plays:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Deal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            character.signatureMove.results.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = entry.opponentPlay, style = MaterialTheme.typography.bodyMedium)
                    SignatureResultDisplay(entry)
                }
            }
        }

        if (character.signatureMove.passiveEffect != null || character.signatureMove.endStepEffect != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val inlineContent = getMoonstoneInlineContent()
            if (character.signatureMove.passiveEffect != null) {
                Text(
                    text = parseAbilityDescription(character.signatureMove.passiveEffect!!, ""),
                    style = MaterialTheme.typography.bodySmall,
                    inlineContent = inlineContent
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (character.signatureMove.endStepEffect != null) {
                val endStepText = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("End Step Effect: ")
                    }
                    append(parseAbilityDescription(character.signatureMove.endStepEffect!!, ""))
                }
                Text(
                    text = endStepText,
                    style = MaterialTheme.typography.bodySmall,
                    inlineContent = inlineContent
                )
            }
        }
    }
}
