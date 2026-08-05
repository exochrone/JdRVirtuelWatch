package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.remote.WebViewForumPageSource
import com.jdrvirtuel.watcher.domain.repository.ForumPageSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    @Binds
    @Singleton
    abstract fun bindForumPageSource(
        webViewForumPageSource: WebViewForumPageSource
    ): ForumPageSource
}
