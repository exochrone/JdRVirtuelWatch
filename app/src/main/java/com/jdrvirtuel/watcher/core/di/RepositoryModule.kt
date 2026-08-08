package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.repository.ChallengeStateRepositoryImpl
import com.jdrvirtuel.watcher.data.repository.ForumRepositoryImpl
import com.jdrvirtuel.watcher.data.repository.TopicRepositoryImpl
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindForumRepository(impl: ForumRepositoryImpl): ForumRepository

    @Binds
    @Singleton
    abstract fun bindTopicRepository(impl: TopicRepositoryImpl): TopicRepository

    @Binds
    @Singleton
    abstract fun bindChallengeStateRepository(impl: ChallengeStateRepositoryImpl): ChallengeStateRepository
}
