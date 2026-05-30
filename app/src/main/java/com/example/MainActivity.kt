package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.feature.common.FileForgeViewModel
import com.example.feature.common.FileForgeViewModelFactory
import com.example.feature.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: FileForgeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FileForgeApplication
        val factory = FileForgeViewModelFactory(app.container)
        viewModel = ViewModelProvider(this, factory)[FileForgeViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationGraph(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationGraph(viewModel: FileForgeViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = "folderPicker"
    ) {
        composable("folderPicker") {
            FolderPickerScreen(
                viewModel = viewModel,
                onNavigateToFileList = { navController.navigate("fileList") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("fileList") {
            FileListScreen(
                viewModel = viewModel,
                onNavigateToRuleBuilder = { navController.navigate("ruleBuilder") },
                onNavigateToOrganizer = { navController.navigate("organizer") },
                onBack = { navController.navigateUp() }
            )
        }
        composable("ruleBuilder") {
            RuleBuilderScreen(
                viewModel = viewModel,
                onNavigateToPreview = { navController.navigate("preview") },
                onBack = { navController.navigateUp() }
            )
        }
        composable("preview") {
            PreviewScreen(
                viewModel = viewModel,
                onRenameExecuted = { success, failed ->
                    navController.navigate("fileList") {
                        popUpTo("fileList") { inclusive = false }
                    }
                },
                onBack = { navController.navigateUp() }
            )
        }
        composable("organizer") {
            OrganizerScreen(
                viewModel = viewModel,
                onOrganizerExecuted = { count ->
                    navController.navigate("fileList") {
                        popUpTo("fileList") { inclusive = false }
                    }
                },
                onBack = { navController.navigateUp() }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() }
            )
        }
    }
}
