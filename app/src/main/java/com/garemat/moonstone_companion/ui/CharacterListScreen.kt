package com.garemat.moonstone_companion.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterListScreen(
    state: CharacterState,
    onEvent: (CharacterEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var expandedCharacterId by remember { mutableStateOf<Int?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFactions by remember { mutableStateOf(setOf<Faction>()) }
    val allTags = remember(state.characters) {
        state.characters.flatMap { it.tags }.distinct().sorted()
    }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showFilterBar by remember { mutableStateOf(false) }

    val filteredCharacters = remember(state.characters, searchQuery, selectedFactions, selectedTags) {
        state.characters.filter { character ->
            val matchesFaction = selectedFactions.isEmpty() || character.factions.any { it in selectedFactions }
            val matchesTags = selectedTags.isEmpty() || character.tags.containsAll(selectedTags)
            val matchesSearch = searchQuery.isEmpty() || 
                character.name.contains(searchQuery, ignoreCase = true) ||
                character.passiveAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) } ||
                character.activeAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) } ||
                character.arcaneAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
            
            matchesFaction && matchesTags && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Character Compendium") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterBar = !showFilterBar }) {
                        Icon(
                            imageVector = if (showFilterBar) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = if (selectedFactions.isNotEmpty() || selectedTags.isNotEmpty() || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showFilterBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search name or abilities...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Factions:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Faction.values().forEach { faction ->
                                val isSelected = selectedFactions.contains(faction)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) getFactionColor(faction) else Color.Transparent)
                                        .border(2.dp, getFactionColor(faction), CircleShape)
                                        .clickable { 
                                            selectedFactions = if (isSelected) {
                                                selectedFactions - faction
                                            } else {
                                                selectedFactions + faction
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getFactionIcon(faction),
                                        contentDescription = faction.name,
                                        tint = if (isSelected) Color.White else getFactionColor(faction)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Tags:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                    },
                                    label = { Text(tag) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        
                        if (searchQuery.isNotEmpty() || selectedFactions.isNotEmpty() || selectedTags.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFactions = emptySet()
                                    selectedTags = emptySet()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredCharacters.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No characters match your search.", color = Color.Gray)
                        }
                    }
                }
                items(filteredCharacters, key = { it.id }) { character ->
                    CharacterListItem(
                        character = character,
                        isExpanded = expandedCharacterId == character.id,
                        onExpandClick = {
                            expandedCharacterId = if (expandedCharacterId == character.id) null else character.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterListItem(
    character: Character,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val imageRes = remember(character.imageName) {
        if (character.imageName != null) {
            context.resources.getIdentifier(character.imageName, "drawable", context.packageName)
        } else 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        Text(
                            text = character.name.take(1),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = character.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                FactionIcons(character.factions)
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                Box(modifier = Modifier.padding(16.dp)) {
                    if (!isFlipped) {
                        CharacterFront(character, onFlip = { isFlipped = true })
                    } else {
                        CharacterBack(character, onFlip = { isFlipped = false })
                    }
                }
            }
        }
    }
}

@Composable
fun FactionIcons(factions: List<Faction>) {
    Row {
        factions.forEach { faction ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(getFactionColor(faction))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFactionIcon(faction),
                    contentDescription = faction.name,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun CharacterFront(character: Character, onFlip: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("Melee", character.melee.toString())
                StatBox("Range", "${character.meleeRange}\"")
                StatBox("Arcane", character.arcane.toString())
                StatBox("Evade", character.evade)
            }
            
            IconButton(onClick = onFlip) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Flip to Signature Move",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (character.passiveAbilities.isNotEmpty()) {
            character.passiveAbilities.forEach { ability ->
                AbilityItem(
                    ability.name,
                    ability.description,
                    oncePerTurn = ability.oncePerTurn,
                    oncePerGame = ability.oncePerGame
                )
            }
        }

        if (character.activeAbilities.isNotEmpty()) {
            if (character.passiveAbilities.isNotEmpty()) {
                AbilityTypeSeparator()
            }
            character.activeAbilities.forEach { ability ->
                val header = "${ability.name} (${ability.cost}) ${ability.range}"
                AbilityItem(
                    name = header, 
                    description = ability.description,
                    oncePerTurn = ability.oncePerTurn,
                    oncePerGame = ability.oncePerGame
                )
            }
        }

        if (character.arcaneAbilities.isNotEmpty()) {
            if (character.passiveAbilities.isNotEmpty() || character.activeAbilities.isNotEmpty()) {
                AbilityTypeSeparator()
            }
            character.arcaneAbilities.forEach { ability ->
                val header = "${ability.name} (${ability.cost}) ${ability.range}"
                AbilityItem(
                    name = header, 
                    description = ability.description,
                    oncePerTurn = ability.oncePerTurn,
                    oncePerGame = ability.oncePerGame,
                    reloadable = ability.reloadable
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Signature Move: ${character.signatureMove.name}.",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .clickable { onFlip() }
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        EnergyTrack(character.health, character.energyTrack)
        
        Text(
            text = "Base: ${character.baseSize}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun AbilityTypeSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.6f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CharacterBack(character: Character, onFlip: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = character.signatureMove.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onFlip) {
                Icon(Icons.Default.Refresh, contentDescription = "Flip Back")
            }
        }
        Text(
            text = "Upgrade for ${character.signatureMove.upgradeFrom}",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic
        )

        if (character.signatureMove.damageType != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Damage Type:", style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic)
                Text(text = character.signatureMove.damageType!!, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opponent Plays:", fontWeight = FontWeight.Bold)
                Text("Deal", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            character.signatureMove.results.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.opponentPlay)
                    SignatureResultDisplay(entry)
                }
            }
        }

        if (character.signatureMove.passiveEffect != null || character.signatureMove.endStepEffect != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                if (character.signatureMove.passiveEffect != null) {
                    Text(
                        text = character.signatureMove.passiveEffect!!,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (character.signatureMove.endStepEffect != null) {
                    val endStepText = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("End Step Effect: ")
                        }
                        append(character.signatureMove.endStepEffect!!)
                    }
                    Text(
                        text = endStepText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SignatureResultDisplay(entry: SignatureResultEntry) {
    if (entry.deal == "Null") {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Black,
                    radius = size.minDimension / 2.2f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(size.width * 0.25f, size.height * 0.75f),
                    end = Offset(size.width * 0.75f, size.height * 0.25f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (entry.isFollowUp) Color(0xFFFFEB3B) else Color.Transparent)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.deal,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        HorizontalDivider(modifier = Modifier.width(40.dp))
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun AbilityItem(
    name: String, 
    description: String, 
    oncePerTurn: Boolean = false, 
    oncePerGame: Boolean = false,
    reloadable: Boolean = false
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
        Text(text = fullTitle, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = parseAbilityDescription(description),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun parseAbilityDescription(description: String) = buildAnnotatedString {
    val regex = "\\[([GBP])\\]([^\\s,.:;]*)|Catastrophe:".toRegex()
    var lastIndex = 0
    
    regex.findAll(description).forEach { match ->
        append(description.substring(lastIndex, match.range.first))
        
        if (match.value == "Catastrophe:") {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)) {
                append("Catastrophe:")
            }
        } else {
            val colorCode = match.groupValues[1]
            val value = match.groupValues[2]
            val bgColor = when (colorCode) {
                "G" -> Color(0xFF2E7D32) // Forest Green
                "B" -> Color(0xFF1565C0) // Ocean Blue
                "P" -> Color(0xFFC2185B) // Dream Pink
                else -> Color.Transparent
            }
            
            withStyle(style = SpanStyle(
                background = bgColor,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift(0.1f)
            )) {
                append(" $value ")
            }
        }
        lastIndex = match.range.last + 1
    }
    append(description.substring(lastIndex))
}

@Composable
fun EnergyTrack(health: Int, energyGainThresholds: List<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        (1..health).forEach { h ->
            val isEnergyGain = h in energyGainThresholds
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isEnergyGain) Color(0xFF2196F3) else Color.Transparent)
                    .border(1.dp, Color.Black, CircleShape)
                    .padding(2.dp)
                    .background(if (isEnergyGain) Color(0xFF2196F3) else Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

fun getFactionColor(faction: Faction): Color {
    return when (faction) {
        Faction.COMMONWEALTH -> Color(0xFFFBC02D) // Yellow Sun
        Faction.DOMINION -> Color(0xFF1976D2) // Blue Moon
        Faction.LESHAVULT -> Color(0xFF388E3C) // Green Elk
        Faction.SHADES -> Color(0xFF424242) // Black Skull
    }
}

fun getFactionIcon(faction: Faction): ImageVector {
    return when (faction) {
        Faction.COMMONWEALTH -> Icons.Default.WbSunny
        Faction.DOMINION -> Icons.Default.Brightness2
        Faction.LESHAVULT -> Icons.Default.Nature
        Faction.SHADES -> Icons.Default.Warning
    }
}
