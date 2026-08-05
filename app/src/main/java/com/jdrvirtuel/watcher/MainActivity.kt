package com.jdrvirtuel.watcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.jdrvirtuel.watcher.core.ui.theme.JdrVirtuelWatcherTheme
import com.jdrvirtuel.watcher.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JdrVirtuelWatcherTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
