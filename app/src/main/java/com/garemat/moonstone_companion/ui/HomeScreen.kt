package com.garemat.moonstone_companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToCharacters: () -> Unit,
    onNavigateToTroupes: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToGameSetup: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Moonstone Companion",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        HomeButton(
            text = "Characters",
            icon = Icons.Default.Person,
            onClick = onNavigateToCharacters
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            text = "My Troupes",
            icon = Icons.Default.Group,
            onClick = onNavigateToTroupes
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            text = "Rules Reference",
            icon = Icons.Default.MenuBook,
            onClick = onNavigateToRules
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeButton(
            text = "Profile",
            icon = Icons.Default.Settings,
            onClick = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToGameSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "START GAME", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun HomeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
