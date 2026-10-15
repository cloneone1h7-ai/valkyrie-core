package com.armo.poster.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Editor : NavRoutes("editor")
    data object Export : NavRoutes("export")
    data object History : NavRoutes("history")
    data object Logs : NavRoutes("logs")
}
