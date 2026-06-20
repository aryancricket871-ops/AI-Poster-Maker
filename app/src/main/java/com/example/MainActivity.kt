package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ads.UnityAdsHelper
import com.example.ui.Editor
import com.example.ui.Home
import com.example.ui.Admin
import com.example.ui.screens.CanvasEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UnityAdsHelper.initialize(this)
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:321abc456def")
                    .setApiKey("dummy_api_key")
                    .setProjectId("dummy-project")
                    .build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { template ->
                    // Navigation with safe-encoded URL mapping
                    navController.navigate(Editor(templateId = template.id, imageUrl = java.net.URLEncoder.encode(template.imageUrl, "UTF-8")))
                },
                onNavigateToAdmin = {
                    navController.navigate(Admin)
                }
            )
        }
        composable<Editor> { backStackEntry ->
            val editor: Editor = backStackEntry.toRoute()
            val decodedUrl = java.net.URLDecoder.decode(editor.imageUrl, "UTF-8")
            CanvasEditorScreen(
                templateImageUrl = decodedUrl,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Admin> {
            com.example.ui.screens.AdminScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
