package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
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
import com.garemat.moonstone_companion.*
import com.garemat.moonstone_companion.ui.theme.LocalAppTheme

@Composable
fun NullSymbol(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val appTheme = LocalAppTheme.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (appTheme == AppTheme.MOONSTONE) Color.Transparent else Color.LightGray.copy(alpha = 0.5f))
            .padding(if (size < 20.dp) 1.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (size < 20.dp) 1.2.dp.toPx() else 1.5.dp.toPx()
            drawCircle(
                color = Color.Black,
                radius = this.size.minDimension / 2.2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            drawLine(
                color = Color.Black,
                start = Offset(this.size.width * 0.25f, this.size.height * 0.75f),
                end = Offset(this.size.width * 0.75f, this.size.height * 0.25f),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun getMoonstoneInlineContent() = mapOf(
    "nullSymbol" to InlineTextContent(
        Placeholder(
            width = 14.sp,
            height = 14.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        NullSymbol(size = 14.dp, modifier = Modifier.padding(horizontal = 2.dp))
    }
)

fun highlightText(text: String, searchQuery: String): AnnotatedString = buildAnnotatedString {
    appendWithHighlight(text, searchQuery)
}

fun AnnotatedString.Builder.appendWithHighlight(text: String, searchQuery: String) {
    if (searchQuery.isEmpty() || !text.contains(searchQuery, ignoreCase = true)) {
        append(text)
        return
    }

    val pattern = Regex.escape(searchQuery).toRegex(RegexOption.IGNORE_CASE)
    var lastIndex = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        withStyle(style = SpanStyle(
            background = Color.Yellow.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold
        )) {
            append(match.value)
        }
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}

@Composable
fun parseAbilityDescription(description: String, searchQuery: String = "") = buildAnnotatedString {
    val regex = "\\[([GBP])\\]([^\\s,.:;]*)|Catastrophe:|\\{Null\\}".toRegex()
    var lastIndex = 0
    
    regex.findAll(description).forEach { match ->
        appendWithHighlight(description.substring(lastIndex, match.range.first), searchQuery)
        
        when (match.value) {
            "Catastrophe:" -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)) {
                    append("Catastrophe:")
                }
            }
            "{Null}" -> {
                appendInlineContent("nullSymbol", "{Null}")
            }
            else -> {
                val colorCode = match.groupValues[1]
                val value = match.groupValues[2]
                val bgColor = when (colorCode) {
                    "G" -> Color(0xFF2E7D32)
                    "B" -> Color(0xFF1565C0)
                    "P" -> Color(0xFFC2185B)
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
    appendWithHighlight(description.substring(lastIndex), searchQuery)
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

// Faction Utilities
fun getFactionColor(faction: Faction): Color {
    return when (faction) {
        Faction.COMMONWEALTH -> Color(0xFFFBC02D)
        Faction.DOMINION -> Color(0xFF1976D2)
        Faction.LESHAVULT -> Color(0xFF388E3C)
        Faction.SHADES -> Color(0xFF424242)
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
fun FactionSelector(
    selectedFactions: Set<Faction>,
    onFactionsChange: (Set<Faction>) -> Unit,
    modifier: Modifier = Modifier,
    singleSelect: Boolean = false,
    onPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .onGloballyPositioned { onPositioned(it) },
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
                        if (singleSelect) {
                            onFactionsChange(setOf(faction))
                        } else {
                            val newSet = if (isSelected) selectedFactions - faction else selectedFactions + faction
                            onFactionsChange(newSet)
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
}

@Composable
fun CharacterFilterHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFactions: Set<Faction>,
    onFactionsChange: (Set<Faction>) -> Unit,
    selectedTags: Set<String>,
    onTagsChange: (Set<String>) -> Unit,
    availableTags: List<String>,
    modifier: Modifier = Modifier,
    isFactionFixed: Boolean = false,
    showCollapseAll: Boolean = false,
    onCollapseAll: () -> Unit = {},
    onClearAll: () -> Unit = {},
    coordsMap: MutableMap<String, LayoutCoordinates> = mutableMapOf()
) {
    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordsMap["SearchField"] = it },
            placeholder = { Text("Search name or abilities...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        if (!isFactionFixed) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Factions:", style = MaterialTheme.typography.labelMedium)
            FactionSelector(
                selectedFactions = selectedFactions,
                onFactionsChange = onFactionsChange,
                onPositioned = { coordsMap["FactionFilter"] = it }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (availableTags.isNotEmpty()) {
            Text("Tags:", style = MaterialTheme.typography.labelMedium)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .onGloballyPositioned { coordsMap["TagFilter"] = it },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(availableTags) { tag ->
                    val isSelected = selectedTags.contains(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newTags = if (isSelected) selectedTags - tag else selectedTags + tag
                            onTagsChange(newTags)
                        },
                        label = { Text(tag) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCollapseAll) {
                TextButton(onClick = onCollapseAll) {
                    Icon(Icons.Default.UnfoldLess, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Collapse All")
                }
            }
            
            if (searchQuery.isNotEmpty() || selectedFactions.isNotEmpty() || selectedTags.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
fun CommonAbilityItem(
    name: String, 
    description: String, 
    searchQuery: String = "",
    oncePerTurn: Boolean = false, 
    oncePerGame: Boolean = false,
    reloadable: Boolean = false,
    isUsed: Boolean = false,
    onUsedChange: ((Boolean) -> Unit)? = null,
    isEditable: Boolean = true
) {
    val inlineContent = getMoonstoneInlineContent()
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val title = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendWithHighlight(name, searchQuery)
                    append(": ")
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
            Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

            if (oncePerGame && onUsedChange != null) {
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
            text = parseAbilityDescription(description, searchQuery),
            style = MaterialTheme.typography.bodySmall,
            inlineContent = inlineContent
        )
    }
}

@Composable
fun CommonStatBox(
    label: String, 
    value: String, 
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    showDivider: Boolean = false
) {
    Column(horizontalAlignment = horizontalAlignment, modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.width(40.dp))
        }
        Text(
            text = value, 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.ExtraBold,
            textAlign = when(horizontalAlignment) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            }
        )
    }
}

@Composable
fun PassiveAbilityItem(name: String, description: String, searchQuery: String) {
    val fullText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            appendWithHighlight(name, searchQuery)
            append(": ")
        }
        append(parseAbilityDescription(description, searchQuery))
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

@Composable
fun CharacterFront(
    character: Character, 
    searchQuery: String,
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    if (LocalAppTheme.current == AppTheme.MOONSTONE) {
        MoonstoneCharacterFront(character, searchQuery, onFlip, onFlipPositioned)
    } else {
        DefaultCharacterFront(character, searchQuery, onFlip, onFlipPositioned)
    }
}

@Composable
fun CharacterBack(
    character: Character, 
    searchQuery: String,
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    if (LocalAppTheme.current == AppTheme.MOONSTONE) {
        MoonstoneCharacterBack(character, searchQuery, onFlip, onFlipPositioned)
    } else {
        DefaultCharacterBack(character, searchQuery, onFlip, onFlipPositioned)
    }
}

@Composable
fun DefaultCharacterFront(
    character: Character, 
    searchQuery: String,
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CommonStatBox("Melee", character.melee.toString(), showDivider = true)
                CommonStatBox("Range", "${character.meleeRange}\"", showDivider = true)
                CommonStatBox("Arcane", character.arcane.toString(), showDivider = true)
                CommonStatBox("Evade", character.evade, showDivider = true)
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

        Column {
            if (character.passiveAbilities.isNotEmpty()) {
                character.passiveAbilities.forEach { ability ->
                    PassiveAbilityItem(ability.name, ability.description, searchQuery)
                }
            }

            if (character.activeAbilities.isNotEmpty()) {
                if (character.passiveAbilities.isNotEmpty()) {
                    AbilityTypeSeparator()
                }
                character.activeAbilities.forEach { ability ->
                    val header = "${ability.name} (${ability.cost}) ${ability.range}"
                    CommonAbilityItem(
                        name = header, 
                        description = ability.description,
                        searchQuery = searchQuery,
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
                    CommonAbilityItem(
                        name = header, 
                        description = ability.description,
                        searchQuery = searchQuery,
                        oncePerTurn = ability.oncePerTurn,
                        oncePerGame = ability.oncePerGame,
                        reloadable = ability.reloadable
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val signatureLinkText = buildAnnotatedString {
            append("Signature Move: ")
            appendWithHighlight(character.signatureMove.name, searchQuery)
            append(".")
        }
        Text(
            text = signatureLinkText,
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
fun MoonstoneCharacterFront(
    character: Character, 
    searchQuery: String,
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString { appendWithHighlight(character.name, searchQuery); append(",") },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Canvas(modifier = Modifier.height(40.dp).width(30.dp)) {
                drawLine(
                    color = Color(0xFF2C1810),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

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

        Column {
            if (character.passiveAbilities.isNotEmpty()) {
                character.passiveAbilities.forEach { ability ->
                    PassiveAbilityItem(ability.name, ability.description, searchQuery)
                }
            }
            
            if (character.activeAbilities.isNotEmpty()) {
                if (character.passiveAbilities.isNotEmpty()) AbilityTypeSeparator()
                character.activeAbilities.forEach { ability ->
                    val header = "${ability.name} (${ability.cost})" + if (ability.oncePerTurn) " - Once per turn" else ""
                    CommonAbilityItem(header, ability.description, searchQuery)
                }
            }
            
            if (character.arcaneAbilities.isNotEmpty()) {
                if (character.passiveAbilities.isNotEmpty() || character.activeAbilities.isNotEmpty()) AbilityTypeSeparator()
                character.arcaneAbilities.forEach { ability ->
                    val header = "${ability.name} (${ability.cost})" + if (ability.oncePerGame) " - Once per game" else ""
                    CommonAbilityItem(header, ability.description, searchQuery)
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
fun DefaultCharacterBack(
    character: Character, 
    searchQuery: String,
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
                text = highlightText(character.signatureMove.name, searchQuery),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onFlip,
                modifier = Modifier.onGloballyPositioned { onFlipPositioned(it) }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Flip Back",
                    tint = MaterialTheme.colorScheme.primary
                )
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
                        text = parseAbilityDescription(character.signatureMove.passiveEffect!!, searchQuery),
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
                        append(parseAbilityDescription(character.signatureMove.endStepEffect!!, searchQuery))
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
    searchQuery: String,
    onFlip: () -> Unit,
    onFlipPositioned: (LayoutCoordinates) -> Unit = {}
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(
                text = highlightText(character.signatureMove.name, searchQuery),
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
                    text = parseAbilityDescription(character.signatureMove.passiveEffect!!, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    inlineContent = getMoonstoneInlineContent()
                )
            }
            if (character.signatureMove.endStepEffect != null) {
                val text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("End Step Effect: ")
                    }
                    append(parseAbilityDescription(character.signatureMove.endStepEffect!!, searchQuery))
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
fun CommonCharacterCard(
    character: Character,
    searchQuery: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPositioned: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    forceFlipped: Boolean? = null,
    selectionControl: @Composable (RowScope.() -> Unit)? = null
) {
    var isFlippedState by remember { mutableStateOf(false) }
    val isFlipped = forceFlipped ?: isFlippedState
    
    val context = LocalContext.current
    val appTheme = LocalAppTheme.current
    
    val imageRes = remember(character.imageName) {
        if (character.imageName != null) {
            val cleanName = character.imageName.substringBeforeLast(".")
            context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        } else 0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .onGloballyPositioned { onPositioned("CharacterCard", it) },
        shape = RoundedCornerShape(if (appTheme == AppTheme.MOONSTONE) 0.dp else 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appTheme == AppTheme.MOONSTONE) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appTheme == AppTheme.MOONSTONE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(if (selectionControl != null) 8.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionControl != null) {
                    selectionControl()
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .size(if (selectionControl != null) 40.dp else 56.dp)
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
                            style = if (selectionControl != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (selectionControl != null) 12.dp else 16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = highlightText(character.name, searchQuery),
                        style = if (selectionControl != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
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
                                searchQuery = searchQuery,
                                onFlip = { isFlippedState = true },
                                onFlipPositioned = { coords -> onPositioned("FlipButton", coords) }
                            )
                        } else {
                            CharacterBack(
                                character = character, 
                                searchQuery = searchQuery,
                                onFlip = { isFlippedState = false },
                                onFlipPositioned = { coords -> onPositioned("FlipButton", coords) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierDisplay(
    character: Character,
    isOffense: Boolean,
    modifier: Modifier = Modifier
) {
    val modifiers = mutableListOf<@Composable () -> Unit>()
    
    fun addModifier(prefix: String, value: String, isOffense: Boolean) {
        if (value == "Null") {
            modifiers.add {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prefix, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    NullSymbol(size = 12.dp, modifier = Modifier.padding(horizontal = 1.dp))
                }
            }
        } else if (value.toIntOrNull() != 0) {
            val sign = if (isOffense) "+" else "-"
            modifiers.add {
                Text("$prefix$sign$value", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isOffense) {
        addModifier("I", character.impactDamageBuff, true)
        addModifier("S", character.slicingDamageBuff, true)
        addModifier("P", character.piercingDamageBuff, true)
    } else {
        if (character.allDamageMitigation != "0") {
            addModifier("ALL", character.allDamageMitigation, false)
        } else {
            addModifier("I", character.impactDamageMitigation, false)
            addModifier("S", character.slicingDamageMitigation, false)
            addModifier("P", character.piercingDamageMitigation, false)
        }
        addModifier("M", character.magicalDamageMitigation, false)
    }
    
    if (modifiers.isNotEmpty() || (isOffense && character.dealsMagicalDamage)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Icon(
                imageVector = if (isOffense) Icons.Default.Hardware else Icons.Default.Shield,
                contentDescription = if (isOffense) "Offense" else "Defense",
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            if (isOffense && character.dealsMagicalDamage) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Magical", modifier = Modifier.size(14.dp), tint = Color(0xFF00B0FF))
                Spacer(modifier = Modifier.width(2.dp))
            }
            modifiers.forEachIndexed { index, modifierComp ->
                modifierComp()
                if (index < modifiers.size - 1) {
                    Text(", ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun HealthTracker(
    totalHealth: Int,
    currentHealth: Int,
    energyTrack: List<Int>,
    onHealthChange: (Int) -> Unit,
    isEditable: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        modifier = modifier.padding(top = 12.dp),
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
