package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.smd_project.Login

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Splash for 5 seconds, then go to Login
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@MainActivity, Login::class.java))
            finish() // prevent going back to splash
        }, 5000)
    }
}
