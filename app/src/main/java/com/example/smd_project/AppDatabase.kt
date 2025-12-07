package com.example.smd_project

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

// bump version if you previously had version=1 and DB already installed
@Database(
    entities = [UserEntity::class, DocumentEntity::class /* add other entities here */],
    version = 2, // <-- increment if this is a schema change
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app.db")
                // Use destructive migration in development to recreate DB when schema changes
                .fallbackToDestructiveMigration()
                .build()
    }
}

class AuthRepository(private val userDao: UserDao, private val api: ApiService) {

    private val TAG = "AuthRepository"

    /**
     * Signup:
     * - call backend (XAMPP) to register
     * - on success: insert into local Room DB
     * - mirror a safe subset of user fields to BOTH Firestore and Realtime Database (non-blocking)
     *
     * NOTE: Passwords are never stored in Firebase. Only a local bcrypt hash is stored locally.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun signup(name: String, email: String, password: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.signup(SignupRequest(name, email, password))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null && body.ok) {
                        // create local record
                        val uuid = body.user_id ?: UUID.randomUUID().toString()
                        val now = Instant.now().toString()
                        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                        val user = UserEntity(
                            userId = uuid,
                            name = name,
                            email = email,
                            passwordHash = hash,
                            createdAt = now,
                            updatedAt = now
                        )
                        userDao.insert(user)

                        // Prepare safe map to push to Firebase services (exclude password/passwordHash)
                        val userMap = hashMapOf(
                            "userId" to uuid,
                            "name" to name,
                            "email" to email,
                            "createdAt" to now,
                            "updatedAt" to now
                        )

                        // --- Firestore mirror (non-blocking) ---
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            firestore.collection("users").document(uuid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Saved user to Firestore: $uuid")
                                }
                                .addOnFailureListener { ex ->
                                    Log.w(TAG, "Failed to save user to Firestore: ${ex.message}")
                                }
                        } catch (fex: Exception) {
                            Log.w(TAG, "Firestore write exception: ${fex.message}")
                        }
                        // --- end Firestore mirror ---

                        // --- Realtime Database mirror (non-blocking) ---
                        try {
                            val rdbRef =
                                FirebaseDatabase.getInstance().getReference("realtime_users")
                                    .child(uuid)
                            rdbRef.setValue(userMap)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Saved user to Realtime DB: $uuid")
                                }
                                .addOnFailureListener { ex ->
                                    Log.w(TAG, "Failed to save user to Realtime DB: ${ex.message}")
                                }
                        } catch (rex: Exception) {
                            Log.w(TAG, "Realtime DB write exception: ${rex.message}")
                        }
                        // --- end Realtime mirror ---

                        return@withContext Result.success(body)
                    } else return@withContext Result.failure(
                        Exception(
                            body?.message ?: "Signup failed"
                        )
                    )
                } else {
                    val err = resp.errorBody()?.string() ?: "Signup error"
                    return@withContext Result.failure(Exception(err))
                }
            } catch (ex: Exception) {
                return@withContext Result.failure(ex)
            }
        }

    /**
     * Login: (unchanged)
     * - try server login
     * - if server fails or returns invalid, fallback to local Room verification using bcrypt hash
     */
    suspend fun login(email: String, password: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.login(LoginRequest(email, password))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null && body.ok) {
                        // optionally update local DB or cache
                        return@withContext Result.success(body)
                    } else {
                        // fallback to local verification
                        val local = userDao.findByEmail(email)
                        if (local != null) {
                            val ok = BCrypt.verifyer()
                                .verify(password.toCharArray(), local.passwordHash).verified
                            if (ok) return@withContext Result.success(
                                SimpleResponse(
                                    ok = true,
                                    message = "local-login",
                                    user_id = local.userId,
                                    name = local.name,
                                    email = local.email
                                )
                            )
                        }
                        return@withContext Result.failure(
                            Exception(
                                body?.message ?: "Invalid credentials"
                            )
                        )
                    }
                } else {
                    // server error -> fallback local
                    val local = userDao.findByEmail(email)
                    if (local != null) {
                        val ok = BCrypt.verifyer()
                            .verify(password.toCharArray(), local.passwordHash).verified
                        if (ok) return@withContext Result.success(
                            SimpleResponse(
                                ok = true,
                                message = "local-login",
                                user_id = local.userId,
                                name = local.name,
                                email = local.email
                            )
                        )
                    }
                    val err = resp.errorBody()?.string() ?: "Login error"
                    return@withContext Result.failure(Exception(err))
                }
            } catch (ex: Exception) {
                // on exception try local fallback
                val local = userDao.findByEmail(email)
                if (local != null) {
                    val ok = BCrypt.verifyer()
                        .verify(password.toCharArray(), local.passwordHash).verified
                    if (ok) return@withContext Result.success(
                        SimpleResponse(
                            ok = true,
                            message = "local-login",
                            user_id = local.userId,
                            name = local.name,
                            email = local.email
                        )
                    )
                }
                return@withContext Result.failure(ex)
            }
        }

    /**
     * Forgot password - forwards to backend API (unchanged)
     */
    suspend fun forgotPassword(email: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.forgotPassword(ForgotRequest(email))
                if (resp.isSuccessful) {
                    val b = resp.body()
                    if (b != null) return@withContext Result.success(b)
                    else return@withContext Result.failure(Exception("Empty response"))
                } else {
                    return@withContext Result.failure(
                        Exception(
                            resp.errorBody()?.string() ?: "Error"
                        )
                    )
                }
            } catch (ex: Exception) {
                return@withContext Result.failure(ex)
            }
        }

    // -------------------------
    // Helpers for realtime DB
    // -------------------------
    /**
     * Update the realtime user node for profile updates.
     * Accepts a UserEntity (local update already done) and pushes safe fields to RTDB.
     */
    suspend fun updateUserRealtime(user: UserEntity) = withContext(Dispatchers.IO) {
        try {
            val userMap = hashMapOf(
                "userId" to user.userId,
                "name" to user.name,
                "email" to user.email,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )
            FirebaseDatabase.getInstance().getReference("realtime_users").child(user.userId)
                .setValue(userMap)
                .addOnSuccessListener { Log.d(TAG, "Realtime user updated: ${user.userId}") }
                .addOnFailureListener { ex -> Log.w(TAG, "Realtime update failed: ${ex.message}") }
        } catch (ex: Exception) {
            Log.w(TAG, "Realtime update exception: ${ex.message}")
        }
    }

    /**
     * Remove user from realtime DB (if you support account deletion).
     */
    suspend fun deleteUserRealtime(userId: String) = withContext(Dispatchers.IO) {
        try {
            FirebaseDatabase.getInstance().getReference("realtime_users").child(userId)
                .removeValue()
                .addOnSuccessListener { Log.d(TAG, "Realtime user removed: $userId") }
                .addOnFailureListener { ex -> Log.w(TAG, "Realtime delete failed: ${ex.message}") }
        } catch (ex: Exception) {
            Log.w(TAG, "Realtime delete exception: ${ex.message}")
        }
    }
}