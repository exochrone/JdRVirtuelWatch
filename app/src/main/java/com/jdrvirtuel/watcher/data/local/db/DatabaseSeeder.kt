package com.jdrvirtuel.watcher.data.local.db

import android.content.Context
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.data.local.dao.ForumDao
import com.jdrvirtuel.watcher.data.local.entity.ForumEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumDao: ForumDao
) {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            if (forumDao.count() == 0) {
                val forums = listOf(
                    ForumEntity(
                        id = 15,
                        name = context.getString(R.string.forum_oneshots),
                        url = "https://www.jdrvirtuel.com/viewforum.php?f=15"
                    ),
                    ForumEntity(
                        id = 16,
                        name = context.getString(R.string.forum_campagnes),
                        url = "https://www.jdrvirtuel.com/viewforum.php?f=16"
                    )
                )
                forums.forEach { forumDao.upsert(it) }
            }
        }
    }
}
