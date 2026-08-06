package com.jdrvirtuel.watcher.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FrenchDateParserTest {

    private val parser = FrenchDateParser()
    private val zoneId = ZoneId.of("Europe/Paris")

    @Test
    fun parse_dateStandard() {
        val dateStr = "ven. 24 juil. 2026 16:26"
        val result = parser.parse(dateStr)
        
        val expected = LocalDateTime.of(2026, 7, 24, 16, 26)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
            
        assertEquals(expected, result)
    }

    @Test
    fun parse_jourSansZero() {
        val dateStr = "sam. 1 août 2026 02:12"
        val result = parser.parse(dateStr)
        
        val expected = LocalDateTime.of(2026, 8, 1, 2, 12)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
            
        assertEquals(expected, result)
    }

    @Test
    fun parse_moisAvecAccent() {
        val dateStr = "mer. 5 août 2026 14:52"
        val result = parser.parse(dateStr)
        
        val expected = LocalDateTime.of(2026, 8, 5, 14, 52)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
            
        assertEquals(expected, result)
        
        val dateStr2 = "jeu. 10 déc. 2026 10:00"
        val result2 = parser.parse(dateStr2)
        val expected2 = LocalDateTime.of(2026, 12, 10, 10, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected2, result2)
    }

    @Test
    fun parse_espaceInsecable() {
        // \u00A0 est un espace insécable
        val dateStr = "ven. 24\u00A0juil.\u00A02026 16:26"
        val result = parser.parse(dateStr)
        
        val expected = LocalDateTime.of(2026, 7, 24, 16, 26)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
            
        assertEquals(expected, result)
    }

    @Test
    fun parse_dateInvalide() {
        assertEquals(0L, parser.parse("date invalide"))
        assertEquals(0L, parser.parse(null))
        assertEquals(0L, parser.parse(""))
    }
}
