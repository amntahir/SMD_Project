package com.example.smd_project

data class VerificationItem(
    val id: String = "",
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val status: String? = "pending",
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
