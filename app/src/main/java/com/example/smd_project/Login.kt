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
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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

    private var verificationQuery: Query? = null
    private var verificationListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.emailInput)
        etPass = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.loginBtn)
        tvForgot = findViewById(R.id.forgotPassword)
        tvSignup = findViewById(R.id.dontHaveAccount)
        back = findViewById(R.id.back)

        val db = AppDatabase.getInstance(applicationContext)
        repo = AuthRepository(db.userDao(), RetrofitProvider.create())
        prefs = PrefsManager(applicationContext)

        requestNotificationPermissionIfNeeded()
        startVerificationStatusListenerForPendingEmail()

        back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPass.text?.toString().orEmpty()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check verification status first
            checkVerificationAndLogin(email, pass)
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }
    }

    /**
     * Listen to Firebase for the last "pending_email" saved on this device.
     * When banker approves it, show a notification: "verification done, please login".
     */
    private fun startVerificationStatusListenerForPendingEmail() {
        val sp = getSharedPreferences("signup_prefs", MODE_PRIVATE)
        val pendingEmail = sp.getString("pending_email", null) ?: return

        val ref = Firebase.database.getReference("verifications")
        verificationQuery = ref.orderByChild("email").equalTo(pendingEmail)

        verificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) return

                var status = "pending"
                for (child in snapshot.children) {
                    val st = child.child("status").getValue(String::class.java)
                    if (!st.isNullOrEmpty()) {
                        status = st
                        break
                    }
                }

                if (status == "approved") {
                    // Show notification that verification is done
                    NotificationHelper.showNotification(
                        this@Login,
                        "Verification Complete",
                        "Your verification is done. Please login now."
                    )

                    // Clear pending email so we don't notify again
                    sp.edit().remove("pending_email").apply()

                    // Stop listening
                    verificationListener?.let { l ->
                        verificationQuery?.removeEventListener(l)
                    }
                    verificationListener = null
                    verificationQuery = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // ignore
            }
        }

        verificationQuery?.addValueEventListener(verificationListener as ValueEventListener)
    }

    /**
     * Check Firebase for this email's verification status before logging in.
     */
    private fun checkVerificationAndLogin(email: String, pass: String) {
        val ref = Firebase.database.getReference("verifications")
        val query = ref.orderByChild("email").equalTo(email)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    // No record (maybe old user) -> normal login
                    performLogin(email, pass)
                    return
                }

                var status = "pending"
                for (child in snapshot.children) {
                    val st = child.child("status").getValue(String::class.java)
                    if (!st.isNullOrEmpty()) {
                        status = st
                        break
                    }
                }

                when (status) {
                    "approved" -> {
                        NotificationHelper.showNotification(
                            this@Login,
                            "Verification Approved",
                            "You are approved. Logging you in."
                        )
                        performLogin(email, pass)
                    }

                    "rejected" -> {
                        NotificationHelper.showNotification(
                            this@Login,
                            "Verification Rejected",
                            "Your signup was not approved by the banker."
                        )
                    }

                    else -> { // pending
                        NotificationHelper.showNotification(
                            this@Login,
                            "Verification Pending",
                            "Your account is still pending banker approval."
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Login,
                    "Could not check verification status.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun performLogin(email: String, pass: String) {
        scope.launch {
            val r = withContext(Dispatchers.IO) { repo.login(email, pass) }
            r.onSuccess { resp ->
                resp.user_id?.let { userId ->
                    prefs.setUserId(userId)
                }

                NotificationHelper.showNotification(
                    this@Login,
                    "Login Successful",
                    "Welcome ${resp.name ?: ""}"
                )

                startActivity(Intent(this@Login, dashboard::class.java))
                finish()
            }.onFailure { ex ->
                Toast.makeText(
                    this@Login,
                    ex.message ?: "Login failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()

        verificationListener?.let { l ->
            verificationQuery?.removeEventListener(l)
        }
        verificationListener = null
        verificationQuery = null
    }
}
