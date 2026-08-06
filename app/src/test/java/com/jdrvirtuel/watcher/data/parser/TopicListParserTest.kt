package com.jdrvirtuel.watcher.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class TopicListParserTest {

    private val dateParser = FrenchDateParser()
    private val parser = TopicListParser(dateParser)
    private val zoneId = ZoneId.of("Europe/Paris")

    private fun loadHtml(): String {
        // En JUnit, les ressources sont dans le classpath. 
        // Si lancé depuis Android Studio, il faut parfois ruser selon la config,
        // mais normalement ClassLoader.getResource suffit.
        val resource = javaClass.classLoader?.getResource("viewforum_f15.html")
        return resource?.readText() ?: File("src/test/resources/viewforum_f15.html").readText()
    }

    @Test
    fun parse_ignoreLesSujetsEpingles() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        assertTrue("Devrait avoir ignoré des épinglés", result.skippedSticky > 0)
        // Vérifier qu'aucun sujet retourné n'est un sticky connu (ex: 32440 dans la spec)
        assertFalse(result.topics.any { it.id == 32440 })
    }

    @Test
    fun parse_extraitLIdentifiantDepuisLUrl() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        val topic = result.topics.find { it.id == 41234 }
        assertNotNull("Le sujet 41234 devrait être présent", topic)
    }

    @Test
    fun parse_extraitLeTitre() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        val topic = result.topics.find { it.id == 41234 }
        assertEquals("[Friponnes RPG][Discord][12/08][1/3 places]", topic?.title)
    }

    @Test
    fun parse_extraitLAuteurEtLaDateDeCreation() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        val topic = result.topics.find { it.id == 41234 }
        assertEquals("Etienneb", topic?.author)
        
        val expectedDate = LocalDateTime.of(2026, 7, 24, 16, 26)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedDate, topic?.createdAt)
    }

    @Test
    fun parse_extraitLeNombreDeReponses() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        val topic = result.topics.find { it.id == 41234 }
        assertEquals(10, topic?.replyCount)
    }

    @Test
    fun parse_extraitLeDernierMessage() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        val topic = result.topics.find { it.id == 41234 }
        assertEquals("Weyland-Yutani Corp", topic?.lastPostAuthor)
        
        val expectedDate = LocalDateTime.of(2026, 8, 5, 14, 52)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedDate, topic?.lastPostAt)
    }

    @Test
    fun parse_detecteLIconeComplet() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        // Sujet 41248 est complet dans la spec
        val fullTopic = result.topics.find { it.id == 41248 }
        assertTrue("Le sujet 41248 devrait être complet", fullTopic?.isFull ?: false)
        
        val normalTopic = result.topics.find { it.id == 41234 }
        assertFalse("Le sujet 41234 ne devrait pas être complet", normalTopic?.isFull ?: true)
    }

    @Test
    fun parse_gereLesNomsColores() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        // Dans viewforum_f15.html, il y a sûrement des admins/modos avec username-coloured
        // On vérifie qu'aucun auteur n'est vide pour les sujets extraits
        result.topics.forEach { 
            assertFalse("L'auteur ne devrait pas être vide pour le sujet ${it.id}", it.author.isEmpty())
            assertFalse("Le dernier auteur ne devrait pas être vide pour le sujet ${it.id}", it.lastPostAuthor.isEmpty())
        }
    }

    @Test
    fun parse_construitDesUrlAbsolues() {
        val html = loadHtml()
        val result = parser.parse(html)
        
        result.topics.forEach { 
            assertTrue("L'URL devrait être absolue: ${it.url}", it.url.startsWith("https://"))
        }
    }

    @Test
    fun parse_htmlVide() {
        val result = parser.parse("")
        assertTrue(result.topics.isEmpty())
        assertEquals(0, result.skippedSticky)
        assertEquals(0, result.skippedInvalid)
    }
}
