package com.garemat.moonstone_companion.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.Character
import com.garemat.moonstone_companion.CharacterEvent
import com.garemat.moonstone_companion.CharacterState
import com.garemat.moonstone_companion.CharacterViewModel
import com.garemat.moonstone_companion.Troupe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroupeListScreen(
    state: CharacterState,
    viewModel: CharacterViewModel,
    onNavigateBack: () -> Unit,
    onAddTroupe: () -> Unit,
    onEditTroupe: () -> Unit
) {
    var troupeToDelete by remember { mutableStateOf<Troupe?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Troupes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Input, contentDescription = "Import Troupe")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTroupe) {
                Icon(Icons.Default.Add, contentDescription = "Create Troupe")
            }
        }
    ) { padding ->
        if (state.troupes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No troupes yet. Create or import one!")
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.troupes) { troupe ->
                    TroupeListItem(
                        troupe = troupe,
                        onClick = { 
                            viewModel.onEvent(CharacterEvent.EditTroupe(troupe))
                            onEditTroupe()
                        },
                        onDelete = { troupeToDelete = troupe },
                        onShare = { 
                            val fullCode = viewModel.generateFullShareCode(troupe, state.characters)
                            shareTroupe(context, troupe.troupeName, fullCode)
                        }
                    )
                }
            }
        }

        if (troupeToDelete != null) {
            AlertDialog(
                onDismissRequest = { troupeToDelete = null },
                title = { Text("Delete Troupe") },
                text = { Text("Are you sure you want to delete '${troupeToDelete?.troupeName}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            troupeToDelete?.let { viewModel.onEvent(CharacterEvent.DeleteTroupe(it)) }
                            troupeToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { troupeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Troupe") },
                text = {
                    Column {
                        Text("Paste the shared troupe code below:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importCode,
                            onValueChange = { importCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste code here") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.importTroupe(importCode, state.characters)
                            if (viewModel.state.value.errorMessage == null) {
                                showImportDialog = false
                                importCode = ""
                                onAddTroupe()
                            }
                        },
                        enabled = importCode.isNotBlank()
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (state.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(CharacterEvent.DismissError) },
                title = { Text("Import Failed") },
                text = { Text(state.errorMessage) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(CharacterEvent.DismissError) }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun TroupeListItem(
    troupe: Troupe,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getFactionColor(troupe.faction))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFactionIcon(troupe.faction),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = troupe.troupeName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${troupe.characterIds.size} Characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share Code")
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun shareTroupe(context: Context, name: String, code: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "$code")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
