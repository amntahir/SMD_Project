package com.example.smd_project

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM Users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM Users WHERE userId = :id")
    suspend fun deleteById(id: String)
}
