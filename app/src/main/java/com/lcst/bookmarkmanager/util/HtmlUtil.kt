package com.lcst.bookmarkmanager.util

import com.lcst.bookmarkmanager.data.Bookmark
import java.io.InputStream
import java.util.Scanner

object HtmlUtil {
    /**
     * Importa bookmark da un file HTML generato dall'export di Chrome
     * Formato Chrome: <DT><A HREF="url" ADD_DATE="timestamp">Title</A>
     */
    fun importBookmarksFromChromeHtml(inputStream: InputStream): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        
        try {
            val content = inputStream.bufferedReader().use { it.readText() }
            
            // Pattern per trovare i tag <A HREF="url">Title</A>
            // Gestisce anche attributi aggiuntivi come ADD_DATE
            val pattern = Regex(
                """<DT><A\s+HREF=["']([^"']+)["'][^>]*>([^<]+)</A>""",
                RegexOption.IGNORE_CASE
            )
            
            val matches = pattern.findAll(content)
            
            matches.forEach { matchResult ->
                val url = matchResult.groupValues[1].trim()
                val title = matchResult.groupValues[2].trim()
                
                if (url.isNotEmpty() && title.isNotEmpty()) {
                    // Decodifica entità HTML comuni
                    val decodedTitle = decodeHtmlEntities(title)
                    val normalizedUrl = normalizeUrl(url)
                    
                    bookmarks.add(Bookmark(title = decodedTitle, url = normalizedUrl))
                }
            }
        } catch (e: Exception) {
            throw Exception("Errore durante il parsing del file HTML: ${e.message}", e)
        }
        
        return bookmarks
    }
    
    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
    
    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}

