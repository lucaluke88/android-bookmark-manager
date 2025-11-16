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
import com.lcst.bookmarkmanager.viewmodel.BookmarkViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
            importFromCsv(uri)
        } else {
            Toast.makeText(this, "Nessun file selezionato", Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingFileOperation: (() -> Unit)? = null

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
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.isEmpty()) {
                    observeBookmarks()
                } else {
                    observeSearchResults(query)
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
        lifecycleScope.launch {
            viewModel.getAllBookmarks().collectLatest { bookmarks: List<Bookmark> ->
                adapter.submitList(bookmarks)
                binding.emptyTextView.visibility =
                    if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeSearchResults(query: String) {
        lifecycleScope.launch {
            viewModel.searchBookmarks(query).collectLatest { bookmarks: List<Bookmark> ->
                adapter.submitList(bookmarks)
                binding.emptyTextView.visibility =
                    if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
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

    private fun importFromCsv(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(this, "Impossibile aprire il file selezionato", Toast.LENGTH_LONG).show()
                return
            }
            
            inputStream.use { stream: InputStream ->
                val bookmarks = CsvUtil.importBookmarks(stream)
                if (bookmarks.isEmpty()) {
                    Toast.makeText(this, "Nessun segnalibro trovato nel file CSV", Toast.LENGTH_SHORT).show()
                    return@use
                }
                
                lifecycleScope.launch {
                    try {
                        bookmarks.forEach { bookmark ->
                            viewModel.insertBookmark(bookmark)
                        }
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "${getString(R.string.import_success)}: ${bookmarks.size} segnalibri importati",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Errore durante l'importazione: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permesso negato per accedere al file. Assicurati di aver concesso i permessi necessari.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
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

