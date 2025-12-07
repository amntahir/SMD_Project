package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ChooseLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_login)

        val btnUser = findViewById<Button>(R.id.btnLoginUser)
        val btnBanker = findViewById<Button>(R.id.btnLoginBanker)

        // existing user login screen
        btnUser.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // banker login screen (Login2)
        btnBanker.setOnClickListener {
            val intent = Intent(this, Login2Activity::class.java)
            startActivity(intent)
        }
    }
}
