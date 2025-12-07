package com.example.smd_project

object EligibilitySession {
    // basic identity
    var userId: String? = null
    var checkId: String? = null

    // step 1
    var age: Int? = null
    var employmentType: String? = null
    var workExperienceYears: Int? = null
    var monthlyIncome: Double? = null
    var monthlyExpenses: Double? = null
    var creditScore: Int? = null

    // step 2
    var otherIncome: Double? = null
    var rentOrEmi: Double? = null

    // step 3
    var hasExistingLoans: Boolean? = null
    var existingLoansAmount: Double? = null
    var creditCardsCount: Int? = null
    var knowsCreditScore: Boolean? = null

    // step 4
    var loanAmount: Double? = null
    var loanPurpose: String? = null
    var loanTenureMonths: Int? = null
    var consentForCreditCheck: Boolean = false

    // results
    var lastEligibilityScore: Double? = null
    var lastEligibilityStatus: String? = null
}
