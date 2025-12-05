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

class eligibility_2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eligibility2)

        val monthlyIncomeDropdown = findViewById<AutoCompleteTextView>(R.id.monthlyIncomeDropdown)
        val otherIncomeDropdown = findViewById<AutoCompleteTextView>(R.id.otherIncomeDropdown)
        val monthlyExpensesDropdown = findViewById<AutoCompleteTextView>(R.id.monthlyExpensesDropdown)
        val rentEmiDropdown = findViewById<AutoCompleteTextView>(R.id.rentEmiDropdown)

        val incomeOptions = listOf("10,000","20,000","30,000","40,000","50,000","75,000","100,000","200,000")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, incomeOptions)

        monthlyIncomeDropdown.setAdapter(adapter)
        monthlyIncomeDropdown.threshold = 1
        otherIncomeDropdown.setAdapter(adapter)
        monthlyExpensesDropdown.setAdapter(adapter)
        rentEmiDropdown.setAdapter(adapter)

        val btnContinue = findViewById<Button>(R.id.btnContinue2)
        val btnPrevious = findViewById<Button>(R.id.btnPrevious2)

        btnPrevious.setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            // Validate required fields: monthlyIncome, monthlyExpenses, rent/EMI
            val mi = monthlyIncomeDropdown.text?.toString()?.trim()
            val me = monthlyExpensesDropdown.text?.toString()?.trim()
            val rent = rentEmiDropdown.text?.toString()?.trim()

            var ok = true
            if (mi.isNullOrEmpty()) {
                monthlyIncomeDropdown.error = "Monthly income is required"
                ok = false
            }
            if (me.isNullOrEmpty()) {
                monthlyExpensesDropdown.error = "Monthly expenses required"
                ok = false
            }
            if (rent.isNullOrEmpty()) {
                rentEmiDropdown.error = "Rent / EMI required"
                ok = false
            }
            if (!ok) return@setOnClickListener

            // Save to session; otherIncome optional but will be stored if provided
            EligibilitySession.monthlyIncome = mi?.replace(",","")?.toDoubleOrNull() ?: EligibilitySession.monthlyIncome
            EligibilitySession.otherIncome = otherIncomeDropdown.text?.toString()?.replace(",","")?.toDoubleOrNull()
            EligibilitySession.monthlyExpenses = me?.replace(",","")?.toDoubleOrNull() ?: EligibilitySession.monthlyExpenses
            EligibilitySession.rentOrEmi = rent?.replace(",","")?.toDoubleOrNull()

            // Final numeric validation
            if (EligibilitySession.monthlyIncome == null || EligibilitySession.monthlyExpenses == null) {
                Toast.makeText(this, "Please enter valid numeric income/expenses", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, eligibility_3::class.java))
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
