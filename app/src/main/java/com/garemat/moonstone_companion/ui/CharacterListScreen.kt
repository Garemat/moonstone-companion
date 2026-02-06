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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.garemat.moonstone_companion.*
import com.garemat.moonstone_companion.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterListScreen(
    state: CharacterState,
    onEvent: (CharacterEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var expandedCharacterId by remember { mutableStateOf<Int?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    
    // Search & Filter State
    var searchQuery by remember { mutableStateOf("") }
    var selectedFactions by remember { mutableStateOf(setOf<Faction>()) }
    
    val availableTags = remember(state.characters, selectedFactions) {
        state.characters
            .filter { char -> selectedFactions.isEmpty() || char.factions.any { it in selectedFactions } }
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showFilterBar by remember { mutableStateOf(false) }

    // Tutorial state
    val coordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    var showTutorialForcefully by remember { mutableStateOf(false) }
    val shouldShowTutorial = (!state.hasSeenCharactersTutorial || showTutorialForcefully)
    var tutorialStepIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Clear selected tags that are no longer available due to faction filtering
    LaunchedEffect(availableTags) {
        selectedTags = selectedTags.filter { it in availableTags }.toSet()
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Character Compendium") },
                    navigationIcon = {
                        IconButton(onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastBackPressTime > 500) {
                                onNavigateBack()
                                lastBackPressTime = currentTime
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showFilterBar = !showFilterBar },
                            modifier = Modifier.onGloballyPositioned { 
                                coordsMap["FilterButtonOpen"] = it 
                                coordsMap["FilterButtonClose"] = it
                            }
                        ) {
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
                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordsMap["SearchField"] = it },
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
                            
                            // Faction Selector
                            Text("Factions:", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).onGloballyPositioned { coordsMap["FactionFilter"] = it },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Faction.entries.forEach { faction ->
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
                                        FactionSymbol(
                                            faction = faction,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (isSelected) Color.White else getFactionColor(faction)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Tag Selector
                            if (availableTags.isNotEmpty()) {
                                Text("Tags:", style = MaterialTheme.typography.labelMedium)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).onGloballyPositioned { coordsMap["TagFilter"] = it },
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableTags.forEach { tag ->
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
                    contentPadding = PaddingValues(bottom = 16.dp),
                    state = listState
                ) {
                    if (filteredCharacters.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No characters match your search.", color = Color.Gray)
                            }
                        }
                    }
                    items(filteredCharacters, key = { it.id }) { character ->
                        val isFirst = filteredCharacters.firstOrNull()?.id == character.id
                        CharacterListItem(
                            character = character,
                            isExpanded = expandedCharacterId == character.id,
                            onExpandClick = {
                                expandedCharacterId = if (expandedCharacterId == character.id) null else character.id
                            },
                            onPositioned = { name, coords -> 
                                if (isFirst) coordsMap[name] = coords 
                            },
                            forceFlipped = if (shouldShowTutorial && isFirst && expandedCharacterId == character.id && tutorialStepIndex >= 8) true else null
                        )
                    }
                }
            }
        }

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
                    steps = charactersScreenTutorialSteps,
                    targetCoordinates = coordsMap,
                    onComplete = {
                        onEvent(CharacterEvent.SetHasSeenTutorial("characters", true))
                        showTutorialForcefully = false
                        expandedCharacterId = null
                        tutorialStepIndex = 0
                    },
                    onSkip = {
                        onEvent(CharacterEvent.SetHasSeenTutorial("characters", true))
                        showTutorialForcefully = false
                        expandedCharacterId = null
                        tutorialStepIndex = 0
                    },
                    onStepChange = { step ->
                        tutorialStepIndex = step
                        when(step) {
                            0 -> showFilterBar = false
                            1, 2, 3, 4 -> showFilterBar = true
                            5 -> {
                                showFilterBar = false
                                expandedCharacterId = null
                            }
                            6 -> {
                                expandedCharacterId = filteredCharacters.firstOrNull()?.id
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            7 -> {
                                expandedCharacterId = filteredCharacters.firstOrNull()?.id
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CharacterListItem(
    character: Character,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    forceFlipped: Boolean? = null
) {
    var isFlippedState by remember { mutableStateOf(false) }
    val isFlipped = forceFlipped ?: isFlippedState
    
    val context = LocalContext.current
    val appTheme = LocalAppTheme.current
    
    // Resolve image resource ID from name
    val imageRes = remember(character.imageName) {
        if (character.imageName != null) {
            val cleanName = character.imageName.substringBeforeLast(".")
            context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        } else 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
            .onGloballyPositioned { onPositioned("FirstCharacterCard", it) },
        shape = RoundedCornerShape(if (appTheme == AppTheme.MOONSTONE) 0.dp else 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appTheme == AppTheme.MOONSTONE) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appTheme == AppTheme.MOONSTONE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Header (Always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Character Image
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
                        style = MaterialTheme.typography.titleLarge,
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
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isExpanded) {
                if (appTheme != AppTheme.MOONSTONE) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (appTheme == AppTheme.MOONSTONE && imageRes != 0) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.25f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (!isFlipped) {
                            CharacterFront(
                                character = character, 
                                onFlip = { isFlippedState = true },
                                onFlipPositioned = { onPositioned("FlipButton", it) }
                            )
                        } else {
                            CharacterBack(
                                character = character, 
                                onFlip = { isFlippedState = false },
                                onFlipPositioned = { onPositioned("FlipButton", it) }
                            )
                        }
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
                FactionSymbol(
                    faction = faction,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun FactionSymbol(faction: Faction, modifier: Modifier = Modifier, tint: Color? = null) {
    val context = LocalContext.current
    val resName = when(faction) {
        Faction.SHADES -> "shades"
        else -> null
    }
    
    val resId = resName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = faction.name,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = getFactionIcon(faction),
            contentDescription = faction.name,
            modifier = modifier,
            tint = tint ?: Color.Unspecified
        )
    }
}

@Composable
fun CharacterFront(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    if (LocalAppTheme.current == AppTheme.MOONSTONE) {
        MoonstoneCharacterFront(character, onFlip, onFlipPositioned)
    } else {
        DefaultCharacterFront(character, onFlip, onFlipPositioned)
    }
}

@Composable
fun DefaultCharacterFront(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        // Stats Row with Flip Button
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
            
            IconButton(
                onClick = onFlip,
                modifier = Modifier.onGloballyPositioned { onFlipPositioned(it) }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Flip to Signature Move",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Abilities
        Column {
            if (character.passiveAbilities.isNotEmpty()) {
                character.passiveAbilities.forEach { ability ->
                    PassiveAbilityItem(ability.name, ability.description)
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Signature Move Link
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

        // Health Bar (Energy Track)
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
fun MoonstoneCharacterFront(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        // Header: Name, Tags, and Faction Symbol
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${character.name},",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = character.tags.joinToString(", "),
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                character.factions.firstOrNull()?.let { faction ->
                    FactionSymbol(
                        faction = faction,
                        modifier = Modifier.size(48.dp).padding(end = 8.dp)
                    )
                }
                IconButton(
                    onClick = onFlip,
                    modifier = Modifier.onGloballyPositioned { onFlipPositioned(it) }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Flip", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Official Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Melee & Range Block
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(text = "Melee", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
                    Text(text = "Range", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
                }
                HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(text = character.melee.toString(), style = MaterialTheme.typography.headlineMedium)
                    Text(text = "${character.meleeRange}\"", style = MaterialTheme.typography.headlineMedium)
                }
            }

            // Diagonal Divider
            Canvas(modifier = Modifier.height(40.dp).width(30.dp)) {
                drawLine(
                    color = Color(0xFF2C1810),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Arcane & Evade Block
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(text = "Arcane", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
                    Text(text = "Evade", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
                }
                HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(text = character.arcane.toString(), style = MaterialTheme.typography.headlineMedium)
                    Text(text = character.evade, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Abilities
        Column {
            if (character.passiveAbilities.isNotEmpty()) {
                character.passiveAbilities.forEach { ability ->
                    PassiveAbilityItem(ability.name, ability.description)
                }
            }
            
            if (character.activeAbilities.isNotEmpty()) {
                if (character.passiveAbilities.isNotEmpty()) AbilityTypeSeparator()
                character.activeAbilities.forEach { ability ->
                    val header = "${ability.name} (${ability.cost})" + if (ability.oncePerTurn) " - Once per turn" else ""
                    AbilityItem(header, ability.description)
                }
            }
            
            if (character.arcaneAbilities.isNotEmpty()) {
                if (character.passiveAbilities.isNotEmpty() || character.activeAbilities.isNotEmpty()) AbilityTypeSeparator()
                character.arcaneAbilities.forEach { ability ->
                    val header = "${ability.name} (${ability.cost})" + if (ability.oncePerGame) " - Once per game" else ""
                    AbilityItem(header, ability.description)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        val signatureText = buildAnnotatedString {
            append("Signature Move on a ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(character.signatureMove.upgradeFrom)
            }
            append(".")
        }
        Text(
            text = signatureText,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            modifier = Modifier.clickable { onFlip() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnergyTrack(character.health, character.energyTrack)
            Column(horizontalAlignment = Alignment.End) {
                Text("Base:", style = MaterialTheme.typography.labelSmall)
                Text(character.baseSize, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun PassiveAbilityItem(name: String, description: String) {
    val fullText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("$name: ")
        }
        append(parseAbilityDescription(description))
    }
    Text(
        text = fullText,
        style = MaterialTheme.typography.bodySmall, 
        inlineContent = getMoonstoneInlineContent(),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun AbilityTypeSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val appTheme = LocalAppTheme.current
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.6f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (appTheme == AppTheme.MOONSTONE) 0.3f else 0.1f)
        )
    }
}

@Composable
fun CharacterBack(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    if (LocalAppTheme.current == AppTheme.MOONSTONE) {
        MoonstoneCharacterBack(character, onFlip, onFlipPositioned)
    } else {
        DefaultCharacterBack(character, onFlip, onFlipPositioned)
    }
}

@Composable
fun DefaultCharacterBack(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    val inlineContent = getMoonstoneInlineContent()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = character.signatureMove.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onFlip,
                modifier = Modifier.onGloballyPositioned { onFlipPositioned(it) }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Flip Back", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = "Upgrade for ${character.signatureMove.upgradeFrom}",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        if (character.signatureMove.damageType != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Damage Type:", style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic)
                Text(text = character.signatureMove.damageType!!, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Signature Table
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

        // Additional Effects
        if (character.signatureMove.passiveEffect != null || character.signatureMove.endStepEffect != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                if (character.signatureMove.passiveEffect != null) {
                    Text(
                        text = parseAbilityDescription(character.signatureMove.passiveEffect!!),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                        inlineContent = inlineContent
                    )
                }
                if (character.signatureMove.endStepEffect != null) {
                    val endStepText = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("End Step Effect: ")
                        }
                        append(parseAbilityDescription(character.signatureMove.endStepEffect!!))
                    }
                    Text(
                        text = endStepText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                        inlineContent = inlineContent
                    )
                }
            }
        }
    }
}

@Composable
fun MoonstoneCharacterBack(
    character: Character, 
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(
                text = character.signatureMove.name,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onFlip,
                modifier = Modifier.onGloballyPositioned { onFlipPositioned(it) }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Flip", tint = MaterialTheme.colorScheme.secondary)
            }
        }
        val upgradeText = buildAnnotatedString {
            append("Upgrade for ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(character.signatureMove.upgradeFrom)
            }
        }
        Text(
            text = upgradeText,
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (character.signatureMove.damageType != null) {
            Text(text = "Damage Type:", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
            Text(text = character.signatureMove.damageType!!, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Opponent plays:", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
            Text("deal", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            character.signatureMove.results.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.opponentPlay, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal))
                    SignatureResultDisplay(entry)
                }
            }
        }

        if (character.signatureMove.passiveEffect != null || character.signatureMove.endStepEffect != null) {
            Spacer(modifier = Modifier.height(16.dp))
            if (character.signatureMove.passiveEffect != null) {
                Text(
                    text = parseAbilityDescription(character.signatureMove.passiveEffect!!),
                    style = MaterialTheme.typography.bodyMedium,
                    inlineContent = getMoonstoneInlineContent()
                )
            }
            if (character.signatureMove.endStepEffect != null) {
                val text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("End Step Effect: ")
                    }
                    append(parseAbilityDescription(character.signatureMove.endStepEffect!!))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    inlineContent = getMoonstoneInlineContent()
                )
            }
        }
    }
}

@Composable
fun SignatureResultDisplay(entry: SignatureResultEntry) {
    val isNull = entry.deal == "Null"
    val appTheme = LocalAppTheme.current
    
    Box(
        modifier = Modifier
            .size(if (isNull) 24.dp else 28.dp)
            .clip(CircleShape)
            .background(if (entry.isFollowUp) Color(0xFFFFEB3B) else Color.Transparent)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isNull) {
            NullSymbol(size = 20.dp)
        } else {
            Text(
                text = entry.deal,
                fontWeight = FontWeight.Bold,
                fontSize = if (appTheme == AppTheme.MOONSTONE) 20.sp else 14.sp,
                color = if (entry.isFollowUp) Color.Black else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        HorizontalDivider(modifier = Modifier.width(40.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
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
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        val title = buildAnnotatedString {
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
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = parseAbilityDescription(description),
            style = MaterialTheme.typography.bodySmall,
            inlineContent = getMoonstoneInlineContent()
        )
    }
}

@Composable
fun parseAbilityDescription(description: String) = buildAnnotatedString {
    val regex = "\\[([GBP])\\]([^\\s,.:;]*)|Catastrophe:|\\{Null\\}".toRegex()
    var lastIndex = 0
    
    regex.findAll(description).forEach { match ->
        append(description.substring(lastIndex, match.range.first))
        
        when {
            match.value == "Catastrophe:" -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)) {
                    append("Catastrophe:")
                }
            }
            match.value == "{Null}" -> {
                appendInlineContent("nullSymbol", "{Null}")
            }
            else -> {
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
        }
        lastIndex = match.range.last + 1
    }
    append(description.substring(lastIndex))
}

@Composable
fun EnergyTrack(health: Int, energyGainThresholds: List<Int>) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        (1..health).forEach { h ->
            val isEnergyGain = h in energyGainThresholds
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isEnergyGain) Color(0xFF2196F3) else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .padding(2.dp)
                    .background(if (isEnergyGain) Color(0xFF2196F3) else Color.Transparent, CircleShape)
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
        Faction.DOMINION -> Icons.Default.Brightness2 // Moon
        Faction.LESHAVULT -> Icons.Default.Nature
        Faction.SHADES -> Icons.Default.Warning // Using warning as a skull placeholder
    }
}
