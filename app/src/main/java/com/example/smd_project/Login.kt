package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smd_project.MainActivity
import com.example.smd_project.R
import com.example.smd_project.AppDatabase
import com.example.smd_project.RetrofitProvider
import com.example.smd_project.PrefsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class Login : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgot: TextView
    private lateinit var tvSignup: TextView
    private lateinit var back: TextView

    private lateinit var repo: AuthRepository
    private lateinit var prefs: PrefsManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // make sure this layout exists

        etEmail = findViewById(R.id.emailInput)
        etPass = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.loginBtn)
        tvForgot = findViewById(R.id.forgotPassword)
        tvSignup = findViewById(R.id.dontHaveAccount)

        val db = AppDatabase.getInstance(applicationContext)
        repo = AuthRepository(db.userDao(), RetrofitProvider.create())
        prefs = PrefsManager(applicationContext)


        back = findViewById(R.id.back)

        back.setOnClickListener {
            // Go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()   // optional: close Login screen so back press doesn’t return here
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPass.text?.toString().orEmpty()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                val r = withContext(Dispatchers.IO) { repo.login(email, pass) }
                r.onSuccess { resp ->
                    // save user id locally
                    resp.user_id?.let { userId ->
                        prefs.setUserId(userId)
                    }
                    Toast.makeText(this@Login, "Welcome ${resp.name ?: ""}", Toast.LENGTH_SHORT).show()
                    // Correct Intent syntax: comma between context and target class
                    startActivity(Intent(this@Login, dashboard::class.java))
                    finish()
                }.onFailure { ex ->
                    Toast.makeText(this@Login, ex.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
            }
        }


        tvSignup.setOnClickListener { startActivity(Intent(this, SignUp::class.java)) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
