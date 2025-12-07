package com.example.smd_project

// -------- Requests --------
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ForgotRequest(
    val email: String
)

data class ResetRequest(
    val token: String,
    val newPassword: String
)

// -------- Responses --------
data class SimpleResponse(
    val ok: Boolean,
    val message: String?,
    val user_id: String? = null,
    val name: String? = null,
    val email: String? = null
)

data class UploadResponse(
    val ok: Boolean,
    val message: String?,
    val documentId: String? = null,
    val storagePath: String? = null,
    val url: String? = null
)

data class DocumentsResponse(
    val ok: Boolean,
    val message: String?,
    val data: List<DocumentItem> = emptyList()
)

data class DocumentItem(
    val documentId: String,
    val originalName: String,
    val extension: String?,
    val storagePath: String?,
    val status: String?,
    val uploadedAt: String?,
    val createdAt: String?
)
