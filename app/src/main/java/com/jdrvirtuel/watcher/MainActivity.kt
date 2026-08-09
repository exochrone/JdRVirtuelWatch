package com.jdrvirtuel.watcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.jdrvirtuel.watcher.core.ui.theme.JdrVirtuelWatcherTheme
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import com.jdrvirtuel.watcher.core.util.SystemSettingsChecker
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.navigation.AppNavHost
import com.jdrvirtuel.watcher.navigation.DiagnosticRoute
import com.jdrvirtuel.watcher.navigation.HomeRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var topicRepository: TopicRepository

    @Inject
    lateinit var appPreferences: AppPreferences
    
    @Inject
    lateinit var systemSettingsChecker: SystemSettingsChecker

    private val browserLauncher by lazy {
        BrowserLauncher(this, appPreferences, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            val shouldShowDiagnostic = systemSettingsChecker.shouldShowDiagnostic()
            val startDestination = if (shouldShowDiagnostic) DiagnosticRoute else HomeRoute
            
            enableEdgeToEdge()
            handleIntent(intent)

            setContent {
                JdrVirtuelWatcherTheme {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "jdrvirtuel" && data.host == "topic") {
            val topicId = data.lastPathSegment?.toIntOrNull() ?: return
            lifecycleScope.launch {
                val topic = topicRepository.getTopicById(topicId)
                
                if (topic != null) {
                    topicRepository.setRead(topicId, true)
                    browserLauncher.openUrl(topic.url)
                }
            }
        }
    }
}
