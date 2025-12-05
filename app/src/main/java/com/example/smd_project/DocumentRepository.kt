package com.example.smd_project

import okhttp3.MultipartBody
import okhttp3.RequestBody
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class DocumentRepository(
    private val api: ApiService,
    private val dao: DocumentDao
) {
    private val TAG = "DocumentRepository"

    suspend fun uploadFile(filePart: MultipartBody.Part, userId: RequestBody, originalName: RequestBody) =
        api.uploadDocument(filePart, userId, originalName)

    suspend fun getDocsFromServer(userId: String) =
        api.getDocs(mapOf("user_id" to userId))

    suspend fun saveLocal(entity: DocumentEntity) = dao.insert(entity)

    suspend fun getLocalDocs(userId: String) = dao.getByUser(userId)

    /**
     * Non-suspending best-effort mirror of document metadata to Realtime Database.
     *
     * IMPORTANT:
     * - This writes only metadata (no file bytes).
     * - It uses listeners and logs failures. It does NOT throw or block.
     * - Node used: realtime_documents/{documentId}
     *
     * This preserves all your existing datatypes/signatures.
     */
    fun saveToRealtime(entity: DocumentEntity) {
        try {
            val map = hashMapOf<String, Any?>(
                "documentId" to entity.documentId,
                "userId" to entity.userId,
                "originalName" to entity.originalName,
                "extension" to entity.extension,
                "storagePath" to entity.storagePath,
                "status" to entity.status,
                "uploadedAt" to entity.uploadedAt,
                "createdAt" to entity.createdAt
            )

            // If you ever need to force a specific DB URL, replace with:
            // val dbUrl = "https://your-project-id-default-rtdb.firebaseio.com"
            // val ref = FirebaseDatabase.getInstance(dbUrl).getReference("realtime_documents").child(entity.documentId)
            val ref = FirebaseDatabase.getInstance().getReference("realtime_documents").child(entity.documentId)

            ref.setValue(map)
                .addOnSuccessListener {
                    Log.d(TAG, "Saved document to Realtime DB: ${entity.documentId}")
                }
                .addOnFailureListener { ex ->
                    Log.w(TAG, "Failed to save document to Realtime DB: ${ex.message}", ex)
                }
        } catch (ex: Exception) {
            // Defensive: log but don't crash caller
            Log.w(TAG, "Realtime DB mirror exception: ${ex.message}", ex)
        }
    }
}
