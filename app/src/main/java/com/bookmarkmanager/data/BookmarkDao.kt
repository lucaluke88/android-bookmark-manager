package com.bookmarkmanager.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY title ASC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE :searchQuery OR url LIKE :searchQuery ORDER BY title ASC")
    fun searchBookmarks(searchQuery: String): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks ORDER BY title ASC")
    suspend fun getAllBookmarksList(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}

