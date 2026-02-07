package com.garemat.moonstone_companion.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Characters : Screen("characters")
    data object Troupes : Screen("troupes")
    data object AddEditTroupe : Screen("add_edit_troupe")
    data object GameSetup : Screen("game_setup")
    data object ActiveGame : Screen("active_game")
    data object Settings : Screen("settings")
    data object Rules : Screen("rules")
    data object Stats : Screen("stats")
}
