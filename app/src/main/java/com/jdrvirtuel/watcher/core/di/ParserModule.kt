package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.parser.FrenchDateParser
import com.jdrvirtuel.watcher.data.parser.TopicListParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideFrenchDateParser(): FrenchDateParser {
        return FrenchDateParser()
    }

    @Provides
    @Singleton
    fun provideTopicListParser(dateParser: FrenchDateParser): TopicListParser {
        return TopicListParser(dateParser)
    }
}
