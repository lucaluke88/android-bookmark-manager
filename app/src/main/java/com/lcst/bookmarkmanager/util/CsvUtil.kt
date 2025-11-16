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
        Scanner(inputStream).use { scanner ->
            // Skip header if present
            if (scanner.hasNextLine()) {
                val firstLine = scanner.nextLine().trim()
                if (firstLine.equals(CSV_HEADER, ignoreCase = true)) {
                    // Header found, continue
                } else {
                    // No header, process first line as data
                    parseCsvLine(firstLine)?.let { bookmarks.add(it) }
                }
            }

            while (scanner.hasNextLine()) {
                val line = scanner.nextLine().trim()
                if (line.isNotEmpty()) {
                    parseCsvLine(line)?.let { bookmarks.add(it) }
                }
            }
        }
        return bookmarks
    }

    private fun parseCsvLine(line: String): Bookmark? {
        return try {
            val parts = parseCsvFields(line)
            if (parts.size >= 2) {
                Bookmark(
                    title = unescapeCsv(parts[0]),
                    url = unescapeCsv(parts[1])
                )
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

