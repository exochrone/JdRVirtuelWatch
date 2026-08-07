package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.ParseResult

interface TopicParser {
    fun parse(html: String): ParseResult
}
