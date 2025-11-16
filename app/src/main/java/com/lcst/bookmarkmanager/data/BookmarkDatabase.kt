package com.lcst.bookmarkmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Bookmark::class], version = 2, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        // Migration da versione 1 a 2: aggiunta indici per migliorare le prestazioni
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crea indici per le colonne title e url per ottimizzare le query di ricerca
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_title ON bookmarks(title)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_url ON bookmarks(url)")
            }
        }

        fun getDatabase(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmark_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Solo per sviluppo - rimuovere in produzione se necessario
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

