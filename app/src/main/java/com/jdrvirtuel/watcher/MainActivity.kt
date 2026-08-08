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
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var topicRepository: TopicRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    private val browserLauncher by lazy {
        BrowserLauncher(this, appPreferences, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)

        setContent {
            JdrVirtuelWatcherTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
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
                // Find topic in either forum to get its URL
                val topic = topicRepository.getTopics(15).find { it.id == topicId }
                    ?: topicRepository.getTopics(16).find { it.id == topicId }
                
                if (topic != null) {
                    topicRepository.setRead(topicId, true)
                    browserLauncher.openUrl(topic.url)
                }
            }
        }
    }
}
