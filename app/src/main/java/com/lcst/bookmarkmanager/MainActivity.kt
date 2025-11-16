package com.lcst.bookmarkmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lcst.bookmarkmanager.adapter.BookmarkAdapter
import com.lcst.bookmarkmanager.data.Bookmark
import com.lcst.bookmarkmanager.databinding.ActivityMainBinding
import com.lcst.bookmarkmanager.databinding.DialogBookmarkBinding
import com.lcst.bookmarkmanager.util.CsvUtil
import com.lcst.bookmarkmanager.util.HtmlUtil
import com.lcst.bookmarkmanager.viewmodel.BookmarkViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BookmarkViewModel by viewModels()
    private lateinit var adapter: BookmarkAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            performFileOperation()
        } else {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
        }
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            exportToCsv(it)
        }
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            importFromFile(uri)
        } else {
            Toast.makeText(this, "Nessun file selezionato", Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingFileOperation: (() -> Unit)? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private var observationJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupSearch()
        setupButtons()
        observeBookmarks()
        handleSharedIntent(intent)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = BookmarkAdapter(
            onEditClick = { bookmark -> showBookmarkDialog(bookmark) },
            onDeleteClick = { bookmark -> deleteBookmark(bookmark) },
            onItemClick = { bookmark -> openUrlInBrowser(bookmark.url) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(false) // Deve essere false quando dentro NestedScrollView
        android.util.Log.d("MainActivity", "RecyclerView inizializzato, adapter: ${adapter.itemCount} item")
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Cancella la ricerca precedente
                searchJob?.cancel()
                
                val query = s?.toString() ?: ""
                
                // Debounce: aspetta 300ms prima di eseguire la ricerca
                searchJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(300)
                    if (query.isEmpty()) {
                        observeBookmarks()
                    } else {
                        observeSearchResults(query)
                    }
                }
            }
        })
    }

    private fun setupButtons() {
        binding.fab.setOnClickListener {
            showBookmarkDialog()
        }

        binding.exportButton.setOnClickListener {
            pendingFileOperation = { exportFileLauncher.launch("bookmarks.csv") }
            checkPermissionsAndPerform { performFileOperation() }
        }

        binding.importButton.setOnClickListener {
            pendingFileOperation = { 
                try {
                    importFileLauncher.launch("*/*")
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Errore nell'apertura del file manager: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            checkPermissionsAndPerform { performFileOperation() }
        }
    }

    private fun observeBookmarks() {
        // Cancella l'osservazione precedente per evitare osservazioni multiple
        observationJob?.cancel()
        observationJob = lifecycleScope.launch {
            viewModel.getAllBookmarks().collectLatest { bookmarks: List<Bookmark> ->
                android.util.Log.d("MainActivity", "Bookmark ricevuti dal Flow: ${bookmarks.size}")
                if (bookmarks.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "Primo bookmark: ${bookmarks.first().title} -> ${bookmarks.first().url}")
                }
                
                // Assicurati di essere sul thread principale per l'aggiornamento UI
                withContext(Dispatchers.Main) {
                    android.util.Log.d("MainActivity", "Chiamata submitList con ${bookmarks.size} bookmark")
                    val currentItemCount = adapter.itemCount
                    android.util.Log.d("MainActivity", "Item count prima di submitList: $currentItemCount")
                    
                    adapter.submitList(bookmarks) {
                        android.util.Log.d("MainActivity", "Callback submitList eseguito. Item count dopo: ${adapter.itemCount}")
                        binding.emptyTextView.visibility =
                            if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility =
                            if (bookmarks.isEmpty()) View.GONE else View.VISIBLE
                        
                        // Forza il layout del RecyclerView
                        binding.recyclerView.requestLayout()
                    }
                    
                    android.util.Log.d("MainActivity", "submitList chiamato, item count immediato: ${adapter.itemCount}")
                }
            }
        }
    }

    private fun observeSearchResults(query: String) {
        // Cancella l'osservazione precedente per evitare osservazioni multiple
        observationJob?.cancel()
        observationJob = lifecycleScope.launch {
            viewModel.searchBookmarks(query).collectLatest { bookmarks: List<Bookmark> ->
                android.util.Log.d("MainActivity", "Risultati ricerca: ${bookmarks.size} bookmark per query '$query'")
                
                // Assicurati di essere sul thread principale per l'aggiornamento UI
                withContext(Dispatchers.Main) {
                    adapter.submitList(bookmarks) {
                        binding.emptyTextView.visibility =
                            if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility =
                            if (bookmarks.isEmpty()) View.GONE else View.VISIBLE
                        binding.recyclerView.requestLayout()
                    }
                }
            }
        }
    }

    private fun showBookmarkDialog(bookmark: Bookmark? = null) {
        val dialogBinding = DialogBookmarkBinding.inflate(layoutInflater)
        val isEdit = bookmark != null

        if (isEdit && bookmark != null) {
            dialogBinding.titleEditText.setText(bookmark.title)
            dialogBinding.urlEditText.setText(bookmark.url)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) getString(R.string.edit) else getString(R.string.add_bookmark))
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.saveButton.setOnClickListener {
            val title = dialogBinding.titleEditText.text?.toString()?.trim() ?: ""
            val url = dialogBinding.urlEditText.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                Toast.makeText(this, getString(R.string.title_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (url.isEmpty()) {
                Toast.makeText(this, getString(R.string.url_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            if (isEdit && bookmark != null) {
                viewModel.updateBookmark(bookmark.copy(title = title, url = normalizedUrl))
                Toast.makeText(this, getString(R.string.bookmark_updated), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertBookmark(Bookmark(title = title, url = normalizedUrl))
                Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Sei sicuro di voler eliminare questo segnalibro?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteBookmark(bookmark)
                Toast.makeText(this, getString(R.string.bookmark_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun checkPermissionsAndPerform(operation: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permissions for document picker
            operation()
        } else {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (permissions.isEmpty()) {
                operation()
            } else {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    private fun performFileOperation() {
        pendingFileOperation?.invoke()
        pendingFileOperation = null
    }

    private fun exportToCsv(uri: Uri) {
        try {
            lifecycleScope.launch {
                val bookmarks = viewModel.getAllBookmarksList()
                contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    CsvUtil.exportBookmarks(bookmarks, outputStream)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.export_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun importFromFile(uri: Uri) {
        // Mostra progress dialog immediatamente
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Importazione in corso")
            .setMessage("Lettura e parsing del file...")
            .setCancelable(false)
            .create()
            .also { it.show() }
        
        lifecycleScope.launch {
            try {
                // Tutto il lavoro pesante su Dispatchers.IO
                val bookmarks = withContext(Dispatchers.IO) {
                    val fileName = getFileName(uri)
                    android.util.Log.d("Import", "File selezionato: $fileName, URI: $uri")
                    
                    // Determina il tipo di file dall'estensione
                    val isHtml = fileName?.endsWith(".html", ignoreCase = true) == true || 
                                fileName?.endsWith(".htm", ignoreCase = true) == true
                    val isCsv = fileName?.endsWith(".csv", ignoreCase = true) == true
                    
                    android.util.Log.d("Import", "Tipo file rilevato - HTML: $isHtml, CSV: $isCsv")
                    
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        throw Exception("Impossibile aprire il file selezionato")
                    }
                    
                    try {
                        when {
                            isHtml -> {
                                android.util.Log.d("Import", "Tentativo import HTML")
                                HtmlUtil.importBookmarksFromChromeHtml(inputStream)
                            }
                            isCsv -> {
                                android.util.Log.d("Import", "Tentativo import CSV")
                                CsvUtil.importBookmarks(inputStream)
                            }
                            else -> {
                                // Se l'estensione non è chiara, rileva dal contenuto
                                android.util.Log.d("Import", "Estensione non chiara, rilevazione dal contenuto")
                                val contentPreview = inputStream.bufferedReader(Charsets.UTF_8).use { 
                                    val buffer = CharArray(1024)
                                    it.read(buffer, 0, 1024)
                                    String(buffer)
                                }
                                
                                val isHtmlContent = contentPreview.contains("<!DOCTYPE", ignoreCase = true) ||
                                                  contentPreview.contains("<DT><A", ignoreCase = true) ||
                                                  contentPreview.contains("NETSCAPE-Bookmark", ignoreCase = true)
                                
                                android.util.Log.d("Import", "Contenuto rilevato come HTML: $isHtmlContent")
                                
                                // Riapri lo stream per l'importazione
                                val importStream = contentResolver.openInputStream(uri)
                                importStream?.use { importS ->
                                    if (isHtmlContent) {
                                        HtmlUtil.importBookmarksFromChromeHtml(importS)
                                    } else {
                                        CsvUtil.importBookmarks(importS)
                                    }
                                } ?: throw Exception("Impossibile aprire il file")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Import", "Errore parsing: ${e.message}", e)
                        // Se il parsing fallisce, prova l'altro formato come fallback
                        try {
                            android.util.Log.d("Import", "Tentativo fallback con formato alternativo")
                            val fallbackStream = contentResolver.openInputStream(uri)
                            fallbackStream?.use { fallback ->
                                if (isHtml) {
                                    CsvUtil.importBookmarks(fallback)
                                } else {
                                    HtmlUtil.importBookmarksFromChromeHtml(fallback)
                                }
                            } ?: throw Exception("Impossibile aprire il file")
                        } catch (fallbackException: Exception) {
                            android.util.Log.e("Import", "Errore fallback: ${fallbackException.message}", fallbackException)
                            throw Exception("Impossibile importare il file. Formato non supportato o file corrotto: ${e.message}")
                        }
                    } finally {
                        inputStream.close()
                    }
                }
                
                android.util.Log.d("Import", "Bookmark parsati: ${bookmarks.size}")
                
                if (bookmarks.isEmpty()) {
                    val fileType = when {
                        uri.toString().contains(".html", ignoreCase = true) -> "HTML"
                        uri.toString().contains(".csv", ignoreCase = true) -> "CSV"
                        else -> ""
                    }
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "Nessun segnalibro trovato nel file $fileType", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Aggiorna il progress dialog
                withContext(Dispatchers.Main) {
                    progressDialog.setMessage("Sto importando ${bookmarks.size} segnalibri nel database...")
                }
                
                // Inserimento nel database (già su Dispatchers.IO dalla coroutine)
                android.util.Log.d("Import", "Inizio inserimento ${bookmarks.size} bookmark nel database")
                android.util.Log.d("Import", "Primo bookmark esempio: ${bookmarks.firstOrNull()?.title} -> ${bookmarks.firstOrNull()?.url}")
                
                try {
                    viewModel.insertBookmarks(bookmarks)
                    android.util.Log.d("Import", "Inserimento completato")
                    
                    // Verifica che i bookmark siano stati inseriti
                    val countAfter = viewModel.getBookmarkCount()
                    val listAfter = viewModel.getAllBookmarksList()
                    android.util.Log.d("Import", "Bookmark nel database dopo inserimento: count=$countAfter, list size=${listAfter.size}")
                    
                    if (listAfter.isNotEmpty()) {
                        android.util.Log.d("Import", "Primo bookmark nel DB: ${listAfter.first().title} -> ${listAfter.first().url}")
                    }
                    
                    if (countAfter == 0 || listAfter.isEmpty()) {
                        android.util.Log.e("Import", "ERRORE: Nessun bookmark nel database dopo l'inserimento!")
                        throw Exception("Inserimento fallito: nessun bookmark nel database (count=$countAfter, list size=${listAfter.size})")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Import", "Errore durante l'inserimento: ${e.message}", e)
                    throw e
                }
                
                // Piccolo delay per assicurarsi che Room abbia completato la transazione
                kotlinx.coroutines.delay(200)
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    
                    // Cancella il debounce job per evitare conflitti
                    searchJob?.cancel()
                    
                    // Pulisci la ricerca per mostrare tutti i bookmark
                    binding.searchEditText.setText("")
                    
                    // Riattiva l'osservazione per aggiornare la lista
                    // Room emetterà automaticamente i nuovi valori tramite Flow
                    // Non serve chiamare observeBookmarks() esplicitamente se l'osservazione è già attiva
                    // Ma lo facciamo per essere sicuri che la lista si aggiorni
                    observeBookmarks()
                    
                    val fileType = when {
                        uri.toString().contains(".html", ignoreCase = true) -> "HTML"
                        uri.toString().contains(".csv", ignoreCase = true) -> "CSV"
                        else -> "sconosciuto"
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "${getString(R.string.import_success)}: ${bookmarks.size} segnalibri importati da file $fileType",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: SecurityException) {
                android.util.Log.e("Import", "SecurityException: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Permesso negato per accedere al file. Assicurati di aver concesso i permessi necessari.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("Import", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        return try {
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            result = it.getString(nameIndex)
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != -1) {
                    result = result?.substring(cut!! + 1)
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile aprire l'URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSharedIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        // Estrai URL dal testo condiviso
                        val url = extractUrlFromText(sharedText)
                        if (url != null) {
                            showShareDialog(url)
                        } else {
                            Toast.makeText(this, "Nessun URL valido trovato nel testo condiviso", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        // Cerca URL nel testo (http://, https://, o www.)
        val urlPattern = Regex("""(https?://[^\s]+|www\.[^\s]+)""", RegexOption.IGNORE_CASE)
        val match = urlPattern.find(text)
        return match?.value?.let { url ->
            // Aggiungi https:// se manca
            if (url.startsWith("www.", ignoreCase = true)) {
                "https://$url"
            } else {
                url
            }
        }
    }

    private fun showShareDialog(url: String) {
        val dialogBinding = DialogBookmarkBinding.inflate(layoutInflater)
        dialogBinding.urlEditText.setText(url)
        dialogBinding.urlEditText.isEnabled = false // L'URL è già impostato, non modificabile

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_bookmark))
            .setMessage("Inserisci un titolo per questo segnalibro")
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.saveButton.setOnClickListener {
            val title = dialogBinding.titleEditText.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                Toast.makeText(this, getString(R.string.title_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            viewModel.insertBookmark(Bookmark(title = title, url = normalizedUrl))
            Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }
}

