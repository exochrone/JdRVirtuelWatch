package com.jdrvirtuel.watcher.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BrowserInfo(
    val packageName: String,
    val label: String
)

class BrowserLauncher(
    private val context: Context,
    private val appPreferences: AppPreferences,
    private val scope: CoroutineScope
) {
    fun openUrl(url: String, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            val preferredPackage = appPreferences.preferredBrowserPackage.first()
            val uri = Uri.parse(url)
            
            val success = if (preferredPackage != null && isPackageInstalled(preferredPackage)) {
                launchCustomTab(uri, preferredPackage)
            } else {
                launchCustomTab(uri, null)
            }
            
            onResult(success)
        }
    }

    fun listCustomTabsBrowsers(): List<BrowserInfo> {
        val pm = context.packageManager
        val ctIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        val ctServices = pm.queryIntentServices(ctIntent, PackageManager.GET_RESOLVED_FILTER)
        
        return ctServices.mapNotNull { serviceInfo ->
            val packageName = serviceInfo.serviceInfo.packageName
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                BrowserInfo(packageName, label)
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun launchCustomTab(uri: Uri, packageName: String?): Boolean {
        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName)
            }
            
            if (context !is android.app.Activity) {
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: Exception) {
            launchStandardBrowser(uri)
        }
    }

    private fun launchStandardBrowser(uri: Uri): Boolean {
        return try {
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
