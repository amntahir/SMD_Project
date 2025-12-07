package com.example.smd_project

import com.squareup.moshi.Json
data class GetEligibilityResponseWrapper(
    val found: Boolean,
    val data: EligibilityResultData? = null
)
// Server wrapper used by one of your endpoints
data class EligibilityResultData(
    val checkId: String?,
    @Json(name = "user_id") val userId: String?,
    val monthlyIncome: Double?,
    val existingLoans: Double?,
    val loanAmount: Double?,
    val loanTenureMonths: Int?,
    val monthlyExpenses: Double?,
    val creditScore: Int?,
    val creditCardsCount: Int?,
    val age: Int?,
    val employmentType: String?,
    val workExperienceYears: Double?,
    val eligibilityStatus: String?,
    val eligibilityScore: Double?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Request DTO used when sending eligibility data to server.
 *
 * NOTE: Kotlin property names are camelCase. JSON field names (snake_case) are mapped via @Json.
 * This keeps Kotlin code clean and avoids unresolved reference errors.
 */
data class EligibilityRequest(
    @Json(name = "user_id") val userId: String,          // maps to user_id in DB/JSON
    val checkId: String? = null,

    @Json(name = "monthly_income") val monthlyIncome: Double? = null,
    @Json(name = "other_income") val otherIncome: Double? = null,
    @Json(name = "monthly_expenses") val monthlyExpenses: Double? = null,
    @Json(name = "rent_or_emi") val rentOrEmi: Double? = null,

    @Json(name = "has_existing_loans") val hasExistingLoans: Boolean? = null,
    @Json(name = "existing_loans_amount") val existingLoansAmount: Double? = null,

    @Json(name = "credit_score") val creditScore: Int? = null,
    @Json(name = "credit_cards_count") val creditCardsCount: Int? = null,

    val age: Int? = null,
    @Json(name = "employment_type") val employmentType: String? = null,
    @Json(name = "work_experience_years") val workExperienceYears: Double? = null,

    @Json(name = "loan_amount") val loanAmount: Double? = null,
    @Json(name = "loan_purpose") val loanPurpose: String? = null,
    @Json(name = "loan_tenure_months") val loanTenureMonths: Int? = null,

    @Json(name = "consent_for_credit_check") val consentForCreditCheck: Boolean? = null,
    val user_id: String
)

/**
 * What server returns after saving eligibility.
 * Keep fields that your repository/firestore expects.
 */
data class EligibilityResponse(
    val success: Boolean,
    val message: String?,
    val checkId: String? = null,
    val eligibilityScore: Double? = null,
    val eligibilityStatus: String? = null,
    val data: Map<String, Any>? = null
)

data class SaveEligibilityResponse(
    val success: Boolean,
    val message: String? = null,
    val id: String? = null
)
