package com.garemat.moonstone_companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.garemat.moonstone_companion.Character
import com.garemat.moonstone_companion.CharacterEvent
import com.garemat.moonstone_companion.CharacterState
import com.garemat.moonstone_companion.CharacterViewModel
import com.garemat.moonstone_companion.Faction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTroupeScreen(
    viewModel: CharacterViewModel,
    state: CharacterState,
    onNavigateBack: () -> Unit
) {
    var expandedCharacterId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isHeaderVisible by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSaveValidationDialog by remember { mutableStateOf(false) }
    var showTutorialForcefully by remember { mutableStateOf(false) }
    val coordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    
    val shouldShowTutorial = (!state.hasSeenTroupesTutorial || showTutorialForcefully)

    // Tag filtering state - Dynamically calculated based on the selected faction
    val availableTags = remember(state.characters, viewModel.selectedTroupeFaction) {
        state.characters
            .filter { it.factions.contains(viewModel.selectedTroupeFaction) }
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    
    val selectedTags = remember { mutableStateListOf<String>() }

    // Clear selected tags that are no longer available due to faction change
    LaunchedEffect(availableTags) {
        val toRemove = selectedTags.filter { it !in availableTags }
        toRemove.forEach { selectedTags.remove(it) }
    }

    val availableCharacters = remember(state.characters, viewModel.selectedTroupeFaction, searchQuery, selectedTags.toList()) {
        state.characters
            .filter { it.factions.contains(viewModel.selectedTroupeFaction) }
            .filter { character ->
                val matchesSearch = searchQuery.isEmpty() || 
                    character.name.contains(searchQuery, ignoreCase = true) ||
                    character.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                
                val matchesTags = selectedTags.isEmpty() || 
                    selectedTags.all { tag -> character.tags.contains(tag) }
                
                matchesSearch && matchesTags
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (viewModel.editingTroupeId == null) "Build Troupe" else "Edit Troupe") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.onGloballyPositioned { coordsMap["SettingsCog"] = it }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Troupe Settings")
                        }
                        IconButton(
                            onClick = { isHeaderVisible = !isHeaderVisible },
                            modifier = Modifier.onGloballyPositioned { coordsMap["FilterButton"] = it }
                        ) {
                            Icon(
                                imageVector = if (isHeaderVisible) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                contentDescription = "Toggle Header"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (viewModel.autoSelectMembers) {
                                    showSaveValidationDialog = true
                                } else {
                                    viewModel.onEvent(CharacterEvent.SaveTroupe)
                                    onNavigateBack()
                                }
                            },
                            enabled = viewModel.newTroupeName.isNotBlank() && viewModel.selectedCharacterIds.isNotEmpty(),
                            modifier = Modifier.onGloballyPositioned { coordsMap["SaveButton"] = it }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save Troupe")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedVisibility(visible = isHeaderVisible) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = viewModel.newTroupeName,
                            onValueChange = { viewModel.newTroupeName = it },
                            label = { Text("Troupe Name") },
                            modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordsMap["TroupeName"] = it },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Select Faction Symbol", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .onGloballyPositioned { coordsMap["FactionSymbols"] = it },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Faction.entries.forEach { faction ->
                                val isSelected = viewModel.selectedTroupeFaction == faction
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) getFactionColor(faction) else Color.Transparent)
                                        .border(2.dp, getFactionColor(faction), CircleShape)
                                        .clickable { 
                                            viewModel.selectedTroupeFaction = faction
                                            viewModel.selectedCharacterIds = emptySet()
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

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name...") },
                            modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordsMap["NameSearch"] = it },
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tag Filter Row - Only showing tags available for the selected faction
                        if (availableTags.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordsMap["CharacterTags"] = it },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(availableTags) { tag ->
                                    val isSelected = selectedTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                        },
                                        label = { Text(tag) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Text(
                    text = "Add Characters (${viewModel.selectedCharacterIds.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp).onGloballyPositioned { coordsMap["AddCharacters"] = it }
                )
                
                if (availableCharacters.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty() && selectedTags.isEmpty()) "No characters found for this faction." else "No characters match search/tags.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(availableCharacters, key = { it.id }) { character ->
                            val isSelected = viewModel.selectedCharacterIds.contains(character.id)
                            val isExpanded = expandedCharacterId == character.id
                            
                            TroupeCharacterCard(
                                character = character,
                                isSelected = isSelected,
                                isExpanded = isExpanded,
                                onToggleSelect = {
                                    if (!character.isUnselectableInTroupe) {
                                        val current = viewModel.selectedCharacterIds.toMutableSet()
                                        if (isSelected) current.remove(character.id) else current.add(character.id)
                                        viewModel.selectedCharacterIds = current
                                    }
                                },
                                onExpandClick = {
                                    expandedCharacterId = if (isExpanded) null else character.id
                                }
                            )
                        }
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
                    steps = buildTroupeTutorialSteps,
                    targetCoordinates = coordsMap,
                    onComplete = {
                        viewModel.onEvent(CharacterEvent.SetHasSeenTutorial("troupes", true)) // Re-using troupes seen state for now or we could add a specific build_troupe key
                        showTutorialForcefully = false
                    },
                    onSkip = {
                        viewModel.onEvent(CharacterEvent.SetHasSeenTutorial("troupes", true))
                        showTutorialForcefully = false
                    }
                )
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Troupe Settings") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Select Members", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Skips the team selection prompt before games.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.autoSelectMembers,
                            onCheckedChange = { viewModel.autoSelectMembers = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSaveValidationDialog) {
        val count = viewModel.selectedCharacterIds.size
        AlertDialog(
            onDismissRequest = { showSaveValidationDialog = false },
            title = { Text("Save Troupe") },
            text = {
                Column {
                    Text("Auto Select is enabled. This troupe will be valid for:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 2 Players: ${if (count <= 6) "Valid" else "Invalid (Max 6)"}", color = if (count <= 6) Color(0xFF2E7D32) else Color.Red)
                    Text("• 3 Players: ${if (count <= 4) "Valid" else "Invalid (Max 4)"}", color = if (count <= 4) Color(0xFF2E7D32) else Color.Red)
                    Text("• 4 Players: ${if (count <= 3) "Valid" else "Invalid (Max 3)"}", color = if (count <= 3) Color(0xFF2E7D32) else Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Do you want to save anyway?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onEvent(CharacterEvent.SaveTroupe)
                    showSaveValidationDialog = false
                    onNavigateBack()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveValidationDialog = false }) {
                    Text("Back to Edit")
                }
            }
        )
    }
}

@Composable
fun TroupeCharacterCard(
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    enabled = !character.isUnselectableInTroupe
                )
                
                Spacer(modifier = Modifier.width(4.dp))

                // Small Portrait
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
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
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = character.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                    if (character.isUnselectableInTroupe) {
                        Text(
                            text = "Summoned character - automatically added to game",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(modifier = Modifier.padding(16.dp)) {
                    if (!isFlipped) {
                        CharacterFront(character = character, onFlip = { isFlipped = true })
                    } else {
                        CharacterBack(character = character, onFlip = { isFlipped = false })
                    }
                }
            }
        }
    }
}
