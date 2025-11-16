package com.lcst.bookmarkmanager.repository

import com.lcst.bookmarkmanager.data.Bookmark
import com.lcst.bookmarkmanager.data.BookmarkDao
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    fun searchBookmarks(query: String): Flow<List<Bookmark>> {
        val searchQuery = "%$query%"
        return bookmarkDao.searchBookmarks(searchQuery)
    }

    suspend fun insertBookmark(bookmark: Bookmark): Long = bookmarkDao.insertBookmark(bookmark)

    suspend fun updateBookmark(bookmark: Bookmark) = bookmarkDao.updateBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.deleteBookmark(bookmark)

    suspend fun deleteAllBookmarks() = bookmarkDao.deleteAllBookmarks()

    suspend fun getAllBookmarksList(): List<Bookmark> = bookmarkDao.getAllBookmarksList()
}

