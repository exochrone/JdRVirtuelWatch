package com.jdrvirtuel.watcher.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class BrowserLauncher(private val context: Context) {
    fun openUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                true
            } catch (e2: ActivityNotFoundException) {
                false
            }
        }
    }
}
