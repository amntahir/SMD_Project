package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // your splash layout

        // Splash for 5 seconds, then go to Choose Login
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@MainActivity, ChooseLoginActivity::class.java))
            finish() // prevent going back to splash
        }, 5000)
    }
}
