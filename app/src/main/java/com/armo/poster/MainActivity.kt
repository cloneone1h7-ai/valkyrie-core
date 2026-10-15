package com.armo.poster

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armo.poster.data.MatchStore
import com.armo.poster.data.TeamRegistry
import com.armo.poster.renderer.PosterRenderer
import com.armo.poster.ui.editor.EditorScreen
import com.armo.poster.ui.editor.EditorViewModel
import com.armo.poster.ui.export.ExportScreen
import com.armo.poster.ui.export.ExportViewModel
import com.armo.poster.ui.history.HistoryScreen
import com.armo.poster.ui.history.HistoryViewModel
import com.armo.poster.ui.logs.LogsScreen
import com.armo.poster.ui.navigation.NavRoutes
import com.armo.poster.ui.theme.ARMOPosterTheme
import com.armo.poster.util.AppLog
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var offscreenHost: FrameLayout
    private lateinit var posterRenderer: PosterRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize logging
        AppLog.init(this)

        // Offscreen WebView host attached to Activity decorView (sized for up to 2400x2400 high-res)
        offscreenHost = FrameLayout(this)
        addContentView(
            offscreenHost,
            FrameLayout.LayoutParams(2400, 2400)
        )

        // Initialize Data & Engine
        val teamRegistry = TeamRegistry().apply { loadFromAssets(applicationContext) }
        val matchStore = MatchStore(File(filesDir, "matches"))
        posterRenderer = PosterRenderer(this, offscreenHost)

        val editorViewModel = EditorViewModel(teamRegistry, matchStore)
        val exportViewModel = ExportViewModel(posterRenderer)
        val historyViewModel = HistoryViewModel(matchStore)

        setContent {
            ARMOPosterTheme {
                val navController = rememberNavController()
                val currentMatch by editorViewModel.matchData.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Editor.route
                ) {
                    composable(NavRoutes.Editor.route) {
                        EditorScreen(
                            viewModel = editorViewModel,
                            onNavigateToExport = {
                                navController.navigate(NavRoutes.Export.route)
                            },
                            onNavigateToHistory = {
                                navController.navigate(NavRoutes.History.route)
                            },
                            onNavigateToLogs = {
                                navController.navigate(NavRoutes.Logs.route)
                            }
                        )
                    }

                    composable(NavRoutes.Export.route) {
                        ExportScreen(
                            viewModel = exportViewModel,
                            matchData = currentMatch,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToLogs = {
                                navController.navigate(NavRoutes.Logs.route)
                            }
                        )
                    }

                    composable(NavRoutes.History.route) {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onOpenMatch = { match ->
                                editorViewModel.loadMatch(match)
                                navController.popBackStack(NavRoutes.Editor.route, inclusive = false)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(NavRoutes.Logs.route) {
                        LogsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
