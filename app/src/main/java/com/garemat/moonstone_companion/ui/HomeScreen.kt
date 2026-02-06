package com.garemat.moonstone_companion.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.CharacterEvent
import com.garemat.moonstone_companion.CharacterState

@Composable
fun HomeScreen(
    state: CharacterState,
    onEvent: (CharacterEvent) -> Unit,
    onNavigateToCharacters: () -> Unit,
    onNavigateToTroupes: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToGameSetup: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }
    
    // Tutorial coordinates tracking
    val coordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    var showTutorialForcefully by remember { mutableStateOf(false) }

    val shouldShowTutorial = (!state.hasSeenHomeTutorial || showTutorialForcefully)

    // Intercept back button to prevent accidental app exit from Home
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (backPressedTime + 2000 > currentTime) {
            (context as? Activity)?.finish()
        } else {
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                onClick = onNavigateToCharacters,
                modifier = Modifier.onGloballyPositioned { coordsMap["Characters"] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                text = "My Troupes",
                icon = Icons.Default.Group,
                onClick = onNavigateToTroupes,
                modifier = Modifier.onGloballyPositioned { coordsMap["My Troupes"] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                text = "Rules Reference",
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToRules,
                modifier = Modifier.onGloballyPositioned { coordsMap["Rules Reference"] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                text = "Settings",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                modifier = Modifier.onGloballyPositioned { coordsMap["Settings"] = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToGameSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .onGloballyPositioned { coordsMap["START GAME"] = it },
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

        IconButton(
            onClick = { showTutorialForcefully = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Help, contentDescription = "Tutorial")
        }

        if (shouldShowTutorial) {
            TutorialOverlay(
                steps = homeScreenTutorialSteps,
                targetCoordinates = coordsMap,
                onComplete = {
                    onEvent(CharacterEvent.SetHasSeenTutorial("home", true))
                    showTutorialForcefully = false
                },
                onSkip = {
                    onEvent(CharacterEvent.SetHasSeenTutorial("home", true))
                    showTutorialForcefully = false
                }
            )
        }
    }
}

@Composable
fun HomeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
