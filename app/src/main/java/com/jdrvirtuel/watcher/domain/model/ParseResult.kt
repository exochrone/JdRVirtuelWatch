package com.jdrvirtuel.watcher.domain.model

data class ParseResult(
    val topics: List<ParsedTopic>,
    val skippedSticky: Int,
    val skippedInvalid: Int
)
