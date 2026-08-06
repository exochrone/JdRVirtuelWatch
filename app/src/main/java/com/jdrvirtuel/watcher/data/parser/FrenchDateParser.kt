package com.jdrvirtuel.watcher.data.parser

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrenchDateParser @Inject constructor() {

    private val formatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy HH:mm")
        .toFormatter(Locale.FRENCH)

    private val dayPrefixRegex = Regex("^\\p{L}{3}\\.\\s*")

    fun parse(dateString: String?): Long {
        if (dateString == null) return 0L

        try {
            // 1. Retirer le préfixe de jour de semaine
            var cleaned = dateString.replace(dayPrefixRegex, "")

            // 2. Normaliser les espaces insécables et multiples
            cleaned = cleaned.replace('\u00A0', ' ')
            cleaned = cleaned.replace('\u202F', ' ')
            cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

            // 3. Analyser
            val localDateTime = LocalDateTime.parse(cleaned, formatter)

            // 4. Conversion en millisecondes epoch (Europe/Paris)
            return localDateTime.atZone(ZoneId.of("Europe/Paris"))
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            return 0L
        }
    }
}
