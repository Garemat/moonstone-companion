package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.garemat.moonstone_companion.Troupe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveGameScreen(
    players: List<Pair<Troupe, List<Character>>>,
    onQuitGame: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(characters) { character ->
                    CharacterGameCard(character = character)
                }
            }
        }
    }
}

@Composable
fun CharacterGameCard(character: Character) {
    var currentHealth by remember { mutableIntStateOf(character.health) }
    var currentEnergy by remember { mutableIntStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    
    val usedAbilities = remember { mutableStateMapOf<String, Boolean>() }

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
                    onHealthChange = { currentHealth = it },
                    onEnergyChange = { currentEnergy = it },
                    onExpandToggle = { isExpanded = !isExpanded },
                    onAbilityToggle = { name, used -> usedAbilities[name] = used },
                    onFlip = { isFlipped = true }
                )
            } else {
                CharacterBack(character = character, onFlip = { isFlipped = false })
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

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ENERGY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (currentEnergy > 0) onEnergyChange(currentEnergy - 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Use", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "$currentEnergy",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { onEnergyChange(currentEnergy + 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Gain", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HealthGrid(
                    totalHealth = character.health,
                    currentHealth = currentHealth,
                    energyTrack = character.energyTrack,
                    onHealthChange = onHealthChange
                )
            }
            
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
                    AbilityItemPlay(
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
                    AbilityItemPlay(
                        name = header, 
                        description = ability.description,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        isUsed = usedAbilities[ability.name] ?: false,
                        onUsedChange = { onAbilityToggle(ability.name, it) }
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
                        onUsedChange = { onAbilityToggle(ability.name, it) }
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
    onUsedChange: (Boolean) -> Unit = {}
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
                        .border(1.2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onUsedChange(!isUsed) },
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
            text = parseAbilityDescription(description),
            style = MaterialTheme.typography.bodySmall
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
    onHealthChange: (Int) -> Unit
) {
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
                            isEnergyPoint -> Color(0xFF2196F3)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    .border(1.dp, if (isEnergyPoint) Color(0xFF1565C0) else Color.DarkGray, CircleShape)
                    .clickable {
                        if (currentHealth == healthValue) onHealthChange(healthValue - 1)
                        else onHealthChange(healthValue)
                    },
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
    val offensiveModifiers = mutableListOf<String>()
    if (character.impactDamageBuff > 0) offensiveModifiers.add("I+${character.impactDamageBuff}")
    if (character.slicingDamageBuff > 0) offensiveModifiers.add("S+${character.slicingDamageBuff}")
    if (character.piercingDamageBuff > 0) offensiveModifiers.add("P+${character.piercingDamageBuff}")
    
    if (offensiveModifiers.isNotEmpty() || character.dealsMagicalDamage) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Hardware, contentDescription = "Offense", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            if (character.dealsMagicalDamage) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Magical", modifier = Modifier.size(14.dp), tint = Color(0xFF00B0FF))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(offensiveModifiers.joinToString(", "), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun DefenseModifiersDisplay(character: Character) {
    val defensiveModifiers = mutableListOf<String>()
    if (character.allDamageMitigation > 0) {
        defensiveModifiers.add("ALL-${character.allDamageMitigation}")
    } else {
        if (character.impactDamageMitigation > 0) defensiveModifiers.add("I-${character.impactDamageMitigation}")
        if (character.slicingDamageMitigation > 0) defensiveModifiers.add("S-${character.slicingDamageMitigation}")
        if (character.piercingDamageMitigation > 0) defensiveModifiers.add("P-${character.piercingDamageMitigation}")
    }
    if (character.magicalDamageMitigation > 0) defensiveModifiers.add("M-${character.magicalDamageMitigation}")

    if (defensiveModifiers.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = "Defense", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(defensiveModifiers.joinToString(", "), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Spacer(modifier = Modifier.height(14.dp))
    }
}
