package com.example.smd_project

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class eligibility_results : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val tvApprovedCount = findViewById<TextView>(R.id.tvApprovedCount)
        val tvConditionalCount = findViewById<TextView>(R.id.tvConditionalCount)
        val tvNotEligibleCount = findViewById<TextView>(R.id.tvNotEligibleCount)

        // HBL elements
        val cardHbl = findViewById<CardView>(R.id.cardOffer1)
        val btnApplyHbl = findViewById<View>(R.id.btnApplyNow1)

        // MCB elements
        val cardMcb = findViewById<CardView>(R.id.cardOffer2)
        val btnApplyMcb = findViewById<View>(R.id.btnApplyNow2)

        // session extraction (safe)
        val monthlyIncome = EligibilitySession.monthlyIncome ?: 0.0
        val otherIncome = EligibilitySession.otherIncome ?: 0.0
        val totalIncome = monthlyIncome + otherIncome
        val monthlyExpenses = EligibilitySession.monthlyExpenses ?: 0.0
        val existingLoansAmount = EligibilitySession.existingLoansAmount ?: 0.0
        val creditScore = EligibilitySession.creditScore ?: 0
        val age = EligibilitySession.age ?: 18
        val loanRequested = EligibilitySession.loanAmount ?: 500000.0
        val tenureMonths = (EligibilitySession.loanTenureMonths ?: 60).coerceAtLeast(6)

        // thresholds
        val hblMinIncome = 35000.0
        val hblMinCreditScore = 650
        val hblMaxExistingLoans = 150000.0
        val hblMaxDti = 0.45

        val mcbMinIncome = 50000.0
        val mcbMinCreditScore = 600
        val mcbMaxExistingLoans = 200000.0
        val minAge = 21

        val dti: Double = if (totalIncome > 0.0) {
            (monthlyExpenses + (existingLoansAmount / 12.0)) / totalIncome
        } else 1.0

        val eligibleHBL = (totalIncome >= hblMinIncome)
                && (creditScore >= hblMinCreditScore)
                && (existingLoansAmount <= hblMaxExistingLoans)
                && (age >= minAge)
                && (dti <= hblMaxDti)

        val eligibleMCB = (totalIncome >= mcbMinIncome)
                && (creditScore >= mcbMinCreditScore)
                && (existingLoansAmount <= mcbMaxExistingLoans)
                && (age >= minAge)

        fun classify(eligible: Boolean, score: Int, minScore: Int): String {
            return if (eligible) "APPROVED"
            else if (score >= minScore - 30) "CONDITIONAL"
            else "NOT_ELIGIBLE"
        }

        val hblStatus = classify(eligibleHBL, creditScore, hblMinCreditScore)
        val mcbStatus = classify(eligibleMCB, creditScore, mcbMinCreditScore)

        var approvedCount = 0
        var conditionalCount = 0
        var notEligibleCount = 0

        listOf(hblStatus, mcbStatus).forEach { s ->
            when (s) {
                "APPROVED" -> approvedCount++
                "CONDITIONAL" -> conditionalCount++
                else -> notEligibleCount++
            }
        }

        val subtitle = when {
            approvedCount >= 2 -> "Good news — several offers match your profile"
            approvedCount == 1 -> "One product looks like a good match"
            conditionalCount > 0 -> "You have conditional offers — a few improvements may help"
            else -> "Sorry — no matching offers right now"
        }

        tvApprovedCount.text = approvedCount.toString()
        tvConditionalCount.text = conditionalCount.toString()
        tvNotEligibleCount.text = notEligibleCount.toString()
        tvSubtitle.text = subtitle

        // helper to set colors & enable/disable apply
        fun applyState(card: CardView?, button: View?, status: String, bankName: String) {
            if (card == null || button == null) return
            when (status) {
                "APPROVED" -> {
                    card.setCardBackgroundColor(Color.parseColor("#E7F6EF")) // green tint
                    button.isEnabled = true
                    button.alpha = 1.0f
                    button.setOnClickListener { Toast.makeText(this, "Applied for $bankName", Toast.LENGTH_SHORT).show() }
                }
                "CONDITIONAL" -> {
                    card.setCardBackgroundColor(Color.parseColor("#FFF6EB")) // yellow tint
                    button.isEnabled = false
                    button.alpha = 0.6f
                    button.setOnClickListener { Toast.makeText(this, "Offer conditional for $bankName", Toast.LENGTH_SHORT).show() }
                }
                else -> {
                    card.setCardBackgroundColor(Color.parseColor("#FFF0F0")) // red tint
                    button.isEnabled = false
                    button.alpha = 0.5f
                    button.setOnClickListener { Toast.makeText(this, "Not eligible for $bankName", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        applyState(cardHbl, btnApplyHbl, hblStatus, "HBL")
        applyState(cardMcb, btnApplyMcb, mcbStatus, "MCB")

        val content: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
