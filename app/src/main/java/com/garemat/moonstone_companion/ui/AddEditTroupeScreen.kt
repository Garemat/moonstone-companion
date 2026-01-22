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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    val allTags = remember(state.characters) {
        state.characters.flatMap { it.tags }.distinct().sorted()
    }
    val selectedTags = remember { mutableStateListOf<String>() }

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
                    IconButton(onClick = { isHeaderVisible = !isHeaderVisible }) {
                        Icon(
                            imageVector = if (isHeaderVisible) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Header"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.onEvent(CharacterEvent.SaveTroupe)
                            onNavigateBack()
                        },
                        enabled = viewModel.newTroupeName.isNotBlank() && viewModel.selectedCharacterIds.isNotEmpty()
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Faction Symbol", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                                Icon(
                                    imageVector = getFactionIcon(faction),
                                    contentDescription = faction.name,
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
                        modifier = Modifier.fillMaxWidth(),
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


                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(allTags) { tag ->
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

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Text(
                text = "Add Characters (${viewModel.selectedCharacterIds.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
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
                                val current = viewModel.selectedCharacterIds.toMutableSet()
                                if (isSelected) current.remove(character.id) else current.add(character.id)
                                viewModel.selectedCharacterIds = current
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
            context.resources.getIdentifier(character.imageName, "drawable", context.packageName)
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
                    onCheckedChange = { onToggleSelect() }
                )
                
                Spacer(modifier = Modifier.width(4.dp))

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
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = character.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
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
