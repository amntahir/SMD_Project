package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smd_project.doc_upload
import com.google.android.material.card.MaterialCardView

class dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Document Simplification
        val cardDocs = findViewById<MaterialCardView>(R.id.cardDocs)
        cardDocs.setOnClickListener {
            startActivity(Intent(this, doc_upload::class.java))
        }



        // Eligibility Check
        val cardEligibility = findViewById<MaterialCardView>(R.id.cardEligibility)
        cardEligibility.setOnClickListener {
            startActivity(Intent(this, eligibility_1::class.java))
        }
    }
}
