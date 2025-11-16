package com.lcst.bookmarkmanager.util

import com.lcst.bookmarkmanager.data.Bookmark
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.Scanner

object CsvUtil {
    private const val CSV_HEADER = "Title,URL"

    fun exportBookmarks(bookmarks: List<Bookmark>, outputStream: OutputStream) {
        PrintWriter(outputStream).use { writer ->
            writer.println(CSV_HEADER)
            bookmarks.forEach { bookmark ->
                writer.println("\"${escapeCsv(bookmark.title)}\",\"${escapeCsv(bookmark.url)}\"")
            }
        }
    }

    fun importBookmarks(inputStream: InputStream): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        try {
            // Usa BufferedReader per gestire meglio l'encoding
            inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                var lineNumber = 0
                var isFirstLine = true
                
                reader.forEachLine { line ->
                    lineNumber++
                    val trimmedLine = line.trim()
                    
                    if (trimmedLine.isEmpty()) {
                        return@forEachLine
                    }
                    
                    if (isFirstLine) {
                        isFirstLine = false
                        // Controlla se è l'header
                        if (trimmedLine.equals(CSV_HEADER, ignoreCase = true) ||
                            trimmedLine.replace(" ", "").equals(CSV_HEADER.replace(" ", ""), ignoreCase = true)) {
                            android.util.Log.d("CsvUtil", "Header trovato, skip")
                            return@forEachLine
                        }
                    }
                    
                    val bookmark = parseCsvLine(trimmedLine)
                    if (bookmark != null) {
                        bookmarks.add(bookmark)
                        // Log solo ogni 100 bookmark per non rallentare
                        if (lineNumber % 100 == 0) {
                            android.util.Log.d("CsvUtil", "Bookmark parsati: $lineNumber")
                        }
                    } else if (lineNumber <= 10) {
                        // Log solo i primi 10 errori per non rallentare
                        android.util.Log.w("CsvUtil", "Linea $lineNumber non parsata: $trimmedLine")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CsvUtil", "Errore durante l'import CSV: ${e.message}", e)
            throw Exception("Errore durante il parsing del file CSV: ${e.message}", e)
        }
        
        android.util.Log.d("CsvUtil", "Totale bookmark importati: ${bookmarks.size}")
        return bookmarks
    }

    private fun parseCsvLine(line: String): Bookmark? {
        return try {
            val parts = parseCsvFields(line)
            if (parts.size >= 2) {
                val title = unescapeCsv(parts[0].trim())
                val url = unescapeCsv(parts[1].trim())
                
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    Bookmark(
                        title = title,
                        url = url
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote
                        currentField.append('"')
                        i += 2  // Skip both quotes
                        continue
                    } else {
                        // Toggle quote state
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField = StringBuilder()
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }
        fields.add(currentField.toString())
        return fields
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun unescapeCsv(value: String): String {
        return value.replace("\"\"", "\"")
    }
}

