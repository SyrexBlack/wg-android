package com.wgandroid.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wgandroid.client.ui.screen.ClientsScreen
import com.wgandroid.client.ui.screen.SettingsScreen
import com.wgandroid.client.ui.theme.WgAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WgAndroidTheme {
                WgAndroidApp()
            }
        }
    }
}

@Composable
fun WgAndroidApp() {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = "clients"
        ) {
            composable("clients") {
                ClientsScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable("settings") {
                val settingsViewModel: com.wgandroid.client.ui.viewmodel.SettingsViewModel = viewModel()
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    settingsViewModel.initializeWithContext(context)
                }
                
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    viewModel = settingsViewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WgAndroidAppPreview() {
    WgAndroidTheme {
        WgAndroidApp()
    }
} 