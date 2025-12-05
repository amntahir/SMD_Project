package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.TextButton
import com.example.smd_project.R
import com.example.smd_project.AuthRepository
import com.example.smd_project.AppDatabase
import com.example.smd_project.RetrofitProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class SignUp : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etConfirm: TextInputEditText

    private lateinit var btnSignup: MaterialButton
    private lateinit var repo: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up) // your uploaded layout. :contentReference[oaicite:10]{index=10}

        etName = findViewById(R.id.signupName)
        etEmail = findViewById(R.id.signupEmail)
        etPass = findViewById(R.id.signupPassword)
        etConfirm = findViewById(R.id.signupConfirmPassword)
        btnSignup = findViewById(R.id.signupBtn)
        val db = AppDatabase.getInstance(applicationContext) // :contentReference[oaicite:11]{index=11}
        repo = AuthRepository(db.userDao(), RetrofitProvider.create())
        val back: TextView = findViewById(R.id.back)
        back.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
        btnSignup.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPass.text?.toString().orEmpty()
            val conf = etConfirm.text?.toString().orEmpty()
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (pass != conf) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            scope.launch {
                val result = withContext(Dispatchers.IO) { repo.signup(name, email, pass) }
                result.onSuccess {
                    Toast.makeText(this@SignUp, "Signup successful", Toast.LENGTH_SHORT).show()
                    finish() // go back to login
                }.onFailure { ex ->
                    Toast.makeText(this@SignUp, ex.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
