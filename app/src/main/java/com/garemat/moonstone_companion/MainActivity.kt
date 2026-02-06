package com.garemat.moonstone_companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.garemat.moonstone_companion.ui.*
import com.garemat.moonstone_companion.ui.theme.MoonstonecompanionTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            
            MoonstonecompanionTheme(appTheme = state.theme) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                                onNavigateBack = { navController.safePopBackStack() }
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
                                onEditTroupe = { navController.navigate(Screen.AddEditTroupe.route) }
                            )
                        }
                        composable(Screen.AddEditTroupe.route) {
                            AddEditTroupeScreen(
                                viewModel = viewModel,
                                state = state,
                                onNavigateBack = { navController.safePopBackStack() }
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
                                }
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
                    }
                }
            }
        }
    }
}

/**
 * Extension function to prevent popping the root of the navigation stack,
 * which results in a blank screen.
 */
fun NavController.safePopBackStack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}
