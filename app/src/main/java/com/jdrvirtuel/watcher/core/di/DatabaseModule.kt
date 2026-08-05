package com.jdrvirtuel.watcher.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.jdrvirtuel.watcher.data.local.dao.ForumDao
import com.jdrvirtuel.watcher.data.local.dao.TopicDao
import com.jdrvirtuel.watcher.data.local.db.AppDatabase
import com.jdrvirtuel.watcher.data.local.db.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        seederProvider: javax.inject.Provider<DatabaseSeeder>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jdrvirtuel_watcher.db"
        )
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                seederProvider.get().seedIfEmpty()
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideForumDao(database: AppDatabase): ForumDao = database.forumDao()

    @Provides
    fun provideTopicDao(database: AppDatabase): TopicDao = database.topicDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("jdrvirtuel_watcher_prefs") }
        )
    }
}
