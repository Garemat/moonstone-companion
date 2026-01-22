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
            MoonstonecompanionTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigateToCharacters = { navController.navigate(Screen.Characters.route) },
                                onNavigateToTroupes = { navController.navigate(Screen.Troupes.route) },
                                onNavigateToRules = { navController.navigate(Screen.Rules.route) },
                                onNavigateToGameSetup = { navController.navigate(Screen.GameSetup.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                            )
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                state = state,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Rules.route) {
                            RulesScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Characters.route) {
                            CharacterListScreen(
                                state = state,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Troupes.route) {
                            TroupeListScreen(
                                state = state,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
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
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.GameSetup.route) {
                            GameSetupScreen(
                                state = state,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onStartGame = { 
                                    navController.navigate(Screen.ActiveGame.route)
                                }
                            )
                        }
                        composable(Screen.ActiveGame.route) {
                            val playersWithCharacters by viewModel.playersWithCharacters.collectAsState()
                            
                            ActiveGameScreen(
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
