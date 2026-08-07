package com.jdrvirtuel.watcher.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {
    private val absoluteFormatter = DateTimeFormatter.ofPattern("'le' d MMMM 'à' HH:mm", Locale.FRENCH)

    fun formatRelative(timestamp: Long?): String {
        if (timestamp == null) return "Jamais synchronisé"

        val now = Instant.now()
        val instant = Instant.ofEpochMilli(timestamp)
        val diffSeconds = ChronoUnit.SECONDS.between(instant, now)

        return when {
            diffSeconds < 60 -> "il y a quelques secondes"
            diffSeconds < 3600 -> {
                val minutes = diffSeconds / 60
                "il y a $minutes minute${if (minutes > 1) "s" else ""}"
            }
            diffSeconds < 86400 -> {
                val hours = diffSeconds / 3600
                "il y a $hours heure${if (hours > 1) "s" else ""}"
            }
            else -> {
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                absoluteFormatter.format(dateTime)
            }
        }
    }
}
