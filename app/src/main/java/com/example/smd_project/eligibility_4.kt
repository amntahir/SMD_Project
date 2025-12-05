package com.example.smd_project

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class eligibility_4 : AppCompatActivity() {

    private lateinit var repo: EligibilityRepository

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eligibility4)

        val api = RetrofitProvider.createEligibilityService()
        repo = EligibilityRepository(api)

        val loanAmountDropdown = findViewById<AutoCompleteTextView>(R.id.spLoanAmount)
        val loanPurposeDropdown = findViewById<AutoCompleteTextView>(R.id.spLoanPurpose)
        val loanTenureDropdown = findViewById<AutoCompleteTextView>(R.id.spLoanTenure)
        val checkboxConsent = findViewById<CheckBox>(R.id.checkboxConsent)
        val btnContinue = findViewById<Button>(R.id.btnContinue4)
        val btnPrevious = findViewById<Button>(R.id.btnPrevious4)
        val btnBack = findViewById<View?>(R.id.btnBackEligibility4)

        val loanAmounts = listOf("50,000","100,000","200,000","500,000","1,000,000")
        val loanPurposes = listOf("Education","Home Renovation","Business","Travel","Medical")
        val loanTenures = listOf("6","12","24","36","48") // months

        loanAmountDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, loanAmounts))
        loanPurposeDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, loanPurposes))
        loanTenureDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, loanTenures))

        listOf(loanAmountDropdown, loanPurposeDropdown, loanTenureDropdown).forEach { field ->
            field.threshold = 0
            field.isFocusable = true
            field.isClickable = true
            field.setOnClickListener { field.showDropDown() }
            field.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) field.showDropDown() }
        }

        btnPrevious.setOnClickListener { finish() }
        btnBack?.setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            // Validation: all fields mandatory on page 4
            val loanAmtStr = loanAmountDropdown.text?.toString()?.trim()
            val loanPurposeStr = loanPurposeDropdown.text?.toString()?.trim()
            val loanTenureStr = loanTenureDropdown.text?.toString()?.trim()
            val consent = checkboxConsent.isChecked

            var ok = true
            if (loanAmtStr.isNullOrEmpty()) {
                loanAmountDropdown.error = "Please select desired loan amount"
                ok = false
            }
            if (loanPurposeStr.isNullOrEmpty()) {
                loanPurposeDropdown.error = "Please select loan purpose"
                ok = false
            }
            if (loanTenureStr.isNullOrEmpty()) {
                loanTenureDropdown.error = "Please select tenure"
                ok = false
            }
            if (!consent) {
                Toast.makeText(this, "Consent for credit check is required", Toast.LENGTH_SHORT).show()
                ok = false
            }
            if (!ok) return@setOnClickListener

            // store session values safely
            EligibilitySession.loanAmount = loanAmtStr?.replace(",","")?.toDoubleOrNull()
            EligibilitySession.loanPurpose = loanPurposeStr
            EligibilitySession.loanTenureMonths = loanTenureStr?.toIntOrNull()
            EligibilitySession.consentForCreditCheck = consent

            // basic numeric checks
            if (EligibilitySession.loanAmount == null || EligibilitySession.loanTenureMonths == null) {
                Toast.makeText(this, "Please enter valid loan amount and tenure", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // prefs and checkId generation (kept)
            val prefs = PrefsManager(this)
            if (EligibilitySession.userId.isNullOrEmpty()) {
                EligibilitySession.userId = prefs.getUserId()
            }
            if (EligibilitySession.checkId.isNullOrEmpty()) {
                EligibilitySession.checkId = "check-" + UUID.randomUUID().toString()
            }

            val req = EligibilityRequest(
                user_id = EligibilitySession.userId ?: "anonymous",
                monthlyIncome = anyToDoubleOrNull(EligibilitySession.monthlyIncome) ?: 0.0,
                otherIncome = anyToDoubleOrNull(EligibilitySession.otherIncome),
                monthlyExpenses = anyToDoubleOrNull(EligibilitySession.monthlyExpenses) ?: 0.0,
                rentOrEmi = anyToDoubleOrNull(EligibilitySession.rentOrEmi),
                hasExistingLoans = EligibilitySession.hasExistingLoans,
                existingLoansAmount = anyToDoubleOrNull(EligibilitySession.existingLoansAmount),
                creditScore = EligibilitySession.creditScore,
                creditCardsCount = EligibilitySession.creditCardsCount ?: 0,
                age = EligibilitySession.age,
                employmentType = EligibilitySession.employmentType,
                workExperienceYears = anyToDoubleOrNull(EligibilitySession.workExperienceYears),
                loanAmount = anyToDoubleOrNull(EligibilitySession.loanAmount) ?: 0.0,
                loanPurpose = EligibilitySession.loanPurpose,
                loanTenureMonths = EligibilitySession.loanTenureMonths ?: 0,
                consentForCreditCheck = EligibilitySession.consentForCreditCheck,
                userId = EligibilitySession.userId ?: "anonymous",
                checkId = EligibilitySession.checkId ?: ""
            )

            lifecycleScope.launch {
                var serverResp: EligibilityResponse? = null
                try {
                    val resp = withContext(Dispatchers.IO) { repo.submitEligibility(req) }
                    if (resp.isSuccessful) serverResp = resp.body()
                } catch (ex: Exception) {
                    // network error - fallback to local
                }

                val finalScore = serverResp?.eligibilityScore ?: computeLocalEligibilityScore()
                val finalStatus = serverResp?.eligibilityStatus ?: deriveStatusFromScore(finalScore)
                EligibilitySession.lastEligibilityScore = finalScore
                EligibilitySession.lastEligibilityStatus = finalStatus

                try {
                    // persist to Firestore & Realtime DB via repo
                    repo.mirrorAll(req, serverResp)
                } catch (ex: Exception) {
                    Toast.makeText(this@eligibility_4, "Firebase sync partially failed: ${ex.message}", Toast.LENGTH_LONG).show()
                }

                // persist results for results screen
                val resultsPrefs = getSharedPreferences("elig_results", MODE_PRIVATE)
                resultsPrefs.edit()
                    .putFloat("lastScore", (finalScore ?: 0.0).toFloat())
                    .putString("lastStatus", finalStatus)
                    .putString("lastCheckId", EligibilitySession.checkId)
                    .apply()

                startActivity(Intent(this@eligibility_4, eligibility_results::class.java))
                finish()
            }
        }

        val content: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun computeLocalEligibilityScore(): Double {
        val income = EligibilitySession.monthlyIncome ?: 0.0
        val expenses = EligibilitySession.monthlyExpenses ?: 0.0
        val loans = EligibilitySession.existingLoansAmount ?: 0.0
        val credit = (EligibilitySession.creditScore ?: 600).coerceIn(300,850)
        val base = (income - expenses - (loans / 12.0)) / 1000.0
        val creditFactor = (credit - 300) / 550.0 * 50.0
        return (base + creditFactor).coerceIn(0.0, 100.0)
    }

    private fun deriveStatusFromScore(score: Double?): String {
        if (score == null) return "PENDING"
        return when {
            score >= 55.0 -> "ELIGIBLE"
            score >= 35.0 -> "PENDING"
            else -> "NOT_ELIGIBLE"
        }
    }

    private fun anyToDoubleOrNull(value: Any?): Double? {
        return when (value) {
            null -> null
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }
}
