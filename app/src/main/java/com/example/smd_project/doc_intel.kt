package com.example.smd_project

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class doc_intel : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "doc_intel"

    private lateinit var repo: DocumentRepository
    private lateinit var prefs: PrefsManager

    private lateinit var rvDocs: RecyclerView
    private lateinit var fabUpload: FloatingActionButton
    private lateinit var adapter: DocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doc_intel)

        // apply window insets to root content to avoid content going under system bars
        val content: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        repo = DocumentRepository(RetrofitProvider.create(), AppDatabase.getInstance(applicationContext).documentDao())
        prefs = PrefsManager(applicationContext)

        rvDocs = findViewById(R.id.rvDocs)
        fabUpload = findViewById(R.id.fabUpload)

        // RecyclerView setup
        adapter = DocumentAdapter(emptyList()) { doc -> openDocument(doc) }
        rvDocs.layoutManager = LinearLayoutManager(this)
        rvDocs.setHasFixedSize(true)
        rvDocs.adapter = adapter
        rvDocs.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        fabUpload.setOnClickListener {
            startActivity(Intent(this, doc_upload::class.java))
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Go back to upload screen (or previous)
            startActivity(Intent(this, doc_upload::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadLocalOrServerDocs()
    }

    private fun loadLocalOrServerDocs() {
        val userId = prefs.getUserId()
        Log.d(TAG, "loadLocalOrServerDocs: userId=$userId")

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Login required to view documents", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            // Load local docs first
            val local = withContext(Dispatchers.IO) {
                try {
                    repo.getLocalDocs(userId)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    emptyList<DocumentEntity>()
                }
            }

            if (local.isNotEmpty()) {
                adapter.submitList(local)
                Log.d(TAG, "Loaded ${local.size} local documents")
                Toast.makeText(this@doc_intel, "Loaded ${local.size} documents", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // If no local docs, fetch from server
            val serverResp = withContext(Dispatchers.IO) {
                try {
                    repo.getDocsFromServer(userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (serverResp == null || !serverResp.isSuccessful) {
                Toast.makeText(this@doc_intel, "Unable to load documents", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val body = serverResp.body()
            if (body == null || !body.ok) {
                Toast.makeText(this@doc_intel, body?.message ?: "No documents found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Convert server items to DocumentEntity and persist locally
            val docsToSave = body.data.map { item ->
                DocumentEntity(
                    documentId = item.documentId ?: java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    originalName = item.originalName ?: "unknown",
                    extension = item.extension ?: "",
                    storagePath = item.storagePath ?: "",
                    status = item.status ?: "UPLOADED",
                    uploadedAt = item.uploadedAt,
                    createdAt = item.createdAt
                )
            }

            withContext(Dispatchers.IO) {
                docsToSave.forEach { try { repo.saveLocal(it) } catch (_: Exception) { } }
            }

            val reloaded = withContext(Dispatchers.IO) { repo.getLocalDocs(userId) }
            adapter.submitList(reloaded)
            Log.d(TAG, "Loaded ${reloaded.size} documents after server sync")
            Toast.makeText(this@doc_intel, "Loaded ${reloaded.size} documents", Toast.LENGTH_SHORT).show()
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
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (ae: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(this, "Unable to open file: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
