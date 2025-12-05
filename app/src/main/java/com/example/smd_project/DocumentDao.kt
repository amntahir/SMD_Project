package com.example.smd_project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentEntity)

    @Query("SELECT * FROM Documents WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getByUser(userId: String): List<DocumentEntity>

    // NEW: search documents for a user by (partial) name (case-insensitive)
    @Query("SELECT * FROM Documents WHERE userId = :userId AND LOWER(originalName) LIKE LOWER(:pattern) ORDER BY createdAt DESC")
    suspend fun searchByName(userId: String, pattern: String): List<DocumentEntity>
}
