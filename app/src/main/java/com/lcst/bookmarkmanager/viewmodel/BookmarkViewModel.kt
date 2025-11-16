package com.lcst.bookmarkmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lcst.bookmarkmanager.data.Bookmark
import com.lcst.bookmarkmanager.data.BookmarkDatabase
import com.lcst.bookmarkmanager.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookmarkRepository

    init {
        val bookmarkDao = BookmarkDatabase.getDatabase(application).bookmarkDao()
        repository = BookmarkRepository(bookmarkDao)
    }

    fun getAllBookmarks(): Flow<List<Bookmark>> = repository.getAllBookmarks()

    fun searchBookmarks(query: String): Flow<List<Bookmark>> = repository.searchBookmarks(query)

    fun insertBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.insertBookmark(bookmark)
        }
    }

    suspend fun insertBookmarks(bookmarks: List<Bookmark>) {
        repository.insertBookmarks(bookmarks)
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    suspend fun getAllBookmarksList(): List<Bookmark> = repository.getAllBookmarksList()

    suspend fun getBookmarkCount(): Int = repository.getBookmarkCount()
}

