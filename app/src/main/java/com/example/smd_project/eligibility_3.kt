package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class eligibility_3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eligibility3)

        val existingLoansDropdown = findViewById<AutoCompleteTextView>(R.id.existingLoansDropdown)
        val creditCardsDropdown = findViewById<AutoCompleteTextView>(R.id.creditCardsDropdown)
        val creditScoreDropdown = findViewById<AutoCompleteTextView>(R.id.creditScoreDropdown)

        existingLoansDropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listOf("Yes","No"))
        )
        creditCardsDropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listOf("None","1","2","3 or more"))
        )
        creditScoreDropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listOf("I know it (enter below)","I don't know","700","650","600","550"))
        )

        listOf(existingLoansDropdown, creditCardsDropdown, creditScoreDropdown).forEach { field ->
            field.threshold = 0
            field.isFocusable = true
            field.isClickable = true
            field.setOnClickListener { field.showDropDown() }
            field.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) field.showDropDown() }
        }

        val btnContinue = findViewById<Button>(R.id.btnContinue3)
        val btnPrevious = findViewById<Button>(R.id.btnPrevious3)

        btnPrevious.setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            val existingLoans = existingLoansDropdown.text?.toString()
            val cardsStr = creditCardsDropdown.text?.toString()
            val csStr = creditScoreDropdown.text?.toString()

            // Validate required fields: existingLoans, creditCards, creditScore (must be numeric or "I know it" + entered earlier)
            var ok = true
            if (existingLoans.isNullOrEmpty()) {
                existingLoansDropdown.error = "Please select Yes or No"
                ok = false
            }
            if (cardsStr.isNullOrEmpty()) {
                creditCardsDropdown.error = "Please select credit cards"
                ok = false
            }
            if (csStr.isNullOrEmpty()) {
                creditScoreDropdown.error = "Please select credit score option"
                ok = false
            }
            if (!ok) return@setOnClickListener

            // Save to session
            EligibilitySession.hasExistingLoans = when (existingLoans) {
                "Yes" -> true
                "No"  -> false
                else  -> null
            }

            EligibilitySession.creditCardsCount = when (cardsStr) {
                "None" -> 0
                "1"    -> 1
                "2"    -> 2
                "3 or more" -> 3
                else -> cardsStr?.toIntOrNull()
            }

            // If user selected a numeric score in dropdown use it; if "I know it" we expect they entered it earlier (step1) - ensure creditScore is present
            val numeric = csStr?.filter { it.isDigit() } ?: ""
            if (numeric.isNotEmpty()) {
                EligibilitySession.creditScore = numeric.toIntOrNull()
            }

            // final check: creditScore must be present and numeric at this point
            if (EligibilitySession.creditScore == null) {
                Toast.makeText(this, "Please provide a numeric credit score", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, eligibility_4::class.java))
            finish()
        }

        val content: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
