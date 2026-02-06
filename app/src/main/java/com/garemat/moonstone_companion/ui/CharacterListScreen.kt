package com.garemat.moonstone_companion.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.garemat.moonstone_companion.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    state: CharacterState,
    onEvent: (CharacterEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var expandedCharacterIds by remember { mutableStateOf(setOf<Int>()) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    
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

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            expandedCharacterIds = emptySet()
        } else {
            val matchingIds = state.characters.filter { character ->
                character.name.contains(searchQuery, ignoreCase = true) ||
                character.passiveAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) } ||
                character.activeAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) } ||
                character.arcaneAbilities.any { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
            }.map { it.id }.toSet()
            
            if (matchingIds.size in 1..3) {
                expandedCharacterIds = matchingIds
            }
        }
    }

    val coordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    var showTutorialForcefully by remember { mutableStateOf(false) }
    val shouldShowTutorial = (!state.hasSeenCharactersTutorial || showTutorialForcefully)
    var tutorialStepIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
                        CharacterFilterHeader(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedFactions = selectedFactions,
                            onFactionsChange = { selectedFactions = it },
                            selectedTags = selectedTags,
                            onTagsChange = { selectedTags = it },
                            availableTags = availableTags,
                            showCollapseAll = expandedCharacterIds.isNotEmpty(),
                            onCollapseAll = { expandedCharacterIds = emptySet() },
                            onClearAll = {
                                searchQuery = ""
                                selectedFactions = emptySet()
                                selectedTags = emptySet()
                                expandedCharacterIds = emptySet()
                            },
                            coordsMap = coordsMap
                        )
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
                        CommonCharacterCard(
                            character = character,
                            searchQuery = searchQuery,
                            isExpanded = expandedCharacterIds.contains(character.id),
                            onExpandClick = {
                                expandedCharacterIds = if (expandedCharacterIds.contains(character.id)) {
                                    expandedCharacterIds - character.id
                                } else {
                                    expandedCharacterIds + character.id
                                }
                            },
                            cardTargetName = if (isFirst) "FirstCharacterCard" else "CharacterCard",
                            onPositioned = { name, coords -> 
                                if (isFirst || name == "FlipButton") coordsMap[name] = coords
                            },
                            forceFlipped = if (shouldShowTutorial && isFirst && expandedCharacterIds.contains(character.id) && tutorialStepIndex >= 8) true else null
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = { showTutorialForcefully = true },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
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
                        expandedCharacterIds = emptySet()
                        tutorialStepIndex = 0
                    },
                    onSkip = {
                        onEvent(CharacterEvent.SetHasSeenTutorial("characters", true))
                        showTutorialForcefully = false
                        expandedCharacterIds = emptySet()
                        tutorialStepIndex = 0
                    },
                    onStepChange = { step ->
                        tutorialStepIndex = step
                        when(step) {
                            0 -> showFilterBar = false
                            1, 2, 3, 4 -> showFilterBar = true
                            5 -> {
                                showFilterBar = false
                                expandedCharacterIds = emptySet()
                            }
                            6 -> {
                                val firstId = filteredCharacters.firstOrNull()?.id
                                if (firstId != null) expandedCharacterIds = setOf(firstId)
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            7 -> {
                                val firstId = filteredCharacters.firstOrNull()?.id
                                if (firstId != null) expandedCharacterIds = setOf(firstId)
                            }
                        }
                    }
                )
            }
        }
    }
}
