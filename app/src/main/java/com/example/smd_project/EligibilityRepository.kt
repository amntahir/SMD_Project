package com.example.smd_project

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.util.UUID

class EligibilityRepository(private val api: EligibilityApiService) {
    private val TAG = "EligibilityRepo"

    // submitEligibility returns Response<EligibilityResponse> (keeps HTTP status info)
    suspend fun submitEligibility(req: EligibilityRequest): Response<EligibilityResponse> {
        return api.saveEligibility(req)
    }

    // Upload supporting doc: callers pass a String userId and a MultipartBody.Part file
    suspend fun uploadSupportingDoc(userId: String, documentPart: MultipartBody.Part): Response<SaveEligibilityResponse> {
        val userIdReqBody: RequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadSupportingDocument(userIdReqBody, documentPart)
    }

    /**
     * Mirror eligibility to Firestore (suspending, uses await()).
     * Keeps the same mapping you already had.
     */
    suspend fun mirrorToFirestore(req: EligibilityRequest, serverResp: EligibilityResponse?) {
        withContext(Dispatchers.IO) {
            try {
                val fs = FirebaseFirestore.getInstance()

                // Choose doc ID (server checkId > request.checkId > generated)
                val docId = serverResp?.checkId ?: req.checkId ?: UUID.randomUUID().toString()

                val map = hashMapOf<String, Any?>(
                    "checkId" to docId,
                    "userId" to req.userId,
                    "monthlyIncome" to req.monthlyIncome,
                    "otherIncome" to req.otherIncome,
                    "monthlyExpenses" to req.monthlyExpenses,
                    "rentOrEmi" to req.rentOrEmi,
                    "hasExistingLoans" to req.hasExistingLoans,
                    "existingLoansAmount" to req.existingLoansAmount,
                    "creditScore" to req.creditScore,
                    "creditCardsCount" to req.creditCardsCount,
                    "age" to req.age,
                    "employmentType" to req.employmentType,
                    "workExperienceYears" to req.workExperienceYears,
                    "loanAmount" to req.loanAmount,
                    "loanPurpose" to req.loanPurpose,
                    "loanTenureMonths" to req.loanTenureMonths,
                    "consentForCreditCheck" to req.consentForCreditCheck,
                    "serverSaved" to (serverResp?.success == true),
                    "serverMessage" to serverResp?.message,
                    "eligibilityScore" to serverResp?.eligibilityScore,
                    "eligibilityStatus" to serverResp?.eligibilityStatus,
                    "createdAt" to Timestamp.now()
                )

                fs.collection("eligibility").document(docId).set(map).await()
                Log.d(TAG, "mirrorToFirestore: wrote eligibility $docId")
            } catch (ex: Exception) {
                Log.w(TAG, "mirrorToFirestore failed: ${ex.message}", ex)
                throw ex
            }
        }
    }

    /**
     * Mirror eligibility to Realtime Database (suspending).
     * Writes to node: realtime_eligibility/{checkId}
     * NOTE: This writes the same safe metadata that Firestore receives (no passwords).
     */
    suspend fun mirrorToRealtime(req: EligibilityRequest, serverResp: EligibilityResponse?) {
        withContext(Dispatchers.IO) {
            try {
                val dbUrl: String? = null
                val dbRefRoot = if (dbUrl.isNullOrEmpty()) {
                    FirebaseDatabase.getInstance()
                } else {
                    FirebaseDatabase.getInstance(dbUrl)
                }

                val docId = serverResp?.checkId ?: req.checkId ?: UUID.randomUUID().toString()
                val map = hashMapOf<String, Any?>(
                    "checkId" to docId,
                    "userId" to req.userId,
                    "monthlyIncome" to req.monthlyIncome,
                    "otherIncome" to req.otherIncome,
                    "monthlyExpenses" to req.monthlyExpenses,
                    "rentOrEmi" to req.rentOrEmi,
                    "hasExistingLoans" to req.hasExistingLoans,
                    "existingLoansAmount" to req.existingLoansAmount,
                    "creditScore" to req.creditScore,
                    "creditCardsCount" to req.creditCardsCount,
                    "age" to req.age,
                    "employmentType" to req.employmentType,
                    "workExperienceYears" to req.workExperienceYears,
                    "loanAmount" to req.loanAmount,
                    "loanPurpose" to req.loanPurpose,
                    "loanTenureMonths" to req.loanTenureMonths,
                    "consentForCreditCheck" to req.consentForCreditCheck,
                    "serverSaved" to (serverResp?.success == true),
                    "serverMessage" to serverResp?.message,
                    "eligibilityScore" to serverResp?.eligibilityScore,
                    "eligibilityStatus" to serverResp?.eligibilityStatus,
                    "createdAt" to System.currentTimeMillis() // use epoch ms for Realtime DB
                )

                val ref = dbRefRoot.getReference("realtime_eligibility").child(docId)
                ref.setValue(map).await()
                Log.d(TAG, "mirrorToRealtime: wrote eligibility $docId")
            } catch (ex: Exception) {
                Log.w(TAG, "mirrorToRealtime failed: ${ex.message}", ex)
                throw ex
            }
        }
    }

    /**
     * Convenience: mirror to both Firestore and Realtime.
     * Caller may catch exceptions and handle fallbacks.
     */
    suspend fun mirrorAll(req: EligibilityRequest, serverResp: EligibilityResponse?) {
        // Firestore first (you already used this). If Firestore fails, still attempt Realtime (or rethrow; here we rethrow to let caller handle)
        try {
            mirrorToFirestore(req, serverResp)
        } catch (ex: Exception) {
            Log.w(TAG, "mirrorAll: Firestore mirror failed: ${ex.message}", ex)
            // continue to try RTDB (do not return early)
        }

        try {
            mirrorToRealtime(req, serverResp)
        } catch (ex: Exception) {
            Log.w(TAG, "mirrorAll: Realtime mirror failed: ${ex.message}", ex)
            // we throw here so caller knows at least one mirror failed (optional)
            throw ex
        }
    }
}
