package com.jdrvirtuel.watcher.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {
    private val absoluteFormatter = DateTimeFormatter.ofPattern("'le' d MMMM 'à' HH:mm", Locale.FRENCH)
    private val absoluteWithYearFormatter = DateTimeFormatter.ofPattern("'le' d MMMM yyyy 'à' HH:mm", Locale.FRENCH)

    fun formatTopicDate(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())
        
        val formatter = if (dateTime.year == now.year) {
            absoluteFormatter
        } else {
            absoluteWithYearFormatter
        }
        
        return formatter.format(dateTime).lowercase(Locale.FRENCH)
    }

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

    private val logFormatter = DateTimeFormatter.ofPattern("dd/MM/yy - HH:mm:ss", Locale.getDefault())

    fun formatLogDate(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return logFormatter.format(dateTime)
    }
}
