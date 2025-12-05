package com.example.smd_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class eligibility_1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eligibility1)

        val inputIncome = findViewById<EditText>(R.id.inputIncome)
        val inputExpenses = findViewById<EditText>(R.id.inputExpenses)
        val inputCreditScore = findViewById<EditText>(R.id.inputCreditScore)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val spAge = findViewById<Spinner>(R.id.spAge)
        val spEmployment = findViewById<Spinner>(R.id.spEmployment)
        val spExperience = findViewById<Spinner>(R.id.spExperience)

        // Age 18..70
        spAge.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (18..70).map { it.toString() })
        spEmployment.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Salaried", "Self-employed", "Unemployed", "Student"))
        spExperience.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("0-1", "1-3", "3-5", "5+"))

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            // Validation: all fields mandatory
            val selectedAge = spAge.selectedItem?.toString()?.toIntOrNull()
            val selectedEmployment = spEmployment.selectedItem?.toString()
            val selectedExperience = spExperience.selectedItem?.toString()

            val incomeStr = inputIncome.text?.toString()?.trim()
            val expensesStr = inputExpenses.text?.toString()?.trim()
            val creditScoreStr = inputCreditScore.text?.toString()?.trim()

            var ok = true

            if (selectedAge == null) {
                Toast.makeText(this, "Please select your age", Toast.LENGTH_SHORT).show()
                ok = false
            }
            if (selectedEmployment.isNullOrEmpty()) {
                Toast.makeText(this, "Please select employment type", Toast.LENGTH_SHORT).show()
                ok = false
            }
            if (selectedExperience.isNullOrEmpty()) {
                Toast.makeText(this, "Please select work experience", Toast.LENGTH_SHORT).show()
                ok = false
            }

            if (incomeStr.isNullOrEmpty()) {
                inputIncome.error = "Monthly income is required"
                ok = false
            }
            if (expensesStr.isNullOrEmpty()) {
                inputExpenses.error = "Monthly expenses are required"
                ok = false
            }
            if (creditScoreStr.isNullOrEmpty()) {
                inputCreditScore.error = "Credit score is required"
                ok = false
            }

            if (!ok) return@setOnClickListener

            // Save to session (safe parsing)
            EligibilitySession.age = selectedAge
            EligibilitySession.employmentType = selectedEmployment
            EligibilitySession.workExperienceYears = when (selectedExperience) {
                "0-1" -> 0
                "1-3" -> 1
                "3-5" -> 3
                "5+"  -> 5
                else -> null
            }
            EligibilitySession.monthlyIncome = incomeStr?.replace(",","")?.toDoubleOrNull()
            EligibilitySession.monthlyExpenses = expensesStr?.replace(",","")?.toDoubleOrNull()
            EligibilitySession.creditScore = creditScoreStr?.toIntOrNull()

            // final safety check
            if (EligibilitySession.monthlyIncome == null || EligibilitySession.monthlyExpenses == null || EligibilitySession.creditScore == null) {
                Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, eligibility_2::class.java))
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
