package com.example.smd_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUp : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var tvBack: TextView

    private lateinit var repo: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // IDs from your XML
        etName = findViewById(R.id.signupName)
        etEmail = findViewById(R.id.signupEmail)
        etPass = findViewById(R.id.signupPassword)
        etConfirm = findViewById(R.id.signupConfirmPassword)
        btnSignup = findViewById(R.id.signupBtn)
        tvBack = findViewById(R.id.back)

        val db = AppDatabase.getInstance(applicationContext)
        repo = AuthRepository(db.userDao(), RetrofitProvider.create())

        requestNotificationPermissionIfNeeded()

        tvBack.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        btnSignup.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPass.text?.toString().orEmpty()
            val conf = etConfirm.text?.toString().orEmpty()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || conf.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != conf) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                val result = withContext(Dispatchers.IO) { repo.signup(name, email, pass) }

                result.onSuccess { resp ->
                    // 1) Save pending verification in Firebase
                    val verRef = Firebase.database.getReference("verifications").push()
                    val data = mapOf(
                        "userId" to (resp.user_id ?: ""),
                        "name" to name,
                        "email" to email,
                        "status" to "pending",
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    verRef.setValue(data)

                    // 2) Remember this email as "pending" on this device
                    val sp = getSharedPreferences("signup_prefs", MODE_PRIVATE)
                    sp.edit().putString("pending_email", email).apply()

                    // 3) Status-bar notification: verification pending
                    NotificationHelper.showNotification(
                        this@SignUp,
                        "Verification Pending",
                        "Your verification request has been sent. Please wait for banker approval."
                    )

                    // Go back to login
                    startActivity(Intent(this@SignUp, Login::class.java))
                    finish()
                }.onFailure { ex ->
                    Toast.makeText(
                        this@SignUp,
                        ex.message ?: "Signup failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
