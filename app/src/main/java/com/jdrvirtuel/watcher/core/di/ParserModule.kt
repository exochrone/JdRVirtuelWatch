package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.parser.TopicListParser
import com.jdrvirtuel.watcher.domain.repository.TopicParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @Singleton
    abstract fun bindTopicParser(
        topicListParser: TopicListParser
    ): TopicParser
}
