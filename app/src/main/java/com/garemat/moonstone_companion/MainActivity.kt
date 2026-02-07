package com.garemat.moonstone_companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.garemat.moonstone_companion.ui.*
import com.garemat.moonstone_companion.ui.theme.MoonstonecompanionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val db by lazy {
        CharacterDatabase.getDatabase(applicationContext)
    }

    private val viewModel by viewModels<CharacterViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CharacterViewModel(application, db.dao) as T
                }
            }
        }
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            var triggerTutorial by remember { mutableStateOf(0) }
            val isMoonstone = state.theme == AppTheme.MOONSTONE
            
            MoonstonecompanionTheme(appTheme = state.theme) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Screens where we show the navigation bars
                val showNavBars = remember(currentDestination) {
                    currentDestination?.route in listOf(
                        Screen.Home.route,
                        Screen.Characters.route,
                        Screen.Troupes.route,
                        Screen.AddEditTroupe.route,
                        Screen.GameSetup.route,
                        Screen.Stats.route
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = showNavBars,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerShape = if (isMoonstone) RoundedCornerShape(0.dp) else DrawerDefaults.shape
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Moonstone Companion",
                                modifier = Modifier.padding(16.dp),
                                style = if (isMoonstone) MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp) else MaterialTheme.typography.titleLarge,
                                color = if (isMoonstone) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                            HorizontalDivider()
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                label = { Text("Rules") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Rules.route)
                                },
                                shape = if (isMoonstone) RoundedCornerShape(0.dp) else CircleShape
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Settings.route)
                                },
                                shape = if (isMoonstone) RoundedCornerShape(0.dp) else CircleShape
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (showNavBars) {
                                CenterAlignedTopAppBar(
                                    title = { Text(
                                        text = when(currentDestination?.route) {
                                            Screen.Characters.route -> "Character Compendium"
                                            Screen.Troupes.route -> "My Troupes"
                                            Screen.AddEditTroupe.route -> if (viewModel.editingTroupeId == null) "Build Troupe" else "Edit Troupe"
                                            Screen.GameSetup.route -> "Game Setup"
                                            Screen.Stats.route -> "Statistics"
                                            else -> "Moonstone Companion"
                                        },
                                        style = if (isMoonstone) MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp) else MaterialTheme.typography.titleLarge
                                    ) },
                                    navigationIcon = {
                                        IconButton(onClick = { triggerTutorial++ }) {
                                            Icon(Icons.Default.Help, contentDescription = "Tutorial")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        titleContentColor = MaterialTheme.colorScheme.primary,
                                        actionIconContentColor = MaterialTheme.colorScheme.primary,
                                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (showNavBars) {
                                NavigationBar(
                                    containerColor = if (isMoonstone) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = if (isMoonstone) 0.dp else NavigationBarDefaults.Elevation
                                ) {
                                    val items = listOf(
                                        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
                                        BottomNavItem("Characters", Screen.Characters.route, Icons.Default.PersonSearch),
                                        BottomNavItem("Play", Screen.GameSetup.route, Icons.Default.PlayArrow),
                                        BottomNavItem("Troupes", Screen.Troupes.route, Icons.Default.Groups),
                                        BottomNavItem("Stats", Screen.Stats.route, Icons.Default.BarChart)
                                    )
                                    items.forEach { item ->
                                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                        val isPlayButton = item.label == "Play"
                                        
                                        NavigationBarItem(
                                            icon = { 
                                                if (isPlayButton) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else if (isMoonstone) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                item.icon, 
                                                                contentDescription = item.label,
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isMoonstone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Icon(item.icon, contentDescription = item.label)
                                                }
                                            },
                                            label = { 
                                                Text(
                                                    text = item.label,
                                                    style = if (isMoonstone) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold) else LocalTextStyle.current
                                                ) 
                                            },
                                            selected = isSelected,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = if (isPlayButton) Color.Transparent else if (isMoonstone) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    state = state,
                                    onEvent = viewModel::onEvent,
                                    onNavigateToCharacters = { navController.navigate(Screen.Characters.route) },
                                    onNavigateToTroupes = { navController.navigate(Screen.Troupes.route) },
                                    onNavigateToRules = { navController.navigate(Screen.Rules.route) },
                                    onNavigateToGameSetup = { navController.navigate(Screen.GameSetup.route) },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    triggerTutorial = triggerTutorial
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    state = state,
                                    onEvent = viewModel::onEvent,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable(Screen.Rules.route) {
                                RulesScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable(Screen.Characters.route) {
                                CharacterListScreen(
                                    state = state,
                                    onEvent = viewModel::onEvent,
                                    onNavigateBack = { navController.safePopBackStack() },
                                    triggerTutorial = triggerTutorial
                                )
                            }
                            composable(Screen.Troupes.route) {
                                TroupeListScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() },
                                    onAddTroupe = { 
                                        viewModel.editingTroupeId = null 
                                        navController.navigate(Screen.AddEditTroupe.route) 
                                    },
                                    onEditTroupe = { navController.navigate(Screen.AddEditTroupe.route) },
                                    triggerTutorial = triggerTutorial
                                )
                            }
                            composable(Screen.AddEditTroupe.route) {
                                AddEditTroupeScreen(
                                    viewModel = viewModel,
                                    state = state,
                                    onNavigateBack = { navController.safePopBackStack() },
                                    triggerTutorial = triggerTutorial
                                )
                            }
                            composable(Screen.GameSetup.route) {
                                GameSetupScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() },
                                    onStartGame = { 
                                        navController.navigate(Screen.ActiveGame.route)
                                    },
                                    onNavigateToAddEditTroupe = {
                                        navController.navigate(Screen.AddEditTroupe.route)
                                    },
                                    triggerTutorial = triggerTutorial
                                )
                            }
                            composable(Screen.ActiveGame.route) {
                                val playersWithCharacters by viewModel.playersWithCharacters.collectAsState()
                                ActiveGameScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    players = playersWithCharacters,
                                    onQuitGame = { 
                                        navController.popBackStack(Screen.Home.route, inclusive = false)
                                    }
                                )
                            }
                            composable(Screen.Stats.route) {
                                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text("Statistics coming soon!", style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)

fun NavController.safePopBackStack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}
