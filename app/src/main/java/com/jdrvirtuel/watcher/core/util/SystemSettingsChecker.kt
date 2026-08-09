package com.jdrvirtuel.watcher.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    suspend fun isManufacturerSleepAcknowledged(): Boolean {
        return appPreferences.isManufacturerSleepAcknowledged.first()
    }

    suspend fun isDiagnosticDismissed(): Boolean {
        return appPreferences.isDiagnosticDismissed.first()
    }

    suspend fun shouldShowDiagnostic(): Boolean {
        if (isDiagnosticDismissed()) return false
        
        return !areNotificationsEnabled() || 
               !isIgnoringBatteryOptimizations() || 
               !isManufacturerSleepAcknowledged()
    }
}
