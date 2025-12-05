package com.example.smd_project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.util.Log

class doc_upload : AppCompatActivity() {
    private val PICK_FILE = 101
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DocumentRepository
    private lateinit var prefs: PrefsManager
    private val TAG = "doc_upload"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doc_upload)

        val api = RetrofitProvider.create()
        val db = AppDatabase.getInstance(applicationContext)
        repo = DocumentRepository(api, db.documentDao())
        prefs = PrefsManager(applicationContext)

        // Wire UI: Upload card/button and back button
        val cardUpload = findViewById<CardView>(R.id.cardUpload)
        val cardBrowse = findViewById<CardView>(R.id.cardBrowse)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        cardUpload.setOnClickListener {
            pickFile()
        }

        cardBrowse.setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
        }

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf","image/*","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        startActivityForResult(Intent.createChooser(intent, "Select document"), PICK_FILE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            uploadUri(uri)
        } else {
            // user canceled picker — stay on this screen
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadUri(uri: Uri) {
        val userId = prefs.getUserId() ?: run {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show(); finish(); return
        }

        scope.launch {
            try {
                val temp = withContext(Dispatchers.IO) { createTempFileFromUri(uri) }
                if (temp == null) { Toast.makeText(this@doc_upload, "Unable to read file", Toast.LENGTH_SHORT).show(); return@launch }

                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                val requestFile = temp.asRequestBody(mime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", temp.name, requestFile)

                // Keep RequestBody creation compatible with your existing code
                val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
                val origNameStr = getFileName(uri) ?: temp.name
                val origNameBody = origNameStr.toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = withContext(Dispatchers.IO) { repo.uploadFile(part, userIdBody, origNameBody) }

                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val body = resp.body()!!
                    val now = java.time.LocalDateTime.now().toString()
                    val docEntity = DocumentEntity(
                        documentId = body.documentId ?: java.util.UUID.randomUUID().toString(),
                        userId = userId,
                        originalName = origNameStr,
                        extension = temp.extension,
                        storagePath = body.storagePath ?: "",
                        status = "UPLOADED",
                        uploadedAt = now,
                        createdAt = now
                    )

                    // ensure local DB save runs on IO and is awaited
                    withContext(Dispatchers.IO) {
                        repo.saveLocal(docEntity)
                    }

                    // Best-effort mirror to Realtime DB (fire-and-forget)
                    try {
                        repo.saveToRealtime(docEntity)
                        Log.d(TAG, "Triggered RTDB mirror for doc ${docEntity.documentId}")
                    } catch (ex: Exception) {
                        Log.w(TAG, "RTDB mirror call raised: ${ex.message}", ex)
                    }

                    Toast.makeText(this@doc_upload, "Uploaded", Toast.LENGTH_SHORT).show()
                    // Return to doc_intel; onResume will refresh
                    startActivity(Intent(this@doc_upload, doc_intel::class.java))
                    finish()
                } else {
                    val err = resp.errorBody()?.string()
                    Toast.makeText(this@doc_upload, "Upload failed: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@doc_upload, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        try {
            val input: InputStream? = contentResolver.openInputStream(uri)
            val name = getFileName(uri) ?: "tempfile"
            val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}_$name")
            val out = FileOutputStream(tempFile)
            input?.copyTo(out)
            out.close()
            input?.close()
            return tempFile
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) result = cursor.getString(idx)
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) result = result?.substring(cut + 1)
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
