package com.jdrvirtuel.watcher.data.parser

import com.jdrvirtuel.watcher.domain.model.ParseResult
import com.jdrvirtuel.watcher.domain.model.ParsedTopic
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicListParser @Inject constructor(
    private val dateParser: FrenchDateParser
) {
    private val topicIdRegex = Regex("[?&]t=(\\d+)")
    private val replyCountRegex = Regex("(\\d+)")
    private val baseUrl = "https://www.jdrvirtuel.com/"

    private val datePatternRegex = Regex("\\p{L}{3}\\.\\s+\\d{1,2}\\s+\\p{L}+\\.?\\s+\\d{4}\\s+\\d{2}:\\d{2}")

    fun parse(html: String): ParseResult {
        if (html.isBlank()) {
            return ParseResult(emptyList(), 0, 0)
        }

        val doc = Jsoup.parse(html)
        var topicRows = doc.select("ul.topiclist.topics li.row")
        if (topicRows.isEmpty()) {
            topicRows = doc.select("li.row")
        }

        val topics = mutableListOf<ParsedTopic>()
        var skippedSticky = 0
        var skippedInvalid = 0

        for (row in topicRows) {
            // Sujet épinglé ?
            if (row.hasClass("sticky") || row.hasClass("announce") || row.hasClass("global")) {
                skippedSticky++
                continue
            }

            val topicTitleLink = row.selectFirst("a.topictitle")
            if (topicTitleLink == null) {
                skippedInvalid++
                continue
            }

            val title = topicTitleLink.text()
            val relativeUrl = topicTitleLink.attr("href")
            val url = if (relativeUrl.startsWith("http")) relativeUrl else baseUrl + relativeUrl.removePrefix("./")

            val idMatch = topicIdRegex.find(url)
            if (idMatch == null) {
                skippedInvalid++
                continue
            }
            val id = idMatch.groupValues[1].toInt()

            val dt = row.selectFirst("dt")
            val styleAttr = dt?.attr("style") ?: ""
            val isFull = styleAttr.contains("complet", ignoreCase = true)

            val topicPoster = row.selectFirst("div.topic-poster")
            val authorLink = topicPoster?.selectFirst("a.username, a.username-coloured")
            val author = authorLink?.text() ?: ""

            val creationDateText = topicPoster?.text()?.substringAfter("»")?.trim()
            val createdAt = dateParser.parse(creationDateText)

            val postsDd = row.selectFirst("dd.posts")
            val replyCountMatch = replyCountRegex.find(postsDd?.text() ?: "")
            val replyCount = replyCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val lastPostDd = row.selectFirst("dd.lastpost")
            val lastPostAuthorLink = lastPostDd?.selectFirst("a.username, a.username-coloured")
            val lastPostAuthor = lastPostAuthorLink?.text() ?: ""

            val lastPostText = lastPostDd?.text() ?: ""
            val lastPostDateMatch = datePatternRegex.findAll(lastPostText).lastOrNull()
            val lastPostDate = dateParser.parse(lastPostDateMatch?.value)

            topics.add(
                ParsedTopic(
                    id = id,
                    title = title,
                    url = url,
                    author = author,
                    createdAt = createdAt,
                    replyCount = replyCount,
                    lastPostAuthor = lastPostAuthor,
                    lastPostAt = lastPostDate,
                    isFull = isFull
                )
            )
        }

        return ParseResult(topics, skippedSticky, skippedInvalid)
    }
}
