package com.garemat.moonstone_companion.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.CharacterViewModel
import com.garemat.moonstone_companion.RuleSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: CharacterViewModel,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val rules by viewModel.rules.collectAsState()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var showTutorialForcefully by remember { mutableStateOf(false) }

    val displayRules = remember(searchQuery, rules) {
        if (searchQuery.isEmpty()) {
            rules
        } else {
            rules.filter { rule ->
                rule.searchable && (
                    rule.title.contains(searchQuery, ignoreCase = true) ||
                    rule.content.contains(searchQuery, ignoreCase = true) ||
                    rule.keywords.any { it.contains(searchQuery, ignoreCase = true) } ||
                    rule.category.contains(searchQuery, ignoreCase = true)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Rules Reference", 
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search keywords, combat, magic...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                if (displayRules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = if (searchQuery.isEmpty()) "No rules available." else "No matching rules found."
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayRules) { rule ->
                            RuleItem(rule, searchQuery)
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
    }
}

@Composable
fun RuleItem(rule: RuleSection, searchQuery: String) {
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-expand if search query matches content or title to help user find the term
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty() && (
            rule.title.contains(searchQuery, ignoreCase = true) ||
            rule.content.contains(searchQuery, ignoreCase = true)
        )) {
            isExpanded = true
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = highlightText(rule.title, searchQuery),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = parseRuleContent(rule.content, searchQuery, accentColor = MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    inlineContent = getMoonstoneInlineContent()
                )
            }
        }
    }
}

@Composable
fun highlightText(text: String, searchQuery: String): AnnotatedString = buildAnnotatedString {
    appendWithHighlight(text, searchQuery)
}

@Composable
fun parseRuleContent(content: String, searchQuery: String, accentColor: Color) = buildAnnotatedString {
    val lines = content.split("\n")
    lines.forEachIndexed { index, line ->
        var currentLine = line
        
        // Handle Bullets
        if (currentLine.trim().startsWith("•")) {
            append("  ")
        }

        // Handle Title separator " – "
        val dashIndex = currentLine.indexOf(" – ")
        if (dashIndex != -1) {
            val title = currentLine.substring(0, dashIndex)
            val rest = currentLine.substring(dashIndex) // includes " – "
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = accentColor)) {
                appendFormattedPart(title, searchQuery)
            }
            appendFormattedPart(rest, searchQuery)
        } else {
            appendFormattedPart(currentLine, searchQuery)
        }

        if (index < lines.size - 1) append("\n")
    }
}

private fun AnnotatedString.Builder.appendFormattedPart(text: String, searchQuery: String) {
    val regex = """(\*\*(.*?)\*\*)|(\{[Nn]ull\})""".toRegex()
    var lastIndex = 0
    
    regex.findAll(text).forEach { match ->
        appendWithHighlight(text.substring(lastIndex, match.range.first), searchQuery)
        
        when {
            match.groupValues[1].isNotEmpty() -> { // **bold**
                val boldText = match.groupValues[2]
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendWithHighlight(boldText, searchQuery)
                }
            }
            match.groupValues[3].isNotEmpty() -> { // {Null} or {null}
                appendInlineContent("nullSymbol", "{Null}")
            }
        }
        lastIndex = match.range.last + 1
    }
    appendWithHighlight(text.substring(lastIndex), searchQuery)
}

private fun AnnotatedString.Builder.appendWithHighlight(text: String, searchQuery: String) {
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
