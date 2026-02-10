package com.garemat.moonstone_companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garemat.moonstone_companion.AppTheme
import com.garemat.moonstone_companion.CharacterEvent
import com.garemat.moonstone_companion.CharacterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: CharacterState,
    onEvent: (CharacterEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(state.name) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("User Profile", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Player Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("App Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column {
                ThemeOption(
                    title = "Default",
                    selected = state.theme == AppTheme.DEFAULT,
                    onSelect = { onEvent(CharacterEvent.ChangeTheme(AppTheme.DEFAULT)) }
                )
                ThemeOption(
                    title = "Moonstone",
                    selected = state.theme == AppTheme.MOONSTONE,
                    onSelect = { onEvent(CharacterEvent.ChangeTheme(AppTheme.MOONSTONE)) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    onEvent(CharacterEvent.UpdateUserName(name))
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
