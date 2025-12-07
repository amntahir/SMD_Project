package com.example.smd_project

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.Locale

class BrowseDocsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var dao: DocumentDao
    private lateinit var prefs: PrefsManager

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnVoiceSearch: ImageButton
    private lateinit var rvResults: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DocumentAdapter

    // Activity Result launcher for speech recognition
    private val speechRecognizerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val matches =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                // Debug: show what we actually got
                Toast.makeText(
                    this,
                    "Heard: ${matches?.joinToString() ?: "nothing"}",
                    Toast.LENGTH_LONG
                ).show()

                val recognizedText = matches?.firstOrNull()

                if (!recognizedText.isNullOrBlank()) {
                    etSearch.setText(recognizedText)
                    doSearch()
                }
            } else {
                Toast.makeText(this, "No voice input detected", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_browse_docs)

        // safe insets
        val content: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        val db = AppDatabase.getInstance(applicationContext)
        dao = db.documentDao()
        prefs = PrefsManager(applicationContext)

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnVoiceSearch = findViewById(R.id.btnvoicesearch)
        rvResults = findViewById(R.id.rvResults)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = DocumentAdapter(emptyList()) { doc -> openDocument(doc) }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.setHasFixedSize(true)
        rvResults.adapter = adapter
        rvResults.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // Typed search button
        btnSearch.setOnClickListener { doSearch() }

        // NEW: voice search button
        btnVoiceSearch.setOnClickListener {
            // Check mic permission first
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO
                )
            } else {
                startVoiceRecognition()
            }
        }

        // allow search on keyboard Done / Search
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                doSearch()
                true
            } else false
        }

        findViewById<ImageButton>(R.id.btnBackBrowse).setOnClickListener { finish() }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // or your language code
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search documents")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Speech recognition not supported on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun doSearch() {
        val q = etSearch.text.toString().trim()
        if (q.isEmpty()) {
            Toast.makeText(this, "Enter a document name to search", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = prefs.getUserId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        // pattern for LIKE query: match anywhere
        val pattern = "%${q.replace("%", "\\%").replace("_", "\\_")}%"

        scope.launch {
            val results = withContext(Dispatchers.IO) {
                try {
                    dao.searchByName(userId, pattern)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    emptyList<DocumentEntity>()
                }
            }

            if (results.isEmpty()) {
                adapter.submitList(emptyList())
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "No documents found for \"$q\""
            } else {
                tvEmpty.visibility = View.GONE
                adapter.submitList(results)
            }
        }
    }

    private fun openDocument(d: DocumentEntity) {
        val path = d.storagePath
        if (path.isNullOrEmpty()) {
            Toast.makeText(this, "No file path available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = Uri.parse(path)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (ae: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(this, "Unable to open file: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required for voice search",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 2001
    }
}
