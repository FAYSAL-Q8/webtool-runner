/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.editor.ToolEditorScreen
import com.example.ui.screens.editor.ToolEditorViewModel
import com.example.ui.screens.runner.WebToolRunnerScreen
import com.example.ui.screens.runner.WebToolRunnerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ui.theme.AppThemeManager.init(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val RUNNER = "runner/{toolId}"
    const val EDITOR = "editor/{toolId}"

    fun runner(toolId: Long) = "runner/$toolId"
    fun editor(toolId: Long) = "editor/$toolId"
    fun create() = "editor/-1"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD
    ) {
        composable(NavRoutes.DASHBOARD) {
            val dashboardViewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onOpenTool = { toolId ->
                    navController.navigate(NavRoutes.runner(toolId))
                },
                onCreateTool = {
                    navController.navigate(NavRoutes.create())
                },
                onEditTool = { toolId ->
                    navController.navigate(NavRoutes.editor(toolId))
                }
            )
        }

        composable(
            route = NavRoutes.RUNNER,
            arguments = listOf(
                navArgument("toolId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getLong("toolId") ?: 1L
            val runnerViewModel: WebToolRunnerViewModel = viewModel()
            WebToolRunnerScreen(
                toolId = toolId,
                viewModel = runnerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoutes.EDITOR,
            arguments = listOf(
                navArgument("toolId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getLong("toolId") ?: -1L
            val editorViewModel: ToolEditorViewModel = viewModel()
            ToolEditorScreen(
                toolId = if (toolId <= 0) null else toolId,
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSavedSuccessfully = { savedId ->
                    navController.popBackStack()
                }
            )
        }
    }
}
