package com.example.smd_project

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Documents")
data class DocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "documentId") val documentId: String,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "originalName") val originalName: String,
    @ColumnInfo(name = "extension") val extension: String?,
    @ColumnInfo(name = "storagePath") val storagePath: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "uploadedAt") val uploadedAt: String?,
    @ColumnInfo(name = "createdAt") val createdAt: String?
)
